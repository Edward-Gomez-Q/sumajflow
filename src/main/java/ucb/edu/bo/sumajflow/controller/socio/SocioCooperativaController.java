package ucb.edu.bo.sumajflow.controller.socio;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ucb.edu.bo.sumajflow.bl.cooperativa.CooperativaBl;
import ucb.edu.bo.sumajflow.dto.socio.CooperativaBalanzaDto;
import ucb.edu.bo.sumajflow.entity.Cooperativa;
import ucb.edu.bo.sumajflow.utils.JwtUtil;

import java.util.Map;

@RestController
@RequestMapping("/socio/cooperativa")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class SocioCooperativaController {
    private final CooperativaBl cooperativaBl;
    private final JwtUtil jwtUtil;

    /**
     * Obtiene la cooperativa asociada a un usuario
     */
    @GetMapping("")
    public ResponseEntity<Map<String, Object>> getCooperativaPorUsuario(
            @RequestHeader("Authorization") String token
    ) {
        Map<String, Object> response = new java.util.HashMap<>();
        try {
            Integer usuarioId = extractUsuarioId(token);
            Cooperativa cooperativa = cooperativaBl.obtenerCooperativaPorSocio(usuarioId);
            CooperativaBalanzaDto cooperativaDto = cooperativaBl.convertToDtoBalanza(cooperativa);
            response.put("success", true);
            response.put("data", cooperativaDto);
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
