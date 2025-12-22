package ucb.edu.bo.sumajflow.bl.transporte;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ucb.edu.bo.sumajflow.bl.NotificacionBl;
import ucb.edu.bo.sumajflow.bl.cooperativa.AuditoriaLotesBl;
import ucb.edu.bo.sumajflow.dto.transporte.AsignacionCamionDetalleDto;
import ucb.edu.bo.sumajflow.dto.transporte.CambioEstadoAsignacionDto;
import ucb.edu.bo.sumajflow.dto.transporte.LoteTransporteDto;
import ucb.edu.bo.sumajflow.dto.transporte.PesajeResponseDto;
import ucb.edu.bo.sumajflow.entity.*;
import ucb.edu.bo.sumajflow.repository.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransporteBl {

    private final AsignacionCamionRepository asignacionCamionRepository;
    private final LotesRepository lotesRepository;
    private final PesajesRepository pesajesRepository;
    private final PersonaRepository personaRepository;
    private final AuditoriaLotesBl auditoriaLotesBl;
    private final NotificacionBl notificacionBl;

    // Mapeo de estados según tipo de operación
    private static final Map<String, List<String>> FLUJO_ESTADOS_VENTA_DIRECTA = Map.of(
            "Esperando iniciar", List.of("En camino a la mina"),
            "En camino a la mina", List.of("Esperando recoger mineral"),
            "Esperando recoger mineral", List.of("En camino a balanza cooperativa"),
            "En camino a balanza cooperativa", List.of("En camino a balanza comercializadora"),
            "En camino a balanza comercializadora", List.of("En camino al almacén"),
            "En camino al almacén", List.of("Viaje terminado")
    );

    private static final Map<String, List<String>> FLUJO_ESTADOS_PROCESAMIENTO = Map.of(
            "Esperando iniciar", List.of("En camino a la mina"),
            "En camino a la mina", List.of("Esperando recoger mineral"),
            "Esperando recoger mineral", List.of("En camino a balanza cooperativa"),
            "En camino a balanza cooperativa", List.of("En camino a balanza ingenio"),
            "En camino a balanza ingenio", List.of("En camino al almacén"),
            "En camino al almacén", List.of("Viaje terminado")
    );

    private static final Map<String, List<String>> FLUJO_ESTADOS_CONCENTRADO = Map.of(
            "Esperando iniciar viaje", List.of("En camino al ingenio"),
            "En camino al ingenio", List.of("Esperando recoger concentrado"),
            "Esperando recoger concentrado", List.of("En camino a balanza ingenio"),
            "En camino a balanza ingenio", List.of("En camino a balanza comercializadora"),
            "En camino a balanza comercializadora", List.of("En camino al almacén comercializadora"),
            "En camino al almacén comercializadora", List.of("Viaje terminado")
    );

    /**
     * Obtener asignaciones de un lote
     */
    @Transactional(readOnly = true)
    public List<AsignacionCamionDetalleDto> getAsignacionesByLote(Integer loteId) {
        log.debug("Obteniendo asignaciones para lote ID: {}", loteId);

        Lotes lote = lotesRepository.findById(loteId)
                .orElseThrow(() -> new IllegalArgumentException("Lote no encontrado"));

        List<AsignacionCamion> asignaciones = asignacionCamionRepository.findByLotesId(lote);

        return asignaciones.stream()
                .map(this::convertToDetalleDto)
                .collect(Collectors.toList());
    }

    /**
     * Obtener detalle de transporte de un lote
     */
    @Transactional(readOnly = true)
    public LoteTransporteDto getDetalleTransporte(Integer loteId) {
        log.debug("Obteniendo detalle de transporte para lote ID: {}", loteId);

        Lotes lote = lotesRepository.findById(loteId)
                .orElseThrow(() -> new IllegalArgumentException("Lote no encontrado"));

        return convertToLoteTransporteDto(lote);
    }

    /**
     * Cambiar estado de una asignación de camión
     */
    @Transactional
    public AsignacionCamionDetalleDto cambiarEstadoAsignacion(
            Integer asignacionId,
            CambioEstadoAsignacionDto dto,
            Integer usuarioId,
            String ipOrigen
    ) {
        log.info("Cambiando estado de asignación ID: {} a: {}", asignacionId, dto.getNuevoEstado());

        // 1. Validaciones
        AsignacionCamion asignacion = asignacionCamionRepository.findById(asignacionId)
                .orElseThrow(() -> new IllegalArgumentException("Asignación no encontrada"));

        validarCambioEstado(asignacion, dto.getNuevoEstado());

        String estadoAnterior = asignacion.getEstado();

        // 2. Actualizar estado de la asignación
        asignacion.setEstado(dto.getNuevoEstado());
        if (dto.getObservaciones() != null) {
            asignacion.setObservaciones(dto.getObservaciones());
        }

        // Actualizar fechas
        if (dto.getNuevoEstado().equals("En camino a la mina") && asignacion.getFechaInicio() == null) {
            asignacion.setFechaInicio(LocalDateTime.now());
        }
        if (dto.getNuevoEstado().equals("Viaje terminado")) {
            asignacion.setFechaFin(LocalDateTime.now());
        }

        asignacionCamionRepository.save(asignacion);

        // 3. Verificar si todos los camiones están en el nuevo estado
        Lotes lote = asignacion.getLotesId();
        boolean todosEnNuevoEstado = verificarTodosCamionesEnEstado(lote, dto.getNuevoEstado());

        // 4. Si todos están en el nuevo estado, actualizar estado del lote
        if (todosEnNuevoEstado) {
            actualizarEstadoLote(lote, dto.getNuevoEstado());
        }

        // 5. Registrar en auditoría
        registrarAuditoriaCambioEstado(asignacion, estadoAnterior, dto.getNuevoEstado(), ipOrigen);

        // 6. Notificar
        notificarCambioEstado(asignacion, estadoAnterior, dto.getNuevoEstado(), todosEnNuevoEstado);

        log.info("Estado de asignación cambiado exitosamente - ID: {}", asignacionId);

        return convertToDetalleDto(asignacion);
    }

    // ==================== MÉTODOS DE VALIDACIÓN ====================

    private void validarCambioEstado(AsignacionCamion asignacion, String nuevoEstado) {
        String estadoActual = asignacion.getEstado();
        Lotes lote = asignacion.getLotesId();

        // Obtener flujo de estados según tipo de operación y mineral
        Map<String, List<String>> flujoEstados = obtenerFlujoEstados(lote);

        // Validar que el nuevo estado sea válido según el flujo
        List<String> estadosPermitidos = flujoEstados.get(estadoActual);
        if (estadosPermitidos == null || !estadosPermitidos.contains(nuevoEstado)) {
            throw new IllegalArgumentException(
                    "No se puede cambiar de estado '" + estadoActual +
                            "' a '" + nuevoEstado + "' en este flujo de transporte"
            );
        }
    }

    private Map<String, List<String>> obtenerFlujoEstados(Lotes lote) {
        if (lote.getTipoMineral().equals("concentrado")) {
            return FLUJO_ESTADOS_CONCENTRADO;
        } else if (lote.getTipoOperacion().equals("venta_directa")) {
            return FLUJO_ESTADOS_VENTA_DIRECTA;
        } else { // procesamiento_planta
            return FLUJO_ESTADOS_PROCESAMIENTO;
        }
    }

    private boolean verificarTodosCamionesEnEstado(Lotes lote, String estado) {
        return asignacionCamionRepository.todasEnEstado(lote, estado);
    }

    // ==================== MÉTODOS DE ACTUALIZACIÓN ====================

    private void actualizarEstadoLote(Lotes lote, String nuevoEstadoAsignacion) {
        String nuevoEstadoLote = mapearEstadoAsignacionALote(nuevoEstadoAsignacion);

        if (nuevoEstadoLote != null && !lote.getEstado().equals(nuevoEstadoLote)) {
            lote.setEstado(nuevoEstadoLote);

            // Actualizar fechas
            if (nuevoEstadoLote.equals("En transporte")) {
                lote.setFechaInicioTransporte(LocalDateTime.now());
            }
            if (nuevoEstadoLote.equals("Completado")) {
                lote.setFechaFinTransporte(LocalDateTime.now());

                // Calcular peso total real
                BigDecimal pesoTotalReal = asignacionCamionRepository.calcularPesoTotalReal(lote);
                lote.setPesoTotalReal(pesoTotalReal);
            }

            lotesRepository.save(lote);

            log.info("Estado del lote actualizado - Lote ID: {}, Nuevo estado: {}",
                    lote.getId(), nuevoEstadoLote);
        }
    }

    private String mapearEstadoAsignacionALote(String estadoAsignacion) {
        // Mapeo de estado de asignación a estado de lote
        return switch (estadoAsignacion) {
            case "En camino a la mina" -> "En camino a la mina";
            case "Esperando recoger mineral" -> "Recogiendo mineral";
            case "En camino a balanza cooperativa" -> "En ruta a balanza cooperativa";
            case "En camino a balanza ingenio" -> "En ruta a balanza ingenio";
            case "En camino a balanza comercializadora" -> "En ruta a balanza comercializadora";
            case "En camino al almacén", "En camino al almacén comercializadora" -> "En ruta a almacén";
            case "Viaje terminado" -> "Completado";
            default -> null;
        };
    }

    // ==================== MÉTODOS DE AUDITORÍA ====================

    private void registrarAuditoriaCambioEstado(
            AsignacionCamion asignacion,
            String estadoAnterior,
            String estadoNuevo,
            String ipOrigen
    ) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("asignacion_id", asignacion.getId());
        metadata.put("transportista_id", asignacion.getTransportistaId().getId());
        metadata.put("numero_camion", asignacion.getNumeroCamion());

        auditoriaLotesBl.registrarAuditoria(
                asignacion.getLotesId().getId(),
                "transportista",
                estadoAnterior,
                estadoNuevo,
                "CAMBIO_ESTADO_TRANSPORTE",
                "Camión #" + asignacion.getNumeroCamion() +
                        " cambió de estado: " + estadoAnterior + " → " + estadoNuevo,
                asignacion.getObservaciones(),
                metadata,
                ipOrigen
        );
    }

    // ==================== MÉTODOS DE NOTIFICACIÓN ====================

    private void notificarCambioEstado(
            AsignacionCamion asignacion,
            String estadoAnterior,
            String estadoNuevo,
            boolean todosEnNuevoEstado
    ) {
        // Notificar al socio
        Integer socioUsuarioId = asignacion.getLotesId().getMinasId()
                .getSocioId().getUsuariosId().getId();

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("asignacionId", asignacion.getId());
        metadata.put("loteId", asignacion.getLotesId().getId());
        metadata.put("numeroCamion", asignacion.getNumeroCamion());
        metadata.put("estadoAnterior", estadoAnterior);
        metadata.put("estadoNuevo", estadoNuevo);

        String mensaje = todosEnNuevoEstado
                ? "Todos los camiones del lote están ahora en estado: " + estadoNuevo
                : "El camión #" + asignacion.getNumeroCamion() + " cambió a estado: " + estadoNuevo;

        notificacionBl.crearNotificacion(
                socioUsuarioId,
                "info",
                "Actualización de transporte",
                mensaje,
                metadata
        );
    }

    // ==================== MÉTODOS DE CONVERSIÓN ====================

    private AsignacionCamionDetalleDto convertToDetalleDto(AsignacionCamion asignacion) {
        AsignacionCamionDetalleDto dto = new AsignacionCamionDetalleDto();

        dto.setId(asignacion.getId());
        dto.setLoteId(asignacion.getLotesId().getId());
        dto.setNumeroCamion(asignacion.getNumeroCamion());
        dto.setEstado(asignacion.getEstado());
        dto.setFechaAsignacion(asignacion.getFechaAsignacion());
        dto.setFechaInicio(asignacion.getFechaInicio());
        dto.setFechaFin(asignacion.getFechaFin());
        dto.setObservaciones(asignacion.getObservaciones());

        // Información del transportista
        Transportista transportista = asignacion.getTransportistaId();
        dto.setTransportistaId(transportista.getId());
        dto.setPlacaVehiculo(transportista.getPlacaVehiculo());
        dto.setMarcaVehiculo(transportista.getMarcaVehiculo());
        dto.setModeloVehiculo(transportista.getModeloVehiculo());

        Persona persona = personaRepository.findByUsuariosId(transportista.getUsuariosId()).orElse(null);
        if (persona != null) {
            dto.setTransportistaNombre(persona.getNombres() + " " + persona.getPrimerApellido());
            dto.setTransportistaCi(persona.getCi());
            dto.setTransportistaTelefono(persona.getNumeroCelular());
        }

        // Información del lote
        Lotes lote = asignacion.getLotesId();
        dto.setMinaNombre(lote.getMinasId().getNombre());
        dto.setTipoOperacion(lote.getTipoOperacion());

        // Pesajes
        List<Pesajes> pesajes = pesajesRepository.findByAsignacionCamionId(asignacion);
        dto.setPesajes(pesajes.stream()
                .map(this::convertPesajeToDto)
                .collect(Collectors.toList())
        );

        return dto;
    }

    private PesajeResponseDto convertPesajeToDto(Pesajes pesaje) {
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

    private LoteTransporteDto convertToLoteTransporteDto(Lotes lote) {
        LoteTransporteDto dto = new LoteTransporteDto();

        dto.setId(lote.getId());
        dto.setMinaNombre(lote.getMinasId().getNombre());
        dto.setTipoOperacion(lote.getTipoOperacion());
        dto.setTipoMineral(lote.getTipoMineral());
        dto.setEstado(lote.getEstado());
        dto.setCamionlesSolicitados(lote.getCamionesSolicitados());
        dto.setPesoTotalEstimado(lote.getPesoTotalEstimado());
        dto.setFechaCreacion(lote.getFechaCreacion());

        // Asignaciones
        List<AsignacionCamion> asignaciones = asignacionCamionRepository.findByLotesId(lote);
        dto.setAsignaciones(asignaciones.stream()
                .map(this::convertToDetalleDto)
                .collect(Collectors.toList())
        );

        // Estadísticas
        long camionesCompletados = asignaciones.stream()
                .filter(a -> a.getEstado().equals("Viaje terminado"))
                .count();
        dto.setCamionesCompletados((int) camionesCompletados);

        BigDecimal pesoTotalReal = asignacionCamionRepository.calcularPesoTotalReal(lote);
        dto.setPesoTotalReal(pesoTotalReal);

        return dto;
    }
}