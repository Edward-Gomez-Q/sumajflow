package ucb.edu.bo.sumajflow.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import ucb.edu.bo.sumajflow.bl.NotificacionBl;
import ucb.edu.bo.sumajflow.dto.NotificacionDto;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Controlador para gestionar notificaciones de usuarios
 */
@RestController
@RequestMapping("/notificaciones")
@CrossOrigin(origins = "*")
public class NotificacionController {

    private final NotificacionBl notificacionBl;

    public NotificacionController(NotificacionBl notificacionBl) {
        this.notificacionBl = notificacionBl;
    }

    /**
     * Obtener notificaciones del usuario autenticado
     * GET /notificaciones
     * GET /notificaciones?soloNoLeidas=true
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> obtenerNotificaciones(
            Authentication authentication,
            @RequestParam(required = false) Boolean soloNoLeidas) {

        Map<String, Object> response = new HashMap<>();

        try {
            // Obtener ID del usuario autenticado
            Integer usuarioId = (Integer) authentication.getDetails();

            // Obtener notificaciones
            List<NotificacionDto> notificaciones = notificacionBl.obtenerNotificaciones(
                    usuarioId, soloNoLeidas);

            response.put("success", true);
            response.put("data", notificaciones);
            response.put("total", notificaciones.size());

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error al obtener notificaciones: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Contar notificaciones no leídas del usuario autenticado
     * GET /notificaciones/no-leidas/count
     */
    @GetMapping("/no-leidas/count")
    public ResponseEntity<Map<String, Object>> contarNoLeidas(Authentication authentication) {
        Map<String, Object> response = new HashMap<>();

        try {
            Integer usuarioId = (Integer) authentication.getDetails();
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
     * Marcar una notificación específica como leída
     * PUT /notificaciones/{id}/leer
     */
    @PutMapping("/{id}/leer")
    public ResponseEntity<Map<String, Object>> marcarComoLeida(
            @PathVariable Integer id,
            Authentication authentication) {

        Map<String, Object> response = new HashMap<>();

        try {
            Integer usuarioId = (Integer) authentication.getDetails();

            // Marcar como leída (el servicio valida que pertenezca al usuario)
            notificacionBl.marcarComoLeida(id, usuarioId);

            response.put("success", true);
            response.put("message", "Notificación marcada como leída");

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);

        } catch (SecurityException e) {
            response.put("success", false);
            response.put("message", "No tienes permiso para modificar esta notificación");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error al marcar notificación como leída: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Marcar todas las notificaciones del usuario como leídas
     */
    @PutMapping("/leer-todas")
    public ResponseEntity<Map<String, Object>> marcarTodasComoLeidas(Authentication authentication) {
        Map<String, Object> response = new HashMap<>();

        try {
            Integer usuarioId = (Integer) authentication.getDetails();
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
     * Eliminar una notificación específica
     * DELETE /notificaciones/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> eliminarNotificacion(
            @PathVariable Integer id,
            Authentication authentication) {

        Map<String, Object> response = new HashMap<>();

        try {
            Integer usuarioId = (Integer) authentication.getDetails();

            // Eliminar notificación (el servicio valida que pertenezca al usuario)
            notificacionBl.eliminarNotificacion(id, usuarioId);

            response.put("success", true);
            response.put("message", "Notificación eliminada exitosamente");

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);

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
}