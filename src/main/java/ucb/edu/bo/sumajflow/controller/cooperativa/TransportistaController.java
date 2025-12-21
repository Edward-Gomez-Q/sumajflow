package ucb.edu.bo.sumajflow.controller.cooperativa;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import ucb.edu.bo.sumajflow.bl.cooperativa.TransportistaBl;
import ucb.edu.bo.sumajflow.dto.transportista.*;
import ucb.edu.bo.sumajflow.utils.HttpUtils;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/cooperativa/transportistas")
@CrossOrigin(origins = "*")
public class TransportistaController {

    private final TransportistaBl transportistaBl;

    public TransportistaController(TransportistaBl transportistaBl) {
        this.transportistaBl = transportistaBl;
    }

    // ==================== ENDPOINTS DE INVITACIONES ====================

    /**
     * Crear invitación con QR
     * POST /cooperativa/transportistas/invitar
     */
    @PostMapping("/invitar")
    public ResponseEntity<Map<String, Object>> crearInvitacionConQR(
            Authentication authentication,
            @Valid @RequestBody TransportistaInvitacionDto dto,
            HttpServletRequest request
    ) {
        Map<String, Object> response = new HashMap<>();

        try {
            Integer usuarioId = (Integer) authentication.getDetails();
            String ipOrigen = HttpUtils.obtenerIpCliente(request);
            String metodoHttp = request.getMethod();
            String endpoint = request.getRequestURI();

            Map<String, Object> resultado = transportistaBl.crearInvitacionConQR(
                    usuarioId, dto, ipOrigen, metodoHttp, endpoint
            );

            response.put("success", true);
            response.put("message", "Invitación con QR creada exitosamente");
            response.put("data", resultado);

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
     * Listar invitaciones
     * GET /cooperativa/transportistas/invitaciones
     */
    @GetMapping("/invitaciones")
    public ResponseEntity<Map<String, Object>> listarInvitaciones(
            Authentication authentication,
            @RequestParam(required = false, defaultValue = "") String estado,
            @RequestParam(required = false, defaultValue = "") String busqueda,
            @RequestParam(defaultValue = "0") Integer pagina,
            @RequestParam(defaultValue = "10") Integer tamanoPagina
    ) {
        Map<String, Object> response = new HashMap<>();

        try {
            Integer usuarioId = (Integer) authentication.getDetails();

            Page<Map<String, Object>> invitaciones = transportistaBl.listarInvitaciones(
                    usuarioId, estado, busqueda, pagina, tamanoPagina
            );

            response.put("success", true);
            response.put("data", invitaciones.getContent());
            response.put("pagina", invitaciones.getNumber());
            response.put("totalPaginas", invitaciones.getTotalPages());
            response.put("totalElementos", invitaciones.getTotalElements());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error interno: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    // ==================== ENDPOINTS DE TRANSPORTISTAS ====================

    /**
     * Listar transportistas
     * GET /cooperativa/transportistas
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> listarTransportistas(
            Authentication authentication,
            @RequestParam(required = false, defaultValue = "") String estado,
            @RequestParam(required = false, defaultValue = "") String busqueda,
            @RequestParam(defaultValue = "0") Integer pagina,
            @RequestParam(defaultValue = "10") Integer tamanoPagina,
            @RequestParam(defaultValue = "createdAt") String ordenarPor,
            @RequestParam(defaultValue = "desc") String direccion
    ) {
        Map<String, Object> response = new HashMap<>();

        try {
            Integer usuarioId = (Integer) authentication.getDetails();

            Map<String, Object> resultado = transportistaBl.listarTransportistas(
                    usuarioId, estado, busqueda, pagina, tamanoPagina, ordenarPor, direccion
            );

            response.put("success", true);
            response.put("data", resultado);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error interno: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Obtener detalle de transportista
     * GET /cooperativa/transportistas/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> obtenerDetalleTransportista(
            Authentication authentication,
            @PathVariable Integer id
    ) {
        Map<String, Object> response = new HashMap<>();

        try {
            Integer usuarioId = (Integer) authentication.getDetails();

            Map<String, Object> detalle = transportistaBl.obtenerDetalleTransportista(usuarioId, id);

            response.put("success", true);
            response.put("data", detalle);

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error interno: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Cambiar estado de transportista
     * PUT /cooperativa/transportistas/{id}/estado
     */
    @PutMapping("/{id}/estado")
    public ResponseEntity<Map<String, Object>> cambiarEstadoTransportista(
            Authentication authentication,
            @PathVariable Integer id,
            @RequestBody Map<String, String> payload,
            HttpServletRequest request
    ) {
        Map<String, Object> response = new HashMap<>();

        try {
            Integer usuarioId = (Integer) authentication.getDetails();
            String nuevoEstado = payload.get("nuevoEstado");
            String motivo = payload.getOrDefault("motivo", "");

            String ipOrigen = HttpUtils.obtenerIpCliente(request);
            String metodoHttp = request.getMethod();
            String endpoint = request.getRequestURI();

            transportistaBl.cambiarEstadoTransportista(
                    usuarioId, id, nuevoEstado, motivo, ipOrigen, metodoHttp, endpoint
            );

            response.put("success", true);
            response.put("message", "Estado actualizado exitosamente");

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error interno: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Health check
     * GET /cooperativa/transportistas/health
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "OK");
        response.put("service", "Transportistas Service");
        return ResponseEntity.ok(response);
    }
}