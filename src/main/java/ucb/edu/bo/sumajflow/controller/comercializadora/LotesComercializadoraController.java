package ucb.edu.bo.sumajflow.controller.comercializadora;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ucb.edu.bo.sumajflow.bl.comercializadora.LotesComercializadoraBl;
import ucb.edu.bo.sumajflow.dto.comercializadora.LotePendienteComercializadoraDto;
import ucb.edu.bo.sumajflow.dto.ingenio.LoteAprobacionDestinoDto;
import ucb.edu.bo.sumajflow.dto.ingenio.LoteRechazoDestinoDto;
import ucb.edu.bo.sumajflow.utils.HttpUtils;
import ucb.edu.bo.sumajflow.utils.JwtUtil;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/comercializadora/lotes")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class LotesComercializadoraController {

    private final LotesComercializadoraBl lotesComercializadoraBl;
    private final JwtUtil jwtUtil;

    /**
     * Obtener lotes pendientes de aprobación por la comercializadora
     * GET /comercializadora/lotes/pendientes
     */
    @GetMapping("/pendientes")
    public ResponseEntity<Map<String, Object>> getLotesPendientes(
            @RequestHeader("Authorization") String token
    ) {
        Map<String, Object> response = new HashMap<>();

        try {
            Integer usuarioId = extractUsuarioId(token);
            List<LotePendienteComercializadoraDto> lotes =
                    lotesComercializadoraBl.getLotesPendientesComercializadora(usuarioId);

            response.put("success", true);
            response.put("data", lotes);
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
     * Obtener detalle completo de un lote
     * GET /comercializadora/lotes/{id}/detalle
     */
    @GetMapping("/{id}/detalle")
    public ResponseEntity<Map<String, Object>> getDetalleLote(
            @PathVariable Integer id,
            @RequestHeader("Authorization") String token
    ) {
        Map<String, Object> response = new HashMap<>();

        try {
            Integer usuarioId = extractUsuarioId(token);
            LotePendienteComercializadoraDto lote = lotesComercializadoraBl.getDetalleLote(id, usuarioId);

            response.put("success", true);
            response.put("data", lote);
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
     * Aprobar lote desde la comercializadora
     * PUT /comercializadora/lotes/{id}/aprobar
     *
     * Body ejemplo:
     * {
     *   "observaciones": "Aprobado para recepción"
     * }
     */
    @PutMapping("/{id}/aprobar")
    public ResponseEntity<Map<String, Object>> aprobarLote(
            @PathVariable Integer id,
            @RequestBody LoteAprobacionDestinoDto aprobacionDto,
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

            LotePendienteComercializadoraDto lote = lotesComercializadoraBl.aprobarLote(
                    id,
                    aprobacionDto,
                    usuarioId,
                    ipOrigen,
                    metodoHttp,
                    endpoint
            );

            response.put("success", true);
            response.put("message", "Lote aprobado exitosamente");
            response.put("data", lote);
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
     * Rechazar lote desde la comercializadora
     * PUT /comercializadora/lotes/{id}/rechazar
     *
     * Body ejemplo:
     * {
     *   "motivoRechazo": "No estamos comprando este tipo de mineral actualmente"
     * }
     */
    @PutMapping("/{id}/rechazar")
    public ResponseEntity<Map<String, Object>> rechazarLote(
            @PathVariable Integer id,
            @RequestBody LoteRechazoDestinoDto rechazoDto,
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

            lotesComercializadoraBl.rechazarLote(
                    id,
                    rechazoDto,
                    usuarioId,
                    ipOrigen,
                    metodoHttp,
                    endpoint
            );

            response.put("success", true);
            response.put("message", "Lote rechazado exitosamente");
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