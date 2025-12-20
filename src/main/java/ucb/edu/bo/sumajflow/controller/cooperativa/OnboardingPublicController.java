package ucb.edu.bo.sumajflow.controller.cooperativa;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ucb.edu.bo.sumajflow.bl.cooperativa.TransportistaBl;
import ucb.edu.bo.sumajflow.dto.transportista.CompletarOnboardingDto;
import ucb.edu.bo.sumajflow.dto.transportista.IniciarOnboardingDto;
import ucb.edu.bo.sumajflow.dto.transportista.ReenviarCodigoDto;
import ucb.edu.bo.sumajflow.dto.transportista.VerificarCodigoDto;
import ucb.edu.bo.sumajflow.utils.HttpUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * Controlador público para onboarding desde app móvil
 */
@RestController
@RequestMapping("/public/onboarding")
@CrossOrigin(origins = "*")
class OnboardingPublicController {

    private final TransportistaBl transportistaBl;

    public OnboardingPublicController(TransportistaBl transportistaBl) {
        this.transportistaBl = transportistaBl;
    }

    /**
     * Iniciar onboarding (después de escanear QR)
     * POST /public/onboarding/iniciar
     */
    @PostMapping("/iniciar")
    public ResponseEntity<Map<String, Object>> iniciarOnboarding(
            @Valid @RequestBody IniciarOnboardingDto dto,
            HttpServletRequest request
    ) {
        Map<String, Object> response = new HashMap<>();

        try {
            String ipOrigen = HttpUtils.obtenerIpCliente(request);

            Map<String, Object> resultado = transportistaBl.iniciarOnboarding(dto, ipOrigen);

            response.put("success", true);
            response.put("data", resultado);

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
     * Verificar código de WhatsApp
     * POST /public/onboarding/verificar-codigo
     */
    @PostMapping("/verificar-codigo")
    public ResponseEntity<Map<String, Object>> verificarCodigo(
            @Valid @RequestBody VerificarCodigoDto dto,
            HttpServletRequest request
    ) {
        Map<String, Object> response = new HashMap<>();

        try {
            String ipOrigen = HttpUtils.obtenerIpCliente(request);

            Map<String, Object> resultado = transportistaBl.verificarCodigo(dto, ipOrigen);

            response.put("success", true);
            response.put("data", resultado);

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
     * Reenviar código de verificación
     * POST /public/onboarding/reenviar-codigo
     */
    @PostMapping("/reenviar-codigo")
    public ResponseEntity<Map<String, Object>> reenviarCodigo(
            @Valid @RequestBody ReenviarCodigoDto dto,
            HttpServletRequest request
    ) {
        Map<String, Object> response = new HashMap<>();

        try {
            String ipOrigen = HttpUtils.obtenerIpCliente(request);

            Map<String, Object> resultado = transportistaBl.reenviarCodigo(dto, ipOrigen);

            response.put("success", true);
            response.put("data", resultado);

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
     * Health check público
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "OK");
        response.put("service", "Public Onboarding Service");
        return ResponseEntity.ok(response);
    }
    /**
     * Obtener datos de la invitación (para pre-llenar formulario)
     * POST /public/onboarding/obtener-datos
     */
    @PostMapping("/obtener-datos")
    public ResponseEntity<Map<String, Object>> obtenerDatosInvitacion(
            @Valid @RequestBody Map<String, String> request,
            HttpServletRequest httpRequest
    ) {
        Map<String, Object> response = new HashMap<>();

        try {
            String token = request.get("token");

            if (token == null || token.isEmpty()) {
                response.put("success", false);
                response.put("message", "Token requerido");
                return ResponseEntity.badRequest().body(response);
            }

            Map<String, Object> resultado = transportistaBl.obtenerDatosInvitacion(token);

            response.put("success", true);
            response.put("data", resultado);

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
    @PostMapping("/completar")
    public ResponseEntity<Map<String, Object>> completarOnboarding(
            @Valid @RequestBody CompletarOnboardingDto dto,
            HttpServletRequest request
    ) {
        Map<String, Object> response = new HashMap<>();

        try {
            String ipOrigen = HttpUtils.obtenerIpCliente(request);
            Map<String, Object> resultado = transportistaBl.completarOnboarding(dto, ipOrigen);

            response.put("success", true);
            response.put("data", resultado);

            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (IllegalArgumentException e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error al completar el registro: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Login de transportista mediante correo y contraseña
     * POST /public/onboarding/login
     */
    /**
     * Login de transportista mediante mensaje de WhatsApp
     * POST /public/onboarding/login
     */
}