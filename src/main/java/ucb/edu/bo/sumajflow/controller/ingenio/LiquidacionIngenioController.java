package ucb.edu.bo.sumajflow.controller.ingenio;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ucb.edu.bo.sumajflow.bl.ingenio.LiquidacionTollIngenioBl;
import ucb.edu.bo.sumajflow.dto.ingenio.LiquidacionTollResponseDto;
import ucb.edu.bo.sumajflow.utils.JwtUtil;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/ingenio/liquidaciones")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class LiquidacionIngenioController {

    private final LiquidacionTollIngenioBl liquidacionTollIngenioBl;
    private final JwtUtil jwtUtil;

    /**
     * Listar liquidaciones de Toll del ingenio
     * GET /ingenio/liquidaciones/toll
     */
    @GetMapping("/toll")
    public ResponseEntity<Map<String, Object>> listarLiquidacionesToll(
            @RequestParam(required = false) String estado,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fechaDesde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fechaHasta,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestHeader("Authorization") String token
    ) {
        Map<String, Object> response = new HashMap<>();

        try {
            Integer usuarioId = extractUsuarioId(token);

            Page<LiquidacionTollResponseDto> liquidaciones = liquidacionTollIngenioBl.listarLiquidacionesToll(
                    usuarioId, estado, fechaDesde, fechaHasta, page, size
            );

            response.put("success", true);
            response.put("data", liquidaciones.getContent());
            response.put("totalElements", liquidaciones.getTotalElements());
            response.put("totalPages", liquidaciones.getTotalPages());
            response.put("currentPage", liquidaciones.getNumber());
            response.put("pageSize", liquidaciones.getSize());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error al listar liquidaciones: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * Obtener detalle de liquidación de Toll
     * GET /ingenio/liquidaciones/toll/{id}
     */
    @GetMapping("/toll/{id}")
    public ResponseEntity<Map<String, Object>> obtenerDetalleToll(
            @PathVariable Integer id,
            @RequestHeader("Authorization") String token
    ) {
        Map<String, Object> response = new HashMap<>();

        try {
            Integer usuarioId = extractUsuarioId(token);

            LiquidacionTollResponseDto liquidacion = liquidacionTollIngenioBl.obtenerDetalleLiquidacion(
                    id, usuarioId
            );

            response.put("success", true);
            response.put("data", liquidacion);
            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error interno del servidor: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * Obtener estadísticas de liquidaciones de Toll
     * GET /ingenio/liquidaciones/toll/estadisticas
     */
    @GetMapping("/toll/estadisticas")
    public ResponseEntity<Map<String, Object>> obtenerEstadisticasToll(
            @RequestHeader("Authorization") String token
    ) {
        Map<String, Object> response = new HashMap<>();

        try {
            Integer usuarioId = extractUsuarioId(token);

            Map<String, Object> estadisticas = liquidacionTollIngenioBl.obtenerEstadisticas(usuarioId);

            response.put("success", true);
            response.put("data", estadisticas);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error al obtener estadísticas: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    private Integer extractUsuarioId(String token) {
        String cleanToken = token.replace("Bearer ", "");
        return jwtUtil.extractUsuarioId(cleanToken);
    }
}