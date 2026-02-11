package ucb.edu.bo.sumajflow.bl.comercializadora;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ucb.edu.bo.sumajflow.bl.LiquidacionVentaBl;
import ucb.edu.bo.sumajflow.bl.LotesWebSocketBl;
import ucb.edu.bo.sumajflow.bl.NotificacionBl;
import ucb.edu.bo.sumajflow.bl.cooperativa.AuditoriaLotesBl;
import ucb.edu.bo.sumajflow.dto.comercializadora.*;
import ucb.edu.bo.sumajflow.dto.socio.*;
import ucb.edu.bo.sumajflow.dto.venta.VentaLiquidacionDetalleDto;
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
    private final AuditoriaLotesRepository auditoriaLotesRepository;
    private final NotificacionBl notificacionBl;
    private final AuditoriaLotesBl auditoriaLotesBl;
    private final TransportistaRepository transportistaRepository;
    private final ObjectMapper objectMapper;
    private final LotesWebSocketBl lotesWebSocketBl;

    // Constantes de estados
    private static final String ESTADO_PENDIENTE_DESTINO = "Pendiente de aprobación por Ingenio/Comercializadora";
    private static final String ESTADO_APROBADO = "Aprobado - Pendiente de iniciar";
    private static final String ESTADO_RECHAZADO = "Rechazado";
    private final LiquidacionVentaBl liquidacionVentaBl;
    private final LiquidacionRepository liquidacionRepository;

    /**
     * Obtener lotes con paginación y filtros
     */
    @Transactional(readOnly = true)
    public LotesComercializadoraPaginadosDto getLotesComercializadoraPaginados(
            Integer usuarioId,
            LoteFiltrosComercializadoraDto filtros
    ) {
        log.debug("Obteniendo lotes para comercializadora con filtros - Usuario ID: {}", usuarioId);

        // Obtener comercializadora del usuario
        Comercializadora comercializadora = obtenerComercializadoraDelUsuario(usuarioId);

        // Normalizar filtros (empty strings → null)
        String estado = (filtros.getEstado() != null && !filtros.getEstado().trim().isEmpty())
                ? filtros.getEstado() : null;
        String tipoMineral = (filtros.getTipoMineral() != null && !filtros.getTipoMineral().trim().isEmpty())
                ? filtros.getTipoMineral() : null;
        String cooperativaNombre = (filtros.getCooperativaNombre() != null && !filtros.getCooperativaNombre().trim().isEmpty())
                ? filtros.getCooperativaNombre() : null;

        // Configurar ordenamiento
        String sortByField = convertirCamelCaseASnakeCase(filtros.getSortBy());
        Sort sort = filtros.getSortDir().equalsIgnoreCase("asc")
                ? Sort.by(sortByField).ascending()
                : Sort.by(sortByField).descending();
        Pageable pageable = PageRequest.of(filtros.getPage(), filtros.getSize(), sort);

        // Ejecutar consulta
        Page<LoteComercializadora> lotesPage = loteComercializadoraRepository.findLotesByComercializadoraWithFilters(
                comercializadora.getId(),
                estado,
                tipoMineral,
                cooperativaNombre,
                filtros.getFechaDesde(),
                filtros.getFechaHasta(),
                pageable
        );

        // Convertir a DTOs
        List<LoteComercializadoraResponseDto> lotesDto = lotesPage.getContent().stream()
                .map(this::convertToComercializadoraResponseDto)
                .collect(Collectors.toList());

        log.info("Se encontraron {} lotes para comercializadora", lotesPage.getTotalElements());

        return new LotesComercializadoraPaginadosDto(
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
     * Obtener detalle completo de un lote
     */
    @Transactional(readOnly = true)
    public LoteDetalleDto getLoteDetalleCompleto(Integer loteId, Integer usuarioId) {
        log.debug("Obteniendo detalle completo del lote ID: {} para comercializadora", loteId);

        // Validar permisos
        Comercializadora comercializadora = obtenerComercializadoraDelUsuario(usuarioId);
        LoteComercializadora loteComercializadora = obtenerLoteComercializadoraConPermisos(loteId, comercializadora);
        Lotes lote = loteComercializadora.getLotesId();

        return convertToDetalleDto(lote, comercializadora);
    }

    /**
     * Aprobar lote desde la comercializadora
     */
    public LoteDetalleDto aprobarLote(
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

        validarEstadoPendiente(lote);

        String estadoAnterior = lote.getEstado();

        // 2. Actualizar estado del lote
        lote.setEstado(ESTADO_APROBADO);
        lote.setFechaAprobacionDestino(LocalDateTime.now());
        if (aprobacionDto.getObservaciones() != null && !aprobacionDto.getObservaciones().trim().isEmpty()) {
            String nuevaObservacion = "Comercializadora: " + aprobacionDto.getObservaciones();
            lote.setObservaciones(lote.getObservaciones() != null
                    ? lote.getObservaciones() + " | " + nuevaObservacion
                    : nuevaObservacion);
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

        LoteDetalleDto loteDto = convertToDetalleDto(lote, comercializadora);

        lotesWebSocketBl.publicarAprobacionDestino(lote, loteDto, usuarioId);

        log.info("Lote aprobado por comercializadora - ID: {}", loteId);

        return loteDto;
    }

    /**
     * Rechazar lote desde la comercializadora
     */
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

        validarEstadoPendiente(lote);

        if (rechazoDto.getMotivoRechazo() == null || rechazoDto.getMotivoRechazo().trim().isEmpty()) {
            throw new IllegalArgumentException("El motivo de rechazo es requerido");
        }

        String estadoAnterior = lote.getEstado();

        // 2. Actualizar estado del lote
        lote.setEstado(ESTADO_RECHAZADO);
        String motivoCompleto = "RECHAZADO POR COMERCIALIZADORA: " + rechazoDto.getMotivoRechazo();
        lote.setObservaciones(lote.getObservaciones() != null
                ? motivoCompleto + " | " + lote.getObservaciones()
                : motivoCompleto
        );
        lotesRepository.save(lote);

        // 3. Actualizar estado en lote_comercializadora
        loteComercializadora.setEstado("Rechazado");
        loteComercializadora.setObservaciones(rechazoDto.getMotivoRechazo());
        loteComercializadoraRepository.save(loteComercializadora);

        actualizarEstadoCamionesRechazados(lote);

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

        lotesWebSocketBl.publicarRechazoDestino(lote, rechazoDto.getMotivoRechazo(), usuarioId);

        log.info("Lote rechazado por comercializadora - ID: {}", loteId);
    }

    /**
     * Actualizar estado de los camiones asignados cuando se rechaza un lote
     */
    private void actualizarEstadoCamionesRechazados(Lotes lote) {
        List<AsignacionCamion> asignaciones = asignacionCamionRepository.findByLotesId(lote);

        if (asignaciones.isEmpty()) {
            log.debug("No hay asignaciones de camiones para el lote ID: {}", lote.getId());
            return;
        }

        log.info("Actualizando estado de {} camiones asignados al lote rechazado ID: {}",
                asignaciones.size(), lote.getId());

        for (AsignacionCamion asignacion : asignaciones) {
            String estadoAnterior = asignacion.getEstado();

            // Cambiar estado a "Cancelado por rechazo"
            asignacion.setEstado("Cancelado por rechazo");
            asignacionCamionRepository.save(asignacion);

            // Poner el estado de los transportes a "aprobado" para que puedan ser reasignados
            Transportista transporte = asignacion.getTransportistaId();
            transporte.setEstado("aprobado");
            transportistaRepository.save(transporte);

            log.debug("Camión #{} actualizado de '{}' a 'Cancelado por rechazo'",
                    asignacion.getNumeroCamion(), estadoAnterior);
        }
    }

    // ==================== MÉTODOS DE VALIDACIÓN ====================

    private void validarEstadoPendiente(Lotes lote) {
        if (!lote.getEstado().equals(ESTADO_PENDIENTE_DESTINO)) {
            throw new IllegalArgumentException(
                    "El lote no está en estado pendiente de aprobación por destino"
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

    private String convertirCamelCaseASnakeCase(String camelCase) {
        if (camelCase == null || camelCase.isEmpty()) return "l.fecha_creacion";

        return switch (camelCase) {
            case "fechaCreacion" -> "l.fecha_creacion";
            case "fechaAprobacionCooperativa" -> "l.fecha_aprobacion_cooperativa";
            case "fechaAprobacionDestino" -> "l.fecha_aprobacion_destino";
            case "estado" -> "l.estado";
            case "tipoMineral" -> "l.tipo_mineral";
            case "pesoTotalEstimado" -> "l.peso_total_estimado";
            default -> "l." + camelCase.replaceAll("([a-z])([A-Z])", "$1_$2").toLowerCase();
        };
    }

    // ==================== MÉTODOS DE CONVERSIÓN DTO ====================

    private void extraerYAsignarPesajes(AsignacionCamion asignacion, AsignacionCamionSimpleDto dto) {
        String observacionesJson = asignacion.getObservaciones();

        if (observacionesJson == null || observacionesJson.isBlank()) {
            log.debug("No hay observaciones para la asignación ID: {}", asignacion.getId());
            return;
        }

        try {
            // Parsear el JSON de observaciones
            ObservacionesViajeDto observaciones = objectMapper.readValue(
                    observacionesJson,
                    ObservacionesViajeDto.class
            );

            // Extraer pesaje origen
            if (observaciones.getPesajeOrigen() != null) {
                PesajeDto pesajeOrigen = observaciones.getPesajeOrigen();
                dto.setPesajeOrigenTaraKg(pesajeOrigen.getPesoTaraKg());
                dto.setPesajeOrigenBrutoKg(pesajeOrigen.getPesoBrutoKg());
                dto.setPesajeOrigenNetoKg(pesajeOrigen.getPesoNetoKg());
                dto.setPesajeOrigenFecha(pesajeOrigen.getTimestamp());

                log.debug("Pesaje origen extraído - Asignación ID: {}, Neto: {} kg",
                        asignacion.getId(), pesajeOrigen.getPesoNetoKg());
            }

            // Extraer pesaje destino
            if (observaciones.getPesajeDestino() != null) {
                PesajeDto pesajeDestino = observaciones.getPesajeDestino();
                dto.setPesajeDestinoTaraKg(pesajeDestino.getPesoTaraKg());
                dto.setPesajeDestinoBrutoKg(pesajeDestino.getPesoBrutoKg());
                dto.setPesajeDestinoNetoKg(pesajeDestino.getPesoNetoKg());
                dto.setPesajeDestinoFecha(pesajeDestino.getTimestamp());

                log.debug("Pesaje destino extraído - Asignación ID: {}, Neto: {} kg",
                        asignacion.getId(), pesajeDestino.getPesoNetoKg());
            }

        } catch (Exception e) {
            log.error("Error al parsear observaciones JSON para asignación ID: {} - JSON: {}",
                    asignacion.getId(), observacionesJson, e);
        }
    }

    /**
     * Convertir a LoteDetalleDto compartido (mismo que usa socio)
     */
    private LoteDetalleDto convertToDetalleDto(Lotes lote, Comercializadora comercializadora) {
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
        Cooperativa cooperativa = mina.getSectoresId().getCooperativaId();
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

        // Información del destino (Comercializadora)
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
                    AsignacionCamionSimpleDto as = new AsignacionCamionSimpleDto(
                            a.getId(),
                            a.getNumeroCamion(),
                            a.getEstado(),
                            a.getFechaAsignacion(),
                            a.getTransportistaId().getId(),
                            personaTransportista.getNombres() + " " + personaTransportista.getPrimerApellido(),
                            a.getTransportistaId().getPlacaVehiculo(),
                            personaTransportista.getNumeroCelular()
                    );

                    // Solo llenar cuando el estado del lote sea "Vendido a comercializadora" o "Procesado"
                    if (List.of("Vendido a comercializadora", "Procesado").contains(a.getLotesId().getEstado())) {
                        extraerYAsignarPesajes(a, as);
                    }

                    return as;
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

        if("Vendido a comercializadora".equals(lote.getEstado())) {
            Liquidacion liquidacion = liquidacionRepository.findByLote(lote).orElse(null);
            if (liquidacion != null) {
                VentaLiquidacionDetalleDto liquidacionDto = liquidacionVentaBl.convertirADtoDetallado(liquidacion);
                dto.setLiquidacionVentaDirecta(liquidacionDto);
            }
        }

        return dto;
    }

    private LoteComercializadoraResponseDto convertToComercializadoraResponseDto(LoteComercializadora loteComercializadora) {
        LoteComercializadoraResponseDto dto = new LoteComercializadoraResponseDto();
        Lotes lote = loteComercializadora.getLotesId();

        // Información básica del lote
        dto.setId(lote.getId());
        dto.setEstado(lote.getEstado());
        dto.setTipoMineral(lote.getTipoMineral());
        dto.setCamionlesSolicitados(lote.getCamionesSolicitados());
        dto.setPesoTotalEstimado(lote.getPesoTotalEstimado());
        dto.setPesoTotalReal(lote.getPesoTotalReal());

        // Información de la mina
        Minas mina = lote.getMinasId();
        dto.setMinaId(mina.getId());
        dto.setMinaNombre(mina.getNombre());

        // Información del sector
        Sectores sector = mina.getSectoresId();
        dto.setSectorId(sector.getId());
        dto.setSectorNombre(sector.getNombre());

        // Información de la cooperativa
        Cooperativa cooperativa = sector.getCooperativaId();
        dto.setCooperativaId(cooperativa.getId());
        dto.setCooperativaNombre(cooperativa.getRazonSocial());

        // Información del socio
        Socio socio = mina.getSocioId();
        dto.setSocioId(socio.getId());

        Persona persona = personaRepository.findByUsuariosId(socio.getUsuariosId()).orElse(null);
        if (persona != null) {
            dto.setSocioNombres(persona.getNombres());
            dto.setSocioApellidos(persona.getPrimerApellido() +
                    (persona.getSegundoApellido() != null ? " " + persona.getSegundoApellido() : ""));
            dto.setSocioCi(persona.getCi());
            dto.setSocioTelefono(persona.getNumeroCelular());
        }

        // Minerales
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

        // Fechas
        dto.setFechaCreacion(lote.getFechaCreacion());
        dto.setFechaAprobacionCooperativa(lote.getFechaAprobacionCooperativa());
        dto.setFechaAprobacionDestino(lote.getFechaAprobacionDestino());

        // Transporte
        List<AsignacionCamion> asignaciones = asignacionCamionRepository.findByLotesId(lote);
        dto.setCamioneAsignados(asignaciones.size());

        // Observaciones
        dto.setObservaciones(lote.getObservaciones());

        // Metadata
        dto.setCreatedAt(lote.getFechaCreacion());
        dto.setUpdatedAt(lote.getUpdatedAt());

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