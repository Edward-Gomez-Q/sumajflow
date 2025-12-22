package ucb.edu.bo.sumajflow.bl.comercializadora;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ucb.edu.bo.sumajflow.bl.NotificacionBl;
import ucb.edu.bo.sumajflow.bl.cooperativa.AuditoriaLotesBl;
import ucb.edu.bo.sumajflow.dto.comercializadora.LotePendienteComercializadoraDto;
import ucb.edu.bo.sumajflow.dto.cooperativa.TransportistaAsignadoDto;
import ucb.edu.bo.sumajflow.dto.ingenio.LoteAprobacionDestinoDto;
import ucb.edu.bo.sumajflow.dto.ingenio.LoteRechazoDestinoDto;
import ucb.edu.bo.sumajflow.entity.*;
import ucb.edu.bo.sumajflow.repository.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class LotesComercializadoraBl {

    // Repositorios
    private final LotesRepository lotesRepository;
    private final LoteComercializadoraRepository loteComercializadoraRepository;
    private final LoteMineralesRepository loteMineralesRepository;
    private final AsignacionCamionRepository asignacionCamionRepository;
    private final ComercializadoraRepository comercializadoraRepository;
    private final UsuariosRepository usuariosRepository;
    private final PersonaRepository personaRepository;
    private final NotificacionBl notificacionBl;
    private final AuditoriaLotesBl auditoriaLotesBl;

    // Constantes de estados
    private static final String ESTADO_PENDIENTE_DESTINO = "Pendiente de aprobación por Ingenio/Comercializadora";
    private static final String ESTADO_APROBADO = "Aprobado - Pendiente de iniciar";
    private static final String ESTADO_RECHAZADO = "Rechazado por destino";

    /**
     * Obtener lotes pendientes de aprobación por la comercializadora
     */
    @Transactional(readOnly = true)
    public List<LotePendienteComercializadoraDto> getLotesPendientesComercializadora(Integer usuarioId) {
        log.debug("Obteniendo lotes pendientes para comercializadora - Usuario ID: {}", usuarioId);

        // Obtener comercializadora del usuario
        Comercializadora comercializadora = obtenerComercializadoraDelUsuario(usuarioId);

        // Buscar lotes pendientes para esta comercializadora
        List<LoteComercializadora> lotesComercializadora = loteComercializadoraRepository.findAll().stream()
                .filter(lc -> lc.getComercializadoraId().getId().equals(comercializadora.getId()))
                .filter(lc -> lc.getEstado().equals(ESTADO_PENDIENTE_DESTINO))
                .collect(Collectors.toList());

        log.info("Se encontraron {} lotes pendientes para comercializadora", lotesComercializadora.size());

        return lotesComercializadora.stream()
                .map(this::convertToPendienteDto)
                .collect(Collectors.toList());
    }

    /**
     * Obtener detalle completo de un lote
     */
    @Transactional(readOnly = true)
    public LotePendienteComercializadoraDto getDetalleLote(Integer loteId, Integer usuarioId) {
        log.debug("Obteniendo detalle del lote ID: {} para comercializadora", loteId);

        // Validar permisos
        Comercializadora comercializadora = obtenerComercializadoraDelUsuario(usuarioId);
        LoteComercializadora loteComercializadora = obtenerLoteComercializadoraConPermisos(loteId, comercializadora);

        return convertToPendienteDto(loteComercializadora);
    }

    /**
     * Aprobar lote desde la comercializadora
     */
    @Transactional
    public LotePendienteComercializadoraDto aprobarLote(
            Integer loteId,
            LoteAprobacionDestinoDto aprobacionDto,
            Integer usuarioId,
            String ipOrigen,
            String metodoHttp,
            String endpoint
    ) {
        log.info("Aprobando lote ID: {} desde comercializadora", loteId);

        // 1. Validaciones
        Comercializadora comercializadora = obtenerComercializadoraDelUsuario(usuarioId);
        LoteComercializadora loteComercializadora = obtenerLoteComercializadoraConPermisos(loteId, comercializadora);
        Lotes lote = loteComercializadora.getLotesId();

        validarEstadoPendiente(loteComercializadora);

        String estadoAnterior = lote.getEstado();

        // 2. Actualizar estado del lote
        lote.setEstado(ESTADO_APROBADO);
        lote.setFechaAprobacionDestino(LocalDateTime.now());
        if (aprobacionDto.getObservaciones() != null) {
            lote.setObservaciones(lote.getObservaciones() != null
                    ? lote.getObservaciones() + " | Comercializadora: " + aprobacionDto.getObservaciones()
                    : "Comercializadora: " + aprobacionDto.getObservaciones());
        }
        lotesRepository.save(lote);

        // 3. Actualizar estado en lote_comercializadora
        loteComercializadora.setEstado("Aprobado");
        loteComercializadora.setFechaAprobacion(LocalDateTime.now());
        if (aprobacionDto.getObservaciones() != null) {
            loteComercializadora.setObservaciones(aprobacionDto.getObservaciones());
        }
        loteComercializadoraRepository.save(loteComercializadora);

        // 4. Registrar en auditoría de lotes
        registrarAuditoriaAprobacion(loteId, estadoAnterior, ESTADO_APROBADO, "comercializadora", ipOrigen);

        // 5. Notificar a cooperativa y socio
        notificarAprobacion(lote, comercializadora.getRazonSocial());

        log.info("Lote aprobado por comercializadora exitosamente - ID: {}", loteId);

        return convertToPendienteDto(loteComercializadora);
    }

    /**
     * Rechazar lote desde la comercializadora
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
        log.info("Rechazando lote ID: {} desde comercializadora", loteId);

        // 1. Validaciones
        Comercializadora comercializadora = obtenerComercializadoraDelUsuario(usuarioId);
        LoteComercializadora loteComercializadora = obtenerLoteComercializadoraConPermisos(loteId, comercializadora);
        Lotes lote = loteComercializadora.getLotesId();

        validarEstadoPendiente(loteComercializadora);

        if (rechazoDto.getMotivoRechazo() == null || rechazoDto.getMotivoRechazo().trim().isEmpty()) {
            throw new IllegalArgumentException("El motivo de rechazo es requerido");
        }

        String estadoAnterior = lote.getEstado();

        // 2. Actualizar estado del lote
        lote.setEstado(ESTADO_RECHAZADO);
        lote.setObservaciones(
                "RECHAZADO POR COMERCIALIZADORA: " + rechazoDto.getMotivoRechazo() +
                        (lote.getObservaciones() != null ? " | " + lote.getObservaciones() : "")
        );
        lotesRepository.save(lote);

        // 3. Actualizar estado en lote_comercializadora
        loteComercializadora.setEstado("Rechazado");
        loteComercializadora.setObservaciones(rechazoDto.getMotivoRechazo());
        loteComercializadoraRepository.save(loteComercializadora);

        // 4. Registrar en auditoría
        registrarAuditoriaRechazo(
                loteId,
                estadoAnterior,
                rechazoDto.getMotivoRechazo(),
                "comercializadora",
                ipOrigen
        );

        // 5. Notificar a cooperativa y socio
        notificarRechazo(lote, comercializadora.getRazonSocial(), rechazoDto.getMotivoRechazo());

        log.info("Lote rechazado por comercializadora - ID: {}", loteId);
    }

    // ==================== MÉTODOS DE VALIDACIÓN ====================

    private void validarEstadoPendiente(LoteComercializadora loteComercializadora) {
        if (!loteComercializadora.getEstado().equals(ESTADO_PENDIENTE_DESTINO)) {
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
                "comercializadora",
                estadoAnterior,
                estadoNuevo,
                "APROBAR_COMERCIALIZADORA",
                "Lote aprobado por la comercializadora",
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
                "comercializadora",
                estadoAnterior,
                ESTADO_RECHAZADO,
                "RECHAZAR_COMERCIALIZADORA",
                "Lote rechazado por la comercializadora",
                motivoRechazo,
                metadata,
                ipOrigen
        );
    }

    // ==================== MÉTODOS DE NOTIFICACIÓN ====================

    private void notificarAprobacion(Lotes lote, String nombreComercializadora) {
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
                        "' ha sido aprobado por " + nombreComercializadora + ". Ya puede iniciar el transporte.",
                metadata
        );

        // Notificar a la cooperativa
        Integer cooperativaUsuarioId = lote.getMinasId().getSectoresId()
                .getCooperativaId().getUsuariosId().getId();

        notificacionBl.crearNotificacion(
                cooperativaUsuarioId,
                "info",
                "Lote aprobado por comercializadora",
                "El lote ID " + lote.getId() + " ha sido aprobado por " + nombreComercializadora,
                metadata
        );
    }

    private void notificarRechazo(Lotes lote, String nombreComercializadora, String motivo) {
        // Notificar al socio
        Integer socioUsuarioId = lote.getMinasId().getSocioId().getUsuariosId().getId();

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("loteId", lote.getId());
        metadata.put("minaNombre", lote.getMinasId().getNombre());
        metadata.put("motivo", motivo);

        notificacionBl.crearNotificacion(
                socioUsuarioId,
                "error",
                "Lote rechazado por comercializadora",
                "Tu lote para la mina '" + lote.getMinasId().getNombre() +
                        "' ha sido rechazado por " + nombreComercializadora + ". Motivo: " + motivo,
                metadata
        );

        // Notificar a la cooperativa
        Integer cooperativaUsuarioId = lote.getMinasId().getSectoresId()
                .getCooperativaId().getUsuariosId().getId();

        notificacionBl.crearNotificacion(
                cooperativaUsuarioId,
                "warning",
                "Lote rechazado por comercializadora",
                "El lote ID " + lote.getId() + " ha sido rechazado por " + nombreComercializadora,
                metadata
        );
    }

    // ==================== MÉTODOS AUXILIARES ====================

    private Comercializadora obtenerComercializadoraDelUsuario(Integer usuarioId) {
        Usuarios usuario = usuariosRepository.findById(usuarioId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        return comercializadoraRepository.findByUsuariosId(usuario)
                .orElseThrow(() -> new IllegalArgumentException("Comercializadora no encontrada"));
    }

    private LoteComercializadora obtenerLoteComercializadoraConPermisos(Integer loteId, Comercializadora comercializadora) {
        Lotes lote = lotesRepository.findById(loteId)
                .orElseThrow(() -> new IllegalArgumentException("Lote no encontrado"));

        LoteComercializadora loteComercializadora = loteComercializadoraRepository.findByLotesId(lote)
                .orElseThrow(() -> new IllegalArgumentException("Relación lote-comercializadora no encontrada"));

        // Verificar permisos
        if (!loteComercializadora.getComercializadoraId().getId().equals(comercializadora.getId())) {
            throw new IllegalArgumentException("No tienes permiso para acceder a este lote");
        }

        return loteComercializadora;
    }

    // ==================== MÉTODOS DE CONVERSIÓN DTO ====================

    private LotePendienteComercializadoraDto convertToPendienteDto(LoteComercializadora loteComercializadora) {
        LotePendienteComercializadoraDto dto = new LotePendienteComercializadoraDto();
        Lotes lote = loteComercializadora.getLotesId();

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