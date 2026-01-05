package ucb.edu.bo.sumajflow.bl.cooperativa;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ucb.edu.bo.sumajflow.bl.NotificacionBl;
import ucb.edu.bo.sumajflow.dto.cooperativa.*;
import ucb.edu.bo.sumajflow.dto.socio.MineralInfoDto;
import ucb.edu.bo.sumajflow.entity.*;
import ucb.edu.bo.sumajflow.repository.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class LotesCooperativaBl {

    // Repositorios
    private final LotesRepository lotesRepository;
    private final LoteIngenioRepository loteIngenioRepository;
    private final LoteComercializadoraRepository loteComercializadoraRepository;
    private final LoteMineralesRepository loteMineralesRepository;
    private final AsignacionCamionRepository asignacionCamionRepository;
    private final TransportistaRepository transportistaRepository;
    private final CooperativaRepository cooperativaRepository;
    private final UsuariosRepository usuariosRepository;
    private final PersonaRepository personaRepository;
    private final IngenioMineroRepository ingenioMineroRepository;
    private final ComercializadoraRepository comercializadoraRepository;
    private final NotificacionBl notificacionBl;
    private final AuditoriaLotesBl auditoriaLotesBl;

    // Constantes de estados
    private static final String ESTADO_PENDIENTE_COOPERATIVA = "Pendiente de aprobación cooperativa";
    private static final String ESTADO_PENDIENTE_DESTINO = "Pendiente de aprobación por Ingenio/Comercializadora";
    private static final String ESTADO_RECHAZADO = "Rechazado";

    /**
     * Obtener lotes pendientes de aprobación por cooperativa
     */
    @Transactional(readOnly = true)
    public List<LotePendienteDto> getLotesPendientesCooperativa(Integer usuarioId) {
        log.debug("Obteniendo lotes pendientes para cooperativa - Usuario ID: {}", usuarioId);

        // Obtener cooperativa del usuario
        Cooperativa cooperativa = obtenerCooperativaDelUsuario(usuarioId);

        // Buscar lotes pendientes
        List<Lotes> lotes = lotesRepository.findAll().stream()
                .filter(l -> l.getEstado().equals(ESTADO_PENDIENTE_COOPERATIVA))
                .filter(l -> perteneceACooperativa(l, cooperativa))
                .collect(Collectors.toList());

        log.info("Se encontraron {} lotes pendientes", lotes.size());

        return lotes.stream()
                .map(this::convertToPendienteDto)
                .collect(Collectors.toList());
    }

    /**
     * Obtener detalle completo de un lote
     */
    @Transactional(readOnly = true)
    public LoteDetalleDto getDetalleLote(Integer loteId, Integer usuarioId) {
        log.debug("Obteniendo detalle del lote ID: {}", loteId);

        // Validar permisos
        Cooperativa cooperativa = obtenerCooperativaDelUsuario(usuarioId);
        Lotes lote = obtenerLoteConPermisos(loteId, cooperativa);

        return convertToDetalleDto(lote);
    }

    /**
     * Obtener transportistas disponibles para la cooperativa
     */
    @Transactional(readOnly = true)
    public List<TransportistaDisponibleDto> getTransportistasDisponibles(Integer usuarioId) {
        log.debug("Obteniendo transportistas disponibles");

        Cooperativa cooperativa = obtenerCooperativaDelUsuario(usuarioId);

        List<Transportista> transportistas = transportistaRepository
                .findDisponiblesByCooperativa(cooperativa.getId());

        log.info("Se encontraron {} transportistas disponibles", transportistas.size());

        return transportistas.stream()
                .map(this::convertToTransportistaDto)
                .collect(Collectors.toList());
    }

    /**
     * Aprobar lote y asignar transportistas
     */
    @Transactional
    public LoteDetalleDto aprobarLote(
            Integer loteId,
            LoteAprobacionDto aprobacionDto,
            Integer usuarioId,
            String ipOrigen,
            String metodoHttp,
            String endpoint
    ) {
        log.info("Aprobando lote ID: {}", loteId);

        // 1. Validaciones
        Cooperativa cooperativa = obtenerCooperativaDelUsuario(usuarioId);
        Lotes lote = obtenerLoteConPermisos(loteId, cooperativa);
        validarEstadoPendienteCooperativa(lote);
        validarAsignaciones(aprobacionDto, lote);

        String estadoAnterior = lote.getEstado();

        // 2. Asignar transportistas
        List<AsignacionCamion> asignaciones = asignarTransportistas(
                lote,
                aprobacionDto.getAsignaciones(),
                aprobacionDto.getFechaAsignacion()
        );

        // 3. Actualizar estado del lote
        lote.setEstado(ESTADO_PENDIENTE_DESTINO);
        lote.setFechaAprobacionCooperativa(LocalDateTime.now());
        if (aprobacionDto.getObservaciones() != null) {
            lote.setObservaciones(lote.getObservaciones() != null
                    ? lote.getObservaciones() + " | " + aprobacionDto.getObservaciones()
                    : aprobacionDto.getObservaciones());
        }
        lotesRepository.save(lote);

        // 4. Actualizar estado en lote_ingenio o lote_comercializadora
        actualizarEstadoDestino(lote, ESTADO_PENDIENTE_DESTINO);

        // 5. Registrar en auditoría de lotes
        auditoriaLotesBl.registrarAprobacionCooperativa(
                loteId,
                estadoAnterior,
                ESTADO_PENDIENTE_DESTINO,
                asignaciones.size(),
                ipOrigen
        );

        // 6. Registrar cada asignación en auditoría
        for (AsignacionCamion asignacion : asignaciones) {
            auditoriaLotesBl.registrarAsignacionTransportista(
                    loteId,
                    asignacion.getTransportistaId().getId(),
                    asignacion.getTransportistaId().getPlacaVehiculo(),
                    asignacion.getNumeroCamion(),
                    ipOrigen
            );
        }

        // 7. Notificar al socio
        notificarAprobacionAlSocio(lote);

        // 8. Notificar al destino (ingenio/comercializadora)
        notificarAlDestino(lote);

        log.info("Lote aprobado exitosamente - ID: {}", loteId);

        return convertToDetalleDto(lote);
    }

    /**
     * Rechazar lote
     */
    @Transactional
    public void rechazarLote(
            Integer loteId,
            LoteRechazoDto rechazoDto,
            Integer usuarioId,
            String ipOrigen,
            String metodoHttp,
            String endpoint
    ) {
        log.info("Rechazando lote ID: {}", loteId);

        // 1. Validaciones
        Cooperativa cooperativa = obtenerCooperativaDelUsuario(usuarioId);
        Lotes lote = obtenerLoteConPermisos(loteId, cooperativa);
        validarEstadoPendienteCooperativa(lote);

        if (rechazoDto.getMotivoRechazo() == null || rechazoDto.getMotivoRechazo().trim().isEmpty()) {
            throw new IllegalArgumentException("El motivo de rechazo es requerido");
        }

        String estadoAnterior = lote.getEstado();

        // 2. Actualizar estado del lote
        lote.setEstado(ESTADO_RECHAZADO);
        lote.setObservaciones(
                "RECHAZADO: " + rechazoDto.getMotivoRechazo() +
                        (lote.getObservaciones() != null ? " | " + lote.getObservaciones() : "")
        );
        lotesRepository.save(lote);

        // 3. Actualizar estado en lote_ingenio o lote_comercializadora
        actualizarEstadoDestino(lote, ESTADO_RECHAZADO);

        // 4. Registrar en auditoría
        auditoriaLotesBl.registrarRechazoCooperativa(
                loteId,
                estadoAnterior,
                rechazoDto.getMotivoRechazo(),
                ipOrigen
        );

        // 5. Notificar al socio
        notificarRechazoAlSocio(lote, rechazoDto.getMotivoRechazo());

        log.info("Lote rechazado - ID: {}", loteId);
    }

    // ==================== MÉTODOS DE VALIDACIÓN ====================

    private void validarEstadoPendienteCooperativa(Lotes lote) {
        if (!lote.getEstado().equals(ESTADO_PENDIENTE_COOPERATIVA)) {
            throw new IllegalArgumentException(
                    "El lote no está en estado pendiente de aprobación por cooperativa"
            );
        }
    }

    private void validarAsignaciones(LoteAprobacionDto aprobacionDto, Lotes lote) {
        if (aprobacionDto.getAsignaciones() == null || aprobacionDto.getAsignaciones().isEmpty()) {
            throw new IllegalArgumentException("Debe asignar al menos un transportista");
        }

        if (aprobacionDto.getAsignaciones().size() != lote.getCamionesSolicitados()) {
            throw new IllegalArgumentException(
                    "Debe asignar exactamente " + lote.getCamionesSolicitados() +
                            " transportista(s) según lo solicitado"
            );
        }

        if (aprobacionDto.getFechaAsignacion() == null) {
            throw new IllegalArgumentException("La fecha de asignación es requerida");
        }

        // Validar que no haya transportistas duplicados
        Set<Integer> transportistasIds = new HashSet<>();
        for (AsignacionTransportistaDto asignacion : aprobacionDto.getAsignaciones()) {
            if (!transportistasIds.add(asignacion.getTransportistaId())) {
                throw new IllegalArgumentException(
                        "No se puede asignar el mismo transportista más de una vez"
                );
            }

            // Validar que el transportista exista y esté disponible
            Transportista transportista = transportistaRepository.findById(asignacion.getTransportistaId())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Transportista ID " + asignacion.getTransportistaId() + " no encontrado"
                    ));

            if (!"aprobado".equals(transportista.getEstado())) {
                throw new IllegalArgumentException(
                        "El transportista " + asignacion.getTransportistaId() + " no está aprobado"
                );
            }
        }

        // Validar números de camión únicos y consecutivos
        Set<Integer> numerosCamion = new HashSet<>();
        for (AsignacionTransportistaDto asignacion : aprobacionDto.getAsignaciones()) {
            if (asignacion.getNumeroCamion() == null || asignacion.getNumeroCamion() <= 0) {
                throw new IllegalArgumentException("El número de camión debe ser mayor a 0");
            }

            if (!numerosCamion.add(asignacion.getNumeroCamion())) {
                throw new IllegalArgumentException(
                        "No se puede asignar el mismo número de camión más de una vez"
                );
            }
        }
    }

    // ==================== MÉTODOS DE ASIGNACIÓN ====================

    private List<AsignacionCamion> asignarTransportistas(
            Lotes lote,
            List<AsignacionTransportistaDto> asignacionesDto,
            java.time.LocalDate fechaAsignacion
    ) {
        List<AsignacionCamion> asignaciones = new ArrayList<>();

        for (AsignacionTransportistaDto dto : asignacionesDto) {
            Transportista transportista = transportistaRepository.findById(dto.getTransportistaId())
                    .orElseThrow(() -> new IllegalArgumentException("Transportista no encontrado"));

            AsignacionCamion asignacion = AsignacionCamion.builder()
                    .lotesId(lote)
                    .transportistaId(transportista)
                    .numeroCamion(dto.getNumeroCamion())
                    .estado("Esperando iniciar")
                    .fechaAsignacion(fechaAsignacion.atStartOfDay())
                    .build();

            asignaciones.add(asignacionCamionRepository.save(asignacion));
        }

        return asignaciones;
    }

    private void actualizarEstadoDestino(Lotes lote, String nuevoEstado) {
        if (lote.getTipoOperacion().equals("procesamiento_planta")) {
            LoteIngenio loteIngenio = loteIngenioRepository.findByLotesId(lote)
                    .orElseThrow(() -> new IllegalArgumentException("Relación lote-ingenio no encontrada"));

            loteIngenio.setEstado(nuevoEstado);
            loteIngenioRepository.save(loteIngenio);

        } else { // venta_directa
            LoteComercializadora loteComercializadora = loteComercializadoraRepository.findByLotesId(lote)
                    .orElseThrow(() -> new IllegalArgumentException("Relación lote-comercializadora no encontrada"));

            loteComercializadora.setEstado(nuevoEstado);
            loteComercializadoraRepository.save(loteComercializadora);
        }
    }

    // ==================== MÉTODOS DE NOTIFICACIÓN ====================

    private void notificarAprobacionAlSocio(Lotes lote) {
        Integer socioUsuarioId = lote.getMinasId().getSocioId().getUsuariosId().getId();

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("loteId", lote.getId());
        metadata.put("minaNombre", lote.getMinasId().getNombre());
        metadata.put("estado", lote.getEstado());

        notificacionBl.crearNotificacion(
                socioUsuarioId,
                "success",
                "Lote aprobado por cooperativa",
                "Tu lote para la mina '" + lote.getMinasId().getNombre() +
                        "' ha sido aprobado por la cooperativa. Ahora está pendiente de aprobación por el destino.",
                metadata
        );
    }

    private void notificarRechazoAlSocio(Lotes lote, String motivo) {
        Integer socioUsuarioId = lote.getMinasId().getSocioId().getUsuariosId().getId();

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("loteId", lote.getId());
        metadata.put("minaNombre", lote.getMinasId().getNombre());
        metadata.put("motivo", motivo);

        notificacionBl.crearNotificacion(
                socioUsuarioId,
                "error",
                "Lote rechazado",
                "Tu lote para la mina '" + lote.getMinasId().getNombre() +
                        "' ha sido rechazado. Motivo: " + motivo,
                metadata
        );
    }

    private void notificarAlDestino(Lotes lote) {
        Integer destinoUsuarioId = null;

        if("procesamiento_planta".equals(lote.getTipoOperacion())) {
            LoteIngenio loteIngenio = loteIngenioRepository.findByLotesId(lote)
                    .orElseThrow(() -> new IllegalArgumentException("Relación lote-ingenio no encontrada"));
            destinoUsuarioId = loteIngenio.getIngenioMineroId().getUsuariosId().getId();

        } else {
            LoteComercializadora loteComercializadora = loteComercializadoraRepository.findByLotesId(lote)
                    .orElseThrow(() -> new IllegalArgumentException("Relación lote-comercializadora no encontrada"));
            destinoUsuarioId = loteComercializadora.getComercializadoraId().getUsuariosId().getId();
        }

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("loteId", lote.getId());
        metadata.put("minaNombre", lote.getMinasId().getNombre());
        metadata.put("camionlesSolicitados", lote.getCamionesSolicitados());

        notificacionBl.crearNotificacion(
                destinoUsuarioId,
                "info",
                "Nuevo lote pendiente de aprobación",
                "Hay un nuevo lote pendiente de aprobación",
                metadata
        );
    }

    // ==================== MÉTODOS AUXILIARES ====================

    private Cooperativa obtenerCooperativaDelUsuario(Integer usuarioId) {
        Usuarios usuario = usuariosRepository.findById(usuarioId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        return cooperativaRepository.findByUsuariosId(usuario)
                .orElseThrow(() -> new IllegalArgumentException("Cooperativa no encontrada"));
    }

    private Lotes obtenerLoteConPermisos(Integer loteId, Cooperativa cooperativa) {
        Lotes lote = lotesRepository.findById(loteId)
                .orElseThrow(() -> new IllegalArgumentException("Lote no encontrado"));

        if (!perteneceACooperativa(lote, cooperativa)) {
            throw new IllegalArgumentException("No tienes permiso para acceder a este lote");
        }

        return lote;
    }

    private boolean perteneceACooperativa(Lotes lote, Cooperativa cooperativa) {
        // El lote pertenece a la cooperativa si la mina del lote pertenece a un socio de esa cooperativa
        Integer cooperativaIdDelLote = lote.getMinasId().getSectoresId().getCooperativaId().getId();
        return cooperativaIdDelLote.equals(cooperativa.getId());
    }

    // ==================== MÉTODOS DE CONVERSIÓN DTO ====================

    private LotePendienteDto convertToPendienteDto(Lotes lote) {
        LotePendienteDto dto = new LotePendienteDto();

        dto.setId(lote.getId());
        dto.setMinaNombre(lote.getMinasId().getNombre());

        // Obtener nombre del socio
        Persona persona = personaRepository.findByUsuariosId(lote.getMinasId().getSocioId().getUsuariosId())
                .orElse(null);
        if (persona != null) {
            dto.setSocioNombre(persona.getNombres() + " " + persona.getPrimerApellido());
            dto.setSocioCi(persona.getCi());
        }

        // Obtener minerales
        List<LoteMinerales> loteMinerales = loteMineralesRepository.findByLotesId(lote);
        dto.setMinerales(
                loteMinerales.stream()
                        .map(lm -> lm.getMineralesId().getNombre())
                        .collect(Collectors.toList())
        );

        dto.setCamionlesSolicitados(lote.getCamionesSolicitados());
        dto.setTipoOperacion(lote.getTipoOperacion());
        dto.setTipoMineral(lote.getTipoMineral());

        // Obtener destino
        if (lote.getTipoOperacion().equals("procesamiento_planta")) {
            LoteIngenio loteIngenio = loteIngenioRepository.findByLotesId(lote).orElse(null);
            if (loteIngenio != null) {
                dto.setDestinoNombre(loteIngenio.getIngenioMineroId().getRazonSocial());
                dto.setDestinoTipo("Ingenio Minero");
            }
        } else {
            LoteComercializadora loteComercializadora = loteComercializadoraRepository.findByLotesId(lote).orElse(null);
            if (loteComercializadora != null) {
                dto.setDestinoNombre(loteComercializadora.getComercializadoraId().getRazonSocial());
                dto.setDestinoTipo("Comercializadora");
            }
        }

        dto.setPesoTotalEstimado(lote.getPesoTotalEstimado());
        dto.setEstado(lote.getEstado());
        dto.setFechaCreacion(lote.getFechaCreacion());
        dto.setObservaciones(lote.getObservaciones());

        return dto;
    }

    private LoteDetalleDto convertToDetalleDto(Lotes lote) {
        LoteDetalleDto dto = new LoteDetalleDto();

        // Información del lote
        dto.setId(lote.getId());
        dto.setEstado(lote.getEstado());
        dto.setFechaCreacion(lote.getFechaCreacion());
        dto.setFechaAprobacionCooperativa(lote.getFechaAprobacionCooperativa());
        dto.setFechaAprobacionDestino(lote.getFechaAprobacionDestino());
        dto.setPesoTotalEstimado(lote.getPesoTotalEstimado());
        dto.setObservaciones(lote.getObservaciones());

        // Información de la mina
        Minas mina = lote.getMinasId();
        dto.setMinaId(mina.getId());
        dto.setMinaNombre(mina.getNombre());
        dto.setMinaLatitud(mina.getLatitud());
        dto.setMinaLongitud(mina.getLongitud());
        dto.setSectorNombre(mina.getSectoresId().getNombre());

        // Información del socio
        Socio socio = mina.getSocioId();
        dto.setSocioId(socio.getId());

        Persona persona = personaRepository.findByUsuariosId(socio.getUsuariosId()).orElse(null);
        if (persona != null) {
            dto.setSocioNombre(persona.getNombres() + " " + persona.getPrimerApellido());
            dto.setSocioCi(persona.getCi());
            dto.setSocioTelefono(persona.getNumeroCelular());
        }

        // Información de minerales
        List<LoteMinerales> loteMinerales = loteMineralesRepository.findByLotesId(lote);
        dto.setMinerales(
                loteMinerales.stream()
                        .map(lm -> new MineralInfoDto(
                                lm.getMineralesId().getId(),
                                lm.getMineralesId().getNombre(),
                                lm.getMineralesId().getNomenclatura()
                        ))
                        .collect(Collectors.toList())
        );

        // Información de operación
        dto.setCamionlesSolicitados(lote.getCamionesSolicitados());
        dto.setTipoOperacion(lote.getTipoOperacion());
        dto.setTipoMineral(lote.getTipoMineral());

        // Información del destino
        if (lote.getTipoOperacion().equals("procesamiento_planta")) {
            LoteIngenio loteIngenio = loteIngenioRepository.findByLotesId(lote).orElse(null);
            if (loteIngenio != null) {
                IngenioMinero ingenio = loteIngenio.getIngenioMineroId();
                dto.setDestinoId(ingenio.getId());
                dto.setDestinoNombre(ingenio.getRazonSocial());
                dto.setDestinoTipo("ingenio");
                dto.setDestinoNit(ingenio.getNit());
                dto.setDestinoContacto(ingenio.getCorreoContacto());
                dto.setDestinoDireccion(ingenio.getDireccion());
            }
        } else {
            LoteComercializadora loteComercializadora = loteComercializadoraRepository.findByLotesId(lote).orElse(null);
            if (loteComercializadora != null) {
                Comercializadora comercializadora = loteComercializadora.getComercializadoraId();
                dto.setDestinoId(comercializadora.getId());
                dto.setDestinoNombre(comercializadora.getRazonSocial());
                dto.setDestinoTipo("comercializadora");
                dto.setDestinoNit(comercializadora.getNit());
                dto.setDestinoContacto(comercializadora.getCorreoContacto());
                dto.setDestinoDireccion(comercializadora.getDireccion());
            }
        }

        return dto;
    }

    private TransportistaDisponibleDto convertToTransportistaDto(Transportista transportista) {
        TransportistaDisponibleDto dto = new TransportistaDisponibleDto();

        dto.setId(transportista.getId());

        // Obtener nombre del transportista
        Persona persona = personaRepository.findByUsuariosId(transportista.getUsuariosId()).orElse(null);
        if (persona != null) {
            dto.setNombreCompleto(persona.getNombres() + " " + persona.getPrimerApellido());
        } else {
            dto.setNombreCompleto("Transportista #" + transportista.getId());
        }

        dto.setCi(transportista.getCi());
        dto.setPlacaVehiculo(transportista.getPlacaVehiculo());
        dto.setMarcaVehiculo(transportista.getMarcaVehiculo());
        dto.setModeloVehiculo(transportista.getModeloVehiculo());
        dto.setCapacidadCarga(transportista.getCapacidadCarga());
        dto.setPesoTara(transportista.getPesoTara());
        dto.setViajesCompletados(transportista.getViajesCompletados());
        dto.setCalificacionPromedio(transportista.getCalificacionPromedio());
        dto.setEstado(transportista.getEstado());

        return dto;
    }
}