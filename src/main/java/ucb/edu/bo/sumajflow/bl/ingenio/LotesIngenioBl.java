package ucb.edu.bo.sumajflow.bl.ingenio;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ucb.edu.bo.sumajflow.bl.NotificacionBl;
import ucb.edu.bo.sumajflow.bl.cooperativa.AuditoriaLotesBl;
import ucb.edu.bo.sumajflow.dto.cooperativa.TransportistaAsignadoDto;
import ucb.edu.bo.sumajflow.dto.ingenio.LoteAprobacionDestinoDto;
import ucb.edu.bo.sumajflow.dto.ingenio.LotePendienteIngenioDto;
import ucb.edu.bo.sumajflow.dto.ingenio.LoteRechazoDestinoDto;
import ucb.edu.bo.sumajflow.entity.*;
import ucb.edu.bo.sumajflow.repository.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class LotesIngenioBl {

    // Repositorios
    private final LotesRepository lotesRepository;
    private final LoteIngenioRepository loteIngenioRepository;
    private final LoteMineralesRepository loteMineralesRepository;
    private final AsignacionCamionRepository asignacionCamionRepository;
    private final IngenioMineroRepository ingenioMineroRepository;
    private final UsuariosRepository usuariosRepository;
    private final PersonaRepository personaRepository;
    private final NotificacionBl notificacionBl;
    private final AuditoriaLotesBl auditoriaLotesBl;

    // Constantes de estados
    private static final String ESTADO_PENDIENTE_DESTINO = "Pendiente de aprobación por Ingenio/Comercializadora";
    private static final String ESTADO_APROBADO = "Aprobado - Pendiente de iniciar";
    private static final String ESTADO_RECHAZADO = "Rechazado por destino";

    /**
     * Obtener lotes pendientes de aprobación por el ingenio
     */
    @Transactional(readOnly = true)
    public List<LotePendienteIngenioDto> getLotesPendientesIngenio(Integer usuarioId) {
        log.debug("Obteniendo lotes pendientes para ingenio - Usuario ID: {}", usuarioId);

        // Obtener ingenio del usuario
        IngenioMinero ingenio = obtenerIngenioDelUsuario(usuarioId);

        // Buscar lotes pendientes para este ingenio
        List<LoteIngenio> lotesIngenio = loteIngenioRepository.findAll().stream()
                .filter(li -> li.getIngenioMineroId().getId().equals(ingenio.getId()))
                .filter(li -> li.getEstado().equals(ESTADO_PENDIENTE_DESTINO))
                .collect(Collectors.toList());

        log.info("Se encontraron {} lotes pendientes para ingenio", lotesIngenio.size());

        return lotesIngenio.stream()
                .map(this::convertToPendienteDto)
                .collect(Collectors.toList());
    }

    /**
     * Obtener detalle completo de un lote
     */
    @Transactional(readOnly = true)
    public LotePendienteIngenioDto getDetalleLote(Integer loteId, Integer usuarioId) {
        log.debug("Obteniendo detalle del lote ID: {} para ingenio", loteId);

        // Validar permisos
        IngenioMinero ingenio = obtenerIngenioDelUsuario(usuarioId);
        LoteIngenio loteIngenio = obtenerLoteIngenioConPermisos(loteId, ingenio);

        return convertToPendienteDto(loteIngenio);
    }

    /**
     * Aprobar lote desde el ingenio
     */
    @Transactional
    public LotePendienteIngenioDto aprobarLote(
            Integer loteId,
            LoteAprobacionDestinoDto aprobacionDto,
            Integer usuarioId,
            String ipOrigen,
            String metodoHttp,
            String endpoint
    ) {
        log.info("Aprobando lote ID: {} desde ingenio", loteId);

        // 1. Validaciones
        IngenioMinero ingenio = obtenerIngenioDelUsuario(usuarioId);
        LoteIngenio loteIngenio = obtenerLoteIngenioConPermisos(loteId, ingenio);
        Lotes lote = loteIngenio.getLotesId();

        validarEstadoPendiente(loteIngenio);

        String estadoAnterior = lote.getEstado();

        // 2. Actualizar estado del lote
        lote.setEstado(ESTADO_APROBADO);
        lote.setFechaAprobacionDestino(LocalDateTime.now());
        if (aprobacionDto.getObservaciones() != null) {
            lote.setObservaciones(lote.getObservaciones() != null
                    ? lote.getObservaciones() + " | Ingenio: " + aprobacionDto.getObservaciones()
                    : "Ingenio: " + aprobacionDto.getObservaciones());
        }
        lotesRepository.save(lote);

        // 3. Actualizar estado en lote_ingenio
        loteIngenio.setEstado("Aprobado");
        loteIngenio.setFechaAprobacion(LocalDateTime.now());
        if (aprobacionDto.getObservaciones() != null) {
            loteIngenio.setObservaciones(aprobacionDto.getObservaciones());
        }
        loteIngenioRepository.save(loteIngenio);

        // 4. Registrar en auditoría de lotes
        registrarAuditoriaAprobacion(loteId, estadoAnterior, ESTADO_APROBADO, "ingenio", ipOrigen);

        // 5. Notificar a cooperativa y socio
        notificarAprobacion(lote, ingenio.getRazonSocial());

        log.info("Lote aprobado por ingenio exitosamente - ID: {}", loteId);

        return convertToPendienteDto(loteIngenio);
    }

    /**
     * Rechazar lote desde el ingenio
     */
    @Transactional
    public void rechazarLote(
            Integer loteId,
            LoteRechazoDestinoDto rechazoDto,
            Integer usuarioId,
            String ipOrigen,
            String metodoHttp,
            String endpoint
    ) {
        log.info("Rechazando lote ID: {} desde ingenio", loteId);

        // 1. Validaciones
        IngenioMinero ingenio = obtenerIngenioDelUsuario(usuarioId);
        LoteIngenio loteIngenio = obtenerLoteIngenioConPermisos(loteId, ingenio);
        Lotes lote = loteIngenio.getLotesId();

        validarEstadoPendiente(loteIngenio);

        if (rechazoDto.getMotivoRechazo() == null || rechazoDto.getMotivoRechazo().trim().isEmpty()) {
            throw new IllegalArgumentException("El motivo de rechazo es requerido");
        }

        String estadoAnterior = lote.getEstado();

        // 2. Actualizar estado del lote
        lote.setEstado(ESTADO_RECHAZADO);
        lote.setObservaciones(
                "RECHAZADO POR INGENIO: " + rechazoDto.getMotivoRechazo() +
                        (lote.getObservaciones() != null ? " | " + lote.getObservaciones() : "")
        );
        lotesRepository.save(lote);

        // 3. Actualizar estado en lote_ingenio
        loteIngenio.setEstado("Rechazado");
        loteIngenio.setObservaciones(rechazoDto.getMotivoRechazo());
        loteIngenioRepository.save(loteIngenio);

        // 4. Registrar en auditoría
        registrarAuditoriaRechazo(
                loteId,
                estadoAnterior,
                rechazoDto.getMotivoRechazo(),
                "ingenio",
                ipOrigen
        );

        // 5. Notificar a cooperativa y socio
        notificarRechazo(lote, ingenio.getRazonSocial(), rechazoDto.getMotivoRechazo());

        log.info("Lote rechazado por ingenio - ID: {}", loteId);
    }

    // ==================== MÉTODOS DE VALIDACIÓN ====================

    private void validarEstadoPendiente(LoteIngenio loteIngenio) {
        if (!loteIngenio.getEstado().equals(ESTADO_PENDIENTE_DESTINO)) {
            throw new IllegalArgumentException(
                    "El lote no está en estado pendiente de aprobación"
            );
        }
    }

    // ==================== MÉTODOS DE AUDITORÍA ====================

    private void registrarAuditoriaAprobacion(
            Integer loteId,
            String estadoAnterior,
            String estadoNuevo,
            String tipoDestino,
            String ipOrigen
    ) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("tipo_destino", tipoDestino);
        metadata.put("tipo_accion", "aprobacion_destino");

        auditoriaLotesBl.registrarAuditoria(
                loteId,
                "ingenio",
                estadoAnterior,
                estadoNuevo,
                "APROBAR_INGENIO",
                "Lote aprobado por el ingenio minero",
                null,
                metadata,
                ipOrigen
        );
    }

    private void registrarAuditoriaRechazo(
            Integer loteId,
            String estadoAnterior,
            String motivoRechazo,
            String tipoDestino,
            String ipOrigen
    ) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("tipo_destino", tipoDestino);
        metadata.put("motivo_rechazo", motivoRechazo);
        metadata.put("tipo_accion", "rechazo_destino");

        auditoriaLotesBl.registrarAuditoria(
                loteId,
                "ingenio",
                estadoAnterior,
                ESTADO_RECHAZADO,
                "RECHAZAR_INGENIO",
                "Lote rechazado por el ingenio minero",
                motivoRechazo,
                metadata,
                ipOrigen
        );
    }

    // ==================== MÉTODOS DE NOTIFICACIÓN ====================

    private void notificarAprobacion(Lotes lote, String nombreIngenio) {
        // Notificar al socio
        Integer socioUsuarioId = lote.getMinasId().getSocioId().getUsuariosId().getId();

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("loteId", lote.getId());
        metadata.put("minaNombre", lote.getMinasId().getNombre());
        metadata.put("estado", lote.getEstado());

        notificacionBl.crearNotificacion(
                socioUsuarioId,
                "success",
                "Lote aprobado completamente",
                "Tu lote para la mina '" + lote.getMinasId().getNombre() +
                        "' ha sido aprobado por " + nombreIngenio + ". Ya puede iniciar el transporte.",
                metadata
        );

        // Notificar a la cooperativa
        Integer cooperativaUsuarioId = lote.getMinasId().getSectoresId()
                .getCooperativaId().getUsuariosId().getId();

        notificacionBl.crearNotificacion(
                cooperativaUsuarioId,
                "info",
                "Lote aprobado por ingenio",
                "El lote ID " + lote.getId() + " ha sido aprobado por " + nombreIngenio,
                metadata
        );
    }

    private void notificarRechazo(Lotes lote, String nombreIngenio, String motivo) {
        // Notificar al socio
        Integer socioUsuarioId = lote.getMinasId().getSocioId().getUsuariosId().getId();

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("loteId", lote.getId());
        metadata.put("minaNombre", lote.getMinasId().getNombre());
        metadata.put("motivo", motivo);

        notificacionBl.crearNotificacion(
                socioUsuarioId,
                "error",
                "Lote rechazado por ingenio",
                "Tu lote para la mina '" + lote.getMinasId().getNombre() +
                        "' ha sido rechazado por " + nombreIngenio + ". Motivo: " + motivo,
                metadata
        );

        // Notificar a la cooperativa
        Integer cooperativaUsuarioId = lote.getMinasId().getSectoresId()
                .getCooperativaId().getUsuariosId().getId();

        notificacionBl.crearNotificacion(
                cooperativaUsuarioId,
                "warning",
                "Lote rechazado por ingenio",
                "El lote ID " + lote.getId() + " ha sido rechazado por " + nombreIngenio,
                metadata
        );
    }

    // ==================== MÉTODOS AUXILIARES ====================

    private IngenioMinero obtenerIngenioDelUsuario(Integer usuarioId) {
        Usuarios usuario = usuariosRepository.findById(usuarioId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        return ingenioMineroRepository.findByUsuariosId(usuario)
                .orElseThrow(() -> new IllegalArgumentException("Ingenio minero no encontrado"));
    }

    private LoteIngenio obtenerLoteIngenioConPermisos(Integer loteId, IngenioMinero ingenio) {
        Lotes lote = lotesRepository.findById(loteId)
                .orElseThrow(() -> new IllegalArgumentException("Lote no encontrado"));

        LoteIngenio loteIngenio = loteIngenioRepository.findByLotesId(lote)
                .orElseThrow(() -> new IllegalArgumentException("Relación lote-ingenio no encontrada"));

        // Verificar permisos
        if (!loteIngenio.getIngenioMineroId().getId().equals(ingenio.getId())) {
            throw new IllegalArgumentException("No tienes permiso para acceder a este lote");
        }

        return loteIngenio;
    }

    // ==================== MÉTODOS DE CONVERSIÓN DTO ====================

    private LotePendienteIngenioDto convertToPendienteDto(LoteIngenio loteIngenio) {
        LotePendienteIngenioDto dto = new LotePendienteIngenioDto();
        Lotes lote = loteIngenio.getLotesId();

        dto.setId(lote.getId());
        dto.setMinaNombre(lote.getMinasId().getNombre());

        // Cooperativa
        dto.setCooperativaNombre(
                lote.getMinasId().getSectoresId().getCooperativaId().getRazonSocial()
        );

        // Socio
        Persona persona = personaRepository.findByUsuariosId(
                lote.getMinasId().getSocioId().getUsuariosId()
        ).orElse(null);
        if (persona != null) {
            dto.setSocioNombre(persona.getNombres() + " " + persona.getPrimerApellido());
            dto.setSocioCi(persona.getCi());
            dto.setSocioTelefono(persona.getNumeroCelular());
        }

        // Minerales
        List<LoteMinerales> loteMinerales = loteMineralesRepository.findByLotesId(lote);
        dto.setMinerales(
                loteMinerales.stream()
                        .map(lm -> lm.getMineralesId().getNombre())
                        .collect(Collectors.toList())
        );

        dto.setCamionlesSolicitados(lote.getCamionesSolicitados());
        dto.setTipoMineral(lote.getTipoMineral());
        dto.setPesoTotalEstimado(lote.getPesoTotalEstimado());
        dto.setEstado(lote.getEstado());
        dto.setFechaCreacion(lote.getFechaCreacion());
        dto.setFechaAprobacionCooperativa(lote.getFechaAprobacionCooperativa());
        dto.setObservaciones(lote.getObservaciones());

        // Transportistas asignados
        List<AsignacionCamion> asignaciones = asignacionCamionRepository.findByLotesId(lote);
        dto.setTransportistasAsignados(
                asignaciones.stream()
                        .map(this::convertToTransportistaAsignadoDto)
                        .collect(Collectors.toList())
        );

        if (!asignaciones.isEmpty()) {
            dto.setFechaAsignacionTransporte(asignaciones.get(0).getFechaAsignacion());
        }

        return dto;
    }

    private TransportistaAsignadoDto convertToTransportistaAsignadoDto(AsignacionCamion asignacion) {
        TransportistaAsignadoDto dto = new TransportistaAsignadoDto();

        dto.setAsignacionId(asignacion.getId());
        dto.setTransportistaId(asignacion.getTransportistaId().getId());

        // Obtener nombre del transportista
        Persona persona = personaRepository.findByUsuariosId(
                asignacion.getTransportistaId().getUsuariosId()
        ).orElse(null);
        if (persona != null) {
            dto.setNombreCompleto(persona.getNombres() + " " + persona.getPrimerApellido());
        } else {
            dto.setNombreCompleto("Transportista #" + asignacion.getTransportistaId().getId());
        }

        dto.setPlacaVehiculo(asignacion.getTransportistaId().getPlacaVehiculo());
        dto.setNumeroCamion(asignacion.getNumeroCamion());
        dto.setEstado(asignacion.getEstado());

        return dto;
    }
}