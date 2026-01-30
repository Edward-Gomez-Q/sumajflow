package ucb.edu.bo.sumajflow.bl.socio;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ucb.edu.bo.sumajflow.bl.ConcentradoBl;
import ucb.edu.bo.sumajflow.bl.NotificacionBl;
import ucb.edu.bo.sumajflow.dto.ingenio.*;
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

    // Repositorios específicos
    private final UsuariosRepository usuariosRepository;
    private final SocioRepository socioRepository;
    private final ConcentradoRepository concentradoRepository;
    private final ComercializadoraRepository comercializadoraRepository;
    private final LiquidacionRepository liquidacionRepository;
    private final PersonaRepository personaRepository;

    // Servicios
    private final ConcentradoBl concentradoBl;
    private final NotificacionBl notificacionBl;

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

        // Obtener concentrados del socio
        List<Concentrado> concentradosSocio = concentradoRepository
                .findBySocioPropietarioIdOrderByCreatedAtDesc(socio);

        // Usar mEtodo común del BL base para filtrar y paginar
        return concentradoBl.listarConcentradosConFiltros(
                concentradosSocio,
                estado,
                mineralPrincipal,
                fechaDesde,
                fechaHasta,
                page,
                size
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

        return concentradoBl.convertirAResponseDto(concentrado);
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

        return concentradoBl.construirProcesosResponseDto(concentrado);
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

        // Validar estado usando BL base
        concentradoBl.validarEstado(concentrado, "listo_para_liquidacion");

        // Transicionar usando BL base
        concentradoBl.transicionarEstado(
                concentrado,
                "liquidacion_servicio_solicitada",
                "Socio solicitó liquidación del servicio de procesamiento",
                solicitudDto.getObservaciones(),
                usuarioId,
                ipOrigen
        );

        // Registrar auditoría adicional
        List<Map<String, Object>> historial = concentradoBl.obtenerHistorial(concentrado);
        Map<String, Object> registro = new HashMap<>();
        registro.put("accion", "SOLICITAR_LIQUIDACION_SERVICIO");
        registro.put("descripcion", "Socio solicitó liquidación de servicio de procesamiento");
        registro.put("socio_id", socio.getId());
        registro.put("usuario_id", usuarioId);
        registro.put("ip_origen", ipOrigen);
        registro.put("timestamp", LocalDateTime.now().toString());
        historial.add(registro);
        concentrado.setObservaciones(concentradoBl.convertirHistorialAJson(historial));
        concentradoRepository.save(concentrado);

        // Notificación y WebSocket
        notificarSolicitudLiquidacionServicio(concentrado, socio);
        concentradoBl.publicarEventoWebSocket(concentrado, "liquidacion_servicio_solicitada");

        log.info("Liquidación de servicio solicitada - Concentrado ID: {}", concentradoId);

        return concentradoBl.convertirAResponseDto(concentrado);
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
        concentradoBl.validarEstado(concentrado, "listo_para_venta");

        // Validar que la comercializadora exista
        Comercializadora comercializadora = comercializadoraRepository.findById(solicitudDto.getComercializadoraId())
                .orElseThrow(() -> new IllegalArgumentException("Comercializadora no encontrada"));

        // Transicionar
        concentradoBl.transicionarEstado(
                concentrado,
                "venta_solicitada",
                "Socio solicitó venta a comercializadora: " + comercializadora.getRazonSocial(),
                solicitudDto.getObservaciones(),
                usuarioId,
                ipOrigen
        );

        // Registrar auditoría
        List<Map<String, Object>> historial = concentradoBl.obtenerHistorial(concentrado);
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
        concentrado.setObservaciones(concentradoBl.convertirHistorialAJson(historial));
        concentradoRepository.save(concentrado);

        // Notificación y WebSocket
        notificarSolicitudVenta(concentrado, comercializadora, socio);
        concentradoBl.publicarEventoWebSocket(concentrado, "venta_solicitada");

        log.info("Venta de concentrado solicitada - Concentrado ID: {}", concentradoId);

        return concentradoBl.convertirAResponseDto(concentrado);
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
        Concentrado concentrado = concentradoBl.obtenerConcentrado(concentradoId);

        // Validar que el concentrado pertenezca al socio
        if (!concentrado.getSocioPropietarioId().getId().equals(socio.getId())) {
            throw new IllegalArgumentException("No tienes permiso para acceder a este concentrado");
        }

        return concentrado;
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

        // Información del socio
        dto.setSocioId(liquidacion.getSocioId().getId());
        Persona persona = personaRepository.findByUsuariosId(liquidacion.getSocioId().getUsuariosId()).orElse(null);
        if (persona != null) {
            dto.setSocioNombres(persona.getNombres());
            dto.setSocioApellidos(persona.getPrimerApellido() +
                    (persona.getSegundoApellido() != null ? " " + persona.getSegundoApellido() : ""));
        }

        return dto;
    }
}