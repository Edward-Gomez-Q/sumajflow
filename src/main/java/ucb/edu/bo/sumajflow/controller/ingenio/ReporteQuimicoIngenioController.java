package ucb.edu.bo.sumajflow.controller.ingenio;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ucb.edu.bo.sumajflow.bl.ingenio.ReporteQuimicoIngenioBl;
import ucb.edu.bo.sumajflow.dto.ingenio.*;
import ucb.edu.bo.sumajflow.utils.HttpUtils;
import ucb.edu.bo.sumajflow.utils.JwtUtil;

import java.util.HashMap;
import java.util.Map;

/**
 * Controlador REST para gestión de Reportes Químicos
 * Endpoints para registro y validación de reportes de laboratorio
 */
@RestController
@RequestMapping("/ingenio/reportes-quimicos")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class ReporteQuimicoIngenioController {

    private final ReporteQuimicoIngenioBl reporteQuimicoIngenioBl;
    private final JwtUtil jwtUtil;

    /**
     * Registrar reporte químico del laboratorio
     * POST /ingenio/reportes-quimicos/concentrados/{concentradoId}
     */
    @PostMapping("/concentrados/{concentradoId}")
    public ResponseEntity<Map<String, Object>> registrarReporteQuimico(
            @PathVariable Integer concentradoId,
            @Valid @RequestBody ReporteQuimicoCreateDto reporteDto,
            @RequestHeader("Authorization") String token,
            HttpServletRequest request
    ) {
        Map<String, Object> response = new HashMap<>();

        try {
            Integer usuarioId = extractUsuarioId(token);
            String ipOrigen = HttpUtils.obtenerIpCliente(request);

            ConcentradoResponseDto concentrado = reporteQuimicoIngenioBl.registrarReporteQuimico(
                    concentradoId, reporteDto, usuarioId, ipOrigen
            );

            response.put("success", true);
            response.put("message", "Reporte químico registrado exitosamente");
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

    /**
     * Validar reporte químico (cambiar estado a "listo_para_liquidacion")
     * PATCH /ingenio/reportes-quimicos/concentrados/{concentradoId}/validar
     */
    @PatchMapping("/concentrados/{concentradoId}/validar")
    public ResponseEntity<Map<String, Object>> validarReporteQuimico(
            @PathVariable Integer concentradoId,
            @RequestHeader("Authorization") String token,
            HttpServletRequest request
    ) {
        Map<String, Object> response = new HashMap<>();

        try {
            Integer usuarioId = extractUsuarioId(token);
            String ipOrigen = HttpUtils.obtenerIpCliente(request);

            ConcentradoResponseDto concentrado = reporteQuimicoIngenioBl.validarReporteQuimico(
                    concentradoId, usuarioId, ipOrigen
            );

            response.put("success", true);
            response.put("message", "Reporte químico validado exitosamente");
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