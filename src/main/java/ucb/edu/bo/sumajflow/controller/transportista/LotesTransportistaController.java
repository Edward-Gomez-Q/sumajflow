package ucb.edu.bo.sumajflow.controller.transportista;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ucb.edu.bo.sumajflow.bl.transporte.TransporteBl;
import ucb.edu.bo.sumajflow.dto.tracking.LoteAsignadoResumenDto;
import ucb.edu.bo.sumajflow.dto.tracking.LoteDetalleViajeDto;
import ucb.edu.bo.sumajflow.dto.transporte.*;
import ucb.edu.bo.sumajflow.utils.JwtUtil;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Controlador unificado para transportistas
 * Gestiona consultas de lotes y flujo completo del viaje
 */
@Slf4j
@RestController
@RequestMapping("/transportista")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class LotesTransportistaController {

    private final TransporteBl transporteBl;
    private final JwtUtil jwtUtil;

    // ==================== CONSULTAS DE LOTES ====================

    /**
     * Obtiene los lotes asignados al transportista autenticado
     * GET /transportista/mis-lotes
     */
    @GetMapping("/mis-lotes")
    public ResponseEntity<Map<String, Object>> getMisLotes(
            @RequestHeader("Authorization") String token,
            @RequestParam(required = false, defaultValue = "activos") String filtro) {

        Map<String, Object> response = new HashMap<>();

        try {
            Integer usuarioId = extractUsuarioId(token);
            List<LoteAsignadoResumenDto> lotesResumen = transporteBl.obtenerLotesTransportista(usuarioId, filtro);

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
     * GET /transportista/lote/{asignacionId}/detalle
     */
    @GetMapping("/lote/{asignacionId}/detalle")
    public ResponseEntity<Map<String, Object>> getDetalleLoteParaViaje(
            @PathVariable Integer asignacionId,
            @RequestHeader("Authorization") String token) {

        Map<String, Object> response = new HashMap<>();

        try {
            Integer usuarioId = extractUsuarioId(token);
            LoteDetalleViajeDto detalle = transporteBl.obtenerDetalleLoteParaViaje(asignacionId, usuarioId);

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

    // ==================== GESTIÓN DEL VIAJE ====================

    /**
     * Iniciar viaje: Esperando iniciar → En camino a la mina
     * POST /transportista/viaje/{asignacionId}/iniciar
     */
    @PostMapping("/viaje/{asignacionId}/iniciar")
    public ResponseEntity<TransicionEstadoResponseDto> iniciarViaje(
            @PathVariable Integer asignacionId,
            @RequestBody Map<String, Object> body,
            @RequestHeader("Authorization") String token
    ) {
        Integer usuarioId = extractUsuarioId(token);

        Double lat = body.get("lat") != null ? ((Number) body.get("lat")).doubleValue() : null;
        Double lng = body.get("lng") != null ? ((Number) body.get("lng")).doubleValue() : null;
        String obs = (String) body.get("observaciones");

        TransicionEstadoResponseDto response = transporteBl.iniciarViaje(
                asignacionId, lat, lng, obs, usuarioId
        );

        return ResponseEntity.ok(response);
    }

    /**
     * Confirmar llegada a mina: En camino a la mina → Esperando carguío
     * POST /transportista/viaje/{asignacionId}/llegada-mina
     */
    @PostMapping("/viaje/{asignacionId}/llegada-mina")
    public ResponseEntity<TransicionEstadoResponseDto> confirmarLlegadaMina(
            @PathVariable Integer asignacionId,
            @Valid @RequestBody ConfirmarLlegadaMinaDto dto,
            @RequestHeader("Authorization") String token
    ) {
        Integer usuarioId = extractUsuarioId(token);
        dto.setAsignacionCamionId(asignacionId);

        TransicionEstadoResponseDto response = transporteBl.confirmarLlegadaMina(dto, usuarioId);
        return ResponseEntity.ok(response);
    }

    /**
     * Confirmar carguío: Esperando carguío → En camino balanza cooperativa
     * POST /transportista/viaje/{asignacionId}/confirmar-carguio
     */
    @PostMapping("/viaje/{asignacionId}/confirmar-carguio")
    public ResponseEntity<TransicionEstadoResponseDto> confirmarCarguio(
            @PathVariable Integer asignacionId,
            @Valid @RequestBody ConfirmarCarguioDto dto,
            @RequestHeader("Authorization") String token
    ) {
        Integer usuarioId = extractUsuarioId(token);
        dto.setAsignacionCamionId(asignacionId);

        TransicionEstadoResponseDto response = transporteBl.confirmarCarguio(dto, usuarioId);
        return ResponseEntity.ok(response);
    }

    /**
     * Registrar pesaje (cooperativa o destino)
     * POST /transportista/viaje/{asignacionId}/registrar-pesaje
     */
    @PostMapping("/viaje/{asignacionId}/registrar-pesaje")
    public ResponseEntity<TransicionEstadoResponseDto> registrarPesaje(
            @PathVariable Integer asignacionId,
            @Valid @RequestBody RegistrarPesajeDto dto,
            @RequestHeader("Authorization") String token
    ) {
        Integer usuarioId = extractUsuarioId(token);
        dto.setAsignacionCamionId(asignacionId);

        TransicionEstadoResponseDto response = transporteBl.registrarPesaje(dto, usuarioId);
        return ResponseEntity.ok(response);
    }

    /**
     * Iniciar descarga: En camino almacén destino → Descargando
     * POST /transportista/viaje/{asignacionId}/iniciar-descarga
     */
    @PostMapping("/viaje/{asignacionId}/iniciar-descarga")
    public ResponseEntity<TransicionEstadoResponseDto> iniciarDescarga(
            @PathVariable Integer asignacionId,
            @RequestBody Map<String, Object> body,
            @RequestHeader("Authorization") String token
    ) {
        Integer usuarioId = extractUsuarioId(token);

        Double lat = ((Number) body.get("lat")).doubleValue();
        Double lng = ((Number) body.get("lng")).doubleValue();

        TransicionEstadoResponseDto response = transporteBl.iniciarDescarga(
                asignacionId, lat, lng, usuarioId
        );

        return ResponseEntity.ok(response);
    }

    /**
     * Confirmar descarga: Descargando → Completado
     * POST /transportista/viaje/{asignacionId}/confirmar-descarga
     */
    @PostMapping("/viaje/{asignacionId}/confirmar-descarga")
    public ResponseEntity<TransicionEstadoResponseDto> confirmarDescarga(
            @PathVariable Integer asignacionId,
            @Valid @RequestBody ConfirmarDescargaDto dto,
            @RequestHeader("Authorization") String token
    ) {
        Integer usuarioId = extractUsuarioId(token);
        dto.setAsignacionCamionId(asignacionId);

        TransicionEstadoResponseDto response = transporteBl.confirmarDescarga(dto, usuarioId);
        return ResponseEntity.ok(response);
    }

    // ==================== MÉTODOS AUXILIARES ====================

    private Integer extractUsuarioId(String token) {
        String cleanToken = token.replace("Bearer ", "");
        return jwtUtil.extractUsuarioId(cleanToken);
    }
}