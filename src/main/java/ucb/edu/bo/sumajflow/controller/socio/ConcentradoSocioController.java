package ucb.edu.bo.sumajflow.controller.socio;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ucb.edu.bo.sumajflow.bl.socio.ConcentradoSocioBl;
import ucb.edu.bo.sumajflow.dto.ingenio.*;
import ucb.edu.bo.sumajflow.utils.HttpUtils;
import ucb.edu.bo.sumajflow.utils.JwtUtil;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Controlador REST para gestión de Concentrados por el SOCIO
 * Endpoints solo para operaciones que corresponden al rol Socio
 */
@RestController
@RequestMapping("/socio/concentrados")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class ConcentradoSocioController {

    private final ConcentradoSocioBl concentradoSocioBl;
    private final JwtUtil jwtUtil;

    /**
     * Listar mis concentrados (solo los del socio autenticado)
     * GET /socio/concentrados
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> listarMisConcentrados(
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

            Page<ConcentradoResponseDto> concentrados = concentradoSocioBl.listarMisConcentrados(
                    usuarioId,
                    estado,
                    mineralPrincipal,
                    fechaDesde,
                    fechaHasta,
                    page,
                    size
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
     * Obtener detalle de mi concentrado (solo si es propietario)
     * GET /socio/concentrados/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> obtenerMiConcentrado(
            @PathVariable Integer id,
            @RequestHeader("Authorization") String token
    ) {
        Map<String, Object> response = new HashMap<>();

        try {
            Integer usuarioId = extractUsuarioId(token);

            ConcentradoResponseDto concentrado = concentradoSocioBl.obtenerMiConcentrado(id, usuarioId);

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
     * Obtener mi dashboard personal de concentrados
     * GET /socio/concentrados/dashboard
     */
    @GetMapping("/dashboard")
    public ResponseEntity<Map<String, Object>> obtenerMiDashboard(
            @RequestHeader("Authorization") String token
    ) {
        Map<String, Object> response = new HashMap<>();

        try {
            Integer usuarioId = extractUsuarioId(token);

            Map<String, Object> dashboard = concentradoSocioBl.obtenerMiDashboard(usuarioId);

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
     * Ver procesos del concentrado (solo lectura)
     * GET /socio/concentrados/{id}/procesos
     */
    @GetMapping("/{id}/procesos")
    public ResponseEntity<Map<String, Object>> verProcesos(
            @PathVariable Integer id,
            @RequestHeader("Authorization") String token
    ) {
        Map<String, Object> response = new HashMap<>();

        try {
            Integer usuarioId = extractUsuarioId(token);

            ProcesosConcentradoResponseDto procesos = concentradoSocioBl.verProcesos(id, usuarioId);

            response.put("success", true);
            response.put("data", procesos);
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
     * Solicitar liquidación de servicio al ingenio
     * POST /socio/concentrados/{id}/solicitar-liquidacion-servicio
     */
    @PostMapping("/{id}/solicitar-liquidacion-servicio")
    public ResponseEntity<Map<String, Object>> solicitarLiquidacionServicio(
            @PathVariable Integer id,
            @Valid @RequestBody SolicitudLiquidacionServicioDto solicitudDto,
            @RequestHeader("Authorization") String token,
            HttpServletRequest request
    ) {
        Map<String, Object> response = new HashMap<>();

        try {
            Integer usuarioId = extractUsuarioId(token);
            String ipOrigen = HttpUtils.obtenerIpCliente(request);

            ConcentradoResponseDto concentrado = concentradoSocioBl.solicitarLiquidacionServicio(
                    id,
                    solicitudDto,
                    usuarioId,
                    ipOrigen
            );

            response.put("success", true);
            response.put("message", "Solicitud de liquidación enviada exitosamente");
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
     * Solicitar venta a una comercializadora
     * POST /socio/concentrados/{id}/solicitar-venta
     */
    @PostMapping("/{id}/solicitar-venta")
    public ResponseEntity<Map<String, Object>> solicitarVenta(
            @PathVariable Integer id,
            @Valid @RequestBody SolicitudVentaDto solicitudDto,
            @RequestHeader("Authorization") String token,
            HttpServletRequest request
    ) {
        Map<String, Object> response = new HashMap<>();

        try {
            Integer usuarioId = extractUsuarioId(token);
            String ipOrigen = HttpUtils.obtenerIpCliente(request);

            ConcentradoResponseDto concentrado = concentradoSocioBl.solicitarVenta(
                    id,
                    solicitudDto,
                    usuarioId,
                    ipOrigen
            );

            response.put("success", true);
            response.put("message", "Solicitud de venta enviada exitosamente");
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
     * Ver todas mis liquidaciones
     * GET /socio/concentrados/liquidaciones
     */
    @GetMapping("/liquidaciones")
    public ResponseEntity<Map<String, Object>> verMisLiquidaciones(
            @RequestHeader("Authorization") String token
    ) {
        Map<String, Object> response = new HashMap<>();

        try {
            Integer usuarioId = extractUsuarioId(token);

            List<LiquidacionServicioResponseDto> liquidaciones = concentradoSocioBl.verMisLiquidaciones(usuarioId);

            response.put("success", true);
            response.put("data", liquidaciones);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error al obtener liquidaciones: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    // Metodo auxiliar para extraer usuario del token
    private Integer extractUsuarioId(String token) {
        String cleanToken = token.replace("Bearer ", "");
        return jwtUtil.extractUsuarioId(cleanToken);
    }
}