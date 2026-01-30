package ucb.edu.bo.sumajflow.controller.ingenio;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ucb.edu.bo.sumajflow.bl.ingenio.ConcentradoIngenioBl;
import ucb.edu.bo.sumajflow.dto.ingenio.*;
import ucb.edu.bo.sumajflow.utils.HttpUtils;
import ucb.edu.bo.sumajflow.utils.JwtUtil;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Controlador REST para gestión de Concentrados (CRUD básico)
 * Endpoints para creación, listado y consulta de concentrados
 */
@RestController
@RequestMapping("/ingenio/concentrados")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class ConcentradoIngenioController {

    private final ConcentradoIngenioBl concentradoIngenioBl;
    private final JwtUtil jwtUtil;

    /**
     * Listar todos los concentrados del ingenio con filtros y paginación
     * GET /ingenio/concentrados
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> listar(
            @RequestParam(required = false) String estado,
            @RequestParam(required = false) String mineralPrincipal,
            @RequestParam(required = false) LocalDateTime fechaDesde,
            @RequestParam(required = false) LocalDateTime fechaHasta,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestHeader("Authorization") String token
    ) {
        Map<String, Object> response = new HashMap<>();

        try {
            Integer usuarioId = extractUsuarioId(token);

            Page<ConcentradoResponseDto> concentrados = concentradoIngenioBl.listarConcentrados(
                    usuarioId, estado, mineralPrincipal, fechaDesde, fechaHasta, page, size
            );

            response.put("success", true);
            response.put("data", concentrados.getContent());
            response.put("totalElements", concentrados.getTotalElements());
            response.put("totalPages", concentrados.getTotalPages());
            response.put("currentPage", concentrados.getNumber());
            response.put("pageSize", concentrados.getSize());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error al listar concentrados: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * Obtener detalle de un concentrado específico
     * GET /ingenio/concentrados/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> obtenerDetalle(
            @PathVariable Integer id,
            @RequestHeader("Authorization") String token
    ) {
        Map<String, Object> response = new HashMap<>();

        try {
            Integer usuarioId = extractUsuarioId(token);
            ConcentradoResponseDto concentrado = concentradoIngenioBl.obtenerDetalle(id, usuarioId);

            response.put("success", true);
            response.put("data", concentrado);
            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error interno del servidor: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * Obtener dashboard de estadísticas del ingenio
     * GET /ingenio/concentrados/dashboard
     */
    @GetMapping("/dashboard")
    public ResponseEntity<Map<String, Object>> obtenerDashboard(
            @RequestHeader("Authorization") String token
    ) {
        Map<String, Object> response = new HashMap<>();

        try {
            Integer usuarioId = extractUsuarioId(token);
            Map<String, Object> dashboard = concentradoIngenioBl.obtenerDashboard(usuarioId);

            response.put("success", true);
            response.put("data", dashboard);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error al obtener dashboard: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * Crear concentrado(s) a partir de lote(s)
     * POST /ingenio/concentrados
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> crearConcentrado(
            @Valid @RequestBody ConcentradoCreateDto createDto,
            @RequestHeader("Authorization") String token,
            HttpServletRequest request
    ) {
        Map<String, Object> response = new HashMap<>();

        try {
            Integer usuarioId = extractUsuarioId(token);
            String ipOrigen = HttpUtils.obtenerIpCliente(request);

            List<ConcentradoResponseDto> concentrados = concentradoIngenioBl.crearConcentrado(
                    createDto, usuarioId, ipOrigen
            );

            String mensaje = concentrados.size() == 1
                    ? "Concentrado creado exitosamente"
                    : concentrados.size() + " concentrados creados exitosamente (" +
                    concentrados.stream()
                            .map(ConcentradoResponseDto::getMineralPrincipal)
                            .reduce((a, b) -> a + ", " + b)
                            .orElse("") + ")";

            response.put("success", true);
            response.put("message", mensaje);
            response.put("data", concentrados);
            response.put("cantidad", concentrados.size());

            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (IllegalArgumentException e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error interno del servidor: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * Obtener información de la planta del ingenio (cupo mínimo, capacidad)
     * GET /ingenio/concentrados/info-planta
     */
    @GetMapping("/info-planta")
    public ResponseEntity<Map<String, Object>> obtenerInfoPlanta(
            @RequestHeader("Authorization") String token
    ) {
        Map<String, Object> response = new HashMap<>();

        try {
            Integer usuarioId = extractUsuarioId(token);
            Map<String, Object> infoPlanta = concentradoIngenioBl.obtenerInfoPlanta(usuarioId);

            response.put("success", true);
            response.put("data", infoPlanta);
            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error al obtener información de la planta: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    private Integer extractUsuarioId(String token) {
        String cleanToken = token.replace("Bearer ", "");
        return jwtUtil.extractUsuarioId(cleanToken);
    }
}