package ucb.edu.bo.sumajflow.bl.socio;

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
import ucb.edu.bo.sumajflow.bl.NotificacionBl;
import ucb.edu.bo.sumajflow.dto.ingenio.*;
import ucb.edu.bo.sumajflow.dto.socio.MineralInfoDto;
import ucb.edu.bo.sumajflow.entity.*;
import ucb.edu.bo.sumajflow.repository.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Servicio de Concentrados para el ROL SOCIO
 * Responsabilidades:
 * - Ver sus propios concentrados (filtrado por socio propietario)
 * - Ver detalle de concentrado (solo lectura)
 * - Ver Kanban de procesos (solo lectura)
 * - Ver reporte químico cuando esté disponible
 * - Solicitar liquidación de servicio
 * - Solicitar venta a comercializadora
 * - Ver sus liquidaciones
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ConcentradoSocioBl {

    // Repositorios
    private final ConcentradoRepository concentradoRepository;
    private final UsuariosRepository usuariosRepository;
    private final SocioRepository socioRepository;
    private final PersonaRepository personaRepository;
    private final LoteMineralesRepository loteMineralesRepository;
    private final LoteProcesoPlantaRepository loteProcesoPlantaRepository;
    private final ComercializadoraRepository comercializadoraRepository;
    private final LiquidacionRepository liquidacionRepository;

    // Servicios
    private final NotificacionBl notificacionBl;
    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper;

    // ==================== LISTAR CONCENTRADOS DEL SOCIO ====================

    /**
     * Listar todos los concentrados del socio con filtros opcionales y paginación
     */
    @Transactional(readOnly = true)
    public Page<ConcentradoResponseDto> listarMisConcentrados(
            Integer usuarioId,
            String estado,
            String mineralPrincipal,
            LocalDateTime fechaDesde,
            LocalDateTime fechaHasta,
            int page,
            int size
    ) {
        log.debug("Listando concentrados del socio - Usuario ID: {}, Página: {}, Tamaño: {}",
                usuarioId, page, size);

        Socio socio = obtenerSocioDelUsuario(usuarioId);

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        // Obtener todos los concentrados del socio
        List<Concentrado> todosLosConcentrados = concentradoRepository
                .findBySocioPropietarioIdOrderByCreatedAtDesc(socio);

        // Aplicar filtros manualmente
        List<Concentrado> concentradosFiltrados = todosLosConcentrados.stream()
                .filter(c -> {
                    if (estado != null && !estado.isEmpty() && !estado.equals(c.getEstado())) {
                        return false;
                    }
                    if (mineralPrincipal != null && !mineralPrincipal.isEmpty() && !mineralPrincipal.equals(c.getMineralPrincipal())) {
                        return false;
                    }
                    if (fechaDesde != null && c.getCreatedAt().isBefore(fechaDesde)) {
                        return false;
                    }
                    if (fechaHasta != null && c.getCreatedAt().isAfter(fechaHasta)) {
                        return false;
                    }
                    return true;
                })
                .collect(Collectors.toList());

        // Aplicar paginación manual
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), concentradosFiltrados.size());

        List<ConcentradoResponseDto> paginaActual = concentradosFiltrados.subList(start, end).stream()
                .map(this::convertirAResponseDto)
                .collect(Collectors.toList());

        return new org.springframework.data.domain.PageImpl<>(
                paginaActual,
                pageable,
                concentradosFiltrados.size()
        );
    }

    /**
     * Obtener detalle de un concentrado específico (solo si es propietario)
     */
    @Transactional(readOnly = true)
    public ConcentradoResponseDto obtenerMiConcentrado(Integer concentradoId, Integer usuarioId) {
        log.debug("Obteniendo detalle del concentrado ID: {}", concentradoId);

        Socio socio = obtenerSocioDelUsuario(usuarioId);
        Concentrado concentrado = obtenerConcentradoConPermisos(concentradoId, socio);

        return convertirAResponseDto(concentrado);
    }

    /**
     * Obtener estadísticas personales del socio
     */
    @Transactional(readOnly = true)
    public Map<String, Object> obtenerMiDashboard(Integer usuarioId) {
        log.debug("Obteniendo dashboard del socio - Usuario ID: {}", usuarioId);

        Socio socio = obtenerSocioDelUsuario(usuarioId);

        List<Concentrado> misConcentrados = concentradoRepository.findBySocioPropietarioIdOrderByCreatedAtDesc(socio);

        Map<String, Object> dashboard = new HashMap<>();
        dashboard.put("totalConcentrados", misConcentrados.size());
        dashboard.put("enProceso", misConcentrados.stream().filter(c -> c.getEstado().contains("proceso")).count());
        dashboard.put("esperandoReporte", misConcentrados.stream().filter(c -> "esperando_reporte_quimico".equals(c.getEstado())).count());
        dashboard.put("pendientePago", misConcentrados.stream().filter(c -> c.getEstado().contains("liquidado") && !c.getEstado().contains("pagado")).count());
        dashboard.put("listoVenta", misConcentrados.stream().filter(c -> "listo_para_venta".equals(c.getEstado())).count());
        dashboard.put("vendidos", misConcentrados.stream().filter(c -> "vendido".equals(c.getEstado())).count());

        return dashboard;
    }

    /**
     * Obtener procesos del concentrado (solo lectura para el socio)
     */
    @Transactional(readOnly = true)
    public ProcesosConcentradoResponseDto verProcesos(Integer concentradoId, Integer usuarioId) {
        log.debug("Viendo procesos del concentrado ID: {} (solo lectura)", concentradoId);

        Socio socio = obtenerSocioDelUsuario(usuarioId);
        Concentrado concentrado = obtenerConcentradoConPermisos(concentradoId, socio);

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

    // ==================== SOLICITAR LIQUIDACIÓN DE SERVICIO ====================

    /**
     * Solicitar liquidación de servicio de procesamiento al ingenio
     */
    @Transactional
    public ConcentradoResponseDto solicitarLiquidacionServicio(
            Integer concentradoId,
            SolicitudLiquidacionServicioDto solicitudDto,
            Integer usuarioId,
            String ipOrigen
    ) {
        log.info("Solicitando liquidación de servicio - Concentrado ID: {}", concentradoId);

        Socio socio = obtenerSocioDelUsuario(usuarioId);
        Concentrado concentrado = obtenerConcentradoConPermisos(concentradoId, socio);

        // Validar estado
        if (!"listo_para_liquidacion".equals(concentrado.getEstado())) {
            throw new IllegalArgumentException(
                    "El concentrado no está listo para liquidación. Estado actual: " + concentrado.getEstado()
            );
        }

        // Transicionar estado
        transicionarEstado(
                concentrado,
                "liquidacion_servicio_solicitada",
                "Socio solicitó liquidación del servicio de procesamiento",
                solicitudDto.getObservaciones(),
                usuarioId,
                ipOrigen
        );

        // Registrar auditoría
        List<Map<String, Object>> historial = obtenerHistorial(concentrado);
        Map<String, Object> registro = new HashMap<>();
        registro.put("accion", "SOLICITAR_LIQUIDACION_SERVICIO");
        registro.put("descripcion", "Socio solicitó liquidación de servicio de procesamiento");
        registro.put("socio_id", socio.getId());
        registro.put("usuario_id", usuarioId);
        registro.put("ip_origen", ipOrigen);
        registro.put("timestamp", LocalDateTime.now().toString());
        historial.add(registro);
        concentrado.setObservaciones(convertirHistorialAJson(historial));
        concentradoRepository.save(concentrado);

        // Notificar al ingenio
        notificarSolicitudLiquidacionServicio(concentrado, socio);

        // WebSocket
        publicarEventoWebSocket(concentrado, "liquidacion_servicio_solicitada");

        log.info("Liquidación de servicio solicitada - Concentrado ID: {}", concentradoId);

        return convertirAResponseDto(concentrado);
    }

    // ==================== SOLICITAR VENTA ====================

    /**
     * Solicitar venta de concentrado a una comercializadora
     */
    @Transactional
    public ConcentradoResponseDto solicitarVenta(
            Integer concentradoId,
            SolicitudVentaDto solicitudDto,
            Integer usuarioId,
            String ipOrigen
    ) {
        log.info("Solicitando venta de concentrado - Concentrado ID: {}", concentradoId);

        Socio socio = obtenerSocioDelUsuario(usuarioId);
        Concentrado concentrado = obtenerConcentradoConPermisos(concentradoId, socio);

        // Validar estado
        if (!"listo_para_venta".equals(concentrado.getEstado())) {
            throw new IllegalArgumentException(
                    "El concentrado no está listo para venta. Estado actual: " + concentrado.getEstado()
            );
        }

        // Validar que la comercializadora exista
        Comercializadora comercializadora = comercializadoraRepository.findById(solicitudDto.getComercializadoraId())
                .orElseThrow(() -> new IllegalArgumentException("Comercializadora no encontrada"));

        // Transicionar estado
        transicionarEstado(
                concentrado,
                "venta_solicitada",
                "Socio solicitó venta a comercializadora: " + comercializadora.getRazonSocial(),
                solicitudDto.getObservaciones(),
                usuarioId,
                ipOrigen
        );

        // Registrar auditoría
        List<Map<String, Object>> historial = obtenerHistorial(concentrado);
        Map<String, Object> registro = new HashMap<>();
        registro.put("accion", "SOLICITAR_VENTA");
        registro.put("descripcion", "Socio solicitó venta del concentrado");
        registro.put("comercializadora_id", comercializadora.getId());
        registro.put("comercializadora_nombre", comercializadora.getRazonSocial());
        registro.put("socio_id", socio.getId());
        registro.put("usuario_id", usuarioId);
        registro.put("ip_origen", ipOrigen);
        registro.put("timestamp", LocalDateTime.now().toString());
        historial.add(registro);
        concentrado.setObservaciones(convertirHistorialAJson(historial));
        concentradoRepository.save(concentrado);

        // Notificar a comercializadora
        notificarSolicitudVenta(concentrado, comercializadora, socio);

        // WebSocket
        publicarEventoWebSocket(concentrado, "venta_solicitada");

        log.info("Venta de concentrado solicitada - Concentrado ID: {}", concentradoId);

        return convertirAResponseDto(concentrado);
    }

    // ==================== VER LIQUIDACIONES ====================

    /**
     * Obtener todas las liquidaciones del socio
     */
    @Transactional(readOnly = true)
    public List<LiquidacionServicioResponseDto> verMisLiquidaciones(Integer usuarioId) {
        log.debug("Obteniendo liquidaciones del socio - Usuario ID: {}", usuarioId);

        Socio socio = obtenerSocioDelUsuario(usuarioId);

        List<Liquidacion> liquidaciones = liquidacionRepository.findBySocioIdOrderByFechaLiquidacionDesc(socio);

        return liquidaciones.stream()
                .map(this::convertirLiquidacionADto)
                .collect(Collectors.toList());
    }

    // ==================== MÉTODOS AUXILIARES PRIVADOS ====================

    private Socio obtenerSocioDelUsuario(Integer usuarioId) {
        Usuarios usuario = usuariosRepository.findById(usuarioId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        return socioRepository.findByUsuariosId(usuario)
                .orElseThrow(() -> new IllegalArgumentException("Socio no encontrado"));
    }

    private Concentrado obtenerConcentradoConPermisos(Integer concentradoId, Socio socio) {
        Concentrado concentrado = concentradoRepository.findById(concentradoId)
                .orElseThrow(() -> new IllegalArgumentException("Concentrado no encontrado"));

        // Validar que el concentrado pertenezca al socio
        if (!concentrado.getSocioPropietarioId().getId().equals(socio.getId())) {
            throw new IllegalArgumentException("No tienes permiso para acceder a este concentrado");
        }

        return concentrado;
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

    private void notificarSolicitudLiquidacionServicio(Concentrado concentrado, Socio socio) {
        // Notificar al ingenio
        Integer ingenioUsuarioId = concentrado.getIngenioMineroId().getUsuariosId().getId();

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("concentradoId", concentrado.getId());
        metadata.put("codigoConcentrado", concentrado.getCodigoConcentrado());
        metadata.put("socioId", socio.getId());

        Persona personaSocio = personaRepository.findByUsuariosId(socio.getUsuariosId()).orElse(null);
        String nombreSocio = personaSocio != null
                ? personaSocio.getNombres() + " " + personaSocio.getPrimerApellido()
                : "Socio ID " + socio.getId();

        notificacionBl.crearNotificacion(
                ingenioUsuarioId,
                "info",
                "Solicitud de liquidación de servicio",
                nombreSocio + " solicitó la liquidación del servicio de procesamiento para el concentrado " + concentrado.getCodigoConcentrado(),
                metadata
        );
    }

    private void notificarSolicitudVenta(Concentrado concentrado, Comercializadora comercializadora, Socio socio) {
        // Notificar a la comercializadora
        Integer comercializadoraUsuarioId = comercializadora.getUsuariosId().getId();

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("concentradoId", concentrado.getId());
        metadata.put("codigoConcentrado", concentrado.getCodigoConcentrado());
        metadata.put("socioId", socio.getId());
        metadata.put("pesoInicial", concentrado.getPesoInicial());
        metadata.put("mineralPrincipal", concentrado.getMineralPrincipal());

        Persona personaSocio = personaRepository.findByUsuariosId(socio.getUsuariosId()).orElse(null);
        String nombreSocio = personaSocio != null
                ? personaSocio.getNombres() + " " + personaSocio.getPrimerApellido()
                : "Socio ID " + socio.getId();

        notificacionBl.crearNotificacion(
                comercializadoraUsuarioId,
                "info",
                "Solicitud de compra de concentrado",
                nombreSocio + " solicitó la venta del concentrado " + concentrado.getCodigoConcentrado() +
                        " (" + concentrado.getPesoInicial() + " kg de " + concentrado.getMineralPrincipal() + ")",
                metadata
        );

        // Notificar al ingenio (informativo)
        Integer ingenioUsuarioId = concentrado.getIngenioMineroId().getUsuariosId().getId();

        notificacionBl.crearNotificacion(
                ingenioUsuarioId,
                "info",
                "Solicitud de venta iniciada",
                "El concentrado " + concentrado.getCodigoConcentrado() + " tiene una solicitud de venta a " + comercializadora.getRazonSocial(),
                metadata
        );
    }

    private void publicarEventoWebSocket(Concentrado concentrado, String evento) {
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("evento", evento);
            payload.put("concentradoId", concentrado.getId());
            payload.put("codigoConcentrado", concentrado.getCodigoConcentrado());
            payload.put("estado", concentrado.getEstado());
            payload.put("timestamp", LocalDateTime.now().toString());

            String canalSocio = "/topic/socio/" + concentrado.getSocioPropietarioId().getId() + "/concentrados";
            messagingTemplate.convertAndSend(canalSocio, payload);

        } catch (Exception e) {
            log.error("Error al publicar evento WebSocket", e);
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
        dto.setNumeroSacos(concentrado.getNumeroSacos());
        dto.setIngenioId(concentrado.getIngenioMineroId().getId());
        dto.setIngenioNombre(concentrado.getIngenioMineroId().getRazonSocial());

        Socio socio = concentrado.getSocioPropietarioId();
        if (socio != null) {
            dto.setSocioId(socio.getId());
            Persona persona = personaRepository.findByUsuariosId(socio.getUsuariosId()).orElse(null);
            if (persona != null) {
                dto.setSocioNombres(persona.getNombres());
                dto.setSocioApellidos(persona.getPrimerApellido() + (persona.getSegundoApellido() != null ? " " + persona.getSegundoApellido() : ""));
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
            Lotes primerLote = concentrado.getLoteConcentradoRelacionList().get(0).getLoteComplejoId();
            List<LoteMinerales> loteMinerales = loteMineralesRepository.findByLotesId(primerLote);
            dto.setMinerales(
                    loteMinerales.stream()
                            .map(lm -> new MineralInfoDto(lm.getMineralesId().getId(), lm.getMineralesId().getNombre(), lm.getMineralesId().getNomenclatura()))
                            .collect(Collectors.toList())
            );
        }

        dto.setFechaInicio(concentrado.getFechaInicio());
        dto.setFechaFin(concentrado.getFechaFin());
        dto.setCreatedAt(concentrado.getCreatedAt());
        dto.setUpdatedAt(concentrado.getUpdatedAt());

        try {
            List<Map<String, Object>> historial = objectMapper.readValue(concentrado.getObservaciones(), new TypeReference<List<Map<String, Object>>>() {});
            dto.setObservaciones(historial.isEmpty() ? null : historial.get(historial.size() - 1));
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

    private LiquidacionServicioResponseDto convertirLiquidacionADto(Liquidacion liquidacion) {
        LiquidacionServicioResponseDto dto = new LiquidacionServicioResponseDto();
        dto.setId(liquidacion.getId());
        dto.setTipoLiquidacion(liquidacion.getTipoLiquidacion());
        dto.setFechaLiquidacion(liquidacion.getFechaLiquidacion());
        dto.setMoneda(liquidacion.getMoneda());
        dto.setPesoLiquidado(liquidacion.getPesoLiquidado());
        dto.setValorBruto(liquidacion.getValorBruto());
        dto.setValorNeto(liquidacion.getValorNeto());
        dto.setEstado(liquidacion.getEstado());
        dto.setCreatedAt(liquidacion.getCreatedAt());
        dto.setUpdatedAt(liquidacion.getUpdatedAt());

        // Obtener concentrado relacionado si existe
        if (!liquidacion.getLiquidacionConcentradoList().isEmpty()) {
            LiquidacionConcentrado lc = liquidacion.getLiquidacionConcentradoList().get(0);
            dto.setConcentradoId(lc.getConcentradoId().getId());
            dto.setCodigoConcentrado(lc.getConcentradoId().getCodigoConcentrado());
        }

        return dto;
    }
}