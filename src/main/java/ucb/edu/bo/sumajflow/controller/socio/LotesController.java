package ucb.edu.bo.sumajflow.controller.socio;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ucb.edu.bo.sumajflow.bl.socio.LotesBl;
import ucb.edu.bo.sumajflow.dto.socio.LoteCreateDto;
import ucb.edu.bo.sumajflow.dto.socio.LoteResponseDto;
import ucb.edu.bo.sumajflow.utils.HttpUtils;
import ucb.edu.bo.sumajflow.utils.JwtUtil;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/socio/lotes")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class LotesController {

    private final LotesBl lotesBl;
    private final JwtUtil jwtUtil;

    /**
     * Crear un nuevo lote
     * POST /socio/lotes
     *
     * Body ejemplo:
     * {
     *   "minaId": 1,
     *   "mineralesIds": [1, 2],
     *   "camionlesSolicitados": 4,
     *   "tipoOperacion": "procesamiento_planta",
     *   "destinoId": 1,
     *   "tipoMineral": "complejo",
     *   "pesoTotalEstimado": 50.5,
     *   "observaciones": "Lote de prueba"
     * }
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> createLote(
            @RequestBody LoteCreateDto dto,
            @RequestHeader("Authorization") String token,
            HttpServletRequest request
    ) {
        Map<String, Object> response = new HashMap<>();

        try {
            Integer usuarioId = extractUsuarioId(token);

            // Extraer contexto HTTP
            String ipOrigen = HttpUtils.obtenerIpCliente(request);
            String metodoHttp = request.getMethod();
            String endpoint = request.getRequestURI();

            LoteResponseDto lote = lotesBl.createLote(
                    dto,
                    usuarioId,
                    ipOrigen,
                    metodoHttp,
                    endpoint
            );

            response.put("success", true);
            response.put("message", "Lote creado exitosamente y enviado para aprobación");
            response.put("data", lote);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (IllegalArgumentException e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error interno del servidor: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    // Método auxiliar para extraer el usuario del token
    private Integer extractUsuarioId(String token) {
        String cleanToken = token.replace("Bearer ", "");
        return jwtUtil.extractUsuarioId(cleanToken);
    }
}