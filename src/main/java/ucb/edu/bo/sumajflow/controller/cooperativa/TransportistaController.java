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

/**
 * Controlador para onboarding de transportistas con QR
 */
@RestController
@RequestMapping("/cooperativa/transportistas")
@CrossOrigin(origins = "*")
public class TransportistaController {

    private final TransportistaBl transportistaBl;

    public TransportistaController(TransportistaBl transportistaBl) {
        this.transportistaBl = transportistaBl;
    }

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

            // Extraer contexto HTTP
            String ipOrigen = HttpUtils.obtenerIpCliente(request);
            String metodoHttp = request.getMethod();
            String endpoint = request.getRequestURI();

            // Crear invitación con QR
            Map<String, Object> resultado = transportistaBl.crearInvitacionConQR(
                    usuarioId,
                    dto,
                    ipOrigen,
                    metodoHttp,
                    endpoint
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
            @RequestParam(required = false) String estado,
            @RequestParam(required = false) String busqueda,
            @RequestParam(defaultValue = "0") Integer pagina,
            @RequestParam(defaultValue = "10") Integer tamanoPagina
    ) {
        Map<String, Object> response = new HashMap<>();

        try {
            Integer usuarioId = (Integer) authentication.getDetails();

            Page<Map<String, Object>> invitaciones = transportistaBl.listarInvitaciones(
                    usuarioId,
                    estado,
                    busqueda,
                    pagina,
                    tamanoPagina
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
        response.put("service", "Transportistas QR Onboarding Service");
        return ResponseEntity.ok(response);
    }
}

