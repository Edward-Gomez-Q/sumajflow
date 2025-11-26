package ucb.edu.bo.sumajflow.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import ucb.edu.bo.sumajflow.bl.SocioBl;
import ucb.edu.bo.sumajflow.dto.socio.SocioEstadoDto;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/socio")
@CrossOrigin(origins = "*")
public class SocioController {

    private final SocioBl socioBl;

    public SocioController(SocioBl socioBl) {
        this.socioBl = socioBl;
    }

    /**
     * Endpoint para obtener el estado de aprobación del socio
     * GET /socio/estado
     */
    @GetMapping("/estado")
    public ResponseEntity<Map<String, Object>> obtenerEstado(Authentication authentication) {
        Map<String, Object> response = new HashMap<>();

        try {
            // Obtener ID del usuario autenticado desde el contexto de seguridad
            Integer usuarioId = (Integer) authentication.getDetails();

            // Obtener estado del socio
            SocioEstadoDto estadoSocio = socioBl.obtenerEstadoSocio(usuarioId);

            response.put("success", true);
            response.put("aprobado", "aprobado".equalsIgnoreCase(estadoSocio.getEstado()));
            response.put("data", estadoSocio);

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
     * Endpoint simple para verificar solo si está aprobado
     * GET /socio/verificar-aprobacion
     */
    @GetMapping("/verificar-aprobacion")
    public ResponseEntity<Map<String, Object>> verificarAprobacion(Authentication authentication) {
        Map<String, Object> response = new HashMap<>();

        try {
            Integer usuarioId = (Integer) authentication.getDetails();
            boolean aprobado = socioBl.estaSocioAprobado(usuarioId);

            response.put("success", true);
            response.put("aprobado", aprobado);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}