package ucb.edu.bo.sumajflow.controller.tracking;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ucb.edu.bo.sumajflow.bl.routing.RoutingService;
import ucb.edu.bo.sumajflow.bl.tracking.TrackingBl;
import ucb.edu.bo.sumajflow.dto.routing.RutaCalculadaDto;
import ucb.edu.bo.sumajflow.dto.tracking.*;
import ucb.edu.bo.sumajflow.entity.*;
import ucb.edu.bo.sumajflow.repository.AsignacionCamionRepository;
import ucb.edu.bo.sumajflow.repository.TransportistaRepository;
import ucb.edu.bo.sumajflow.utils.JwtUtil;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Controlador específico para la aplicación móvil del transportista
 * Endpoints optimizados para el flujo del conductor
 */
@Slf4j
@RestController
@RequestMapping("/movil/transportista")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class TransportistaMovilController {

    private final TrackingBl trackingBl;
    private final AsignacionCamionRepository asignacionCamionRepository;
    private final TransportistaRepository transportistaRepository;
    private final RoutingService routingService;
    private final JwtUtil jwtUtil;

    /**
     * Obtiene los lotes asignados al transportista autenticado
     * GET /movil/transportista/mis-lotes
     *
     * Retorna lotes pendientes y activos para mostrar en el dashboard
     */
    @GetMapping("/mis-lotes")
    public ResponseEntity<Map<String, Object>> getMisLotes(
            @RequestHeader("Authorization") String token,
            @RequestParam(required = false, defaultValue = "activos") String filtro) {

        Map<String, Object> response = new HashMap<>();

        try {
            Integer usuarioId = extractUsuarioId(token);

            // Obtener transportista por usuario
            var transportista = transportistaRepository.findByUsuariosId_Id(usuarioId)
                    .orElseThrow(() -> new IllegalArgumentException("Transportista no encontrado"));

            // Obtener asignaciones del transportista
            List<AsignacionCamion> asignaciones;

            if ("activos".equals(filtro)) {
                asignaciones = asignacionCamionRepository.findByTransportistaIdAndEstadoNotIn(
                        transportista,
                        List.of("Viaje terminado", "Cancelado por rechazo")
                );
            } else if ("completados".equals(filtro)) {
                asignaciones = asignacionCamionRepository.findByTransportistaIdAndEstado(
                        transportista,
                        "Viaje terminado"
                );
            } else {
                asignaciones = asignacionCamionRepository.findByTransportistaId(transportista);
            }

            // Convertir a DTOs resumidos
            List<LoteAsignadoResumenDto> lotesResumen = asignaciones.stream()
                    .map(this::convertToLoteResumen)
                    .collect(Collectors.toList());

            response.put("success", true);
            response.put("data", lotesResumen);
            response.put("total", lotesResumen.size());

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);

        } catch (Exception e) {
            log.error("Error al obtener lotes del transportista", e);
            response.put("success", false);
            response.put("message", "Error interno del servidor");
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * Obtiene el detalle completo de un lote asignado para iniciar el viaje
     * GET /movil/transportista/lote/{asignacionId}/detalle
     */
    @GetMapping("/lote/{asignacionId}/detalle")
    public ResponseEntity<Map<String, Object>> getDetalleLoteParaViaje(
            @PathVariable Integer asignacionId,
            @RequestHeader("Authorization") String token) {

        Map<String, Object> response = new HashMap<>();

        try {
            Integer usuarioId = extractUsuarioId(token);

            // Validar que la asignación pertenece al transportista
            AsignacionCamion asignacion = asignacionCamionRepository.findById(asignacionId)
                    .orElseThrow(() -> new IllegalArgumentException("Asignación no encontrada"));

            if (!asignacion.getTransportistaId().getUsuariosId().getId().equals(usuarioId)) {
                throw new SecurityException("No tienes permiso para ver esta asignación");
            }

            // Construir detalle completo
            LoteDetalleViajeDto detalle = construirDetalleViaje(asignacion);

            response.put("success", true);
            response.put("data", detalle);

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException | SecurityException e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);

        } catch (Exception e) {
            log.error("Error al obtener detalle del lote", e);
            response.put("success", false);
            response.put("message", "Error interno del servidor");
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * Inicia el viaje (cambia estado a "En camino a la mina")
     * POST /movil/transportista/lote/{asignacionId}/iniciar-viaje
     */
    @PostMapping("/lote/{asignacionId}/iniciar-viaje")
    public ResponseEntity<Map<String, Object>> iniciarViaje(
            @PathVariable Integer asignacionId,
            @RequestBody(required = false) Map<String, Double> ubicacionInicial,
            @RequestHeader("Authorization") String token) {

        Map<String, Object> response = new HashMap<>();

        try {
            Integer usuarioId = extractUsuarioId(token);

            // Validar permisos
            AsignacionCamion asignacion = asignacionCamionRepository.findById(asignacionId)
                    .orElseThrow(() -> new IllegalArgumentException("Asignación no encontrada"));

            if (!asignacion.getTransportistaId().getUsuariosId().getId().equals(usuarioId)) {
                throw new SecurityException("No tienes permiso para esta acción");
            }

            // Validar estado actual
            if (!"Esperando iniciar".equals(asignacion.getEstado()) &&
                    !"asignado".equals(asignacion.getEstado())) {
                throw new IllegalArgumentException("El viaje ya fue iniciado o no está en estado válido");
            }

            // Obtener ubicación inicial si se proporcionó
            Double lat = ubicacionInicial != null ? ubicacionInicial.get("lat") : null;
            Double lng = ubicacionInicial != null ? ubicacionInicial.get("lng") : null;

            // Iniciar tracking
            TrackingResponseDto tracking = trackingBl.iniciarTracking(asignacionId, lat, lng);

            response.put("success", true);
            response.put("message", "Viaje iniciado exitosamente");
            response.put("data", tracking);

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException | SecurityException e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);

        } catch (Exception e) {
            log.error("Error al iniciar viaje", e);
            response.put("success", false);
            response.put("message", "Error interno del servidor");
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * Obtiene el estado actual del viaje en curso
     * GET /movil/transportista/lote/{asignacionId}/estado-viaje
     */
    @GetMapping("/lote/{asignacionId}/estado-viaje")
    public ResponseEntity<Map<String, Object>> getEstadoViaje(
            @PathVariable Integer asignacionId,
            @RequestHeader("Authorization") String token) {

        Map<String, Object> response = new HashMap<>();

        try {
            TrackingResponseDto tracking = trackingBl.getTrackingByAsignacion(asignacionId);

            response.put("success", true);
            response.put("data", tracking);

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);

        } catch (Exception e) {
            log.error("Error al obtener estado del viaje", e);
            response.put("success", false);
            response.put("message", "Error interno del servidor");
            return ResponseEntity.internalServerError().body(response);
        }
    }

    // ==================== MÉTODOS AUXILIARES ====================

    private Integer extractUsuarioId(String token) {
        String cleanToken = token.replace("Bearer ", "");
        return jwtUtil.extractUsuarioId(cleanToken);
    }

    private LoteAsignadoResumenDto convertToLoteResumen(AsignacionCamion asignacion) {
        Lotes lote = asignacion.getLotesId();

        return LoteAsignadoResumenDto.builder()
                .asignacionId(asignacion.getId())
                .loteId(lote.getId())
                .codigoLote("LT-" + lote.getFechaCreacion().getYear() + "-" + String.format("%04d", lote.getId()))
                .minaNombre(lote.getMinasId().getNombre())
                .tipoOperacion(lote.getTipoOperacion())
                .tipoMineral(lote.getTipoMineral())
                .estado(asignacion.getEstado())
                .numeroCamion(asignacion.getNumeroCamion())
                .fechaAsignacion(asignacion.getFechaAsignacion())
                .mineralTags(obtenerMineralesTags(lote))
                .build();
    }

    private List<String> obtenerMineralesTags(Lotes lote) {
        return lote.getLoteMineralesList() != null
                ? lote.getLoteMineralesList().stream()
                .map(lm -> lm.getMineralesId().getNomenclatura())
                .collect(Collectors.toList())
                : List.of();
    }

    /**
     * Construye el detalle completo del viaje incluyendo coordenadas y cálculo de ruta
     */
    private LoteDetalleViajeDto construirDetalleViaje(AsignacionCamion asignacion) {
        Lotes lote = asignacion.getLotesId();
        var mina = lote.getMinasId();
        var socio = mina.getSocioId();
        var persona = socio.getUsuariosId().getPersona();
        var cooperativa = mina.getSectoresId().getCooperativaId();
        var sector = mina.getSectoresId();

        // ========== EXTRAER COORDENADAS DE TODOS LOS PUNTOS ==========

        // Punto 1: Mina (Origen)
        BigDecimal minaLat = mina.getLatitud();
        BigDecimal minaLng = mina.getLongitud();
        String minaColor = sector.getColor() != null ? sector.getColor() : "#1E3A8A";

        // Punto 2: Balanza Cooperativa
        BigDecimal balanzaCoopLat = null;
        BigDecimal balanzaCoopLng = null;
        if (!cooperativa.getBalanzaCooperativaList().isEmpty()) {
            var balanzaCoop = cooperativa.getBalanzaCooperativaList().getFirst();
            balanzaCoopLat = balanzaCoop.getLatitud();
            balanzaCoopLng = balanzaCoop.getLongitud();
        }

        // Puntos 3 y 4: Balanza Destino y Almacén (varían según tipo de operación)
        BigDecimal balanzaDestinoLat = null;
        BigDecimal balanzaDestinoLng = null;
        BigDecimal almacenLat = null;
        BigDecimal almacenLng = null;
        String destinoNombre = null;
        String destinoTipo = null;
        String destinoColor = null;

        if ("procesamiento_planta".equals(lote.getTipoOperacion())) {
            // Es un ingenio
            var ingenio = lote.getLoteIngenioList().getFirst().getIngenioMineroId();
            destinoNombre = ingenio.getRazonSocial();
            destinoTipo = "Ingenio";
            destinoColor = "#059669"; // Verde para ingenios

            if (!ingenio.getBalanzasIngenioList().isEmpty()) {
                var balanza = ingenio.getBalanzasIngenioList().getFirst();
                balanzaDestinoLat = balanza.getLatitud();
                balanzaDestinoLng = balanza.getLongitud();
            }
            if (!ingenio.getAlmacenesIngenioList().isEmpty()) {
                var almacen = ingenio.getAlmacenesIngenioList().getFirst();
                almacenLat = almacen.getLatitud();
                almacenLng = almacen.getLongitud();
            }
        } else {
            // Es una comercializadora
            var comercializadora = lote.getLoteComercializadoraList().getFirst().getComercializadoraId();
            destinoNombre = comercializadora.getRazonSocial();
            destinoTipo = "Comercializadora";
            destinoColor = "#DC2626"; // Rojo para comercializadoras

            if (!comercializadora.getBalanzasList().isEmpty()) {
                var balanza = comercializadora.getBalanzasList().getFirst();
                balanzaDestinoLat = balanza.getLatitud();
                balanzaDestinoLng = balanza.getLongitud();
            }
            if (!comercializadora.getAlmacenesList().isEmpty()) {
                var almacen = comercializadora.getAlmacenesList().getFirst();
                almacenLat = almacen.getLatitud();
                almacenLng = almacen.getLongitud();
            }
        }

        // ========== CALCULAR RUTA CON OSRM ==========

        Double distanciaKm = 0.0;
        Double tiempoHoras = 0.0;
        Boolean rutaExitosa = false;
        String metodoCalculo = "linea_recta";

        // Validar que tenemos todas las coordenadas necesarias
        if (minaLat != null && minaLng != null &&
                balanzaCoopLat != null && balanzaCoopLng != null &&
                balanzaDestinoLat != null && balanzaDestinoLng != null &&
                almacenLat != null && almacenLng != null) {

            try {
                RutaCalculadaDto ruta = routingService.calcularRutaCompleta(
                        minaLat, minaLng,
                        balanzaCoopLat, balanzaCoopLng,
                        balanzaDestinoLat, balanzaDestinoLng,
                        almacenLat, almacenLng
                );

                distanciaKm = ruta.getDistanciaKm();
                tiempoHoras = ruta.getTiempoHoras();
                rutaExitosa = ruta.getExitosa();
                metodoCalculo = ruta.getMetodoCalculo();

                log.info("Ruta calculada para lote {}: {} km, {} horas (método: {})",
                        lote.getId(),
                        String.format("%.2f", distanciaKm),
                        String.format("%.2f", tiempoHoras),
                        metodoCalculo);

            } catch (Exception e) {
                log.error("Error al calcular ruta para lote {}: {}", lote.getId(), e.getMessage());
            }
        } else {
            log.warn("Coordenadas incompletas para calcular ruta del lote {}", lote.getId());
        }

        // ========== CONSTRUIR WAYPOINTS PARA EL MAPA ==========

        LoteDetalleViajeDto.WaypointDto puntoOrigen = LoteDetalleViajeDto.WaypointDto.builder()
                .nombre(mina.getNombre())
                .tipo("mina")
                .latitud(minaLat != null ? minaLat.doubleValue() : null)
                .longitud(minaLng != null ? minaLng.doubleValue() : null)
                .color(minaColor)
                .orden(1)
                .build();

        LoteDetalleViajeDto.WaypointDto puntoBalanzaCoop = LoteDetalleViajeDto.WaypointDto.builder()
                .nombre(cooperativa.getRazonSocial())
                .tipo("balanza_coop")
                .latitud(balanzaCoopLat != null ? balanzaCoopLat.doubleValue() : null)
                .longitud(balanzaCoopLng != null ? balanzaCoopLng.doubleValue() : null)
                .color("#F59E0B") // Amarillo para balanzas
                .orden(2)
                .build();

        LoteDetalleViajeDto.WaypointDto puntoBalanzaDestino = LoteDetalleViajeDto.WaypointDto.builder()
                .nombre(destinoNombre)
                .tipo("balanza_destino")
                .latitud(balanzaDestinoLat != null ? balanzaDestinoLat.doubleValue() : null)
                .longitud(balanzaDestinoLng != null ? balanzaDestinoLng.doubleValue() : null)
                .color("#F59E0B") // Amarillo para balanzas
                .orden(3)
                .build();

        LoteDetalleViajeDto.WaypointDto puntoAlmacenDestino = LoteDetalleViajeDto.WaypointDto.builder()
                .nombre(destinoNombre)
                .tipo("almacen")
                .latitud(almacenLat != null ? almacenLat.doubleValue() : null)
                .longitud(almacenLng != null ? almacenLng.doubleValue() : null)
                .color(destinoColor)
                .orden(4)
                .build();

        // ========== CONSTRUIR DTO FINAL ==========

        return LoteDetalleViajeDto.builder()
                .asignacionId(asignacion.getId())
                .loteId(lote.getId())
                .codigoLote("LT-" + lote.getFechaCreacion().getYear() + "-" + String.format("%04d", lote.getId()))
                // Información del socio
                .socioNombre(persona.getNombres() + " " + persona.getPrimerApellido() +
                        (persona.getSegundoApellido() != null ? " " + persona.getSegundoApellido() : ""))
                .socioTelefono(persona.getNumeroCelular())
                // Información de la mina (mantener compatibilidad)
                .minaNombre(mina.getNombre())
                .minaLat(minaLat != null ? minaLat.doubleValue() : null)
                .minaLng(minaLng != null ? minaLng.doubleValue() : null)
                // Información del viaje
                .tipoOperacion(lote.getTipoOperacion())
                .tipoMineral(lote.getTipoMineral())
                .mineralTags(obtenerMineralesTags(lote))
                .destinoNombre(destinoNombre)
                .destinoTipo(destinoTipo)
                // Ruta calculada
                .distanciaEstimadaKm(distanciaKm)
                .tiempoEstimadoHoras(tiempoHoras)
                .rutaCalculadaConExito(rutaExitosa)
                .metodoCalculo(metodoCalculo)
                // Waypoints para el mapa de Flutter
                .puntoOrigen(puntoOrigen)
                .puntoBalanzaCoop(puntoBalanzaCoop)
                .puntoBalanzaDestino(puntoBalanzaDestino)
                .puntoAlmacenDestino(puntoAlmacenDestino)
                // Estado
                .estado(asignacion.getEstado())
                .numeroCamion(asignacion.getNumeroCamion())
                .totalCamiones(lote.getCamionesSolicitados())
                .build();
    }
}