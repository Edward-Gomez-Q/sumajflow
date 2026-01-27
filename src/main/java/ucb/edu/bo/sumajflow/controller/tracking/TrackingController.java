package ucb.edu.bo.sumajflow.controller.tracking;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ucb.edu.bo.sumajflow.bl.tracking.TrackingBl;
import ucb.edu.bo.sumajflow.dto.tracking.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Controlador REST para operaciones de tracking GPS
 */
@Slf4j
@RestController
@RequestMapping("/tracking")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class TrackingController {

    private final TrackingBl trackingBl;

    // ==================== INICIAR TRACKING ====================

    /**
     * Inicia el tracking para una asignación de camión
     * POST /tracking/iniciar
     *
     * Body:
     * {
     *   "asignacionCamionId": 15,
     *   "latInicial": -19.5836,
     *   "lngInicial": -65.7531
     * }
     */
    @PostMapping("/iniciar")
    public ResponseEntity<Map<String, Object>> iniciarTracking(
            @Valid @RequestBody IniciarTrackingDto dto,
            @RequestHeader("Authorization") String token) {

        Map<String, Object> response = new HashMap<>();

        try {
            log.info("Iniciando tracking para asignación ID: {}", dto.getAsignacionCamionId());

            TrackingResponseDto tracking = trackingBl.iniciarTracking(
                    dto.getAsignacionCamionId(),
                    dto.getLatInicial(),
                    dto.getLngInicial());

            response.put("success", true);
            response.put("message", "Tracking iniciado exitosamente");
            response.put("data", tracking);

            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (IllegalArgumentException e) {
            log.warn("Error de validación al iniciar tracking: {}", e.getMessage());
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);

        } catch (Exception e) {
            log.error("Error al iniciar tracking", e);
            response.put("success", false);
            response.put("message", "Error interno del servidor: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    // ==================== ACTUALIZAR UBICACIÓN ====================

    /**
     * Actualiza la ubicación actual del camión
     * POST /tracking/ubicacion
     *
     * Body:
     * {
     *   "asignacionCamionId": 15,
     *   "lat": -19.5836,
     *   "lng": -65.7531,
     *   "precision": 10.5,
     *   "velocidad": 45.2,
     *   "rumbo": 180.0,
     *   "altitud": 4000.0,
     *   "timestampCaptura": "2025-01-07T14:30:00",
     *   "esOffline": false
     * }
     */
    @PostMapping("/ubicacion")
    public ResponseEntity<Map<String, Object>> actualizarUbicacion(
            @Valid @RequestBody ActualizarUbicacionDto dto,
            @RequestHeader("Authorization") String token) {

        Map<String, Object> response = new HashMap<>();

        try {
            log.debug("Actualizando ubicación - Asignación: {}, Lat: {}, Lng: {}",
                    dto.getAsignacionCamionId(), dto.getLat(), dto.getLng());

            ActualizacionUbicacionResponseDto resultado = trackingBl.actualizarUbicacion(dto);

            response.put("success", true);
            response.put("data", resultado);

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            log.warn("Error de validación al actualizar ubicación: {}", e.getMessage());
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);

        } catch (Exception e) {
            log.error("Error al actualizar ubicación", e);
            response.put("success", false);
            response.put("message", "Error interno del servidor: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    // ==================== SINCRONIZACIÓN OFFLINE ====================

    /**
     * Sincroniza ubicaciones capturadas offline
     * POST /tracking/sincronizar
     *
     * Body:
     * {
     *   "asignacionCamionId": 15,
     *   "ubicaciones": [
     *     {"lat": -19.5836, "lng": -65.7531, "timestamp": "2025-01-07T14:30:00", ...},
     *     {"lat": -19.5840, "lng": -65.7535, "timestamp": "2025-01-07T14:30:30", ...}
     *   ]
     * }
     */
    @PostMapping("/sincronizar")
    public ResponseEntity<Map<String, Object>> sincronizarUbicaciones(
            @Valid @RequestBody SincronizarUbicacionesDto dto,
            @RequestHeader("Authorization") String token) {

        Map<String, Object> response = new HashMap<>();

        try {
            log.info("Sincronizando {} ubicaciones para asignación ID: {}",
                    dto.getUbicaciones().size(), dto.getAsignacionCamionId());

            SincronizacionResponseDto resultado = trackingBl.sincronizarUbicacionesOffline(
                    dto.getAsignacionCamionId(),
                    dto.getUbicaciones());

            response.put("success", resultado.getSuccess());
            response.put("data", resultado);

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            log.warn("Error de validación al sincronizar: {}", e.getMessage());
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);

        } catch (Exception e) {
            log.error("Error al sincronizar ubicaciones", e);
            response.put("success", false);
            response.put("message", "Error interno del servidor: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    // ==================== PUNTOS DE CONTROL ====================

    /**
     * Registra llegada a un punto de control
     * POST /tracking/punto-control/llegada
     */
    @PostMapping("/punto-control/llegada")
    public ResponseEntity<Map<String, Object>> registrarLlegada(
            @Valid @RequestBody RegistrarPuntoControlDto dto,
            @RequestHeader("Authorization") String token) {

        Map<String, Object> response = new HashMap<>();

        try {
            log.info("Registrando llegada a {} para asignación ID: {}",
                    dto.getTipoPunto(), dto.getAsignacionCamionId());

            TrackingResponseDto tracking = trackingBl.registrarLlegadaPuntoControl(
                    dto.getAsignacionCamionId(),
                    dto.getTipoPunto(),
                    dto.getLat(),
                    dto.getLng(),
                    dto.getObservaciones());

            response.put("success", true);
            response.put("message", "Llegada registrada exitosamente");
            response.put("data", tracking);

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            log.warn("Error al registrar llegada: {}", e.getMessage());
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);

        } catch (Exception e) {
            log.error("Error al registrar llegada", e);
            response.put("success", false);
            response.put("message", "Error interno del servidor: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * Registra salida de un punto de control
     * POST /tracking/punto-control/salida
     */
    @PostMapping("/punto-control/salida")
    public ResponseEntity<Map<String, Object>> registrarSalida(
            @Valid @RequestBody RegistrarPuntoControlDto dto,
            @RequestHeader("Authorization") String token) {

        Map<String, Object> response = new HashMap<>();

        try {
            log.info("Registrando salida de {} para asignación ID: {}",
                    dto.getTipoPunto(), dto.getAsignacionCamionId());

            TrackingResponseDto tracking = trackingBl.registrarSalidaPuntoControl(
                    dto.getAsignacionCamionId(),
                    dto.getTipoPunto(),
                    dto.getObservaciones());

            response.put("success", true);
            response.put("message", "Salida registrada exitosamente");
            response.put("data", tracking);

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            log.warn("Error al registrar salida: {}", e.getMessage());
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);

        } catch (Exception e) {
            log.error("Error al registrar salida", e);
            response.put("success", false);
            response.put("message", "Error interno del servidor: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    // ==================== CONSULTAS ====================

    /**
     * Obtiene el tracking de una asignación
     * GET /tracking/asignacion/{asignacionId}
     */
    @GetMapping("/asignacion/{asignacionId}")
    public ResponseEntity<Map<String, Object>> getTrackingByAsignacion(
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
            log.error("Error al obtener tracking", e);
            response.put("success", false);
            response.put("message", "Error interno del servidor: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * Obtiene el monitoreo de todos los camiones de un lote
     * GET /tracking/lote/{loteId}/monitoreo
     */
    @GetMapping("/lote/{loteId}/monitoreo")
    public ResponseEntity<Map<String, Object>> getMonitoreoLote(
            @PathVariable Integer loteId,
            @RequestHeader("Authorization") String token) {

        Map<String, Object> response = new HashMap<>();

        try {
            MonitoreoLoteDto monitoreo = trackingBl.getMonitoreoLote(loteId);

            response.put("success", true);
            response.put("data", monitoreo);

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);

        } catch (Exception e) {
            log.error("Error al obtener monitoreo del lote", e);
            response.put("success", false);
            response.put("message", "Error interno del servidor: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * Obtiene el historial de ubicaciones de una asignación
     * GET /tracking/asignacion/{asignacionId}/historial
     */
    @GetMapping("/asignacion/{asignacionId}/historial")
    public ResponseEntity<Map<String, Object>> getHistorialUbicaciones(
            @PathVariable Integer asignacionId,
            @RequestHeader("Authorization") String token) {

        Map<String, Object> response = new HashMap<>();

        try {
            HistorialUbicacionesDto historial = trackingBl.getHistorialUbicaciones(asignacionId);

            response.put("success", true);
            response.put("data", historial);

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);

        } catch (Exception e) {
            log.error("Error al obtener historial", e);
            response.put("success", false);
            response.put("message", "Error interno del servidor: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    @GetMapping("/asignacion/{asignacionId}/historial-por-estado")
    public ResponseEntity<Map<String, Object>> getHistorialPorEstado(
            @PathVariable Integer asignacionId,
            @RequestHeader("Authorization") String token) {

        Map<String, Object> response = new HashMap<>();

        try {
            HistorialPorEstadoDto historial = trackingBl.getHistorialPorEstado(asignacionId);

            response.put("success", true);
            response.put("data", historial);

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);

        } catch (Exception e) {
            log.error("Error al obtener historial por estado", e);
            response.put("success", false);
            response.put("message", "Error interno del servidor: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }
    /**
     * Obtiene el resumen final de tracking para asignaciones completadas
     * GET /tracking/asignacion/{asignacionId}/resumen-final
     */
    @GetMapping("/asignacion/{asignacionId}/resumen-final")
    public ResponseEntity<Map<String, Object>> getResumenFinal(
            @PathVariable Integer asignacionId,
            @RequestHeader("Authorization") String token) {

        Map<String, Object> response = new HashMap<>();

        try {
            ResumenFinalTrackingDto resumen = trackingBl.getResumenFinalTracking(asignacionId);

            response.put("success", true);
            response.put("data", resumen);

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);

        } catch (Exception e) {
            log.error("Error al obtener resumen final", e);
            response.put("success", false);
            response.put("message", "Error interno del servidor: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }
}