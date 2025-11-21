package ucb.edu.bo.sumajflow.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ucb.edu.bo.sumajflow.bl.AuthBl;
import ucb.edu.bo.sumajflow.dto.login.LoginRequestDto;
import ucb.edu.bo.sumajflow.dto.login.LoginResponseDto;
import ucb.edu.bo.sumajflow.dto.OnBoardingDto;
import ucb.edu.bo.sumajflow.entity.Usuarios;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    private final AuthBl authBl;

    public AuthController(AuthBl authBl) {
        this.authBl = authBl;
    }

    /**
     * Endpoint de login
     * POST /auth/login
     */
    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody LoginRequestDto loginRequest) {
        Map<String, Object> response = new HashMap<>();

        try {
            LoginResponseDto loginResponse = authBl.login(
                    loginRequest.getEmail(),
                    loginRequest.getPassword()
            );

            response.put("success", true);
            response.put("message", "Inicio de sesión exitoso");
            response.put("token", loginResponse.getToken());
            response.put("refreshToken", loginResponse.getRefreshToken());
            response.put("user", loginResponse.getUser());

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error interno del servidor: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Endpoint para refrescar el token
     * POST /auth/refresh
     */
    @PostMapping("/refresh")
    public ResponseEntity<Map<String, Object>> refresh(@RequestBody Map<String, String> request) {
        Map<String, Object> response = new HashMap<>();

        try {
            String refreshToken = request.get("refreshToken");

            if (refreshToken == null || refreshToken.isEmpty()) {
                response.put("success", false);
                response.put("message", "Refresh token es requerido");
                return ResponseEntity.badRequest().body(response);
            }

            LoginResponseDto loginResponse = authBl.refreshToken(refreshToken);

            response.put("success", true);
            response.put("message", "Token refrescado exitosamente");
            response.put("token", loginResponse.getToken());
            response.put("refreshToken", loginResponse.getRefreshToken());
            response.put("user", loginResponse.getUser());

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error interno del servidor: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Endpoint para registrar nuevos usuarios (onboarding)
     * POST /auth/register
     */
    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> register(@RequestBody OnBoardingDto onBoardingDto) {
        Map<String, Object> response = new HashMap<>();

        try {
            // Validación básica
            if (onBoardingDto.getPersona() == null || onBoardingDto.getUsuario() == null) {
                response.put("success", false);
                response.put("message", "Datos de persona y usuario son requeridos");
                return ResponseEntity.badRequest().body(response);
            }

            // Procesar el onboarding según el tipo de usuario
            Usuarios usuario = authBl.processOnBoarding(onBoardingDto);

            response.put("success", true);
            response.put("message", "Usuario registrado exitosamente");
            response.put("data", Map.of(
                    "usuario_id", usuario.getId(),
                    "correo", usuario.getCorreo(),
                    "tipo_usuario", usuario.getTipoUsuarioId().getTipoUsuario()
            ));

            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (IllegalArgumentException e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);

        } catch (UnsupportedOperationException e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error interno del servidor: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Endpoint de prueba para verificar que el servicio está funcionando
     * GET /auth/health
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "OK");
        response.put("service", "Auth Service");
        return ResponseEntity.ok(response);
    }
}