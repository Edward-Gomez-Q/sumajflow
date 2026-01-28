package ucb.edu.bo.sumajflow.controller.cooperativa;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import ucb.edu.bo.sumajflow.bl.BalanzaBl;
import ucb.edu.bo.sumajflow.dto.BalanzaResponseDto;
import ucb.edu.bo.sumajflow.dto.BalanzaUpdateDto;
import ucb.edu.bo.sumajflow.utils.HttpUtils;
import ucb.edu.bo.sumajflow.utils.JwtUtil;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/cooperativa/balanza")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class BalanzaCooperativaController {

    private final BalanzaBl balanzaBl;
    private final JwtUtil jwtUtil;

    /**
     * Obtiene la balanza de la cooperativa
     * GET /api/cooperativa/balanza
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> obtenerBalanza(@RequestHeader("Authorization")  String token) {
        Map<String, Object> response = new HashMap<>();

        try {
            Integer usuarioId = extractUsuarioId(token);

            BalanzaResponseDto balanza = balanzaBl.obtenerBalanzaCooperativa(usuarioId);

            response.put("success", true);
            response.put("message", "Balanza obtenida exitosamente");
            response.put("data", balanza);

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error interno del servidor: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    // Metodo auxiliar para extraer el usuario del token
    private Integer extractUsuarioId(String token) {
        String cleanToken = token.replace("Bearer ", "");
        return jwtUtil.extractUsuarioId(cleanToken);
    }

    /**
     * Actualiza la balanza de la cooperativa
     * PUT /api/cooperativa/balanza
     */
    @PutMapping
    public ResponseEntity<Map<String, Object>> actualizarBalanza(
            @RequestBody BalanzaUpdateDto updateDto,
            @RequestHeader("Authorization")  String token,
            HttpServletRequest request
    ) {
        Map<String, Object> response = new HashMap<>();

        try {
            Integer usuarioId = extractUsuarioId(token);
            String ipOrigen = HttpUtils.obtenerIpCliente(request);

            BalanzaResponseDto balanza = balanzaBl.actualizarBalanzaCooperativa(
                    usuarioId,
                    updateDto,
                    ipOrigen,
                    "PUT",
                    "/api/cooperativa/balanza"
            );

            response.put("success", true);
            response.put("message", "Balanza actualizada exitosamente");
            response.put("data", balanza);

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error interno del servidor: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}