package ucb.edu.bo.sumajflow.bl.cooperativa;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ucb.edu.bo.sumajflow.bl.LotesWebSocketBl;
import ucb.edu.bo.sumajflow.bl.NotificacionBl;
import ucb.edu.bo.sumajflow.dto.cooperativa.*;
import ucb.edu.bo.sumajflow.dto.socio.AsignacionCamionSimpleDto;
import ucb.edu.bo.sumajflow.dto.socio.AuditoriaLoteDto;
import ucb.edu.bo.sumajflow.dto.socio.LoteDetalleDto;
import ucb.edu.bo.sumajflow.dto.socio.MineralInfoDto;
import ucb.edu.bo.sumajflow.entity.*;
import ucb.edu.bo.sumajflow.repository.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

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
    private final AuditoriaLotesRepository auditoriaLotesRepository;
    private final AsignacionCamionRepository asignacionCamionRepo;
    private final ObjectMapper objectMapper;
    private final LotesWebSocketBl lotesWebSocketBl;

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
                .toList();

        log.info("Se encontraron {} lotes pendientes", lotes.size());

        return lotes.stream()
                .map(this::convertToPendienteDto)
                .collect(Collectors.toList());
    }

    /**
     * Obtener detalle completo de un lote - AHORA USA LoteDetalleDto COMPARTIDO
     */
    @Transactional(readOnly = true)
    public LoteDetalleDto getDetalleLote(Integer loteId, Integer usuarioId) {
        log.debug("Obteniendo detalle del lote ID: {}", loteId);

        // Validar permisos
        Cooperativa cooperativa = obtenerCooperativaDelUsuario(usuarioId);
        Lotes lote = obtenerLoteConPermisos(loteId, cooperativa);

        return convertToDetalleDto(lote, cooperativa);
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
     * Obtener todos los lotes de la cooperativa con filtros y paginación
     */
    @Transactional(readOnly = true)
    public LotesCooperativaPaginadosDto getLotesCooperativaPaginados(
            Integer usuarioId,
            LoteFiltrosCooperativaDto filtros
    ) {
        log.debug("Obteniendo lotes paginados para cooperativa - Usuario ID: {} con filtros: {}", usuarioId, filtros);

        // Obtener cooperativa del usuario
        Cooperativa cooperativa = obtenerCooperativaDelUsuario(usuarioId);

        // Normalizar filtros (convertir cadenas vacías a null para PostgreSQL)
        String estado = (filtros.getEstado() != null && !filtros.getEstado().trim().isEmpty())
                ? filtros.getEstado() : null;
        String tipoOperacion = (filtros.getTipoOperacion() != null && !filtros.getTipoOperacion().trim().isEmpty())
                ? filtros.getTipoOperacion() : null;
        String tipoMineral = (filtros.getTipoMineral() != null && !filtros.getTipoMineral().trim().isEmpty())
                ? filtros.getTipoMineral() : null;
        Integer socioId = (filtros.getSocioId() != null && filtros.getSocioId() > 0)
                ? filtros.getSocioId() : null;
        Integer minaId = (filtros.getMinaId() != null && filtros.getMinaId() > 0)
                ? filtros.getMinaId() : null;
        Integer sectorId = (filtros.getSectorId() != null && filtros.getSectorId() > 0)
                ? filtros.getSectorId() : null;

        // Convertir sortBy de camelCase a snake_case para query nativa
        String sortByField = convertirCamelCaseASnakeCase(filtros.getSortBy());

        // Crear Pageable con ordenamiento
        Sort sort = filtros.getSortDir().equalsIgnoreCase("asc")
                ? Sort.by(sortByField).ascending()
                : Sort.by(sortByField).descending();

        Pageable pageable = PageRequest.of(filtros.getPage(), filtros.getSize(), sort);

        // Obtener lotes con filtros
        Page<Lotes> lotesPage = lotesRepository.findLotesByCooperativaWithFilters(
                cooperativa.getId(),
                estado,
                tipoOperacion,
                tipoMineral,
                filtros.getFechaDesde(),
                filtros.getFechaHasta(),
                socioId,
                minaId,
                sectorId,
                pageable
        );

        log.info("Se encontraron {} lotes en la página {} de {} para cooperativa ID: {}",
                lotesPage.getNumberOfElements(),
                lotesPage.getNumber(),
                lotesPage.getTotalPages(),
                cooperativa.getId());

        // Convertir a DTOs
        List<LoteCooperativaResponseDto> lotesDto = lotesPage.getContent().stream()
                .map(this::convertToCooperativaResponseDto)
                .collect(Collectors.toList());

        return new LotesCooperativaPaginadosDto(
                lotesDto,
                lotesPage.getTotalElements(),
                lotesPage.getTotalPages(),
                lotesPage.getNumber(),
                lotesPage.getSize(),
                lotesPage.hasNext(),
                lotesPage.hasPrevious()
        );
    }

    /**
     * Aprobar lote y asignar transportistas
     */
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

        LoteDetalleDto loteDto = convertToDetalleDto(lote, cooperativa);

        lotesWebSocketBl.publicarAprobacionCooperativa(lote, loteDto, usuarioId);

        log.info("Lote aprobado exitosamente - ID: {}", loteId);

        return loteDto;
    }

    /**
     * Rechazar lote
     */
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

        lotesWebSocketBl.publicarRechazoCooperativa(lote, rechazoDto.getMotivoRechazo(), usuarioId);

        log.info("Lote rechazado - ID: {}", loteId);
    }

    /**
     * Obtener detalle completo de un lote (cualquier estado) - AHORA USA LoteDetalleDto COMPARTIDO
     */
    @Transactional(readOnly = true)
    public LoteDetalleDto getLoteDetalleCompleto(Integer loteId, Integer usuarioId) {
        log.debug("Obteniendo detalle completo del lote ID: {} para cooperativa - Usuario ID: {}", loteId, usuarioId);

        // Obtener cooperativa del usuario
        Cooperativa cooperativa = obtenerCooperativaDelUsuario(usuarioId);

        // Obtener lote
        Lotes lote = lotesRepository.findById(loteId)
                .orElseThrow(() -> new IllegalArgumentException("Lote no encontrado"));

        // Verificar que el lote pertenece a la cooperativa
        if (!perteneceACooperativa(lote, cooperativa)) {
            throw new IllegalArgumentException("No tienes permiso para acceder a este lote");
        }

        return convertToDetalleDto(lote, cooperativa);
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
            transportista.setEstado("en_ruta");
            transportistaRepository.save(transportista);
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

    private String convertirCamelCaseASnakeCase(String camelCase) {
        if (camelCase == null || camelCase.isEmpty()) {
            return "fecha_creacion";
        }
        return switch (camelCase) {
            case "fechaCreacion" -> "fecha_creacion";
            case "fechaAprobacionCooperativa" -> "fecha_aprobacion_cooperativa";
            case "fechaAprobacionDestino" -> "fecha_aprobacion_destino";
            case "fechaInicioTransporte" -> "fecha_inicio_transporte";
            case "fechaFinTransporte" -> "fecha_fin_transporte";
            case "tipoOperacion" -> "tipo_operacion";
            case "tipoMineral" -> "tipo_mineral";
            case "pesoTotalEstimado" -> "peso_total_estimado";
            case "pesoTotalReal" -> "peso_total_real";
            case "camionesSolicitados" -> "camiones_solicitados";
            case "minasId" -> "minas_id";
            case "estado" -> "estado";
            case "id" -> "id";
            default -> camelCase.replaceAll("([a-z])([A-Z])", "$1_$2").toLowerCase();
        };
    }

    // ==================== MÉTODOS DE CONVERSIÓN DTO ====================

    /**
     * Convertir a LoteDetalleDto compartido (mismo que usan socio, ingenio, comercializadora)
     */
    private LoteDetalleDto convertToDetalleDto(Lotes lote, Cooperativa cooperativa) {
        LoteDetalleDto dto = new LoteDetalleDto();

        dto.setId(lote.getId());
        dto.setEstado(lote.getEstado());
        dto.setCamionlesSolicitados(lote.getCamionesSolicitados());
        dto.setTipoOperacion(lote.getTipoOperacion());
        dto.setTipoMineral(lote.getTipoMineral());

        // Información de la mina
        Minas mina = lote.getMinasId();
        dto.setMinaId(mina.getId());
        dto.setMinaNombre(mina.getNombre());
        dto.setMinaFotoUrl(mina.getFotoUrl());
        dto.setMinaLatitud(mina.getLatitud() != null ? mina.getLatitud().doubleValue() : null);
        dto.setMinaLongitud(mina.getLongitud() != null ? mina.getLongitud().doubleValue() : null);
        dto.setSectorNombre(mina.getSectoresId().getNombre());

        // Balanza de la cooperativa
        if (!cooperativa.getBalanzaCooperativaList().isEmpty()) {
            dto.setCooperativaBalanzaLatitud(cooperativa.getBalanzaCooperativaList().getFirst().getLatitud());
            dto.setCooperativaBalanzaLongitud(cooperativa.getBalanzaCooperativaList().getFirst().getLongitud());
        }

        // Minerales
        List<LoteMinerales> loteMinerales = loteMineralesRepository.findByLotesId(lote);
        List<MineralInfoDto> mineralesDto = loteMinerales.stream()
                .map(lm -> new MineralInfoDto(
                        lm.getMineralesId().getId(),
                        lm.getMineralesId().getNombre(),
                        lm.getMineralesId().getNomenclatura()
                ))
                .collect(Collectors.toList());
        dto.setMinerales(mineralesDto);

        // Información del destino (Ingenio o Comercializadora)
        if (lote.getTipoOperacion().equals("procesamiento_planta")) {
            LoteIngenio loteIngenio = loteIngenioRepository.findByLotesId(lote).orElse(null);
            if (loteIngenio != null) {
                IngenioMinero ingenio = loteIngenio.getIngenioMineroId();
                dto.setDestinoId(ingenio.getId());
                dto.setDestinoNombre(ingenio.getRazonSocial());
                dto.setDestinoTipo("ingenio");
                dto.setDestinoNIT(ingenio.getNit());
                dto.setDestinoDireccion(ingenio.getDireccion());
                dto.setDestinoDepartamento(ingenio.getDepartamento());
                dto.setDestinoMunicipio(ingenio.getMunicipio());
                dto.setDestinoTelefono(ingenio.getNumeroTelefonoMovil());

                if (!ingenio.getAlmacenesIngenioList().isEmpty()) {
                    dto.setDestinoAlmacenLatitud(ingenio.getAlmacenesIngenioList().getFirst().getLatitud());
                    dto.setDestinoAlmacenLongitud(ingenio.getAlmacenesIngenioList().getFirst().getLongitud());
                }
                if (!ingenio.getBalanzasIngenioList().isEmpty()) {
                    dto.setDestinoBalanzaLatitud(ingenio.getBalanzasIngenioList().getFirst().getLatitud());
                    dto.setDestinoBalanzaLongitud(ingenio.getBalanzasIngenioList().getFirst().getLongitud());
                }
            }
        } else {
            LoteComercializadora loteComercializadora = loteComercializadoraRepository.findByLotesId(lote).orElse(null);
            if (loteComercializadora != null) {
                Comercializadora comercializadora = loteComercializadora.getComercializadoraId();
                dto.setDestinoId(comercializadora.getId());
                dto.setDestinoNombre(comercializadora.getRazonSocial());
                dto.setDestinoTipo("comercializadora");
                dto.setDestinoNIT(comercializadora.getNit());
                dto.setDestinoDireccion(comercializadora.getDireccion());
                dto.setDestinoDepartamento(comercializadora.getDepartamento());
                dto.setDestinoMunicipio(comercializadora.getMunicipio());
                dto.setDestinoTelefono(comercializadora.getNumeroTelefonoMovil());

                if (!comercializadora.getAlmacenesList().isEmpty()) {
                    dto.setDestinoAlmacenLatitud(comercializadora.getAlmacenesList().getFirst().getLatitud());
                    dto.setDestinoAlmacenLongitud(comercializadora.getAlmacenesList().getFirst().getLongitud());
                }
                if (!comercializadora.getBalanzasList().isEmpty()) {
                    dto.setDestinoBalanzaLatitud(comercializadora.getBalanzasList().getFirst().getLatitud());
                    dto.setDestinoBalanzaLongitud(comercializadora.getBalanzasList().getFirst().getLongitud());
                }
            }
        }

        // Fechas
        dto.setFechaCreacion(lote.getFechaCreacion());
        dto.setFechaAprobacionCooperativa(lote.getFechaAprobacionCooperativa());
        dto.setFechaAprobacionDestino(lote.getFechaAprobacionDestino());
        dto.setFechaInicioTransporte(lote.getFechaInicioTransporte());
        dto.setFechaFinTransporte(lote.getFechaFinTransporte());

        dto.setPesoTotalEstimado(lote.getPesoTotalEstimado());
        dto.setPesoTotalReal(lote.getPesoTotalReal());
        dto.setObservaciones(lote.getObservaciones());

        // Información del socio
        Socio socio = mina.getSocioId();
        Persona persona = socio.getUsuariosId().getPersona();
        dto.setSocioId(socio.getId());
        dto.setSocioNombres(persona.getNombres());
        dto.setSocioApellidos(persona.getPrimerApellido() + " " + (persona.getSegundoApellido() != null ? persona.getSegundoApellido() : ""));

        // Asignaciones de camiones
        List<AsignacionCamion> asignaciones = asignacionCamionRepository.findByLotesId(lote);
        dto.setCamioneAsignados(asignaciones.size());

        List<AsignacionCamionSimpleDto> asignacionesDto = asignaciones.stream()
                .map(a -> {
                    Persona personaTransportista = a.getTransportistaId().getUsuariosId().getPersona();
                    return new AsignacionCamionSimpleDto(
                            a.getId(),
                            a.getNumeroCamion(),
                            a.getEstado(),
                            a.getFechaAsignacion(),
                            a.getTransportistaId().getId(),
                            personaTransportista.getNombres() + " " + personaTransportista.getPrimerApellido(),
                            a.getTransportistaId().getPlacaVehiculo(),
                            personaTransportista.getNumeroCelular()
                    );
                })
                .collect(Collectors.toList());
        dto.setAsignaciones(asignacionesDto);

        // Historial de cambios
        List<AuditoriaLotes> auditorias = auditoriaLotesRepository.findByLoteIdOrderByFechaRegistroDesc(lote);
        List<AuditoriaLoteDto> auditoriasDto = auditorias.stream()
                .map(a -> new AuditoriaLoteDto(
                        a.getId(),
                        a.getEstadoAnterior(),
                        a.getEstadoNuevo(),
                        a.getAccion(),
                        a.getDescripcion(),
                        a.getObservaciones(),
                        a.getFechaRegistro(),
                        a.getTipoUsuario(),
                        parseMetadataToStringMap(a.getMetadata())
                ))
                .collect(Collectors.toList());
        dto.setHistorialCambios(auditoriasDto);

        dto.setCreatedAt(lote.getFechaCreacion());
        dto.setUpdatedAt(lote.getUpdatedAt());

        return dto;
    }

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

    private LoteCooperativaResponseDto convertToCooperativaResponseDto(Lotes lote) {
        LoteCooperativaResponseDto dto = new LoteCooperativaResponseDto();

        // Información básica del lote
        dto.setId(lote.getId());
        dto.setEstado(lote.getEstado());
        dto.setCamionesSolicitados(lote.getCamionesSolicitados());
        dto.setTipoOperacion(lote.getTipoOperacion());
        dto.setTipoMineral(lote.getTipoMineral());

        // Información de la mina
        Minas mina = lote.getMinasId();
        dto.setMinaId(mina.getId());
        dto.setMinaNombre(mina.getNombre());
        dto.setMinaLatitud(mina.getLatitud());
        dto.setMinaLongitud(mina.getLongitud());

        // Información del sector
        Sectores sector = mina.getSectoresId();
        dto.setSectorId(sector.getId());
        dto.setSectorNombre(sector.getNombre());
        dto.setSectorColor(sector.getColor());

        // Información del socio (dueño de la mina)
        Socio socio = mina.getSocioId();
        dto.setSocioId(socio.getId());
        dto.setSocioEstado(socio.getEstado());

        // Obtener datos de la persona del socio
        Persona persona = personaRepository.findByUsuariosId(socio.getUsuariosId()).orElse(null);
        if (persona != null) {
            dto.setSocioNombres(persona.getNombres());
            dto.setSocioApellidos(persona.getPrimerApellido() +
                    (persona.getSegundoApellido() != null ? " " + persona.getSegundoApellido() : ""));
            dto.setSocioCi(persona.getCi());
            dto.setSocioTelefono(persona.getNumeroCelular());
        }

        // Obtener minerales del lote
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

        // Obtener información del destino (ingenio o comercializadora)
        if ("procesamiento_planta".equals(lote.getTipoOperacion())) {
            LoteIngenio loteIngenio = loteIngenioRepository.findByLotesId(lote).orElse(null);
            if (loteIngenio != null) {
                IngenioMinero ingenio = loteIngenio.getIngenioMineroId();
                dto.setDestinoId(ingenio.getId());
                dto.setDestinoNombre(ingenio.getRazonSocial());
                dto.setDestinoTipo("ingenio");
                dto.setDestinoNit(ingenio.getNit());
            }
        } else { // venta_directa
            LoteComercializadora loteComercializadora = loteComercializadoraRepository.findByLotesId(lote).orElse(null);
            if (loteComercializadora != null) {
                Comercializadora comercializadora = loteComercializadora.getComercializadoraId();
                dto.setDestinoId(comercializadora.getId());
                dto.setDestinoNombre(comercializadora.getRazonSocial());
                dto.setDestinoTipo("comercializadora");
                dto.setDestinoNit(comercializadora.getNit());
            }
        }

        // Fechas
        dto.setFechaCreacion(lote.getFechaCreacion());
        dto.setFechaAprobacionCooperativa(lote.getFechaAprobacionCooperativa());
        dto.setFechaAprobacionDestino(lote.getFechaAprobacionDestino());
        dto.setFechaInicioTransporte(lote.getFechaInicioTransporte());
        dto.setFechaFinTransporte(lote.getFechaFinTransporte());

        // Pesos
        dto.setPesoTotalEstimado(lote.getPesoTotalEstimado());
        dto.setPesoTotalReal(lote.getPesoTotalReal());

        // Cantidad de camiones asignados
        List<AsignacionCamion> asignaciones = asignacionCamionRepository.findByLotesId(lote);
        dto.setCamioneAsignados(asignaciones.size());

        // Observaciones
        dto.setObservaciones(lote.getObservaciones());

        // Metadata
        dto.setCreatedAt(lote.getFechaCreacion());
        dto.setUpdatedAt(lote.getUpdatedAt());

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

    private Map<String, String> parseMetadataToStringMap(String json) {
        if (json == null || json.isBlank()) return Map.of();

        try {
            Map<String, Object> raw = objectMapper.readValue(
                    json,
                    new TypeReference<Map<String, Object>>() {}
            );

            Map<String, String> out = new HashMap<>();
            raw.forEach((k, v) -> out.put(k, v == null ? null : String.valueOf(v)));
            return out;

        } catch (Exception e) {
            log.warn("No se pudo parsear metadata JSON: {}", json, e);
            return Map.of();
        }
    }
}