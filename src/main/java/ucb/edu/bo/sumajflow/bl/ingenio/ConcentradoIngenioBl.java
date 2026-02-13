package ucb.edu.bo.sumajflow.bl.ingenio;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ucb.edu.bo.sumajflow.bl.ConcentradoBl;
import ucb.edu.bo.sumajflow.bl.NotificacionBl;
import ucb.edu.bo.sumajflow.dto.ingenio.*;
import ucb.edu.bo.sumajflow.entity.*;
import ucb.edu.bo.sumajflow.repository.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Servicio de Concentrados para el ROL INGENIO
 * Responsabilidades: Creación y consulta de concentrados
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
    private final ProcesosPlantaRepository procesosPlantaRepository;
    private final LoteProcesoPlantaRepository loteProcesoPlantaRepository;
    private final PlantaRepository plantaRepository;


    // Servicios
    private final ConcentradoBl concentradoBl;
    private final NotificacionBl notificacionBl;
    private final SimpMessagingTemplate messagingTemplate;
    private final ConcentradoMineralAnalyzer mineralAnalyzer;
    private final LiquidacionTollIngenioBl liquidacionTollBl;


    // Constantes
    private static final String ESTADO_CREADO = "creado";
    private static final String ESTADO_EN_CAMINO_PLANTA = "en_camino_a_planta";
    private static final String ESTADO_LOTE_TRANSPORTE_COMPLETO = "Transporte completo";
    private static final String ZINC = "Zn";
    private static final String PLOMO = "Pb";
    private static final String PLATA = "Ag";

    // ==================== LISTAR Y CONSULTAR CONCENTRADOS ====================

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

        List<Concentrado> concentradosIngenio = concentradoRepository
                .findByIngenioMineroIdOrderByCreatedAtDesc(ingenio);

        return concentradoBl.listarConcentradosConFiltros(
                concentradosIngenio, estado, mineralPrincipal, fechaDesde, fechaHasta, page, size
        );
    }

    /**
     * Obtener detalle de un concentrado específico
     */
    @Transactional(readOnly = true)
    public ConcentradoResponseDto obtenerDetalle(Integer concentradoId, Integer usuarioId) {
        log.debug("Obteniendo detalle del concentrado ID: {}", concentradoId);

        IngenioMinero ingenio = obtenerIngenioDelUsuario(usuarioId);
        Concentrado concentrado = obtenerConcentradoConPermisos(concentradoId, ingenio);

        return concentradoBl.convertirAResponseDto(concentrado);
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
        dashboard.put("vendidoAComercializadora", todos.stream().filter(c -> "vendido_a_comercializadora".equals(c.getEstado())).count());

        BigDecimal pesoTotal = todos.stream()
                .map(Concentrado::getPesoInicial)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        dashboard.put("pesoTotalProcesado", pesoTotal);

        return dashboard;
    }

    /**
     * Obtener información de la planta del ingenio
     */
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

    // ==================== CREAR CONCENTRADO ====================

    /**
     * Crear concentrado(s) a partir de lotes aprobados
     * Crea múltiples concentrados si el lote tiene Zn+Pb
     */
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

        // 3. Validar cupo mínimo planta
        validarPesoMinimoPlanta(lotes, ingenio);

        // 4. Socio propietario
        Socio socioPropietario = lotes.getFirst().getMinasId().getSocioId();

        // 5. Minerales del batch
        BatchMineralContext ctx = construirContextoMineralesBatch(lotes);

        if (ctx.mineralesBatch.isEmpty()) {
            throw new IllegalArgumentException("Los lotes seleccionados no tienen minerales asociados");
        }

        // 6. Planificación según minerales
        ConcentradoMineralAnalyzer.ConcentradosPlanificados planificacion =
                mineralAnalyzer.determinarConcentradosDesdeSet(ctx.mineralesBatch);

        log.info("Minerales batch: {} => Se crearán {} concentrado(s)",
                ctx.mineralesBatch, planificacion.getConcentrados().size());
        boolean esMultiple = lotes.size() > 1;

        // 7. Crear concentrados
        List<Concentrado> concentradosCreados = new ArrayList<>();
        for (int i = 0; i < planificacion.getConcentrados().size(); i++) {
            ConcentradoMineralAnalyzer.ConcentradoPlanificado planificado = planificacion.getConcentrados().get(i);

            Concentrado concentrado = crearConcentradoIndividual(
                    createDto, lotes, ingenio, socioPropietario, planificado,
                    esMultiple, i + 1, usuarioId, ipOrigen, ctx
            );

            concentradosCreados.add(concentrado);
        }

        Planta planta = plantaRepository.findByIngenioMineroId(ingenio)
                .orElseThrow(() -> new IllegalArgumentException("El ingenio no tiene planta configurada"));

        Liquidacion liquidacionToll = liquidacionTollBl.crearLiquidacionToll(
                lotes, socioPropietario, ingenio, planta.getCostoProcesamiento(), createDto, usuarioId
        );

        log.info("✅ Liquidación de Toll creada - ID: {}, Monto: {} BOB",
                liquidacionToll.getId(), liquidacionToll.getValorNetoBob());

        // 8. Marcar lotes en planta
        lotes.forEach(lote -> {
            lote.setEstado("En planta");
            lotesRepository.save(lote);
        });

        // 9. Auditoría y notificación
        registrarAuditoriaCreacionMultiple(concentradosCreados, lotes, usuarioId, ipOrigen);
        notificarCreacionMultiple(concentradosCreados, ingenio, socioPropietario);

        //Publicar en websocket
        for (Concentrado concentrado : concentradosCreados) {
            concentradoBl.publicarEventoWebSocket(concentrado, "concentrado_creado");
        }
        // 10. DTOs
        return concentradosCreados.stream()
                .map(concentradoBl::convertirAResponseDto)
                .collect(Collectors.toList());
    }

    // ==================== MÉTODOS PRIVADOS ====================

    private Concentrado crearConcentradoIndividual(
            ConcentradoCreateDto createDto,
            List<Lotes> lotes,
            IngenioMinero ingenio,
            Socio socioPropietario,
            ConcentradoMineralAnalyzer.ConcentradoPlanificado planificado,
            Boolean esMultiple,
            int sufijo,
            Integer usuarioId,
            String ipOrigen,
            BatchMineralContext ctx
    ) {
        String mineralPrincipal = planificado.getMineralPrincipal();

        // 1. Código
        String codigoConcentrado = generarCodigoConcentrado(ingenio, mineralPrincipal, sufijo);

        // 2. Peso inicial
        BigDecimal pesoConcentrado = calcularPesoInicialConcentrado(
                createDto, lotes, mineralPrincipal, esMultiple, ctx
        );

        // 3. Minerales secundarios
        String mineralesSecundarios = mineralAnalyzer.construirMineralesSecundarios(planificado);

        // 4. Historial inicial
        List<Map<String, Object>> historial = new ArrayList<>();
        Map<String, Object> registroInicial = concentradoBl.crearRegistroHistorial(
                ESTADO_CREADO,
                "Concentrado creado - Mineral principal: " + mineralPrincipal,
                createDto.getObservacionesIniciales(),
                usuarioId,
                ipOrigen
        );

        if (Boolean.TRUE.equals(planificado.getRequiereRevision())) {
            registroInicial.put("requiere_revision", true);
            registroInicial.put("motivo_revision", planificado.getObservacionRevision());
        }
        historial.add(registroInicial);

        // 5. Persistir concentrado
        Concentrado concentrado = Concentrado.builder()
                .codigoConcentrado(codigoConcentrado)
                .ingenioMineroId(ingenio)
                .socioPropietarioId(socioPropietario)
                .pesoInicial(pesoConcentrado)
                .mineralPrincipal(mineralPrincipal)
                .mineralesSecundarios(mineralesSecundarios)
                .loteOrigenMultiple(esMultiple)
                .estado(ESTADO_CREADO)
                .observaciones(concentradoBl.convertirHistorialAJson(historial))
                .build();

        concentrado = concentradoRepository.save(concentrado);

        // 6. Relaciones lote<->concentrado FILTRADAS
        crearRelacionesConLotesFiltradas(concentrado, lotes, mineralPrincipal, esMultiple, ctx);

        // 7. Inicializar procesos planta
        inicializarProcesosPlanta(concentrado, ingenio);

        // 8. Transición estado
        concentradoBl.transicionarEstado(
                concentrado,
                ESTADO_EN_CAMINO_PLANTA,
                "Concentrado de " + mineralPrincipal + " en camino a planta",
                null,
                usuarioId,
                ipOrigen
        );

        return concentrado;
    }

    private BatchMineralContext construirContextoMineralesBatch(List<Lotes> lotes) {
        Map<Integer, Set<String>> mineralesPorLote = new HashMap<>();
        Set<String> mineralesBatch = new HashSet<>();

        for (Lotes lote : lotes) {
            Set<String> setLote = lote.getLoteMineralesList().stream()
                    .map(lm -> lm.getMineralesId().getNomenclatura())
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());

            mineralesPorLote.put(lote.getId(), setLote);
            mineralesBatch.addAll(setLote);
        }

        return new BatchMineralContext(mineralesBatch, mineralesPorLote);
    }

    private BigDecimal calcularPesoInicialConcentrado(
            ConcentradoCreateDto createDto,
            List<Lotes> lotes,
            String mineralPrincipal,
            Boolean esMultiple,
            BatchMineralContext ctx
    ) {
        // Calcular peso total de todos los lotes en kg
        BigDecimal pesoTotalKg = BigDecimal.ZERO;

        for (Lotes lote : lotes) {
            BigDecimal pesoLote = (lote.getPesoTotalReal() != null) ? lote.getPesoTotalReal() : BigDecimal.ZERO;
            pesoTotalKg = pesoTotalKg.add(pesoLote);
        }

        // Si no hay peso en los lotes, usar el peso del DTO
        if (pesoTotalKg.compareTo(BigDecimal.ZERO) == 0 && createDto.getPesoInicial() != null) {
            pesoTotalKg = createDto.getPesoInicial();
        }

        // Convertir de kilogramos a toneladas (dividir entre 1000)
        BigDecimal pesoToneladas = pesoTotalKg.divide(BigDecimal.valueOf(1000), 2, RoundingMode.HALF_UP);

        return pesoToneladas.setScale(2, RoundingMode.HALF_UP);
    }

    private record BatchMineralContext(
            Set<String> mineralesBatch,
            Map<Integer, Set<String>> mineralesPorLote
    ) {}

    private void validarPesoMinimoPlanta(List<Lotes> lotes, IngenioMinero ingenio) {
        Planta planta = plantaRepository.findByIngenioMineroId(ingenio)
                .orElseThrow(() -> new IllegalArgumentException("El ingenio no tiene planta configurada"));

        BigDecimal pesoTotal = calcularPesoTotal(lotes);

        if (pesoTotal.compareTo(planta.getCupoMinimo()) < 0) {
            throw new IllegalArgumentException(
                    String.format("El peso total de los lotes (%.2f kg) no cumple con el cupo mínimo de la planta (%.2f kg). " +
                                    "Necesitas %.2f kg adicionales.",
                            pesoTotal, planta.getCupoMinimo(), planta.getCupoMinimo().subtract(pesoTotal))
            );
        }

        log.info("Validación de peso exitosa - Peso total: {} kg, Cupo mínimo: {} kg",
                pesoTotal, planta.getCupoMinimo());
    }

    private IngenioMinero obtenerIngenioDelUsuario(Integer usuarioId) {
        Usuarios usuario = usuariosRepository.findById(usuarioId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        return ingenioMineroRepository.findByUsuariosId(usuario)
                .orElseThrow(() -> new IllegalArgumentException("Ingenio minero no encontrado"));
    }

    private Concentrado obtenerConcentradoConPermisos(Integer concentradoId, IngenioMinero ingenio) {
        Concentrado concentrado = concentradoBl.obtenerConcentrado(concentradoId);

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

    private void crearRelacionesConLotesFiltradas(
            Concentrado concentrado,
            List<Lotes> lotes,
            String mineralPrincipal,
            Boolean esMultiple,
            BatchMineralContext ctx
    ) {
        int countRel = 0;

        for (Lotes lote : lotes) {
            Set<String> m = ctx.mineralesPorLote().getOrDefault(lote.getId(), Set.of());

            if (!loteAportaAMineral(m, mineralPrincipal)) {
                continue;
            }

            BigDecimal pesoLote = (lote.getPesoTotalReal() != null) ? lote.getPesoTotalReal() : BigDecimal.ZERO;

            // Convertir de kg a toneladas SIN dividir (peso completo)
            BigDecimal pesoEntradaTon = pesoLote.divide(BigDecimal.valueOf(1000), 2, RoundingMode.HALF_UP);

            LoteConcentradoRelacion relacion = LoteConcentradoRelacion.builder()
                    .loteComplejoId(lote)
                    .concentradoId(concentrado)
                    .pesoEntrada(pesoEntradaTon.setScale(2, RoundingMode.HALF_UP))
                    .fechaCreacion(LocalDateTime.now())
                    .build();

            concentrado.addLoteConcentradoRelacion(relacion);
            loteConcentradoRelacionRepository.save(relacion);
            countRel++;
        }

        if (countRel == 0) {
            throw new IllegalStateException("No hay lotes compatibles para el concentrado " + mineralPrincipal);
        }
    }
    private boolean loteAportaAMineral(Set<String> mineralesLote, String mineralPrincipal) {
        if (mineralesLote == null || mineralesLote.isEmpty()) return false;

        if (mineralesLote.size() == 1 && mineralesLote.contains(PLATA)) {
            return PLATA.equals(mineralPrincipal);
        }

        return mineralesLote.contains(mineralPrincipal);
    }

    private boolean loteEsMixtoZnPb(Set<String> mineralesLote) {
        return mineralesLote != null && mineralesLote.contains(ZINC) && mineralesLote.contains(PLOMO);
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

    private void registrarAuditoriaCreacionMultiple(
            List<Concentrado> concentrados,
            List<Lotes> lotes,
            Integer usuarioId,
            String ipOrigen
    ) {
        for (Concentrado concentrado : concentrados) {
            Map<String, Object> detalles = new HashMap<>();
            detalles.put("mineral_principal", concentrado.getMineralPrincipal());
            detalles.put("lotes_ids", lotes.stream().map(Lotes::getId).collect(Collectors.toList()));
            detalles.put("cantidad_lotes", lotes.size());
            detalles.put("concentrados_hermanos",
                    concentrados.stream().map(Concentrado::getId).collect(Collectors.toList()));

            List<Map<String, Object>> historial = concentradoBl.obtenerHistorial(concentrado);
            if (!historial.isEmpty()) {
                Map<String, Object> primerRegistro = historial.get(0);
                detalles.put("obs_iniciales", primerRegistro.get("observaciones"));
            }

            registrarAuditoriaSimple(concentrado, "CREAR_CONCENTRADO", detalles, usuarioId);
        }
    }

    private void registrarAuditoriaSimple(
            Concentrado concentrado,
            String accion,
            Map<String, Object> detalles,
            Integer usuarioId
    ) {
        List<Map<String, Object>> historial = concentradoBl.obtenerHistorial(concentrado);

        Map<String, Object> registro = new HashMap<>();
        registro.put("accion", accion);
        registro.put("timestamp", LocalDateTime.now().toString());
        registro.put("usuario_id", usuarioId);
        registro.put("detalles", detalles);

        historial.add(registro);
        concentrado.setObservaciones(concentradoBl.convertirHistorialAJson(historial));
        concentradoRepository.save(concentrado);
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
}