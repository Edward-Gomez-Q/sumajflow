package ucb.edu.bo.sumajflow.controller.ingenio;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ucb.edu.bo.sumajflow.bl.AlmacenBl;
import ucb.edu.bo.sumajflow.dto.AlmacenResponseDto;
import ucb.edu.bo.sumajflow.dto.AlmacenUpdateDto;
import ucb.edu.bo.sumajflow.utils.HttpUtils;
import ucb.edu.bo.sumajflow.utils.JwtUtil;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/ingenio/almacen")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class AlmacenIngenioController {

    private final AlmacenBl almacenBl;
    private final JwtUtil jwtUtil;

    /**
     * Obtiene el almacén del ingenio
     * GET /api/ingenio/almacen
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> obtenerAlmacen(
            @RequestHeader("Authorization") String token
    ) {
        Map<String, Object> response = new HashMap<>();

        try {
            Integer usuarioId = extractUsuarioId(token);

            AlmacenResponseDto almacen = almacenBl.obtenerAlmacenIngenio(usuarioId);

            response.put("success", true);
            response.put("message", "Almacén obtenido exitosamente");
            response.put("data", almacen);

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

    /**
     * Actualiza el almacén del ingenio
     * PUT /api/ingenio/almacen
     */
    @PutMapping
    public ResponseEntity<Map<String, Object>> actualizarAlmacen(
            @RequestBody AlmacenUpdateDto updateDto,
            @RequestHeader("Authorization") String token,
            HttpServletRequest request
    ) {
        Map<String, Object> response = new HashMap<>();

        try {
            Integer usuarioId = extractUsuarioId(token);
            String ipOrigen = HttpUtils.obtenerIpCliente(request);

            AlmacenResponseDto almacen = almacenBl.actualizarAlmacenIngenio(
                    usuarioId,
                    updateDto,
                    ipOrigen,
                    "PUT",
                    "/api/ingenio/almacen"
            );

            response.put("success", true);
            response.put("message", "Almacén actualizado exitosamente");
            response.put("data", almacen);

            return ResponseEntity.ok(response);

        } catch (IllegalStateException e) {
            // Excepción específica para lotes en proceso
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT).body(response);

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

    // Metodo auxiliar para extraer el usuario del token
    private Integer extractUsuarioId(String token) {
        String cleanToken = token.replace("Bearer ", "");
        return jwtUtil.extractUsuarioId(cleanToken);
    }
}