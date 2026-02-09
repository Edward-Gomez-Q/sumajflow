// src/main/java/ucb/edu/bo/sumajflow/controller/comercializadora/TablaPreciosMineralController.java
package ucb.edu.bo.sumajflow.controller.comercializadora;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ucb.edu.bo.sumajflow.bl.comercializadora.TablaPreciosMineralBl;
import ucb.edu.bo.sumajflow.dto.comercializadora.TablaPreciosMineralDto;
import ucb.edu.bo.sumajflow.dto.comercializadora.ValidacionPreciosResponseDto;
import ucb.edu.bo.sumajflow.utils.JwtUtil;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/comercializadora/tabla-precios")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class TablaPreciosMineralController {

    private final TablaPreciosMineralBl tablaPreciosBl;
    private final JwtUtil jwtUtil;

    /**
     * Crear nuevo rango de precios
     * POST /comercializadora/tabla-precios
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> crear(
            @Valid @RequestBody TablaPreciosMineralDto dto,
            @RequestHeader("Authorization") String token
    ) {
        Map<String, Object> response = new HashMap<>();

        try {
            Integer usuarioId = extractUsuarioId(token);
            TablaPreciosMineralDto creado = tablaPreciosBl.crear(dto, usuarioId);

            response.put("success", true);
            response.put("message", "Rango de precios creado exitosamente");
            response.put("data", creado);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (IllegalArgumentException e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error interno: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * Actualizar rango de precios
     * PUT /comercializadora/tabla-precios/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> actualizar(
            @PathVariable Integer id,
            @Valid @RequestBody TablaPreciosMineralDto dto,
            @RequestHeader("Authorization") String token
    ) {
        Map<String, Object> response = new HashMap<>();

        try {
            Integer usuarioId = extractUsuarioId(token);
            TablaPreciosMineralDto actualizado = tablaPreciosBl.actualizar(id, dto, usuarioId);

            response.put("success", true);
            response.put("message", "Rango de precios actualizado exitosamente");
            response.put("data", actualizado);
            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error interno: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * Eliminar rango de precios
     * DELETE /comercializadora/tabla-precios/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> eliminar(
            @PathVariable Integer id,
            @RequestHeader("Authorization") String token
    ) {
        Map<String, Object> response = new HashMap<>();

        try {
            Integer usuarioId = extractUsuarioId(token);
            tablaPreciosBl.eliminar(id, usuarioId);

            response.put("success", true);
            response.put("message", "Rango de precios eliminado exitosamente");
            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error interno: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * Desactivar rango de precios
     * POST /comercializadora/tabla-precios/{id}/desactivar
     */
    @PostMapping("/{id}/desactivar")
    public ResponseEntity<Map<String, Object>> desactivar(
            @PathVariable Integer id,
            @RequestHeader("Authorization") String token
    ) {
        Map<String, Object> response = new HashMap<>();

        try {
            Integer usuarioId = extractUsuarioId(token);
            TablaPreciosMineralDto desactivado = tablaPreciosBl.desactivar(id, usuarioId);

            response.put("success", true);
            response.put("message", "Rango de precios desactivado exitosamente");
            response.put("data", desactivado);
            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error interno: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * Listar rangos de precios
     * GET /comercializadora/tabla-precios
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> listar(
            @RequestParam(required = false) String mineral,
            @RequestParam(required = false) Boolean activo,
            @RequestHeader("Authorization") String token
    ) {
        Map<String, Object> response = new HashMap<>();

        try {
            Integer usuarioId = extractUsuarioId(token);
            List<TablaPreciosMineralDto> precios = tablaPreciosBl.listar(usuarioId, mineral, activo);

            response.put("success", true);
            response.put("data", precios);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error al listar: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * Listar rangos agrupados por mineral
     * GET /comercializadora/tabla-precios/agrupados
     */
    @GetMapping("/agrupados")
    public ResponseEntity<Map<String, Object>> listarAgrupados(
            @RequestHeader("Authorization") String token
    ) {
        Map<String, Object> response = new HashMap<>();

        try {
            Integer usuarioId = extractUsuarioId(token);
            Map<String, List<TablaPreciosMineralDto>> agrupados =
                    tablaPreciosBl.listarAgrupadosPorMineral(usuarioId);

            response.put("success", true);
            response.put("data", agrupados);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error al listar: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * Validar configuración de precios
     * GET /comercializadora/tabla-precios/validar
     */
    @GetMapping("/validar")
    public ResponseEntity<Map<String, Object>> validar(
            @RequestHeader("Authorization") String token
    ) {
        Map<String, Object> response = new HashMap<>();

        try {
            Integer usuarioId = extractUsuarioId(token);
            // Obtener ID de comercializadora desde usuario
            ValidacionPreciosResponseDto validacion = tablaPreciosBl.validarConfiguracion(usuarioId);

            response.put("success", true);
            response.put("data", validacion);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error al validar: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    private Integer extractUsuarioId(String token) {
        return jwtUtil.extractUsuarioId(token.replace("Bearer ", ""));
    }
}