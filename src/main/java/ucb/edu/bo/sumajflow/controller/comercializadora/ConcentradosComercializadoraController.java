package ucb.edu.bo.sumajflow.controller.comercializadora;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ucb.edu.bo.sumajflow.bl.comercializadora.VentaComercializadoraBl;
import ucb.edu.bo.sumajflow.dto.ingenio.ConcentradoResponseDto;
import ucb.edu.bo.sumajflow.utils.JwtUtil;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Controlador REST para Concentrados - ROL COMERCIALIZADORA
 * Endpoints para listar y consultar concentrados de liquidaciones aprobadas
 */
@RestController
@RequestMapping("/comercializadora/concentrados")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class ConcentradosComercializadoraController {

    private final VentaComercializadoraBl ventaComercializadoraBl;
    private final JwtUtil jwtUtil;

    /**
     * Listar concentrados de liquidaciones aprobadas
     * GET /comercializadora/concentrados
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> listarConcentrados(
            @RequestParam(required = false) String estado,
            @RequestParam(required = false) String mineralPrincipal,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fechaDesde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fechaHasta,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestHeader("Authorization") String token
    ) {
        Map<String, Object> response = new HashMap<>();

        try {
            Integer usuarioId = extractUsuarioId(token);
            Page<ConcentradoResponseDto> concentrados = ventaComercializadoraBl.listarConcentrados(
                    usuarioId, estado, mineralPrincipal, fechaDesde, fechaHasta, page, size);

            response.put("success", true);
            response.put("data", concentrados.getContent());
            response.put("totalElements", concentrados.getTotalElements());
            response.put("totalPages", concentrados.getTotalPages());
            response.put("currentPage", concentrados.getNumber());
            response.put("pageSize", concentrados.getSize());
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error al listar concentrados: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * Obtener detalle de un concentrado
     * GET /comercializadora/concentrados/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> obtenerDetalleConcentrado(
            @PathVariable Integer id,
            @RequestHeader("Authorization") String token
    ) {
        Map<String, Object> response = new HashMap<>();

        try {
            Integer usuarioId = extractUsuarioId(token);
            ConcentradoResponseDto concentrado = ventaComercializadoraBl.obtenerDetalleConcentrado(id, usuarioId);

            response.put("success", true);
            response.put("data", concentrado);
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

    private Integer extractUsuarioId(String token) {
        String cleanToken = token.replace("Bearer ", "");
        return jwtUtil.extractUsuarioId(cleanToken);
    }
}