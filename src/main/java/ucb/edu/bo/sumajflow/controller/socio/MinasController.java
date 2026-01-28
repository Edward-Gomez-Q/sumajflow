package ucb.edu.bo.sumajflow.controller.socio;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ucb.edu.bo.sumajflow.bl.socio.MinasBl;
import ucb.edu.bo.sumajflow.dto.socio.MinaCreateDto;
import ucb.edu.bo.sumajflow.dto.socio.MinaResponseDto;
import ucb.edu.bo.sumajflow.utils.HttpUtils;
import ucb.edu.bo.sumajflow.utils.JwtUtil;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Controlador REST para gestión de Minas
 * Endpoints para CRUD completo de minas de socios
 */
@RestController
@RequestMapping("/socio/minas")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class MinasController {

    private final MinasBl minasBl;
    private final JwtUtil jwtUtil;

    /**
     * Obtener todas las minas activas del socio autenticado
     * GET /socio/minas
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getMinas(
            @RequestHeader("Authorization") String token
    ) {
        Map<String, Object> response = new HashMap<>();

        try {
            Integer usuarioId = extractUsuarioId(token);
            List<MinaResponseDto> minas = minasBl.getMinasBySocio(usuarioId);

            response.put("success", true);
            response.put("data", minas);
            return ResponseEntity.ok(response);

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

    /**
     * Obtener una mina por ID
     * GET /socio/minas/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getMina(
            @PathVariable Integer id,
            @RequestHeader("Authorization") String token
    ) {
        Map<String, Object> response = new HashMap<>();

        try {
            Integer usuarioId = extractUsuarioId(token);
            MinaResponseDto mina = minasBl.getMinaById(id, usuarioId);

            response.put("success", true);
            response.put("data", mina);
            return ResponseEntity.ok(response);

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

    /**
     * Crear una nueva mina
     * POST /socio/minas
     *
     * Body ejemplo:
     * {
     *   "nombre": "Mina San José",
     *   "fotoUrl": "https://example.com/uploads/mina123.jpg",
     *   "latitud": -16.5000,
     *   "longitud": -68.1500,
     *   "sectorId": 1
     * }
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> createMina(
            @RequestBody MinaCreateDto dto,
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

            MinaResponseDto mina = minasBl.createMina(
                    dto,
                    usuarioId,
                    ipOrigen,
                    metodoHttp,
                    endpoint
            );

            response.put("success", true);
            response.put("message", "Mina creada exitosamente");
            response.put("data", mina);
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

    /**
     * Actualizar una mina existente
     * PUT /socio/minas/{id}
     *
     * Body ejemplo:
     * {
     *   "nombre": "Mina San José Actualizada",
     *   "fotoUrl": "https://example.com/uploads/mina123_updated.jpg",
     *   "latitud": -16.5100,
     *   "longitud": -68.1600,
     *   "sectorId": 1
     * }
     */
    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> updateMina(
            @PathVariable Integer id,
            @RequestBody MinaCreateDto dto,
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

            MinaResponseDto mina = minasBl.updateMina(
                    id,
                    dto,
                    usuarioId,
                    ipOrigen,
                    metodoHttp,
                    endpoint
            );

            response.put("success", true);
            response.put("message", "Mina actualizada exitosamente");
            response.put("data", mina);
            return ResponseEntity.ok(response);

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

    /**
     * Eliminar una mina (eliminación lógica)
     * DELETE /socio/minas/{id}
     *
     * NOTA: Solo se puede eliminar si NO tiene lotes activos
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteMina(
            @PathVariable Integer id,
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

            minasBl.deleteMina(
                    id,
                    usuarioId,
                    ipOrigen,
                    metodoHttp,
                    endpoint
            );

            response.put("success", true);
            response.put("message", "Mina eliminada exitosamente");
            return ResponseEntity.ok(response);

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

    /**
     * Obtener estadísticas de minas del socio
     * GET /socio/minas/estadisticas
     */
    @GetMapping("/estadisticas")
    public ResponseEntity<Map<String, Object>> getEstadisticas(
            @RequestHeader("Authorization") String token
    ) {
        Map<String, Object> response = new HashMap<>();

        try {
            Integer usuarioId = extractUsuarioId(token);
            Map<String, Object> estadisticas = minasBl.getEstadisticas(usuarioId);

            response.put("success", true);
            response.put("data", estadisticas);
            return ResponseEntity.ok(response);

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