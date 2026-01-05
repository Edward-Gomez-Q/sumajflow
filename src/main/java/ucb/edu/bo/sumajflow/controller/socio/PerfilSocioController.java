package ucb.edu.bo.sumajflow.controller.socio;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ucb.edu.bo.sumajflow.bl.ProfileBl;
import ucb.edu.bo.sumajflow.dto.profile.UpdatePersonalDataDto;
import ucb.edu.bo.sumajflow.dto.profile.UpdatePasswordDto;
import ucb.edu.bo.sumajflow.utils.HttpUtils;
import ucb.edu.bo.sumajflow.utils.JwtUtil;

import java.util.HashMap;
import java.util.Map;

/**
 * Controlador de perfil para usuarios tipo SOCIO
 */
@RestController
@RequestMapping("/socio/perfil")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class PerfilSocioController {

    private final ProfileBl profileBl;
    private final JwtUtil jwtUtil;

    /**
     * Obtener perfil completo
     * GET /socio/perfil
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> obtenerPerfil(
            @RequestHeader("Authorization") String token
    ) {
        Map<String, Object> response = new HashMap<>();

        try {
            Integer usuarioId = extractUsuarioId(token);
            Map<String, Object> profileData = profileBl.obtenerPerfil(usuarioId);

            response.put("success", true);
            response.put("message", "Perfil obtenido exitosamente");
            response.put("data", profileData);

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error al obtener el perfil: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Actualizar datos personales
     * PUT /socio/perfil/datos-personales
     */
    @PutMapping("/datos-personales")
    public ResponseEntity<Map<String, Object>> actualizarDatosPersonales(
            @Valid @RequestBody UpdatePersonalDataDto dto,
            @RequestHeader("Authorization") String token,
            HttpServletRequest request
    ) {
        Map<String, Object> response = new HashMap<>();

        try {
            Integer usuarioId = extractUsuarioId(token);
            String ipOrigen = HttpUtils.obtenerIpCliente(request);

            profileBl.actualizarDatosPersonales(usuarioId, dto, ipOrigen);

            response.put("success", true);
            response.put("message", "Datos personales actualizados exitosamente");

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error al actualizar datos: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Actualizar correo electrónico
     * PUT /socio/perfil/correo
     */
    @PutMapping("/correo")
    public ResponseEntity<Map<String, Object>> actualizarCorreo(
            @RequestBody Map<String, String> requestBody,
            @RequestHeader("Authorization") String token,
            HttpServletRequest request
    ) {
        Map<String, Object> response = new HashMap<>();

        try {
            Integer usuarioId = extractUsuarioId(token);
            String nuevoCorreo = requestBody.get("correo");
            String contrasenaActual = requestBody.get("contrasena_actual");
            String ipOrigen = HttpUtils.obtenerIpCliente(request);

            if (nuevoCorreo == null || nuevoCorreo.trim().isEmpty()) {
                response.put("success", false);
                response.put("message", "El nuevo correo es requerido");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }

            if (contrasenaActual == null || contrasenaActual.trim().isEmpty()) {
                response.put("success", false);
                response.put("message", "La contraseña actual es requerida");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }

            profileBl.actualizarCorreo(usuarioId, nuevoCorreo, contrasenaActual, ipOrigen);

            response.put("success", true);
            response.put("message", "Correo actualizado exitosamente");

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error al actualizar correo: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Actualizar contraseña
     * PUT /socio/perfil/contrasena
     */
    @PutMapping("/contrasena")
    public ResponseEntity<Map<String, Object>> actualizarContrasena(
            @Valid @RequestBody UpdatePasswordDto dto,
            @RequestHeader("Authorization") String token,
            HttpServletRequest request
    ) {
        Map<String, Object> response = new HashMap<>();

        try {
            Integer usuarioId = extractUsuarioId(token);
            String ipOrigen = HttpUtils.obtenerIpCliente(request);

            profileBl.actualizarContrasena(usuarioId, dto, ipOrigen);

            response.put("success", true);
            response.put("message", "Contraseña actualizada exitosamente");

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error al actualizar contraseña: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Actualizar dirección
     * PUT /socio/perfil/direccion
     */
    @PutMapping("/direccion")
    public ResponseEntity<Map<String, Object>> actualizarDireccion(
            @RequestBody Map<String, String> addressData,
            @RequestHeader("Authorization") String token,
            HttpServletRequest request
    ) {
        Map<String, Object> response = new HashMap<>();

        try {
            Integer usuarioId = extractUsuarioId(token);
            String ipOrigen = HttpUtils.obtenerIpCliente(request);

            profileBl.actualizarDireccion(usuarioId, addressData, ipOrigen);

            response.put("success", true);
            response.put("message", "Dirección actualizada exitosamente");

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error al actualizar dirección: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Método auxiliar para extraer el usuario del token
     */
    private Integer extractUsuarioId(String token) {
        String cleanToken = token.replace("Bearer ", "");
        return jwtUtil.extractUsuarioId(cleanToken);
    }
}