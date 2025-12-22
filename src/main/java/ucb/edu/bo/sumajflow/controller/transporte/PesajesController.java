package ucb.edu.bo.sumajflow.controller.transporte;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ucb.edu.bo.sumajflow.bl.transporte.PesajesBl;
import ucb.edu.bo.sumajflow.dto.transporte.PesajeCreateDto;
import ucb.edu.bo.sumajflow.dto.transporte.PesajeResponseDto;
import ucb.edu.bo.sumajflow.utils.HttpUtils;
import ucb.edu.bo.sumajflow.utils.JwtUtil;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/transporte/pesajes")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class PesajesController {

    private final PesajesBl pesajesBl;
    private final JwtUtil jwtUtil;

    /**
     * Registrar un nuevo pesaje
     * POST /transporte/pesajes
     *
     * Body ejemplo:
     * {
     *   "asignacionCamionId": 1,
     *   "tipoPesaje": "cooperativa",
     *   "pesoBruto": 5250.50,
     *   "pesoTara": 2100.00,
     *   "observaciones": "Pesaje realizado en balanza #1"
     * }
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> registrarPesaje(
            @RequestBody PesajeCreateDto dto,
            @RequestHeader("Authorization") String token,
            HttpServletRequest request
    ) {
        Map<String, Object> response = new HashMap<>();

        try {
            Integer usuarioId = extractUsuarioId(token);

            // Extraer contexto HTTP
            String ipOrigen = HttpUtils.obtenerIpCliente(request);

            PesajeResponseDto pesaje = pesajesBl.registrarPesaje(dto, usuarioId, ipOrigen);

            response.put("success", true);
            response.put("message", "Pesaje registrado exitosamente");
            response.put("data", pesaje);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);

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
     * Obtener pesajes de una asignación
     * GET /transporte/pesajes/asignacion/{asignacionId}
     */
    @GetMapping("/asignacion/{asignacionId}")
    public ResponseEntity<Map<String, Object>> getPesajesByAsignacion(
            @PathVariable Integer asignacionId,
            @RequestHeader("Authorization") String token
    ) {
        Map<String, Object> response = new HashMap<>();

        try {
            List<PesajeResponseDto> pesajes = pesajesBl.getPesajesByAsignacion(asignacionId);

            response.put("success", true);
            response.put("data", pesajes);
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