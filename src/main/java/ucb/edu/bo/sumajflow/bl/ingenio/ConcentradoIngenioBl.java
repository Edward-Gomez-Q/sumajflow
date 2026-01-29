package ucb.edu.bo.sumajflow.bl.ingenio;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ucb.edu.bo.sumajflow.bl.AuditoriaBl;
import ucb.edu.bo.sumajflow.bl.NotificacionBl;
import ucb.edu.bo.sumajflow.dto.ingenio.*;
import ucb.edu.bo.sumajflow.dto.socio.MineralInfoDto;
import ucb.edu.bo.sumajflow.entity.*;
import ucb.edu.bo.sumajflow.repository.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Servicio de Concentrados para el ROL INGENIO
 * Responsabilidades:
 * - Crear concentrado a partir de lotes aprobados
 * - Gestionar Kanban de procesos
 * - Registrar y validar reportes químicos
 * - Gestionar liquidación de servicio (aprobar, registrar pago)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ConcentradoIngenioBl {

    // Repositorios
    private final ConcentradoRepository concentradoRepository;
    private final LotesRepository lotesRepository;
    private final LoteConcentradoRelacionRepository loteConcentradoRelacionRepository;
    private final IngenioMineroRepository ingenioMineroRepository;
    private final UsuariosRepository usuariosRepository;
    private final PersonaRepository personaRepository;
    private final LoteMineralesRepository loteMineralesRepository;
    private final ProcesosPlantaRepository procesosPlantaRepository;
    private final LoteProcesoPlantaRepository loteProcesoPlantaRepository;
    private final PlantaRepository plantaRepository;
    private final ReporteQuimicoRepository reporteQuimicoRepository;
    private final LiquidacionRepository liquidacionRepository;
    private final LiquidacionConcentradoRepository liquidacionConcentradoRepository;

    // Servicios
    private final NotificacionBl notificacionBl;
    private final AuditoriaBl auditoriaBl;
    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper;
    private final ConcentradoMineralAnalyzer mineralAnalyzer;


    // Constantes de estados
    private static final String ESTADO_CREADO = "creado";
    private static final String ESTADO_EN_CAMINO_PLANTA = "en_camino_a_planta";
    private static final String ESTADO_LOTE_TRANSPORTE_COMPLETO = "Transporte completo";

    // ==================== LISTAR CONCENTRADOS DEL INGENIO ====================

    /**
     * Listar todos los concentrados del ingenio con filtros opcionales y paginación
     */
    @Transactional(readOnly = true)
    public Page<ConcentradoResponseDto> listarConcentrados(
            Integer usuarioId,
            String estado,
            String mineralPrincipal,
            LocalDateTime fechaDesde,
            LocalDateTime fechaHasta,
            int page,
            int size
    ) {
        log.debug("Listando concentrados del ingenio - Usuario ID: {}, Página: {}, Tamaño: {}",
                usuarioId, page, size);

        IngenioMinero ingenio = obtenerIngenioDelUsuario(usuarioId);

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        Page<Concentrado> concentrados = concentradoRepository.findConcentradosConFiltros(
                ingenio,
                estado,
                mineralPrincipal,
                fechaDesde,
                fechaHasta,
                pageable
        );

        return concentrados.map(this::convertirAResponseDto);
    }


    /**
     * Obtener detalle de un concentrado específico
     */
    @Transactional(readOnly = true)
    public ConcentradoResponseDto obtenerDetalle(Integer concentradoId, Integer usuarioId) {
        log.debug("Obteniendo detalle del concentrado ID: {}", concentradoId);

        IngenioMinero ingenio = obtenerIngenioDelUsuario(usuarioId);
        Concentrado concentrado = obtenerConcentradoConPermisos(concentradoId, ingenio);

        return convertirAResponseDto(concentrado);
    }

    /**
     * Obtener estadísticas del dashboard del ingenio
     */
    @Transactional(readOnly = true)
    public Map<String, Object> obtenerDashboard(Integer usuarioId) {
        log.debug("Obteniendo dashboard del ingenio - Usuario ID: {}", usuarioId);

        IngenioMinero ingenio = obtenerIngenioDelUsuario(usuarioId);

        List<Concentrado> todos = concentradoRepository.findByIngenioMineroIdOrderByCreatedAtDesc(ingenio);

        Map<String, Object> dashboard = new HashMap<>();
        dashboard.put("total", todos.size());
        dashboard.put("enProceso", todos.stream().filter(c -> "en_proceso".equals(c.getEstado())).count());
        dashboard.put("esperandoReporte", todos.stream().filter(c -> "esperando_reporte_quimico".equals(c.getEstado())).count());
        dashboard.put("listoLiquidacion", todos.stream().filter(c -> "listo_para_liquidacion".equals(c.getEstado())).count());
        dashboard.put("vendidos", todos.stream().filter(c -> "vendido".equals(c.getEstado())).count());

        BigDecimal pesoTotal = todos.stream()
                .map(Concentrado::getPesoInicial)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        dashboard.put("pesoTotalProcesado", pesoTotal);

        return dashboard;
    }

    // ==================== CREAR CONCENTRADO ====================

    /**
     * Crear concentrado(s) a partir de lotes aprobados
     * ACTUALIZADO: Crea múltiples concentrados si el lote tiene Zn+Pb
     *
     * @return Lista de concentrados creados (puede ser 1 o 2)
     */
    @Transactional
    public List<ConcentradoResponseDto> crearConcentrado(
            ConcentradoCreateDto createDto,
            Integer usuarioId,
            String ipOrigen
    ) {
        log.info("Creando concentrado(s) - Usuario ID: {}", usuarioId);

        // 1. Validar ingenio
        IngenioMinero ingenio = obtenerIngenioDelUsuario(usuarioId);

        // 2. Validar y obtener lotes
        List<Lotes> lotes = validarYObtenerLotes(createDto.getLotesIds(), ingenio);

        validarPesoMinimoPlanta(lotes, ingenio);

        // 3. Determinar socio propietario
        Socio socioPropietario = lotes.getFirst().getMinasId().getSocioId();

        // 4. Obtener minerales del primer lote (asumimos que todos los lotes tienen los mismos minerales)
        Lotes primerLote = lotes.getFirst();
        List<LoteMinerales> loteMinerales = loteMineralesRepository.findByLotesId(primerLote);

        if (loteMinerales.isEmpty()) {
            throw new IllegalArgumentException("El lote no tiene minerales asociados");
        }

        // 5. Determinar qué concentrados crear según minerales
        ConcentradoMineralAnalyzer.ConcentradosPlanificados planificacion =
                mineralAnalyzer.determinarConcentrados(loteMinerales);

        log.info("Se crearán {} concentrado(s) para el lote", planificacion.getConcentrados().size());

        // 6. Crear cada concentrado planificado
        List<Concentrado> concentradosCreados = new ArrayList<>();

        for (int i = 0; i < planificacion.getConcentrados().size(); i++) {
            ConcentradoMineralAnalyzer.ConcentradoPlanificado planificado =
                    planificacion.getConcentrados().get(i);

            Concentrado concentrado = crearConcentradoIndividual(
                    createDto,
                    lotes,
                    ingenio,
                    socioPropietario,
                    planificado,
                    planificacion.getEsMultiple(),
                    i + 1,
                    usuarioId,
                    ipOrigen
            );

            concentradosCreados.add(concentrado);
        }
        // Registrar el estado de los lotes de Transporte completo a En planta
        lotes.forEach(lote -> {
            lote.setEstado("En planta");
            lotesRepository.save(lote);
        });


        // 7. Auditoría y notificación
        registrarAuditoriaCreacionMultiple(concentradosCreados, lotes, usuarioId, ipOrigen);
        notificarCreacionMultiple(concentradosCreados, ingenio, socioPropietario);

        // 8. Convertir a DTOs
        List<ConcentradoResponseDto> response = concentradosCreados.stream()
                .map(this::convertirAResponseDto)
                .collect(Collectors.toList());

        log.info("Concentrado(s) creado(s) exitosamente - Total: {}", concentradosCreados.size());

        return response;
    }

    /**
     * Crea un concentrado individual según la planificación
     */
    private Concentrado crearConcentradoIndividual(
            ConcentradoCreateDto createDto,
            List<Lotes> lotes,
            IngenioMinero ingenio,
            Socio socioPropietario,
            ConcentradoMineralAnalyzer.ConcentradoPlanificado planificado,
            Boolean esMultiple,
            int sufijo,
            Integer usuarioId,
            String ipOrigen
    ) {
        // 1. Generar código único
        String codigoConcentrado = generarCodigoConcentrado(ingenio, planificado.getMineralPrincipal(), sufijo);

        // 2. Calcular peso según porcentaje
        BigDecimal pesoConcentrado = createDto.getPesoInicial()
                .multiply(BigDecimal.valueOf(planificado.getPorcentajePeso()))
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

        // 3. Construir minerales secundarios
        String mineralesSecundarios = mineralAnalyzer.construirMineralesSecundarios(planificado);

        // 4. Crear observaciones iniciales
        List<Map<String, Object>> historial = new ArrayList<>();
        Map<String, Object> registroInicial = crearRegistroHistorial(
                ESTADO_CREADO,
                "Concentrado creado - Mineral principal: " + planificado.getMineralPrincipal(),
                createDto.getObservacionesIniciales(),
                usuarioId,
                ipOrigen
        );

        // Agregar info de revisión si es necesario
        if (Boolean.TRUE.equals(planificado.getRequiereRevision())) {
            registroInicial.put("requiere_revision", true);
            registroInicial.put("motivo_revision", planificado.getObservacionRevision());
        }

        historial.add(registroInicial);

        // 5. Crear entidad Concentrado
        Concentrado concentrado = Concentrado.builder()
                .codigoConcentrado(codigoConcentrado)
                .ingenioMineroId(ingenio)
                .socioPropietarioId(socioPropietario)
                .pesoInicial(pesoConcentrado)
                .mineralPrincipal(planificado.getMineralPrincipal())
                .mineralesSecundarios(mineralesSecundarios)
                .loteOrigenMultiple(esMultiple)
                .estado(ESTADO_CREADO)
                .observaciones(convertirHistorialAJson(historial))
                .fechaInicio(LocalDateTime.now())
                .build();

        concentrado = concentradoRepository.save(concentrado);

        // 6. Crear relaciones con lotes
        crearRelacionesConLotes(concentrado, lotes, planificado.getPorcentajePeso());

        // 7. Inicializar Kanban de procesos
        inicializarProcesosPlanta(concentrado, ingenio);

        // 8. Transicionar a "en_camino_a_planta"
        transicionarEstado(
                concentrado,
                ESTADO_EN_CAMINO_PLANTA,
                "Concentrado de " + planificado.getMineralPrincipal() + " en camino a planta",
                null,
                usuarioId,
                ipOrigen
        );

        // 9. Publicar evento WebSocket
        publicarEventoWebSocket(concentrado, "concentrado_creado");

        return concentrado;
    }


    /**
     * Validar que el peso total de los lotes cumple con el cupo mínimo de la planta
     */
    private void validarPesoMinimoPlanta(List<Lotes> lotes, IngenioMinero ingenio) {
        // 1. Obtener la planta del ingenio
        Planta planta = plantaRepository.findByIngenioMineroId(ingenio)
                .orElseThrow(() -> new IllegalArgumentException("El ingenio no tiene planta configurada"));

        // 2. Calcular peso total de los lotes
        BigDecimal pesoTotal = calcularPesoTotal(lotes);

        // 3. Validar contra cupo mínimo
        if (pesoTotal.compareTo(planta.getCupoMinimo()) < 0) {
            throw new IllegalArgumentException(
                    String.format("El peso total de los lotes (%.2f kg) no cumple con el cupo mínimo de la planta (%.2f kg). " +
                                    "Necesitas %.2f kg adicionales.",
                            pesoTotal,
                            planta.getCupoMinimo(),
                            planta.getCupoMinimo().subtract(pesoTotal))
            );
        }

        log.info("Validación de peso exitosa - Peso total: {} kg, Cupo mínimo: {} kg",
                pesoTotal, planta.getCupoMinimo());
    }

    // ==================== KANBAN DE PROCESOS ====================

    /**
     * Obtener procesos del concentrado (Kanban)
     */
    @Transactional(readOnly = true)
    public ProcesosConcentradoResponseDto obtenerProcesos(Integer concentradoId, Integer usuarioId) {
        log.debug("Obteniendo procesos del concentrado ID: {}", concentradoId);

        IngenioMinero ingenio = obtenerIngenioDelUsuario(usuarioId);
        Concentrado concentrado = obtenerConcentradoConPermisos(concentradoId, ingenio);

        List<LoteProcesoPlanta> procesos = loteProcesoPlantaRepository
                .findByConcentradoIdOrderByOrdenAsc(concentrado);

        List<ProcesoPlantaDto> procesosDto = procesos.stream()
                .map(this::convertirProcesoADto)
                .collect(Collectors.toList());

        long completados = procesos.stream().filter(p -> "completado".equals(p.getEstado())).count();
        long pendientes = procesos.stream().filter(p -> "pendiente".equals(p.getEstado())).count();

        ProcesoPlantaDto procesoActual = procesos.stream()
                .filter(p -> "en_proceso".equals(p.getEstado()) || "pendiente".equals(p.getEstado()))
                .findFirst()
                .map(this::convertirProcesoADto)
                .orElse(null);

        return ProcesosConcentradoResponseDto.builder()
                .concentradoId(concentrado.getId())
                .codigoConcentrado(concentrado.getCodigoConcentrado())
                .estadoConcentrado(concentrado.getEstado())
                .totalProcesos(procesos.size())
                .procesosCompletados((int) completados)
                .procesosPendientes((int) pendientes)
                .procesoActual(procesoActual)
                .todosProcesos(procesosDto)
                .build();
    }

    /**
     * Avanzar al siguiente proceso del Kanban
     */
    @Transactional
    public ProcesosConcentradoResponseDto avanzarProceso(
            Integer concentradoId,
            Integer procesoId,
            ProcesoAvanzarDto avanzarDto,
            Integer usuarioId,
            String ipOrigen
    ) {
        log.info("Avanzando proceso - Concentrado ID: {}, Proceso ID: {}", concentradoId, procesoId);

        IngenioMinero ingenio = obtenerIngenioDelUsuario(usuarioId);
        Concentrado concentrado = obtenerConcentradoConPermisos(concentradoId, ingenio);

        LoteProcesoPlanta proceso = loteProcesoPlantaRepository.findById(procesoId)
                .orElseThrow(() -> new IllegalArgumentException("Proceso no encontrado"));

        if (!proceso.getConcentradoId().getId().equals(concentradoId)) {
            throw new IllegalArgumentException("El proceso no pertenece a este concentrado");
        }

        validarOrdenProcesos(concentrado, proceso);

        String estadoAnterior = proceso.getEstado();

        if ("pendiente".equals(estadoAnterior)) {
            // Iniciar proceso
            proceso.setEstado("en_proceso");
            proceso.setFechaInicio(LocalDateTime.now());
            if (avanzarDto.getObservaciones() != null) {
                proceso.setObservaciones(avanzarDto.getObservaciones());
            }
            loteProcesoPlantaRepository.save(proceso);

            if (proceso.getOrden() == 1 && "en_camino_a_planta".equals(concentrado.getEstado())) {
                transicionarEstado(
                        concentrado,
                        "en_proceso",
                        "Inicio del procesamiento en planta - " + proceso.getProcesoId().getNombre(),
                        avanzarDto.getObservaciones(),
                        usuarioId,
                        ipOrigen
                );
            }

            registrarAuditoriaKanban(concentrado, proceso, "INICIAR_PROCESO", "Proceso iniciado", usuarioId, ipOrigen);

        } else if ("en_proceso".equals(estadoAnterior)) {
            // Completar proceso
            proceso.setEstado("completado");
            proceso.setFechaFin(LocalDateTime.now());
            if (avanzarDto.getObservaciones() != null) {
                String obsActuales = proceso.getObservaciones();
                proceso.setObservaciones(
                        obsActuales != null ? obsActuales + " | " + avanzarDto.getObservaciones() : avanzarDto.getObservaciones()
                );
            }
            loteProcesoPlantaRepository.save(proceso);

            registrarAuditoriaKanban(concentrado, proceso, "COMPLETAR_PROCESO", "Proceso completado", usuarioId, ipOrigen);

            if (loteProcesoPlantaRepository.todosCompletados(concentrado)) {
                transicionarEstado(
                        concentrado,
                        "esperando_reporte_quimico",
                        "Todos los procesos completados. Esperando reporte químico.",
                        null,
                        usuarioId,
                        ipOrigen
                );

                notificarProcesamientoCompleto(concentrado);
                publicarEventoWebSocket(concentrado, "procesamiento_completo");
            }

        } else {
            throw new IllegalArgumentException("El proceso ya está completado");
        }

        publicarEventoKanban(concentrado, proceso, estadoAnterior);

        return obtenerProcesos(concentradoId, usuarioId);
    }

    /**
     * Mover concentrado directamente a un proceso específico
     * Completa automáticamente los procesos intermedios si es necesario
     *
     * @param concentradoId ID del concentrado
     * @param moverDto Datos del movimiento (proceso destino, observaciones)
     * @param usuarioId Usuario que realiza la acción
     * @param ipOrigen IP del cliente
     * @return Estado actualizado del Kanban
     */
    @Transactional
    public ProcesosConcentradoResponseDto moverAProceso(
            Integer concentradoId,
            ProcesoMoverDto moverDto,
            Integer usuarioId,
            String ipOrigen
    ) {
        log.info("Moviendo concentrado {} al proceso {}", concentradoId, moverDto.getProcesoDestinoId());

        // 1. Validaciones iniciales
        IngenioMinero ingenio = obtenerIngenioDelUsuario(usuarioId);
        Concentrado concentrado = obtenerConcentradoConPermisos(concentradoId, ingenio);

        LoteProcesoPlanta procesoDestino = loteProcesoPlantaRepository.findById(moverDto.getProcesoDestinoId())
                .orElseThrow(() -> new IllegalArgumentException("Proceso destino no encontrado"));

        if (!procesoDestino.getConcentradoId().getId().equals(concentradoId)) {
            throw new IllegalArgumentException("El proceso destino no pertenece a este concentrado");
        }

        // 2. Obtener todos los procesos ordenados
        List<LoteProcesoPlanta> todosProcesos = loteProcesoPlantaRepository
                .findByConcentradoIdOrderByOrdenAsc(concentrado);

        // 3. Determinar proceso actual (primer pendiente o en_proceso)
        LoteProcesoPlanta procesoActual = todosProcesos.stream()
                .filter(p -> "pendiente".equals(p.getEstado()) || "en_proceso".equals(p.getEstado()))
                .findFirst()
                .orElse(null);

        if (procesoActual == null) {
            throw new IllegalArgumentException("Todos los procesos ya están completados");
        }

        // 4. Validar que no se intente retroceder
        if (procesoDestino.getOrden() < procesoActual.getOrden()) {
            throw new IllegalArgumentException(
                    "No puedes retroceder a un proceso anterior. Proceso actual: " + procesoActual.getProcesoId().getNombre()
            );
        }

        // 5. Si es el mismo proceso, solo cambiar su estado
        if (procesoDestino.getId().equals(procesoActual.getId())) {
            return avanzarProcesoActual(concentrado, procesoActual, moverDto.getObservaciones(), usuarioId, ipOrigen);
        }

        // 6. Si salta procesos, completar los intermedios automáticamente
        if (procesoDestino.getOrden() > procesoActual.getOrden()) {
            completarProcesosIntermedios(
                    concentrado,
                    todosProcesos,
                    procesoActual.getOrden(),
                    procesoDestino.getOrden(),
                    moverDto.getObservaciones(),
                    usuarioId,
                    ipOrigen
            );
        }

        // 7. Actualizar estado del concentrado si es el primer proceso
        if (procesoActual.getOrden() == 1 && "en_camino_a_planta".equals(concentrado.getEstado())) {
            transicionarEstado(
                    concentrado,
                    "en_proceso",
                    "Inicio del procesamiento en planta",
                    moverDto.getObservaciones(),
                    usuarioId,
                    ipOrigen
            );
        }

        // 8. Si completó todos, cambiar estado del concentrado
        if (loteProcesoPlantaRepository.todosCompletados(concentrado)) {
            transicionarEstado(
                    concentrado,
                    "esperando_reporte_quimico",
                    "Todos los procesos completados. Esperando reporte químico.",
                    null,
                    usuarioId,
                    ipOrigen
            );

            notificarProcesamientoCompleto(concentrado);
            publicarEventoWebSocket(concentrado, "procesamiento_completo");
        }

        // 9. Publicar evento WebSocket
        publicarEventoWebSocket(concentrado, "kanban_actualizado");

        // 10. Retornar estado actualizado
        return obtenerProcesos(concentradoId, usuarioId);
    }

    /**
     * Avanzar el proceso actual (iniciar si está pendiente, completar si está en_proceso)
     */
    private ProcesosConcentradoResponseDto avanzarProcesoActual(
            Concentrado concentrado,
            LoteProcesoPlanta proceso,
            String observaciones,
            Integer usuarioId,
            String ipOrigen
    ) {
        String estadoAnterior = proceso.getEstado();

        if ("pendiente".equals(estadoAnterior)) {
            // Iniciar proceso
            proceso.setEstado("en_proceso");
            proceso.setFechaInicio(LocalDateTime.now());
            if (observaciones != null) {
                proceso.setObservaciones(observaciones);
            }
            loteProcesoPlantaRepository.save(proceso);

            registrarAuditoriaKanban(concentrado, proceso, "INICIAR_PROCESO", "Proceso iniciado", usuarioId, ipOrigen);

        } else if ("en_proceso".equals(estadoAnterior)) {
            // Completar proceso
            proceso.setEstado("completado");
            proceso.setFechaFin(LocalDateTime.now());
            if (observaciones != null) {
                String obsActuales = proceso.getObservaciones();
                proceso.setObservaciones(
                        obsActuales != null ? obsActuales + " | " + observaciones : observaciones
                );
            }
            loteProcesoPlantaRepository.save(proceso);

            registrarAuditoriaKanban(concentrado, proceso, "COMPLETAR_PROCESO", "Proceso completado", usuarioId, ipOrigen);
        }

        publicarEventoKanban(concentrado, proceso, estadoAnterior);

        return obtenerProcesos(concentrado.getId(), usuarioId);
    }

    /**
     * Completar automáticamente procesos intermedios cuando se salta al destino
     */
    private void completarProcesosIntermedios(
            Concentrado concentrado,
            List<LoteProcesoPlanta> todosProcesos,
            Integer ordenInicial,
            Integer ordenFinal,
            String observaciones,
            Integer usuarioId,
            String ipOrigen
    ) {
        log.info("Completando procesos intermedios desde orden {} hasta {}", ordenInicial, ordenFinal);

        // Filtrar procesos en el rango (desde ordenInicial hasta ordenFinal-1)
        List<LoteProcesoPlanta> procesosACompletar = todosProcesos.stream()
                .filter(p -> p.getOrden() >= ordenInicial && p.getOrden() < ordenFinal)
                .sorted((a, b) -> a.getOrden().compareTo(b.getOrden()))
                .collect(Collectors.toList());

        for (LoteProcesoPlanta proceso : procesosACompletar) {
            String estadoAnterior = proceso.getEstado();

            // Si está pendiente, primero iniciarlo
            if ("pendiente".equals(estadoAnterior)) {
                proceso.setEstado("en_proceso");
                proceso.setFechaInicio(LocalDateTime.now());
                proceso.setObservaciones("Auto-iniciado (movimiento rápido Kanban)");
                loteProcesoPlantaRepository.save(proceso);

                registrarAuditoriaKanban(
                        concentrado,
                        proceso,
                        "AUTO_INICIAR_PROCESO",
                        "Proceso auto-iniciado por salto en Kanban",
                        usuarioId,
                        ipOrigen
                );
            }

            // Luego completarlo
            proceso.setEstado("completado");
            proceso.setFechaFin(LocalDateTime.now());

            String obs = proceso.getObservaciones() != null
                    ? proceso.getObservaciones() + " | Completado automáticamente"
                    : "Completado automáticamente (movimiento rápido Kanban)";

            if (observaciones != null && !observaciones.isBlank()) {
                obs += " - " + observaciones;
            }

            proceso.setObservaciones(obs);
            loteProcesoPlantaRepository.save(proceso);

            registrarAuditoriaKanban(
                    concentrado,
                    proceso,
                    "AUTO_COMPLETAR_PROCESO",
                    "Proceso auto-completado por salto en Kanban",
                    usuarioId,
                    ipOrigen
            );

            publicarEventoKanban(concentrado, proceso, estadoAnterior);

            log.info("Proceso {} auto-completado (orden {})", proceso.getProcesoId().getNombre(), proceso.getOrden());
        }

        // Iniciar el proceso destino si está pendiente
        LoteProcesoPlanta procesoDestino = todosProcesos.stream()
                .filter(p -> p.getOrden().equals(ordenFinal))
                .findFirst()
                .orElse(null);

        if (procesoDestino != null && "pendiente".equals(procesoDestino.getEstado())) {
            procesoDestino.setEstado("en_proceso");
            procesoDestino.setFechaInicio(LocalDateTime.now());
            if (observaciones != null) {
                procesoDestino.setObservaciones(observaciones);
            }
            loteProcesoPlantaRepository.save(procesoDestino);

            registrarAuditoriaKanban(
                    concentrado,
                    procesoDestino,
                    "INICIAR_PROCESO",
                    "Proceso iniciado tras completar intermedios",
                    usuarioId,
                    ipOrigen
            );

            publicarEventoKanban(concentrado, procesoDestino, "pendiente");
        }
    }

    // ==================== REPORTE QUÍMICO ====================

    /**
     * Registrar reporte químico (PDF del laboratorio)
     */
    @Transactional
    public ConcentradoResponseDto registrarReporteQuimico(
            Integer concentradoId,
            ReporteQuimicoCreateDto reporteDto,
            Integer usuarioId,
            String ipOrigen
    ) {
        log.info("Registrando reporte químico - Concentrado ID: {}", concentradoId);

        IngenioMinero ingenio = obtenerIngenioDelUsuario(usuarioId);
        Concentrado concentrado = obtenerConcentradoConPermisos(concentradoId, ingenio);

        if (!"esperando_reporte_quimico".equals(concentrado.getEstado())) {
            throw new IllegalArgumentException(
                    "El concentrado no está en estado 'esperando_reporte_quimico'. Estado actual: " + concentrado.getEstado()
            );
        }

        ReporteQuimico reporte = ReporteQuimico.builder()
                .numeroReporte(reporteDto.getNumeroReporte())
                .laboratorio(reporteDto.getLaboratorio())
                .fechaAnalisis(reporteDto.getFechaAnalisis())
                .leyAg(reporteDto.getLeyAg())
                .leyPb(reporteDto.getLeyPb())
                .leyZn(reporteDto.getLeyZn())
                .humedad(reporteDto.getHumedad())
                .tipoAnalisis(reporteDto.getTipoAnalisis())
                .urlPdf(reporteDto.getUrlPdf())
                .build();

        reporte = reporteQuimicoRepository.save(reporte);

        transicionarEstado(
                concentrado,
                "reporte_quimico_registrado",
                "Reporte químico registrado - Nro: " + reporteDto.getNumeroReporte(),
                "Laboratorio: " + reporteDto.getLaboratorio(),
                usuarioId,
                ipOrigen
        );

        registrarAuditoriaReporteQuimico(concentrado, reporte, "REGISTRAR_REPORTE", usuarioId, ipOrigen);
        notificarReporteRegistrado(concentrado, reporte);
        publicarEventoWebSocket(concentrado, "reporte_quimico_registrado");

        return convertirAResponseDto(concentrado);
    }

    /**
     * Validar reporte químico (cambiar estado a "listo_para_liquidacion")
     */
    @Transactional
    public ConcentradoResponseDto validarReporteQuimico(
            Integer concentradoId,
            Integer usuarioId,
            String ipOrigen
    ) {
        log.info("Validando reporte químico - Concentrado ID: {}", concentradoId);

        IngenioMinero ingenio = obtenerIngenioDelUsuario(usuarioId);
        Concentrado concentrado = obtenerConcentradoConPermisos(concentradoId, ingenio);

        if (!"reporte_quimico_registrado".equals(concentrado.getEstado())) {
            throw new IllegalArgumentException(
                    "El concentrado no está en estado 'reporte_quimico_registrado'. Estado actual: " + concentrado.getEstado()
            );
        }

        transicionarEstado(
                concentrado,
                "listo_para_liquidacion",
                "Reporte químico validado. Listo para solicitar liquidación del servicio.",
                null,
                usuarioId,
                ipOrigen
        );

        List<Map<String, Object>> historial = obtenerHistorial(concentrado);
        Map<String, Object> registro = new HashMap<>();
        registro.put("accion", "VALIDAR_REPORTE");
        registro.put("descripcion", "Reporte químico validado por operador del ingenio");
        registro.put("usuario_id", usuarioId);
        registro.put("ip_origen", ipOrigen);
        registro.put("timestamp", LocalDateTime.now().toString());
        historial.add(registro);
        concentrado.setObservaciones(convertirHistorialAJson(historial));
        concentradoRepository.save(concentrado);

        notificarReporteValidado(concentrado);
        publicarEventoWebSocket(concentrado, "listo_para_liquidacion");

        return convertirAResponseDto(concentrado);
    }

    // ==================== LIQUIDACIÓN DE SERVICIO ====================

    /**
     * Revisar solicitud de liquidación (cambiar a "en_revision")
     */
    @Transactional
    public ConcentradoResponseDto revisarLiquidacionServicio(
            Integer concentradoId,
            Integer usuarioId,
            String ipOrigen
    ) {
        log.info("Revisando liquidación de servicio - Concentrado ID: {}", concentradoId);

        IngenioMinero ingenio = obtenerIngenioDelUsuario(usuarioId);
        Concentrado concentrado = obtenerConcentradoConPermisos(concentradoId, ingenio);

        if (!"liquidacion_servicio_solicitada".equals(concentrado.getEstado())) {
            throw new IllegalArgumentException(
                    "El concentrado no tiene solicitud de liquidación pendiente. Estado actual: " + concentrado.getEstado()
            );
        }

        transicionarEstado(
                concentrado,
                "liquidacion_servicio_en_revision",
                "Ingenio está revisando la solicitud de liquidación de servicio",
                null,
                usuarioId,
                ipOrigen
        );

        publicarEventoWebSocket(concentrado, "liquidacion_servicio_en_revision");

        return convertirAResponseDto(concentrado);
    }

    /**
     * Aprobar liquidación de servicio (definir costo)
     */
    @Transactional
    public LiquidacionServicioResponseDto aprobarLiquidacionServicio(
            Integer concentradoId,
            AprobarLiquidacionServicioDto aprobarDto,
            Integer usuarioId,
            String ipOrigen
    ) {
        log.info("Aprobando liquidación de servicio - Concentrado ID: {}", concentradoId);

        IngenioMinero ingenio = obtenerIngenioDelUsuario(usuarioId);
        Concentrado concentrado = obtenerConcentradoConPermisos(concentradoId, ingenio);

        if (!"liquidacion_servicio_en_revision".equals(concentrado.getEstado())) {
            throw new IllegalArgumentException(
                    "La liquidación no está en revisión. Estado actual: " + concentrado.getEstado()
            );
        }

        Liquidacion liquidacion = Liquidacion.builder()
                .socioId(concentrado.getSocioPropietarioId())
                .tipoLiquidacion("servicio_procesamiento")
                .fechaLiquidacion(LocalDateTime.now())
                .moneda("BOB")
                .pesoLiquidado(concentrado.getPesoInicial())
                .valorBruto(aprobarDto.getCostoServicio())
                .valorNeto(aprobarDto.getCostoServicio())
                .estado("pendiente_pago")
                .build();

        liquidacion = liquidacionRepository.save(liquidacion);

        LiquidacionConcentrado liquidacionConcentrado = LiquidacionConcentrado.builder()
                .concentradoId(concentrado)
                .liquidacionId(liquidacion)
                .build();

        concentrado.addLiquidacionConcentrado(liquidacionConcentrado);
        liquidacionConcentradoRepository.save(liquidacionConcentrado);

        transicionarEstado(
                concentrado,
                "servicio_ingenio_liquidado",
                "Liquidación de servicio aprobada - Monto: " + aprobarDto.getCostoServicio() + " BOB",
                aprobarDto.getObservaciones(),
                usuarioId,
                ipOrigen
        );

        List<Map<String, Object>> historial = obtenerHistorial(concentrado);
        Map<String, Object> registro = new HashMap<>();
        registro.put("accion", "APROBAR_LIQUIDACION_SERVICIO");
        registro.put("liquidacion_id", liquidacion.getId());
        registro.put("costo_servicio", aprobarDto.getCostoServicio());
        registro.put("url_documento", aprobarDto.getUrlDocumentoLiquidacion());
        registro.put("usuario_id", usuarioId);
        registro.put("ip_origen", ipOrigen);
        registro.put("timestamp", LocalDateTime.now().toString());
        historial.add(registro);
        concentrado.setObservaciones(convertirHistorialAJson(historial));
        concentradoRepository.save(concentrado);

        notificarLiquidacionAprobada(concentrado, liquidacion);
        publicarEventoWebSocket(concentrado, "servicio_ingenio_liquidado");

        return convertirLiquidacionADto(liquidacion, concentrado);
    }

    /**
     * Registrar pago del servicio
     */
    @Transactional
    public ConcentradoResponseDto registrarPagoServicio(
            Integer concentradoId,
            RegistrarPagoServicioDto pagoDto,
            Integer usuarioId,
            String ipOrigen
    ) {
        log.info("Registrando pago de servicio - Concentrado ID: {}", concentradoId);

        IngenioMinero ingenio = obtenerIngenioDelUsuario(usuarioId);
        Concentrado concentrado = obtenerConcentradoConPermisos(concentradoId, ingenio);

        if (!"servicio_ingenio_liquidado".equals(concentrado.getEstado())) {
            throw new IllegalArgumentException(
                    "El servicio no está liquidado. Estado actual: " + concentrado.getEstado()
            );
        }

        LiquidacionConcentrado liquidacionConcentrado = concentrado.getLiquidacionConcentradoList().stream()
                .filter(lc -> "servicio_procesamiento".equals(lc.getLiquidacionId().getTipoLiquidacion()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No se encontró liquidación de servicio"));

        Liquidacion liquidacion = liquidacionConcentrado.getLiquidacionId();
        liquidacion.setEstado("pagado");
        liquidacionRepository.save(liquidacion);

        transicionarEstado(
                concentrado,
                "servicio_ingenio_pagado",
                "Pago de servicio registrado - Monto: " + pagoDto.getMontoPagado() + " BOB",
                pagoDto.getObservaciones(),
                usuarioId,
                ipOrigen
        );

        List<Map<String, Object>> historial = obtenerHistorial(concentrado);
        Map<String, Object> registro = new HashMap<>();
        registro.put("accion", "REGISTRAR_PAGO_SERVICIO");
        registro.put("liquidacion_id", liquidacion.getId());
        registro.put("monto_pagado", pagoDto.getMontoPagado());
        registro.put("fecha_pago", pagoDto.getFechaPago().toString());
        registro.put("metodo_pago", pagoDto.getMetodoPago());
        registro.put("numero_comprobante", pagoDto.getNumeroComprobante());
        registro.put("url_comprobante", pagoDto.getUrlComprobante());
        registro.put("usuario_id", usuarioId);
        registro.put("ip_origen", ipOrigen);
        registro.put("timestamp", LocalDateTime.now().toString());
        historial.add(registro);
        concentrado.setObservaciones(convertirHistorialAJson(historial));
        concentradoRepository.save(concentrado);

        transicionarEstado(
                concentrado,
                "listo_para_venta",
                "Servicio de procesamiento pagado. Concentrado listo para venta.",
                null,
                usuarioId,
                ipOrigen
        );

        notificarServicioPagado(concentrado);
        publicarEventoWebSocket(concentrado, "listo_para_venta");

        return convertirAResponseDto(concentrado);
    }

    // ==================== MÉTODOS AUXILIARES PRIVADOS ====================

    private IngenioMinero obtenerIngenioDelUsuario(Integer usuarioId) {
        Usuarios usuario = usuariosRepository.findById(usuarioId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        return ingenioMineroRepository.findByUsuariosId(usuario)
                .orElseThrow(() -> new IllegalArgumentException("Ingenio minero no encontrado"));
    }

    private Concentrado obtenerConcentradoConPermisos(Integer concentradoId, IngenioMinero ingenio) {
        Concentrado concentrado = concentradoRepository.findById(concentradoId)
                .orElseThrow(() -> new IllegalArgumentException("Concentrado no encontrado"));

        if (!concentrado.getIngenioMineroId().getId().equals(ingenio.getId())) {
            throw new IllegalArgumentException("No tienes permiso para acceder a este concentrado");
        }

        return concentrado;
    }

    private List<Lotes> validarYObtenerLotes(List<Integer> lotesIds, IngenioMinero ingenio) {
        if (lotesIds == null || lotesIds.isEmpty()) {
            throw new IllegalArgumentException("Debe seleccionar al menos 1 lote");
        }

        List<Lotes> lotes = new ArrayList<>();

        for (Integer loteId : lotesIds) {
            Lotes lote = lotesRepository.findById(loteId)
                    .orElseThrow(() -> new IllegalArgumentException("Lote no encontrado - ID: " + loteId));

            if (!ESTADO_LOTE_TRANSPORTE_COMPLETO.equals(lote.getEstado())) {
                throw new IllegalArgumentException(
                        "El lote ID " + loteId + " no está en estado 'Transporte completo'. Estado actual: " + lote.getEstado()
                );
            }

            boolean perteneceAlIngenio = lote.getLoteIngenioList().stream()
                    .anyMatch(li -> li.getIngenioMineroId().getId().equals(ingenio.getId()));

            if (!perteneceAlIngenio) {
                throw new IllegalArgumentException("El lote ID " + loteId + " no pertenece a este ingenio");
            }

            boolean yaEnConcentrado = loteConcentradoRelacionRepository.existsByLoteComplejoId(lote);

            if (yaEnConcentrado) {
                throw new IllegalArgumentException("El lote ID " + loteId + " ya está asociado a otro concentrado");
            }

            lotes.add(lote);
        }

        Socio primerSocio = lotes.getFirst().getMinasId().getSocioId();
        boolean todosDelMismoSocio = lotes.stream()
                .allMatch(l -> l.getMinasId().getSocioId().getId().equals(primerSocio.getId()));

        if (!todosDelMismoSocio) {
            throw new IllegalArgumentException("Todos los lotes deben pertenecer al mismo socio");
        }

        return lotes;
    }

    private BigDecimal calcularPesoTotal(List<Lotes> lotes) {
        return lotes.stream()
                .map(Lotes::getPesoTotalReal)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }

    private String determinarMineralPrincipal(List<Lotes> lotes) {
        Map<String, Long> frecuenciaMinerales = lotes.stream()
                .collect(Collectors.groupingBy(Lotes::getTipoMineral, Collectors.counting()));

        return frecuenciaMinerales.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);
    }

    /**
     * Genera código único para concentrado
     * Formato: CONC-ING{id}-{mineral}-{timestamp}-{random}
     */
    private String generarCodigoConcentrado(IngenioMinero ingenio, String mineralPrincipal, int sufijo) {
        String timestamp = String.valueOf(System.currentTimeMillis()).substring(7);
        String random = String.format("%03d", new Random().nextInt(1000));
        String codigo = String.format("CONC-ING%d-%s-%s-%s-%d",
                ingenio.getId(), mineralPrincipal, timestamp, random, sufijo);

        while (concentradoRepository.existsByCodigo(codigo)) {
            random = String.format("%03d", new Random().nextInt(1000));
            codigo = String.format("CONC-ING%d-%s-%s-%s-%d",
                    ingenio.getId(), mineralPrincipal, timestamp, random, sufijo);
        }

        return codigo;
    }

    /**
     * Crea relaciones entre concentrado y lotes
     * El peso se distribuye según el porcentaje del concentrado
     */
    private void crearRelacionesConLotes(Concentrado concentrado, List<Lotes> lotes, Integer porcentajePeso) {
        for (Lotes lote : lotes) {
            BigDecimal pesoEntrada = lote.getPesoTotalReal()
                    .multiply(BigDecimal.valueOf(porcentajePeso))
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

            LoteConcentradoRelacion relacion = LoteConcentradoRelacion.builder()
                    .loteComplejoId(lote)
                    .concentradoId(concentrado)
                    .pesoEntrada(pesoEntrada)
                    .fechaCreacion(LocalDateTime.now())
                    .build();

            concentrado.addLoteConcentradoRelacion(relacion);
            loteConcentradoRelacionRepository.save(relacion);
        }
    }

    private void inicializarProcesosPlanta(Concentrado concentrado, IngenioMinero ingenio) {
        Planta planta = plantaRepository.findByIngenioMineroId(ingenio)
                .orElseThrow(() -> new IllegalArgumentException("El ingenio no tiene planta configurada"));

        List<ProcesosPlanta> procesosPlanta = procesosPlantaRepository.findByPlantaIdOrderByOrdenAsc(planta);

        if (procesosPlanta.isEmpty()) {
            log.warn("No hay procesos configurados para la planta ID: {}", planta.getId());
            return;
        }

        for (ProcesosPlanta procesoPlanta : procesosPlanta) {
            LoteProcesoPlanta loteProcesoPlanta = LoteProcesoPlanta.builder()
                    .concentradoId(concentrado)
                    .procesoId(procesoPlanta.getProcesosId())
                    .orden(procesoPlanta.getOrden())
                    .estado("pendiente")
                    .build();

            concentrado.addLoteProcesoPlanta(loteProcesoPlanta);
            loteProcesoPlantaRepository.save(loteProcesoPlanta);
        }
    }
    private void transicionarEstado(
            Concentrado concentrado,
            String nuevoEstado,
            String descripcion,
            String observacionesAdicionales,
            Integer usuarioId,
            String ipOrigen
    ) {
        String estadoAnterior = concentrado.getEstado();
        concentrado.setEstado(nuevoEstado);

        List<Map<String, Object>> historial = obtenerHistorial(concentrado);
        Map<String, Object> registro = crearRegistroHistorial(
                nuevoEstado,
                descripcion,
                observacionesAdicionales,
                usuarioId,
                ipOrigen
        );
        registro.put("estado_anterior", estadoAnterior);
        historial.add(registro);

        concentrado.setObservaciones(convertirHistorialAJson(historial));
        concentradoRepository.save(concentrado);
    }

    private Map<String, Object> crearRegistroHistorial(
            String estado,
            String descripcion,
            String observaciones,
            Integer usuarioId,
            String ipOrigen
    ) {
        Map<String, Object> registro = new HashMap<>();
        registro.put("estado", estado);
        registro.put("descripcion", descripcion);
        registro.put("observaciones", observaciones);
        registro.put("usuario_id", usuarioId);
        registro.put("ip_origen", ipOrigen);
        registro.put("timestamp", LocalDateTime.now().toString());
        return registro;
    }

    private List<Map<String, Object>> obtenerHistorial(Concentrado concentrado) {
        if (concentrado.getObservaciones() == null || concentrado.getObservaciones().isBlank()) {
            return new ArrayList<>();
        }

        try {
            return objectMapper.readValue(
                    concentrado.getObservaciones(),
                    new TypeReference<List<Map<String, Object>>>() {}
            );
        } catch (JsonProcessingException e) {
            log.error("Error al parsear historial del concentrado ID: {}", concentrado.getId(), e);
            return new ArrayList<>();
        }
    }

    private String convertirHistorialAJson(List<Map<String, Object>> historial) {
        try {
            return objectMapper.writeValueAsString(historial);
        } catch (JsonProcessingException e) {
            log.error("Error al convertir historial a JSON", e);
            return "[]";
        }
    }
    private void registrarAuditoriaCreacionMultiple(
            List<Concentrado> concentrados,
            List<Lotes> lotes,
            Integer usuarioId,
            String ipOrigen
    ) {
        for (Concentrado concentrado : concentrados) {
            List<Map<String, Object>> historial = obtenerHistorial(concentrado);
            Map<String, Object> ultimoRegistro = historial.getLast();
            ultimoRegistro.put("lotes_ids", lotes.stream().map(Lotes::getId).collect(Collectors.toList()));
            ultimoRegistro.put("cantidad_lotes", lotes.size());
            ultimoRegistro.put("accion", "CREAR_CONCENTRADO");
            ultimoRegistro.put("concentrados_hermanos",
                    concentrados.stream().map(Concentrado::getId).collect(Collectors.toList()));

            concentrado.setObservaciones(convertirHistorialAJson(historial));
            concentradoRepository.save(concentrado);
        }
    }

    private void notificarCreacionMultiple(
            List<Concentrado> concentrados,
            IngenioMinero ingenio,
            Socio socio
    ) {
        Integer socioUsuarioId = socio.getUsuariosId().getId();

        String minerales = concentrados.stream()
                .map(Concentrado::getMineralPrincipal)
                .collect(Collectors.joining(", "));

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("concentrados", concentrados.stream()
                .map(c -> Map.of(
                        "id", c.getId(),
                        "codigo", c.getCodigoConcentrado(),
                        "mineral", c.getMineralPrincipal(),
                        "peso", c.getPesoInicial()
                ))
                .collect(Collectors.toList()));
        metadata.put("ingenioNombre", ingenio.getRazonSocial());

        String mensaje = concentrados.size() == 1
                ? "Se ha creado el concentrado de " + minerales + " con tus lotes en el ingenio " + ingenio.getRazonSocial()
                : "Se han creado " + concentrados.size() + " concentrados (" + minerales + ") con tus lotes en el ingenio " + ingenio.getRazonSocial();

        notificacionBl.crearNotificacion(
                socioUsuarioId,
                "info",
                "Concentrado(s) creado(s)",
                mensaje,
                metadata
        );
    }



    private void validarOrdenProcesos(Concentrado concentrado, LoteProcesoPlanta procesoActual) {
        List<LoteProcesoPlanta> procesos = loteProcesoPlantaRepository
                .findByConcentradoIdOrderByOrdenAsc(concentrado);

        if (procesoActual.getOrden() == 1) {
            return;
        }

        LoteProcesoPlanta procesoAnterior = procesos.stream()
                .filter(p -> p.getOrden() == procesoActual.getOrden() - 1)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No se encontró el proceso anterior"));

        if (!"completado".equals(procesoAnterior.getEstado())) {
            throw new IllegalArgumentException(
                    "No puedes avanzar a este proceso. Primero debes completar: " + procesoAnterior.getProcesoId().getNombre()
            );
        }
    }

    private void registrarAuditoriaCreacion(Concentrado concentrado, List<Lotes> lotes, Integer usuarioId, String ipOrigen) {
        List<Map<String, Object>> historial = obtenerHistorial(concentrado);
        Map<String, Object> ultimoRegistro = historial.getLast();
        ultimoRegistro.put("lotes_ids", lotes.stream().map(Lotes::getId).collect(Collectors.toList()));
        ultimoRegistro.put("cantidad_lotes", lotes.size());
        ultimoRegistro.put("accion", "CREAR_CONCENTRADO");

        concentrado.setObservaciones(convertirHistorialAJson(historial));
        concentradoRepository.save(concentrado);
    }

    private void registrarAuditoriaKanban(
            Concentrado concentrado,
            LoteProcesoPlanta proceso,
            String accion,
            String descripcion,
            Integer usuarioId,
            String ipOrigen
    ) {
        List<Map<String, Object>> historial = obtenerHistorial(concentrado);
        Map<String, Object> registro = new HashMap<>();
        registro.put("accion", accion);
        registro.put("proceso_id", proceso.getId());
        registro.put("proceso_nombre", proceso.getProcesoId().getNombre());
        registro.put("proceso_orden", proceso.getOrden());
        registro.put("estado_proceso", proceso.getEstado());
        registro.put("descripcion", descripcion);
        registro.put("usuario_id", usuarioId);
        registro.put("ip_origen", ipOrigen);
        registro.put("timestamp", LocalDateTime.now().toString());

        historial.add(registro);
        concentrado.setObservaciones(convertirHistorialAJson(historial));
        concentradoRepository.save(concentrado);
    }

    private void registrarAuditoriaReporteQuimico(
            Concentrado concentrado,
            ReporteQuimico reporte,
            String accion,
            Integer usuarioId,
            String ipOrigen
    ) {
        List<Map<String, Object>> historial = obtenerHistorial(concentrado);
        Map<String, Object> registro = new HashMap<>();
        registro.put("accion", accion);
        registro.put("reporte_id", reporte.getId());
        registro.put("numero_reporte", reporte.getNumeroReporte());
        registro.put("laboratorio", reporte.getLaboratorio());
        registro.put("ley_ag", reporte.getLeyAg());
        registro.put("ley_pb", reporte.getLeyPb());
        registro.put("ley_zn", reporte.getLeyZn());
        registro.put("usuario_id", usuarioId);
        registro.put("ip_origen", ipOrigen);
        registro.put("timestamp", LocalDateTime.now().toString());

        historial.add(registro);
        concentrado.setObservaciones(convertirHistorialAJson(historial));
        concentradoRepository.save(concentrado);
    }

    private void notificarCreacion(Concentrado concentrado, IngenioMinero ingenio, Socio socio) {
        Integer socioUsuarioId = socio.getUsuariosId().getId();

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("concentradoId", concentrado.getId());
        metadata.put("codigoConcentrado", concentrado.getCodigoConcentrado());
        metadata.put("ingenioNombre", ingenio.getRazonSocial());
        metadata.put("pesoInicial", concentrado.getPesoInicial());
        metadata.put("estado", concentrado.getEstado());

        notificacionBl.crearNotificacion(
                socioUsuarioId,
                "info",
                "Concentrado creado",
                "Se ha creado el concentrado " + concentrado.getCodigoConcentrado() +
                        " con tus lotes en el ingenio " + ingenio.getRazonSocial() +
                        ". Peso inicial: " + concentrado.getPesoInicial() + " kg",
                metadata
        );
    }

    private void notificarProcesamientoCompleto(Concentrado concentrado) {
        Integer socioUsuarioId = concentrado.getSocioPropietarioId().getUsuariosId().getId();

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("concentradoId", concentrado.getId());
        metadata.put("codigoConcentrado", concentrado.getCodigoConcentrado());
        metadata.put("estado", concentrado.getEstado());

        notificacionBl.crearNotificacion(
                socioUsuarioId,
                "success",
                "Procesamiento completado",
                "El procesamiento del concentrado " + concentrado.getCodigoConcentrado() + " ha finalizado. Se espera el reporte químico.",
                metadata
        );
    }

    private void notificarReporteRegistrado(Concentrado concentrado, ReporteQuimico reporte) {
        Integer socioUsuarioId = concentrado.getSocioPropietarioId().getUsuariosId().getId();

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("concentradoId", concentrado.getId());
        metadata.put("codigoConcentrado", concentrado.getCodigoConcentrado());
        metadata.put("reporteId", reporte.getId());
        metadata.put("numeroReporte", reporte.getNumeroReporte());

        notificacionBl.crearNotificacion(
                socioUsuarioId,
                "info",
                "Reporte químico registrado",
                "Se ha registrado el reporte químico " + reporte.getNumeroReporte() + " para el concentrado " + concentrado.getCodigoConcentrado(),
                metadata
        );
    }

    private void notificarReporteValidado(Concentrado concentrado) {
        Integer socioUsuarioId = concentrado.getSocioPropietarioId().getUsuariosId().getId();

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("concentradoId", concentrado.getId());
        metadata.put("codigoConcentrado", concentrado.getCodigoConcentrado());
        metadata.put("estado", concentrado.getEstado());

        notificacionBl.crearNotificacion(
                socioUsuarioId,
                "success",
                "Solicitar liquidación de servicio",
                "El reporte químico del concentrado " + concentrado.getCodigoConcentrado() + " ha sido validado. Ya puedes solicitar la liquidación del servicio de procesamiento.",
                metadata
        );
    }

    private void notificarLiquidacionAprobada(Concentrado concentrado, Liquidacion liquidacion) {
        Integer socioUsuarioId = concentrado.getSocioPropietarioId().getUsuariosId().getId();

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("concentradoId", concentrado.getId());
        metadata.put("codigoConcentrado", concentrado.getCodigoConcentrado());
        metadata.put("liquidacionId", liquidacion.getId());
        metadata.put("montoLiquidacion", liquidacion.getValorNeto());

        notificacionBl.crearNotificacion(
                socioUsuarioId,
                "success",
                "Liquidación de servicio aprobada",
                "La liquidación del servicio de procesamiento para el concentrado " + concentrado.getCodigoConcentrado() + " ha sido aprobada. Monto: " + liquidacion.getValorNeto() + " BOB. Procede a realizar el pago.",
                metadata
        );
    }

    private void notificarServicioPagado(Concentrado concentrado) {
        Integer socioUsuarioId = concentrado.getSocioPropietarioId().getUsuariosId().getId();

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("concentradoId", concentrado.getId());
        metadata.put("codigoConcentrado", concentrado.getCodigoConcentrado());
        metadata.put("estado", concentrado.getEstado());

        notificacionBl.crearNotificacion(
                socioUsuarioId,
                "success",
                "Servicio pagado - Listo para venta",
                "El pago del servicio de procesamiento ha sido registrado. El concentrado " + concentrado.getCodigoConcentrado() + " está listo para solicitar venta.",
                metadata
        );

        Integer ingenioUsuarioId = concentrado.getIngenioMineroId().getUsuariosId().getId();

        notificacionBl.crearNotificacion(
                ingenioUsuarioId,
                "success",
                "Servicio pagado",
                "El pago del servicio de procesamiento para el concentrado " + concentrado.getCodigoConcentrado() + " ha sido registrado.",
                metadata
        );
    }

    private void publicarEventoWebSocket(Concentrado concentrado, String evento) {
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("evento", evento);
            payload.put("concentradoId", concentrado.getId());
            payload.put("codigoConcentrado", concentrado.getCodigoConcentrado());
            payload.put("mineralPrincipal", concentrado.getMineralPrincipal());
            payload.put("estado", concentrado.getEstado());
            payload.put("timestamp", LocalDateTime.now().toString());

            String canal = "/topic/ingenio/" + concentrado.getIngenioMineroId().getId() + "/concentrados";
            messagingTemplate.convertAndSend(canal, payload);

            String canalSocio = "/topic/socio/" + concentrado.getSocioPropietarioId().getId() + "/concentrados";
            messagingTemplate.convertAndSend(canalSocio, payload);

        } catch (Exception e) {
            log.error("Error al publicar evento WebSocket", e);
        }
    }

    private void publicarEventoKanban(Concentrado concentrado, LoteProcesoPlanta proceso, String estadoAnterior) {
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("evento", "etapa_actualizada");
            payload.put("concentradoId", concentrado.getId());
            payload.put("codigoConcentrado", concentrado.getCodigoConcentrado());
            payload.put("procesoId", proceso.getId());
            payload.put("procesoNombre", proceso.getProcesoId().getNombre());
            payload.put("estadoAnterior", estadoAnterior);
            payload.put("estadoNuevo", proceso.getEstado());
            payload.put("orden", proceso.getOrden());
            payload.put("timestamp", LocalDateTime.now().toString());

            String canal = "/topic/ingenio/" + concentrado.getIngenioMineroId().getId() + "/concentrados";
            messagingTemplate.convertAndSend(canal, payload);

            String canalSocio = "/topic/socio/" + concentrado.getSocioPropietarioId().getId() + "/concentrados";
            messagingTemplate.convertAndSend(canalSocio, payload);

        } catch (Exception e) {
            log.error("Error al publicar evento Kanban WebSocket", e);
        }
    }

    private ConcentradoResponseDto convertirAResponseDto(Concentrado concentrado) {
        ConcentradoResponseDto dto = new ConcentradoResponseDto();
        dto.setId(concentrado.getId());
        dto.setCodigoConcentrado(concentrado.getCodigoConcentrado());
        dto.setEstado(concentrado.getEstado());
        dto.setPesoInicial(concentrado.getPesoInicial());
        dto.setPesoFinal(concentrado.getPesoFinal());
        dto.setMerma(concentrado.getMerma());
        dto.setMineralPrincipal(concentrado.getMineralPrincipal());
        dto.setMineralesSecundarios(concentrado.getMineralesSecundarios()); // NUEVO
        dto.setLoteOrigenMultiple(concentrado.getLoteOrigenMultiple()); // NUEVO
        dto.setNumeroSacos(concentrado.getNumeroSacos());
        dto.setIngenioId(concentrado.getIngenioMineroId().getId());
        dto.setIngenioNombre(concentrado.getIngenioMineroId().getRazonSocial());

        Socio socio = concentrado.getSocioPropietarioId();
        if (socio != null) {
            dto.setSocioId(socio.getId());
            Persona persona = personaRepository.findByUsuariosId(socio.getUsuariosId()).orElse(null);
            if (persona != null) {
                dto.setSocioNombres(persona.getNombres());
                dto.setSocioApellidos(persona.getPrimerApellido() +
                        (persona.getSegundoApellido() != null ? " " + persona.getSegundoApellido() : ""));
                dto.setSocioCi(persona.getCi());
            }
        }

        List<LoteSimpleDto> lotesDto = concentrado.getLoteConcentradoRelacionList().stream()
                .map(relacion -> {
                    Lotes lote = relacion.getLoteComplejoId();
                    return LoteSimpleDto.builder()
                            .id(lote.getId())
                            .minaNombre(lote.getMinasId().getNombre())
                            .tipoMineral(lote.getTipoMineral())
                            .pesoTotalReal(lote.getPesoTotalReal())
                            .estado(lote.getEstado())
                            .build();
                })
                .collect(Collectors.toList());
        dto.setLotes(lotesDto);

        if (!concentrado.getLoteConcentradoRelacionList().isEmpty()) {
            Lotes primerLote = concentrado.getLoteConcentradoRelacionList().getFirst().getLoteComplejoId();
            List<LoteMinerales> loteMinerales = loteMineralesRepository.findByLotesId(primerLote);
            dto.setMinerales(
                    loteMinerales.stream()
                            .map(lm -> new MineralInfoDto(
                                    lm.getMineralesId().getId(),
                                    lm.getMineralesId().getNombre(),
                                    lm.getMineralesId().getNomenclatura()))
                            .collect(Collectors.toList())
            );
        }

        dto.setFechaInicio(concentrado.getFechaInicio());
        dto.setFechaFin(concentrado.getFechaFin());
        dto.setCreatedAt(concentrado.getCreatedAt());
        dto.setUpdatedAt(concentrado.getUpdatedAt());

        try {
            List<Map<String, Object>> historial = objectMapper.readValue(
                    concentrado.getObservaciones(),
                    new TypeReference<List<Map<String, Object>>>() {}
            );
            dto.setObservaciones(historial.isEmpty() ? null : historial.getLast());
        } catch (Exception e) {
            log.warn("Error al parsear observaciones del concentrado ID: {}", concentrado.getId());
        }

        return dto;
    }

    private ProcesoPlantaDto convertirProcesoADto(LoteProcesoPlanta proceso) {
        return ProcesoPlantaDto.builder()
                .id(proceso.getId())
                .nombreProceso(proceso.getProcesoId().getNombre())
                .orden(proceso.getOrden())
                .estado(proceso.getEstado())
                .fechaInicio(proceso.getFechaInicio())
                .fechaFin(proceso.getFechaFin())
                .observaciones(proceso.getObservaciones())
                .build();
    }

    private LiquidacionServicioResponseDto convertirLiquidacionADto(Liquidacion liquidacion, Concentrado concentrado) {
        LiquidacionServicioResponseDto dto = new LiquidacionServicioResponseDto();
        dto.setId(liquidacion.getId());
        dto.setConcentradoId(concentrado.getId());
        dto.setCodigoConcentrado(concentrado.getCodigoConcentrado());
        dto.setSocioId(liquidacion.getSocioId().getId());

        Persona persona = personaRepository.findByUsuariosId(liquidacion.getSocioId().getUsuariosId()).orElse(null);
        if (persona != null) {
            dto.setSocioNombres(persona.getNombres());
            dto.setSocioApellidos(persona.getPrimerApellido() + (persona.getSegundoApellido() != null ? " " + persona.getSegundoApellido() : ""));
        }

        dto.setTipoLiquidacion(liquidacion.getTipoLiquidacion());
        dto.setFechaLiquidacion(liquidacion.getFechaLiquidacion());
        dto.setMoneda(liquidacion.getMoneda());
        dto.setPesoLiquidado(liquidacion.getPesoLiquidado());
        dto.setValorBruto(liquidacion.getValorBruto());
        dto.setValorNeto(liquidacion.getValorNeto());
        dto.setEstado(liquidacion.getEstado());
        dto.setCreatedAt(liquidacion.getCreatedAt());
        dto.setUpdatedAt(liquidacion.getUpdatedAt());

        return dto;
    }


    @Transactional(readOnly = true)
    public Map<String, Object> obtenerInfoPlanta(Integer usuarioId) {
        log.debug("Obteniendo información de la planta - Usuario ID: {}", usuarioId);

        IngenioMinero ingenio = obtenerIngenioDelUsuario(usuarioId);

        Planta planta = plantaRepository.findByIngenioMineroId(ingenio)
                .orElseThrow(() -> new IllegalArgumentException("El ingenio no tiene planta configurada"));

        Map<String, Object> info = new HashMap<>();
        info.put("cupoMinimo", planta.getCupoMinimo());
        info.put("capacidadProcesamiento", planta.getCapacidadProcesamiento());
        info.put("costoProcesamiento", planta.getCostoProcesamiento());
        info.put("departamento", planta.getDepartamento());
        info.put("provincia", planta.getProvincia());
        info.put("municipio", planta.getMunicipio());
        info.put("direccion", planta.getDireccion());

        return info;
    }
}