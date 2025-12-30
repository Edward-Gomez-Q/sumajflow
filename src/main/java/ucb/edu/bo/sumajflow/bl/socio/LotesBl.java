package ucb.edu.bo.sumajflow.bl.socio;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ucb.edu.bo.sumajflow.bl.AuditoriaBl;
import ucb.edu.bo.sumajflow.bl.NotificacionBl;
import ucb.edu.bo.sumajflow.dto.socio.*;
import ucb.edu.bo.sumajflow.entity.*;
import ucb.edu.bo.sumajflow.repository.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class LotesBl {

    // Repositorios
    private final LotesRepository lotesRepository;
    private final LoteMineralesRepository loteMineralesRepository;
    private final LoteIngenioRepository loteIngenioRepository;
    private final LoteComercializadoraRepository loteComercializadoraRepository;
    private final MinasRepository minasRepository;
    private final MineralesRepository mineralesRepository;
    private final IngenioMineroRepository ingenioMineroRepository;
    private final ComercializadoraRepository comercializadoraRepository;
    private final SocioRepository socioRepository;
    private final UsuariosRepository usuariosRepository;
    private final AuditoriaBl auditoriaBl;
    private final NotificacionBl notificacionBl;
    private final AuditoriaLotesRepository auditoriaLotesRepository;
    private final AsignacionCamionRepository asignacionCamionRepository;

    // Constantes de estados
    private static final String ESTADO_INICIAL = "Pendiente de aprobación cooperativa";

    @Transactional(readOnly = true)
    public LotesPaginadosDto getLotesPaginados(Integer usuarioId, LoteFiltrosDto filtros) {
        log.debug("Obteniendo lotes paginados para usuario ID: {} con filtros", usuarioId);

        Usuarios usuario = obtenerUsuario(usuarioId);
        Socio socio = obtenerSocioDelUsuario(usuario);

        List<Minas> minasDelSocio = minasRepository.findByActiveSocio(socio);

        if (minasDelSocio.isEmpty()) {
            log.info("El socio no tiene minas activas");
            return new LotesPaginadosDto(new ArrayList<>(), 0, 0, filtros.getPage(), filtros.getSize(), false, false);
        }

        // Convertir List<Minas> a Integer[] de IDs
        Integer[] minaIds = minasDelSocio.stream()
                .map(Minas::getId)
                .toArray(Integer[]::new);

        // Normalizar filtros (pasar null para evitar problemas con PostgreSQL)
        String estado = (filtros.getEstado() != null && !filtros.getEstado().trim().isEmpty())
                ? filtros.getEstado() : null;
        String tipoOperacion = (filtros.getTipoOperacion() != null && !filtros.getTipoOperacion().trim().isEmpty())
                ? filtros.getTipoOperacion() : null;
        String tipoMineral = (filtros.getTipoMineral() != null && !filtros.getTipoMineral().trim().isEmpty())
                ? filtros.getTipoMineral() : null;
        Integer minaId = (filtros.getMinaId() != null && filtros.getMinaId() > 0)
                ? filtros.getMinaId() : null;

        // Convertir sortBy de camelCase a snake_case para query nativa
        String sortByField = convertirCamelCaseASnakeCase(filtros.getSortBy());

        // Crear Pageable con el campo correcto
        Sort sort = filtros.getSortDir().equalsIgnoreCase("asc")
                ? Sort.by(sortByField).ascending()
                : Sort.by(sortByField).descending();

        Pageable pageable = PageRequest.of(filtros.getPage(), filtros.getSize(), sort);

        // Obtener lotes con filtros
        Page<Lotes> lotesPage = lotesRepository.findByMinasIdInWithFilters(
                minaIds,
                estado,
                tipoOperacion,
                tipoMineral,
                filtros.getFechaDesde(),
                filtros.getFechaHasta(),
                minaId,
                pageable
        );

        log.info("Se encontraron {} lotes en la página {} de {}",
                lotesPage.getNumberOfElements(),
                lotesPage.getNumber(),
                lotesPage.getTotalPages());

        List<LoteResponseDto> lotesDto = lotesPage.getContent().stream()
                .map(this::convertToDtoComplete)
                .collect(Collectors.toList());

        return new LotesPaginadosDto(
                lotesDto,
                lotesPage.getTotalElements(),
                lotesPage.getTotalPages(),
                lotesPage.getNumber(),
                lotesPage.getSize(),
                lotesPage.hasNext(),
                lotesPage.hasPrevious()
        );
    }

    // Método auxiliar para convertir camelCase a snake_case
    private String convertirCamelCaseASnakeCase(String camelCase) {
        if (camelCase == null || camelCase.isEmpty()) {
            return "fecha_creacion"; // Default
        }

        // Mapeo de campos comunes
        switch (camelCase) {
            case "fechaCreacion":
                return "fecha_creacion";
            case "fechaAprobacionCooperativa":
                return "fecha_aprobacion_cooperativa";
            case "fechaAprobacionDestino":
                return "fecha_aprobacion_destino";
            case "fechaInicioTransporte":
                return "fecha_inicio_transporte";
            case "fechaFinTransporte":
                return "fecha_fin_transporte";
            case "tipoOperacion":
                return "tipo_operacion";
            case "tipoMineral":
                return "tipo_mineral";
            case "pesoTotalEstimado":
                return "peso_total_estimado";
            case "pesoTotalReal":
                return "peso_total_real";
            case "camionesSolicitados":
                return "camiones_solicitados";
            case "minasId":
                return "minas_id";
            case "estado":
                return "estado";
            case "id":
                return "id";
            default:
                // Conversión genérica: camelCase -> snake_case
                return camelCase.replaceAll("([a-z])([A-Z])", "$1_$2").toLowerCase();
        }
    }


    /**
     * Obtener detalle completo de un lote
     */
    @Transactional(readOnly = true)
    public LoteDetalleDto getLoteDetalleCompleto(Integer loteId, Integer usuarioId) {
        log.debug("Obteniendo detalle completo del lote ID: {} para usuario ID: {}", loteId, usuarioId);

        Usuarios usuario = obtenerUsuario(usuarioId);
        Socio socio = obtenerSocioDelUsuario(usuario);

        Lotes lote = lotesRepository.findById(loteId)
                .orElseThrow(() -> new IllegalArgumentException("Lote no encontrado"));

        if (!lote.getMinasId().getSocioId().getId().equals(socio.getId())) {
            throw new IllegalArgumentException("No tienes permiso para acceder a este lote");
        }

        return convertToDetalleDto(lote, socio);
    }

    /**
     * Convertir a DTO de detalle completo
     */
    private LoteDetalleDto convertToDetalleDto(Lotes lote, Socio socio) {
        LoteDetalleDto dto = new LoteDetalleDto();

        dto.setId(lote.getId());
        dto.setEstado(lote.getEstado());
        dto.setCamionlesSolicitados(lote.getCamionesSolicitados());
        dto.setTipoOperacion(lote.getTipoOperacion());
        dto.setTipoMineral(lote.getTipoMineral());

        Minas mina = lote.getMinasId();
        dto.setMinaId(mina.getId());
        dto.setMinaNombre(mina.getNombre());
        dto.setMinaFotoUrl(mina.getFotoUrl());
        dto.setMinaLatitud(mina.getLatitud() != null ? mina.getLatitud().doubleValue() : null);
        dto.setMinaLongitud(mina.getLongitud() != null ? mina.getLongitud().doubleValue() : null);
        dto.setSectorNombre(mina.getSectoresId().getNombre());

        List<LoteMinerales> loteMinerales = loteMineralesRepository.findByLotesId(lote);
        List<MineralInfoDto> mineralesDto = loteMinerales.stream()
                .map(lm -> new MineralInfoDto(
                        lm.getMineralesId().getId(),
                        lm.getMineralesId().getNombre(),
                        lm.getMineralesId().getNomenclatura()
                ))
                .collect(Collectors.toList());
        dto.setMinerales(mineralesDto);

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
                dto.setDestinoLatitud(ingenio.getAlmacenesIngenioList().getFirst().getLatitud());
                dto.setDestinoLongitud(ingenio.getAlmacenesIngenioList().getFirst().getLongitud());
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
                dto.setDestinoLatitud(comercializadora.getAlmacenesList().getFirst().getLatitud());
                dto.setDestinoLongitud(comercializadora.getAlmacenesList().getFirst().getLongitud());
            }
        }

        dto.setFechaCreacion(lote.getFechaCreacion());
        dto.setFechaAprobacionCooperativa(lote.getFechaAprobacionCooperativa());
        dto.setFechaAprobacionDestino(lote.getFechaAprobacionDestino());
        dto.setFechaInicioTransporte(lote.getFechaInicioTransporte());
        dto.setFechaFinTransporte(lote.getFechaFinTransporte());

        dto.setPesoTotalEstimado(lote.getPesoTotalEstimado());
        dto.setPesoTotalReal(lote.getPesoTotalReal());
        dto.setObservaciones(lote.getObservaciones());

        Persona persona = socio.getUsuariosId().getPersona();
        dto.setSocioId(socio.getId());
        dto.setSocioNombres(persona.getNombres());
        dto.setSocioApellidos(persona.getPrimerApellido() + " " + (persona.getSegundoApellido() != null ? persona.getSegundoApellido() : ""));

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
                        a.getTipoUsuario()
                ))
                .collect(Collectors.toList());
        dto.setHistorialCambios(auditoriasDto);

        dto.setCreatedAt(lote.getFechaCreacion());
        dto.setUpdatedAt(lote.getUpdatedAt());

        return dto;
    }

    /**
     * Crear un nuevo lote
     */
    @Transactional
    public LoteResponseDto createLote(
            LoteCreateDto dto,
            Integer usuarioId,
            String ipOrigen,
            String metodoHttp,
            String endpoint
    ) {
        log.info("Creando nuevo lote para mina ID: {}", dto.getMinaId());

        // 1. Validaciones generales
        validarDatosLote(dto);

        // 2. Obtener y validar usuario/socio
        Usuarios usuario = obtenerUsuario(usuarioId);
        Socio socio = obtenerSocioDelUsuario(usuario);

        // 3. Obtener y validar mina
        Minas mina = validarYObtenerMina(dto.getMinaId(), socio);

        // 4. Validar y obtener minerales
        List<Minerales> minerales = validarYObtenerMinerales(dto.getMineralesIds());

        // 5. Validar tipo de operación y tipo de mineral
        validarTipoOperacionYMineral(dto.getTipoOperacion(), dto.getTipoMineral());

        // 6. Validar y obtener destino (ingenio o comercializadora)
        Object destino = validarYObtenerDestino(
                dto.getTipoOperacion(),
                dto.getDestinoId(),
                dto.getTipoMineral()
        );

        // 7. Crear el lote
        Lotes lote = crearYGuardarLote(dto, mina);

        // 8. Crear relaciones lote_minerales
        crearRelacionesMinerales(lote, minerales);

        // 9. Crear registro en lote_ingenio o lote_comercializadora
        crearRelacionDestino(lote, dto.getTipoOperacion(), dto.getDestinoId());

        // 10. Registrar en auditoría
        registrarAuditoriaCreacion(usuario, lote, mina, ipOrigen, metodoHttp, endpoint);

        // 11. Crear notificación
        enviarNotificacionCreacion(usuarioId, lote, mina);

        log.info("Lote creado exitosamente - ID: {}", lote.getId());

        // 12. Retornar DTO
        return convertToDto(lote, minerales, destino, dto.getTipoOperacion());
    }

    // ==================== MÉTODOS DE VALIDACIÓN ====================

    private void validarDatosLote(LoteCreateDto dto) {
        if (dto.getMinaId() == null) {
            throw new IllegalArgumentException("La mina es requerida");
        }

        if (dto.getMineralesIds() == null || dto.getMineralesIds().isEmpty()) {
            throw new IllegalArgumentException("Debe seleccionar al menos un mineral");
        }

        if (dto.getCamionlesSolicitados() == null || dto.getCamionlesSolicitados() <= 0) {
            throw new IllegalArgumentException("El número de camiones debe ser mayor a 0");
        }

        if (dto.getTipoOperacion() == null || dto.getTipoOperacion().trim().isEmpty()) {
            throw new IllegalArgumentException("El tipo de operación es requerido");
        }

        if (!dto.getTipoOperacion().equals("procesamiento_planta") &&
                !dto.getTipoOperacion().equals("venta_directa")) {
            throw new IllegalArgumentException(
                    "Tipo de operación inválido. Debe ser 'procesamiento_planta' o 'venta_directa'"
            );
        }

        if (dto.getDestinoId() == null) {
            throw new IllegalArgumentException("El destino es requerido");
        }

        if (dto.getTipoMineral() == null || dto.getTipoMineral().trim().isEmpty()) {
            throw new IllegalArgumentException("El tipo de mineral es requerido");
        }

        if (!dto.getTipoMineral().equals("complejo") && !dto.getTipoMineral().equals("concentrado")) {
            throw new IllegalArgumentException(
                    "Tipo de mineral inválido. Debe ser 'complejo' o 'concentrado'"
            );
        }
    }

    private void validarTipoOperacionYMineral(String tipoOperacion, String tipoMineral) {
        // Regla: concentrado solo puede ir a comercializadora
        if (tipoMineral.equals("concentrado")) {
            if (tipoOperacion.equals("procesamiento_planta")) {
                throw new IllegalArgumentException(
                        "Mineral tipo 'concentrado' solo puede tener operación 'venta_directa' a comercializadora"
                );
            }
        }
    }

    private Minas validarYObtenerMina(Integer minaId, Socio socio) {
        Minas mina = minasRepository.findByIdAndEstadoActivo(minaId)
                .orElseThrow(() -> new IllegalArgumentException("Mina no encontrada o inactiva"));

        // Verificar permisos
        if (!mina.getSocioId().getId().equals(socio.getId())) {
            throw new IllegalArgumentException("No tienes permiso para crear lotes en esta mina");
        }

        return mina;
    }

    private List<Minerales> validarYObtenerMinerales(List<Integer> mineralesIds) {
        List<Minerales> minerales = new ArrayList<>();

        for (Integer mineralId : mineralesIds) {
            Minerales mineral = mineralesRepository.findById(mineralId)
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Mineral con ID " + mineralId + " no encontrado"
                    ));
            minerales.add(mineral);
        }

        return minerales;
    }

    private Object validarYObtenerDestino(
            String tipoOperacion,
            Integer destinoId,
            String tipoMineral
    ) {
        if (tipoOperacion.equals("procesamiento_planta")) {
            // Debe ser un ingenio
            return ingenioMineroRepository.findById(destinoId)
                    .orElseThrow(() -> new IllegalArgumentException("Ingenio minero no encontrado"));

        } else { // venta_directa
            // Debe ser una comercializadora
            Comercializadora comercializadora = comercializadoraRepository.findById(destinoId)
                    .orElseThrow(() -> new IllegalArgumentException("Comercializadora no encontrada"));

            // Validar que si es concentrado, solo pueda ir a comercializadora
            // (ya validado arriba, pero doble check)
            return comercializadora;
        }
    }

    // ==================== MÉTODOS DE CREACIÓN ====================

    private Lotes crearYGuardarLote(LoteCreateDto dto, Minas mina) {
        Lotes lote = Lotes.builder()
                .minasId(mina)
                .camionesSolicitados(dto.getCamionlesSolicitados())
                .tipoOperacion(dto.getTipoOperacion())
                .tipoMineral(dto.getTipoMineral())
                .estado(ESTADO_INICIAL)
                .fechaCreacion(LocalDateTime.now())
                .pesoTotalEstimado(dto.getPesoTotalEstimado())
                .observaciones(dto.getObservaciones())
                .build();

        return lotesRepository.save(lote);
    }

    private void crearRelacionesMinerales(Lotes lote, List<Minerales> minerales) {
        for (Minerales mineral : minerales) {
            LoteMinerales loteMinerales = LoteMinerales.builder()
                    .lotesId(lote)
                    .mineralesId(mineral)
                    .build();

            loteMineralesRepository.save(loteMinerales);
        }
    }

    private void crearRelacionDestino(Lotes lote, String tipoOperacion, Integer destinoId) {
        if (tipoOperacion.equals("procesamiento_planta")) {
            // Crear en lote_ingenio
            IngenioMinero ingenio = ingenioMineroRepository.findById(destinoId)
                    .orElseThrow(() -> new IllegalArgumentException("Ingenio no encontrado"));

            LoteIngenio loteIngenio = LoteIngenio.builder()
                    .lotesId(lote)
                    .ingenioMineroId(ingenio)
                    .estado("Pendiente de aprobación cooperativa")
                    .build();

            loteIngenioRepository.save(loteIngenio);

        } else { // venta_directa
            // Crear en lote_comercializadora
            Comercializadora comercializadora = comercializadoraRepository.findById(destinoId)
                    .orElseThrow(() -> new IllegalArgumentException("Comercializadora no encontrada"));

            LoteComercializadora loteComercializadora = LoteComercializadora.builder()
                    .lotesId(lote)
                    .comercializadoraId(comercializadora)
                    .estado("Pendiente de aprobación cooperativa")
                    .build();

            loteComercializadoraRepository.save(loteComercializadora);
        }
    }

    // ==================== MÉTODOS AUXILIARES ====================

    private Usuarios obtenerUsuario(Integer usuarioId) {
        return usuariosRepository.findById(usuarioId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));
    }

    private Socio obtenerSocioDelUsuario(Usuarios usuario) {
        return socioRepository.findByUsuariosId(usuario)
                .orElseThrow(() -> new IllegalArgumentException("Socio no encontrado"));
    }

    private void registrarAuditoriaCreacion(
            Usuarios usuario,
            Lotes lote,
            Minas mina,
            String ipOrigen,
            String metodoHttp,
            String endpoint
    ) {
        Map<String, Object> datosNuevos = new HashMap<>();
        datosNuevos.put("lote_id", lote.getId());
        datosNuevos.put("mina_id", mina.getId());
        datosNuevos.put("mina_nombre", mina.getNombre());
        datosNuevos.put("estado", lote.getEstado());
        datosNuevos.put("tipo_operacion", lote.getTipoOperacion());
        datosNuevos.put("tipo_mineral", lote.getTipoMineral());
        datosNuevos.put("camiones_solicitados", lote.getCamionesSolicitados());

        log.info("Metodo hhttp: {}", metodoHttp);
        auditoriaBl.registrar(
                usuario,
                "lotes",
                "INSERT",
                "Creación de lote",
                lote.getId(),
                null,
                datosNuevos,
                null,
                ipOrigen,
                null,
                metodoHttp,
                endpoint,
                "lotes",
                "ALTO"
        );
    }

    private void enviarNotificacionCreacion(Integer usuarioId, Lotes lote, Minas mina) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("loteId", lote.getId());
        metadata.put("minaId", mina.getId());
        metadata.put("minaNombre", mina.getNombre());
        metadata.put("estado", lote.getEstado());

        notificacionBl.crearNotificacion(
                usuarioId,
                "success",
                "Lote creado",
                "El lote para la mina '" + mina.getNombre() +
                        "' ha sido creado y está pendiente de aprobación por la cooperativa",
                metadata
        );
    }
    /**
     * Convertir entidad Lotes a DTO completo con toda la información
     */
    private LoteResponseDto convertToDtoComplete(Lotes lote) {
        LoteResponseDto dto = new LoteResponseDto();

        // Información básica del lote
        dto.setId(lote.getId());
        dto.setEstado(lote.getEstado());
        dto.setCamionlesSolicitados(lote.getCamionesSolicitados());
        dto.setTipoOperacion(lote.getTipoOperacion());
        dto.setTipoMineral(lote.getTipoMineral());

        // Información de la mina
        dto.setMinaId(lote.getMinasId().getId());
        dto.setMinaNombre(lote.getMinasId().getNombre());

        // Obtener minerales del lote
        List<LoteMinerales> loteMinerales = loteMineralesRepository.findByLotesId(lote);
        List<MineralInfoDto> mineralesDto = loteMinerales.stream()
                .map(lm -> new MineralInfoDto(
                        lm.getMineralesId().getId(),
                        lm.getMineralesId().getNombre(),
                        lm.getMineralesId().getNomenclatura()
                ))
                .collect(Collectors.toList());
        dto.setMinerales(mineralesDto);

        // Obtener destino (ingenio o comercializadora)
        if (lote.getTipoOperacion().equals("procesamiento_planta")) {
            LoteIngenio loteIngenio = loteIngenioRepository.findByLotesId(lote)
                    .orElse(null);
            if (loteIngenio != null) {
                dto.setDestinoId(loteIngenio.getIngenioMineroId().getId());
                dto.setDestinoNombre(loteIngenio.getIngenioMineroId().getRazonSocial());
                dto.setDestinoTipo("ingenio");
            }
        } else {
            LoteComercializadora loteComercializadora = loteComercializadoraRepository.findByLotesId(lote)
                    .orElse(null);
            if (loteComercializadora != null) {
                dto.setDestinoId(loteComercializadora.getComercializadoraId().getId());
                dto.setDestinoNombre(loteComercializadora.getComercializadoraId().getRazonSocial());
                dto.setDestinoTipo("comercializadora");
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

        // Observaciones
        dto.setObservaciones(lote.getObservaciones());

        // Metadatos
        dto.setCreatedAt(lote.getFechaCreacion());
        dto.setUpdatedAt(lote.getUpdatedAt());

        return dto;
    }

    private LoteResponseDto convertToDto(
            Lotes lote,
            List<Minerales> minerales,
            Object destino,
            String tipoOperacion
    ) {
        LoteResponseDto dto = new LoteResponseDto();

        dto.setId(lote.getId());
        dto.setMinaId(lote.getMinasId().getId());
        dto.setMinaNombre(lote.getMinasId().getNombre());

        // Minerales
        List<MineralInfoDto> mineralesDto = minerales.stream()
                .map(m -> new MineralInfoDto(m.getId(), m.getNombre(), m.getNomenclatura()))
                .collect(Collectors.toList());
        dto.setMinerales(mineralesDto);

        dto.setCamionlesSolicitados(lote.getCamionesSolicitados());
        dto.setTipoOperacion(lote.getTipoOperacion());
        dto.setTipoMineral(lote.getTipoMineral());
        dto.setEstado(lote.getEstado());
        dto.setFechaCreacion(lote.getFechaCreacion());
        dto.setPesoTotalEstimado(lote.getPesoTotalEstimado());
        dto.setObservaciones(lote.getObservaciones());

        // Destino
        if (tipoOperacion.equals("procesamiento_planta")) {
            IngenioMinero ingenio = (IngenioMinero) destino;
            dto.setDestinoId(ingenio.getId());
            dto.setDestinoNombre(ingenio.getRazonSocial());
            dto.setDestinoTipo("ingenio");
        } else {
            Comercializadora comercializadora = (Comercializadora) destino;
            dto.setDestinoId(comercializadora.getId());
            dto.setDestinoNombre(comercializadora.getRazonSocial());
            dto.setDestinoTipo("comercializadora");
        }

        dto.setCreatedAt(lote.getFechaCreacion());
        dto.setUpdatedAt(lote.getUpdatedAt());

        return dto;
    }
}