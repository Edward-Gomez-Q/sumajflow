package ucb.edu.bo.sumajflow.controller.transportista;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ucb.edu.bo.sumajflow.bl.NotificacionBl;
import ucb.edu.bo.sumajflow.utils.JwtUtil;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/transportista/notificaciones")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class NotificacionesTransportistaController {
    private final NotificacionBl notificacionBl;
    private final JwtUtil jwtUtil;

    /**
     * Obtener todas las notificaciones con filtros
     * GET /cooperativa/notificaciones?soloNoLeidas=true&tipo=info&page=0&size=20
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> obtenerNotificaciones(
            @RequestHeader("Authorization") String token,
            @RequestParam(required = false) Boolean soloNoLeidas,
            @RequestParam(required = false) String tipo,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "20") Integer size
    ) {
        Map<String, Object> response = new HashMap<>();

        try {
            Integer usuarioId = extractUsuarioId(token);

            Map<String, Object> resultado = notificacionBl.obtenerNotificacionesPaginadas(
                    usuarioId, soloNoLeidas, tipo, page, size
            );

            response.put("success", true);
            response.put("data", resultado);

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error al obtener notificaciones: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Contar notificaciones no leídas
     * GET /cooperativa/notificaciones/count
     */
    @GetMapping("/count")
    public ResponseEntity<Map<String, Object>> contarNoLeidas(
            @RequestHeader("Authorization") String token
    ) {
        Map<String, Object> response = new HashMap<>();

        try {
            Integer usuarioId = extractUsuarioId(token);
            Long count = notificacionBl.contarNoLeidas(usuarioId);

            response.put("success", true);
            response.put("count", count);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error al contar notificaciones: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Marcar una notificación como leída
     * PUT /cooperativa/notificaciones/{id}/leer
     */
    @PutMapping("/{id}/leer")
    public ResponseEntity<Map<String, Object>> marcarComoLeida(
            @PathVariable Integer id,
            @RequestHeader("Authorization") String token
    ) {
        Map<String, Object> response = new HashMap<>();

        try {
            Integer usuarioId = extractUsuarioId(token);
            notificacionBl.marcarComoLeida(id, usuarioId);

            response.put("success", true);
            response.put("message", "Notificación marcada como leída");

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);

        } catch (SecurityException e) {
            response.put("success", false);
            response.put("message", "No tienes permiso para modificar esta notificación");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error al marcar como leída: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Marcar todas como leídas
     * PUT /cooperativa/notificaciones/leer-todas
     */
    @PutMapping("/leer-todas")
    public ResponseEntity<Map<String, Object>> marcarTodasComoLeidas(
            @RequestHeader("Authorization") String token
    ) {
        Map<String, Object> response = new HashMap<>();

        try {
            Integer usuarioId = extractUsuarioId(token);
            notificacionBl.marcarTodasComoLeidas(usuarioId);

            response.put("success", true);
            response.put("message", "Todas las notificaciones marcadas como leídas");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error al marcar todas como leídas: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Eliminar una notificación
     * DELETE /cooperativa/notificaciones/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> eliminarNotificacion(
            @PathVariable Integer id,
            @RequestHeader("Authorization") String token
    ) {
        Map<String, Object> response = new HashMap<>();

        try {
            Integer usuarioId = extractUsuarioId(token);
            notificacionBl.eliminarNotificacion(id, usuarioId);

            response.put("success", true);
            response.put("message", "Notificación eliminada exitosamente");

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);

        } catch (SecurityException e) {
            response.put("success", false);
            response.put("message", "No tienes permiso para eliminar esta notificación");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error al eliminar notificación: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    private Integer extractUsuarioId(String token) {
        String cleanToken = token.replace("Bearer ", "");
        return jwtUtil.extractUsuarioId(cleanToken);
    }
}
