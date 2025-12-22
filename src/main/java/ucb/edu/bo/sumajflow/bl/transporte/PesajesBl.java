package ucb.edu.bo.sumajflow.bl.transporte;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ucb.edu.bo.sumajflow.bl.NotificacionBl;
import ucb.edu.bo.sumajflow.bl.cooperativa.AuditoriaLotesBl;
import ucb.edu.bo.sumajflow.dto.transporte.PesajeCreateDto;
import ucb.edu.bo.sumajflow.dto.transporte.PesajeResponseDto;
import ucb.edu.bo.sumajflow.entity.AsignacionCamion;
import ucb.edu.bo.sumajflow.entity.Pesajes;
import ucb.edu.bo.sumajflow.repository.AsignacionCamionRepository;
import ucb.edu.bo.sumajflow.repository.PesajesRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PesajesBl {

    private final PesajesRepository pesajesRepository;
    private final AsignacionCamionRepository asignacionCamionRepository;
    private final AuditoriaLotesBl auditoriaLotesBl;
    private final NotificacionBl notificacionBl;

    /**
     * Registrar un nuevo pesaje
     */
    @Transactional
    public PesajeResponseDto registrarPesaje(
            PesajeCreateDto dto,
            Integer usuarioId,
            String ipOrigen
    ) {
        log.info("Registrando pesaje - Asignación ID: {}, Tipo: {}",
                dto.getAsignacionCamionId(), dto.getTipoPesaje());

        // 1. Validaciones
        validarDatosPesaje(dto);

        // 2. Obtener asignación de camión
        AsignacionCamion asignacion = asignacionCamionRepository.findById(dto.getAsignacionCamionId())
                .orElseThrow(() -> new IllegalArgumentException("Asignación de camión no encontrada"));

        // 3. Validar que no exista ya un pesaje del mismo tipo
        validarPesajeUnico(asignacion, dto.getTipoPesaje());

        // 4. Validar estado de asignación según tipo de pesaje
        validarEstadoParaPesaje(asignacion, dto.getTipoPesaje());

        // 5. Calcular peso neto
        BigDecimal pesoNeto = dto.getPesoBruto().subtract(dto.getPesoTara());
        if (pesoNeto.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("El peso neto no puede ser negativo");
        }

        // 6. Crear pesaje
        Pesajes pesaje = crearYGuardarPesaje(dto, asignacion, pesoNeto);

        // 7. Registrar en auditoría
        registrarAuditoriaPesaje(pesaje, asignacion, ipOrigen);

        // 8. Notificar
        notificarPesajeRegistrado(pesaje, asignacion);

        log.info("Pesaje registrado exitosamente - ID: {}", pesaje.getId());

        return convertToDto(pesaje);
    }

    /**
     * Obtener pesajes de una asignación
     */
    @Transactional(readOnly = true)
    public List<PesajeResponseDto> getPesajesByAsignacion(Integer asignacionId) {
        log.debug("Obteniendo pesajes para asignación ID: {}", asignacionId);

        AsignacionCamion asignacion = asignacionCamionRepository.findById(asignacionId)
                .orElseThrow(() -> new IllegalArgumentException("Asignación no encontrada"));

        List<Pesajes> pesajes = pesajesRepository.findByAsignacionCamionId(asignacion);

        return pesajes.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    // ==================== MÉTODOS DE VALIDACIÓN ====================

    private void validarDatosPesaje(PesajeCreateDto dto) {
        if (dto.getAsignacionCamionId() == null) {
            throw new IllegalArgumentException("La asignación de camión es requerida");
        }

        if (dto.getTipoPesaje() == null || dto.getTipoPesaje().trim().isEmpty()) {
            throw new IllegalArgumentException("El tipo de pesaje es requerido");
        }

        if (!List.of("cooperativa", "ingenio", "comercializadora").contains(dto.getTipoPesaje())) {
            throw new IllegalArgumentException(
                    "Tipo de pesaje inválido. Debe ser: cooperativa, ingenio o comercializadora"
            );
        }

        if (dto.getPesoBruto() == null || dto.getPesoBruto().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("El peso bruto debe ser mayor a 0");
        }

        if (dto.getPesoTara() == null || dto.getPesoTara().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("El peso tara no puede ser negativo");
        }

        if (dto.getPesoBruto().compareTo(dto.getPesoTara()) < 0) {
            throw new IllegalArgumentException("El peso bruto no puede ser menor que el peso tara");
        }
    }

    private void validarPesajeUnico(AsignacionCamion asignacion, String tipoPesaje) {
        boolean existe = pesajesRepository.existsByAsignacionAndTipo(asignacion, tipoPesaje);
        if (existe) {
            throw new IllegalArgumentException(
                    "Ya existe un pesaje de tipo '" + tipoPesaje + "' para esta asignación"
            );
        }
    }

    private void validarEstadoParaPesaje(AsignacionCamion asignacion, String tipoPesaje) {
        String estadoActual = asignacion.getEstado();

        // Mapeo de estados requeridos según tipo de pesaje
        Map<String, List<String>> estadosPermitidos = Map.of(
                "cooperativa", List.of("En camino a balanza cooperativa"),
                "ingenio", List.of("En camino a balanza ingenio", "En camino a balanza comercializadora"),
                "comercializadora", List.of("En camino a balanza comercializadora")
        );

        List<String> permitidos = estadosPermitidos.get(tipoPesaje);
        if (permitidos != null && !permitidos.contains(estadoActual)) {
            throw new IllegalArgumentException(
                    "No se puede registrar pesaje de tipo '" + tipoPesaje +
                            "' en el estado actual: " + estadoActual
            );
        }
    }

    // ==================== MÉTODOS DE CREACIÓN ====================

    private Pesajes crearYGuardarPesaje(
            PesajeCreateDto dto,
            AsignacionCamion asignacion,
            BigDecimal pesoNeto
    ) {
        Pesajes pesaje = Pesajes.builder()
                .asignacionCamionId(asignacion)
                .tipoPesaje(dto.getTipoPesaje())
                .pesoBruto(dto.getPesoBruto())
                .pesoTara(dto.getPesoTara())
                .pesoNeto(pesoNeto)
                .fechaPesaje(LocalDateTime.now())
                .observaciones(dto.getObservaciones())
                .build();

        return pesajesRepository.save(pesaje);
    }

    // ==================== MÉTODOS DE AUDITORÍA ====================

    private void registrarAuditoriaPesaje(
            Pesajes pesaje,
            AsignacionCamion asignacion,
            String ipOrigen
    ) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("pesaje_id", pesaje.getId());
        metadata.put("tipo_pesaje", pesaje.getTipoPesaje());
        metadata.put("peso_neto", pesaje.getPesoNeto());
        metadata.put("asignacion_id", asignacion.getId());
        metadata.put("lote_id", asignacion.getLotesId().getId());

        auditoriaLotesBl.registrarAuditoria(
                asignacion.getLotesId().getId(),
                "sistema",
                null,
                null,
                "REGISTRAR_PESAJE",
                "Pesaje registrado - Tipo: " + pesaje.getTipoPesaje() +
                        ", Peso neto: " + pesaje.getPesoNeto() + " kg",
                pesaje.getObservaciones(),
                metadata,
                ipOrigen
        );
    }

    // ==================== MÉTODOS DE NOTIFICACIÓN ====================

    private void notificarPesajeRegistrado(Pesajes pesaje, AsignacionCamion asignacion) {
        Integer socioUsuarioId = asignacion.getLotesId().getMinasId()
                .getSocioId().getUsuariosId().getId();

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("pesajeId", pesaje.getId());
        metadata.put("loteId", asignacion.getLotesId().getId());
        metadata.put("tipoPesaje", pesaje.getTipoPesaje());
        metadata.put("pesoNeto", pesaje.getPesoNeto());

        notificacionBl.crearNotificacion(
                socioUsuarioId,
                "info",
                "Pesaje registrado",
                "Se ha registrado un pesaje en " + pesaje.getTipoPesaje() +
                        " con peso neto de " + pesaje.getPesoNeto() + " kg",
                metadata
        );
    }

    // ==================== MÉTODOS DE CONVERSIÓN ====================

    private PesajeResponseDto convertToDto(Pesajes pesaje) {
        return new PesajeResponseDto(
                pesaje.getId(),
                pesaje.getAsignacionCamionId().getId(),
                pesaje.getTipoPesaje(),
                pesaje.getPesoBruto(),
                pesaje.getPesoTara(),
                pesaje.getPesoNeto(),
                pesaje.getFechaPesaje(),
                pesaje.getObservaciones()
        );
    }
}