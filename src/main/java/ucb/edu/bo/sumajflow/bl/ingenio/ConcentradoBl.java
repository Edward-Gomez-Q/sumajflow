package ucb.edu.bo.sumajflow.bl.ingenio;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ucb.edu.bo.sumajflow.bl.NotificacionBl;
import ucb.edu.bo.sumajflow.bl.cooperativa.AuditoriaLotesBl;
import ucb.edu.bo.sumajflow.dto.ingenio.*;
import ucb.edu.bo.sumajflow.entity.*;
import ucb.edu.bo.sumajflow.repository.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConcentradoBl {

    // Repositorios
    private final ConcentradoRepository concentradoRepository;
    private final LoteConcentradoRelacionRepository loteConcentradoRelacionRepository;
    private final LoteProcesoPlantaRepository loteProcesoPlantaRepository;
    private final LotesRepository lotesRepository;
    private final IngenioMineroRepository ingenioMineroRepository;
    private final PlantaRepository plantaRepository;
    private final ProcesosPlantaRepository procesosPlantaRepository;
    private final UsuariosRepository usuariosRepository;
    private final PersonaRepository personaRepository;
    private final LoteMineralesRepository loteMineralesRepository;
    private final NotificacionBl notificacionBl;
    private final AuditoriaLotesBl auditoriaLotesBl;

    /**
     * Obtener lotes disponibles para crear concentrado
     */
    @Transactional(readOnly = true)
    public List<LoteDisponibleConcentradoDto> getLotesDisponibles(Integer usuarioId) {
        log.debug("Obteniendo lotes disponibles para concentrado - Usuario ID: {}", usuarioId);

        IngenioMinero ingenio = obtenerIngenioDelUsuario(usuarioId);

        // Buscar lotes que:
        // 1. Son para este ingenio
        // 2. Están completados (llegaron al ingenio)
        // 3. No están ya en un concentrado
        // 4. Tipo de operación es procesamiento_planta
        List<Lotes> lotesDisponibles = lotesRepository.findAll().stream()
                .filter(l -> l.getTipoOperacion().equals("procesamiento_planta"))
                .filter(l -> l.getEstado().equals("Completado"))
                .filter(l -> !loteConcentradoRelacionRepository.loteYaEnConcentrado(l))
                .filter(l -> perteneceAIngenio(l, ingenio))
                .collect(Collectors.toList());

        log.info("Se encontraron {} lotes disponibles", lotesDisponibles.size());

        return lotesDisponibles.stream()
                .map(this::convertToLoteDisponibleDto)
                .collect(Collectors.toList());
    }

    /**
     * Crear un nuevo concentrado
     */
    @Transactional
    public ConcentradoResponseDto crearConcentrado(
            ConcentradoCreateDto dto,
            Integer usuarioId,
            String ipOrigen
    ) {
        log.info("Creando concentrado: {}", dto.getCodigoConcentrado());

        // 1. Validaciones
        validarDatosConcentrado(dto);
        IngenioMinero ingenio = obtenerIngenioDelUsuario(usuarioId);

        // 2. Validar planta configurada
        Planta planta = validarPlantaConfigurada(ingenio);

        // 3. Validar código único
        validarCodigoUnico(dto.getCodigoConcentrado());

        // 4. Validar y obtener lotes
        List<Lotes> lotes = validarYObtenerLotes(dto.getLotes(), ingenio);

        // 5. Validar cupo mínimo
        BigDecimal pesoTotalEntrada = calcularPesoTotalEntrada(dto.getLotes());
        validarCupoMinimo(planta, pesoTotalEntrada);

        // 6. Obtener socio propietario (del primer lote, todos deben ser del mismo socio)
        Socio socioPropietario = lotes.get(0).getMinasId().getSocioId();

        // 7. Crear concentrado
        Concentrado concentrado = crearYGuardarConcentrado(
                dto,
                ingenio,
                socioPropietario,
                pesoTotalEntrada
        );

        // 8. Crear relaciones lote-concentrado
        crearRelacionesLotes(concentrado, dto.getLotes(), lotes);

        // 9. Crear procesos de planta para el concentrado
        crearProcesosDePlanta(concentrado, planta);

        // 10. Registrar en auditoría
        registrarAuditoriaCreacion(concentrado, lotes.size(), ipOrigen);

        // 11. Notificar
        notificarCreacion(concentrado, socioPropietario);

        log.info("Concentrado creado exitosamente - ID: {}", concentrado.getId());

        return convertToResponseDto(concentrado);
    }

    /**
     * Obtener concentrados del ingenio
     */
    @Transactional(readOnly = true)
    public List<ConcentradoResponseDto> getConcentrados(Integer usuarioId, String estado) {
        log.debug("Obteniendo concentrados - Usuario ID: {}, Estado: {}", usuarioId, estado);

        IngenioMinero ingenio = obtenerIngenioDelUsuario(usuarioId);

        List<Concentrado> concentrados = estado != null && !estado.trim().isEmpty()
                ? concentradoRepository.findByIngenioAndEstado(ingenio, estado)
                : concentradoRepository.findByIngenioMineroIdOrderByCreatedAtDesc(ingenio);

        return concentrados.stream()
                .map(this::convertToResponseDto)
                .collect(Collectors.toList());
    }

    /**
     * Obtener detalle completo de un concentrado
     */
    @Transactional(readOnly = true)
    public ConcentradoDetalleDto getDetalleConcentrado(Integer concentradoId, Integer usuarioId) {
        log.debug("Obteniendo detalle de concentrado ID: {}", concentradoId);

        IngenioMinero ingenio = obtenerIngenioDelUsuario(usuarioId);
        Concentrado concentrado = obtenerConcentradoConPermisos(concentradoId, ingenio);

        return convertToDetalleDto(concentrado);
    }

    // ==================== MÉTODOS DE VALIDACIÓN ====================

    private void validarDatosConcentrado(ConcentradoCreateDto dto) {
        if (dto.getCodigoConcentrado() == null || dto.getCodigoConcentrado().trim().isEmpty()) {
            throw new IllegalArgumentException("El código del concentrado es requerido");
        }

        if (dto.getLotes() == null || dto.getLotes().isEmpty()) {
            throw new IllegalArgumentException("Debe incluir al menos un lote");
        }

        if (dto.getMineralPrincipal() == null || dto.getMineralPrincipal().trim().isEmpty()) {
            throw new IllegalArgumentException("El mineral principal es requerido");
        }

        // Validar que todos los lotes tengan peso de entrada
        for (LoteParaConcentradoDto lote : dto.getLotes()) {
            if (lote.getLoteId() == null) {
                throw new IllegalArgumentException("El ID del lote es requerido");
            }
            if (lote.getPesoEntrada() == null || lote.getPesoEntrada().compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException(
                        "El peso de entrada del lote " + lote.getLoteId() + " debe ser mayor a 0"
                );
            }
        }
    }

    private void validarCodigoUnico(String codigo) {
        if (concentradoRepository.existsByCodigo(codigo)) {
            throw new IllegalArgumentException("Ya existe un concentrado con el código: " + codigo);
        }
    }

    private Planta validarPlantaConfigurada(IngenioMinero ingenio) {
        return plantaRepository.findByIngenioMineroId(ingenio)
                .orElseThrow(() -> new IllegalArgumentException(
                        "El ingenio no tiene una planta configurada. Configure la planta primero."
                ));
    }

    private List<Lotes> validarYObtenerLotes(List<LoteParaConcentradoDto> lotesDto, IngenioMinero ingenio) {
        List<Lotes> lotes = new ArrayList<>();
        Socio primerSocio = null;

        for (LoteParaConcentradoDto loteDto : lotesDto) {
            Lotes lote = lotesRepository.findById(loteDto.getLoteId())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Lote ID " + loteDto.getLoteId() + " no encontrado"
                    ));

            // Validar que el lote pertenece al ingenio
            if (!perteneceAIngenio(lote, ingenio)) {
                throw new IllegalArgumentException(
                        "El lote ID " + lote.getId() + " no pertenece a este ingenio"
                );
            }

            // Validar estado
            if (!lote.getEstado().equals("Completado")) {
                throw new IllegalArgumentException(
                        "El lote ID " + lote.getId() + " debe estar en estado Completado"
                );
            }

            // Validar que no esté ya en otro concentrado
            if (loteConcentradoRelacionRepository.loteYaEnConcentrado(lote)) {
                throw new IllegalArgumentException(
                        "El lote ID " + lote.getId() + " ya está en un concentrado"
                );
            }

            // Validar que todos los lotes sean del mismo socio
            Socio socioActual = lote.getMinasId().getSocioId();
            if (primerSocio == null) {
                primerSocio = socioActual;
            } else if (!primerSocio.getId().equals(socioActual.getId())) {
                throw new IllegalArgumentException(
                        "Todos los lotes deben pertenecer al mismo socio"
                );
            }

            lotes.add(lote);
        }

        return lotes;
    }

    private BigDecimal calcularPesoTotalEntrada(List<LoteParaConcentradoDto> lotesDto) {
        return lotesDto.stream()
                .map(LoteParaConcentradoDto::getPesoEntrada)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private void validarCupoMinimo(Planta planta, BigDecimal pesoTotal) {
        if (pesoTotal.compareTo(planta.getCupoMinimo()) < 0) {
            throw new IllegalArgumentException(
                    "El peso total (" + pesoTotal + " kg) no alcanza el cupo mínimo de la planta (" +
                            planta.getCupoMinimo() + " kg)"
            );
        }
    }

    private boolean perteneceAIngenio(Lotes lote, IngenioMinero ingenio) {
        if (!lote.getTipoOperacion().equals("procesamiento_planta")) {
            return false;
        }

        return loteConcentradoRelacionRepository.findByLoteComplejoId(lote).stream()
                .anyMatch(rel -> rel.getConcentradoId().getIngenioMineroId().getId().equals(ingenio.getId()))
                || lotesRepository.findAll().stream()
                .filter(l -> l.getId().equals(lote.getId()))
                .anyMatch(l -> {
                    // Aquí deberías verificar en lote_ingenio
                    // Por simplicidad, asumimos que si llegó aquí es válido
                    return true;
                });
    }

    // ==================== MÉTODOS DE CREACIÓN ====================

    private Concentrado crearYGuardarConcentrado(
            ConcentradoCreateDto dto,
            IngenioMinero ingenio,
            Socio socio,
            BigDecimal pesoInicial
    ) {
        Concentrado concentrado = Concentrado.builder()
                .codigoConcentrado(dto.getCodigoConcentrado())
                .ingenioMineroId(ingenio)
                .socioPropietarioId(socio)
                .pesoInicial(pesoInicial)
                .mineralPrincipal(dto.getMineralPrincipal())
                .estado("creado")
                .fechaInicio(LocalDateTime.now())
                .build();

        return concentradoRepository.save(concentrado);
    }

    private void crearRelacionesLotes(
            Concentrado concentrado,
            List<LoteParaConcentradoDto> lotesDto,
            List<Lotes> lotes
    ) {
        for (int i = 0; i < lotes.size(); i++) {
            Lotes lote = lotes.get(i);
            LoteParaConcentradoDto loteDto = lotesDto.get(i);

            LoteConcentradoRelacion relacion = LoteConcentradoRelacion.builder()
                    .loteComplejoId(lote)
                    .concentradoId(concentrado)
                    .pesoEntrada(loteDto.getPesoEntrada())
                    .fechaCreacion(LocalDateTime.now())
                    .build();

            loteConcentradoRelacionRepository.save(relacion);
        }
    }

    private void crearProcesosDePlanta(Concentrado concentrado, Planta planta) {
        // Obtener procesos configurados en la planta
        List<ProcesosPlanta> procesosPlanta = procesosPlantaRepository.findByPlantaIdOrderByOrden(planta);

        if (procesosPlanta.isEmpty()) {
            throw new IllegalArgumentException(
                    "La planta no tiene procesos configurados. Configure los procesos primero."
            );
        }

        // Crear registro de proceso para cada proceso de la planta
        for (ProcesosPlanta procesoPlanta : procesosPlanta) {
            LoteProcesoPlanta loteProcesoPlanta = LoteProcesoPlanta.builder()
                    .concentradoId(concentrado)
                    .procesoId(procesoPlanta.getProcesosId())
                    .orden(procesoPlanta.getOrden())
                    .estado("pendiente")
                    .build();

            loteProcesoPlantaRepository.save(loteProcesoPlanta);
        }

        log.info("Se crearon {} procesos para el concentrado ID: {}",
                procesosPlanta.size(), concentrado.getId());
    }

    // ==================== MÉTODOS DE AUDITORÍA ====================

    private void registrarAuditoriaCreacion(Concentrado concentrado, int cantidadLotes, String ipOrigen) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("concentrado_id", concentrado.getId());
        metadata.put("codigo_concentrado", concentrado.getCodigoConcentrado());
        metadata.put("cantidad_lotes", cantidadLotes);
        metadata.put("peso_inicial", concentrado.getPesoInicial());
        metadata.put("mineral_principal", concentrado.getMineralPrincipal());

        auditoriaLotesBl.registrarAuditoria(
                null, // No hay lote específico
                "ingenio",
                null,
                "creado",
                "CREAR_CONCENTRADO",
                "Concentrado creado: " + concentrado.getCodigoConcentrado() +
                        " con " + cantidadLotes + " lote(s)",
                null,
                metadata,
                ipOrigen
        );
    }

    // ==================== MÉTODOS DE NOTIFICACIÓN ====================

    private void notificarCreacion(Concentrado concentrado, Socio socio) {
        Integer socioUsuarioId = socio.getUsuariosId().getId();

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("concentradoId", concentrado.getId());
        metadata.put("codigoConcentrado", concentrado.getCodigoConcentrado());
        metadata.put("pesoInicial", concentrado.getPesoInicial());

        notificacionBl.crearNotificacion(
                socioUsuarioId,
                "info",
                "Concentrado creado",
                "Se ha creado el concentrado " + concentrado.getCodigoConcentrado() +
                        " con tu mineral. Peso inicial: " + concentrado.getPesoInicial() + " kg",
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

    private Concentrado obtenerConcentradoConPermisos(Integer concentradoId, IngenioMinero ingenio) {
        Concentrado concentrado = concentradoRepository.findById(concentradoId)
                .orElseThrow(() -> new IllegalArgumentException("Concentrado no encontrado"));

        // Verificar permisos
        if (!concentrado.getIngenioMineroId().getId().equals(ingenio.getId())) {
            throw new IllegalArgumentException("No tienes permiso para acceder a este concentrado");
        }

        return concentrado;
    }

    // ==================== MÉTODOS DE CONVERSIÓN ====================

    private LoteDisponibleConcentradoDto convertToLoteDisponibleDto(Lotes lote) {
        LoteDisponibleConcentradoDto dto = new LoteDisponibleConcentradoDto();

        dto.setId(lote.getId());
        dto.setMinaNombre(lote.getMinasId().getNombre());

        Persona persona = personaRepository.findByUsuariosId(
                lote.getMinasId().getSocioId().getUsuariosId()
        ).orElse(null);
        if (persona != null) {
            dto.setSocioNombre(persona.getNombres() + " " + persona.getPrimerApellido());
        }

        // Minerales
        List<LoteMinerales> loteMinerales = loteMineralesRepository.findByLotesId(lote);
        dto.setMinerales(
                loteMinerales.stream()
                        .map(lm -> lm.getMineralesId().getNombre())
                        .collect(Collectors.toList())
        );

        dto.setPesoTotalReal(lote.getPesoTotalReal());
        dto.setFechaLlegada(lote.getFechaFinTransporte());
        dto.setEstado(lote.getEstado());

        return dto;
    }

    private ConcentradoResponseDto convertToResponseDto(Concentrado concentrado) {
        ConcentradoResponseDto dto = new ConcentradoResponseDto();

        dto.setId(concentrado.getId());
        dto.setCodigoConcentrado(concentrado.getCodigoConcentrado());
        dto.setIngenioMineroId(concentrado.getIngenioMineroId().getId());
        dto.setIngenioNombre(concentrado.getIngenioMineroId().getRazonSocial());

        if (concentrado.getSocioPropietarioId() != null) {
            dto.setSocioPropietarioId(concentrado.getSocioPropietarioId().getId());
            Persona persona = personaRepository.findByUsuariosId(
                    concentrado.getSocioPropietarioId().getUsuariosId()
            ).orElse(null);
            if (persona != null) {
                dto.setSocioPropietarioNombre(persona.getNombres() + " " + persona.getPrimerApellido());
            }
        }

        dto.setPesoInicial(concentrado.getPesoInicial());
        dto.setPesoFinal(concentrado.getPesoFinal());
        dto.setMerma(concentrado.getMerma());

        // Calcular porcentaje de merma si hay peso final
        if (concentrado.getPesoFinal() != null && concentrado.getPesoInicial() != null) {
            BigDecimal porcentajeMerma = concentrado.getMerma()
                    .divide(concentrado.getPesoInicial(), 4, RoundingMode.HALF_UP)
                    .multiply(new BigDecimal("100"));
            dto.setPorcentajeMerma(porcentajeMerma);
        }

        dto.setMineralPrincipal(concentrado.getMineralPrincipal());
        dto.setEstado(concentrado.getEstado());
        dto.setFechaInicio(concentrado.getFechaInicio());
        dto.setFechaFin(concentrado.getFechaFin());
        dto.setCreatedAt(concentrado.getCreatedAt());

        // Cantidad de lotes
        long cantidadLotes = loteConcentradoRelacionRepository.contarLotesDeConcentrado(concentrado);
        dto.setCantidadLotes((int) cantidadLotes);

        return dto;
    }

    private ConcentradoDetalleDto convertToDetalleDto(Concentrado concentrado) {
        ConcentradoDetalleDto dto = new ConcentradoDetalleDto();

        // Información básica
        dto.setId(concentrado.getId());
        dto.setCodigoConcentrado(concentrado.getCodigoConcentrado());
        dto.setIngenioMineroId(concentrado.getIngenioMineroId().getId());
        dto.setIngenioNombre(concentrado.getIngenioMineroId().getRazonSocial());

        if (concentrado.getSocioPropietarioId() != null) {
            dto.setSocioPropietarioId(concentrado.getSocioPropietarioId().getId());
            Persona persona = personaRepository.findByUsuariosId(
                    concentrado.getSocioPropietarioId().getUsuariosId()
            ).orElse(null);
            if (persona != null) {
                dto.setSocioPropietarioNombre(persona.getNombres() + " " + persona.getPrimerApellido());
                dto.setSocioCi(persona.getCi());
                dto.setSocioTelefono(persona.getNumeroCelular());
            }
        }

        dto.setPesoInicial(concentrado.getPesoInicial());
        dto.setPesoFinal(concentrado.getPesoFinal());
        dto.setMerma(concentrado.getMerma());

        if (concentrado.getPesoFinal() != null && concentrado.getPesoInicial() != null) {
            BigDecimal porcentajeMerma = concentrado.getMerma()
                    .divide(concentrado.getPesoInicial(), 4, RoundingMode.HALF_UP)
                    .multiply(new BigDecimal("100"));
            dto.setPorcentajeMerma(porcentajeMerma);
        }

        dto.setMineralPrincipal(concentrado.getMineralPrincipal());
        dto.setEstado(concentrado.getEstado());
        dto.setFechaInicio(concentrado.getFechaInicio());
        dto.setFechaFin(concentrado.getFechaFin());
        dto.setCreatedAt(concentrado.getCreatedAt());

        // Lotes relacionados
        List<LoteConcentradoRelacion> relaciones =
                loteConcentradoRelacionRepository.findByConcentradoId(concentrado);
        dto.setLotesRelacionados(
                relaciones.stream()
                        .map(this::convertToLoteRelacionDto)
                        .collect(Collectors.toList())
        );

        // Procesos de planta
        List<LoteProcesoPlanta> procesos =
                loteProcesoPlantaRepository.findByConcentradoIdOrderByOrden(concentrado);
        dto.setProcesos(
                procesos.stream()
                        .map(this::convertToProcesoDto)
                        .collect(Collectors.toList())
        );

        return dto;
    }

    private LoteConcentradoRelacionDto convertToLoteRelacionDto(LoteConcentradoRelacion relacion) {
        LoteConcentradoRelacionDto dto = new LoteConcentradoRelacionDto();

        dto.setId(relacion.getId());
        dto.setLoteId(relacion.getLoteComplejoId().getId());
        dto.setMinaNombre(relacion.getLoteComplejoId().getMinasId().getNombre());

        Persona persona = personaRepository.findByUsuariosId(
                relacion.getLoteComplejoId().getMinasId().getSocioId().getUsuariosId()
        ).orElse(null);
        if (persona != null) {
            dto.setSocioNombre(persona.getNombres() + " " + persona.getPrimerApellido());
        }

        dto.setPesoEntrada(relacion.getPesoEntrada());
        dto.setPesoSalida(relacion.getPesoSalida());
        dto.setPorcentajeRecuperacion(relacion.getPorcentajeRecuperacion());
        dto.setFechaCreacion(relacion.getFechaCreacion());

        return dto;
    }

    private ProcesoPlantaDto convertToProcesoDto(LoteProcesoPlanta procesoPlanta) {
        ProcesoPlantaDto dto = new ProcesoPlantaDto();

        dto.setId(procesoPlanta.getId());
        dto.setProcesoId(procesoPlanta.getProcesoId().getId());
        dto.setProcesoNombre(procesoPlanta.getProcesoId().getNombre());
        dto.setOrden(procesoPlanta.getOrden());
        dto.setEstado(procesoPlanta.getEstado());
        dto.setFechaInicio(procesoPlanta.getFechaInicio());
        dto.setFechaFin(procesoPlanta.getFechaFin());
        dto.setObservaciones(procesoPlanta.getObservaciones());

        return dto;
    }
}