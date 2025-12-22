package ucb.edu.bo.sumajflow.controller.transporte;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ucb.edu.bo.sumajflow.bl.transporte.TransporteBl;
import ucb.edu.bo.sumajflow.dto.transporte.AsignacionCamionDetalleDto;
import ucb.edu.bo.sumajflow.dto.transporte.CambioEstadoAsignacionDto;
import ucb.edu.bo.sumajflow.dto.transporte.LoteTransporteDto;
import ucb.edu.bo.sumajflow.utils.HttpUtils;
import ucb.edu.bo.sumajflow.utils.JwtUtil;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/transporte")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class TransporteController {

    private final TransporteBl transporteBl;
    private final JwtUtil jwtUtil;

    /**
     * Obtener asignaciones de camiones de un lote
     * GET /transporte/lotes/{loteId}/asignaciones
     */
    @GetMapping("/lotes/{loteId}/asignaciones")
    public ResponseEntity<Map<String, Object>> getAsignacionesByLote(
            @PathVariable Integer loteId,
            @RequestHeader("Authorization") String token
    ) {
        Map<String, Object> response = new HashMap<>();

        try {
            List<AsignacionCamionDetalleDto> asignaciones =
                    transporteBl.getAsignacionesByLote(loteId);

            response.put("success", true);
            response.put("data", asignaciones);
            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error interno del servidor: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * Obtener detalle de transporte de un lote
     * GET /transporte/lotes/{loteId}/detalle
     */
    @GetMapping("/lotes/{loteId}/detalle")
    public ResponseEntity<Map<String, Object>> getDetalleTransporte(
            @PathVariable Integer loteId,
            @RequestHeader("Authorization") String token
    ) {
        Map<String, Object> response = new HashMap<>();

        try {
            LoteTransporteDto detalle = transporteBl.getDetalleTransporte(loteId);

            response.put("success", true);
            response.put("data", detalle);
            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error interno del servidor: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * Cambiar estado de una asignación de camión
     * PUT /transporte/asignaciones/{asignacionId}/cambiar-estado
     *
     * Body ejemplo:
     * {
     *   "nuevoEstado": "En camino a la mina",
     *   "observaciones": "Salida confirmada"
     * }
     */
    @PutMapping("/asignaciones/{asignacionId}/cambiar-estado")
    public ResponseEntity<Map<String, Object>> cambiarEstadoAsignacion(
            @PathVariable Integer asignacionId,
            @RequestBody CambioEstadoAsignacionDto dto,
            @RequestHeader("Authorization") String token,
            HttpServletRequest request
    ) {
        Map<String, Object> response = new HashMap<>();

        try {
            Integer usuarioId = extractUsuarioId(token);

            // Extraer contexto HTTP
            String ipOrigen = HttpUtils.obtenerIpCliente(request);

            AsignacionCamionDetalleDto asignacion = transporteBl.cambiarEstadoAsignacion(
                    asignacionId,
                    dto,
                    usuarioId,
                    ipOrigen
            );

            response.put("success", true);
            response.put("message", "Estado actualizado exitosamente");
            response.put("data", asignacion);
            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error interno del servidor: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(response);
        }
    }

    // Método auxiliar para extraer el usuario del token
    private Integer extractUsuarioId(String token) {
        String cleanToken = token.replace("Bearer ", "");
        return jwtUtil.extractUsuarioId(cleanToken);
    }
}