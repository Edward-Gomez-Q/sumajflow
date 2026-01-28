package ucb.edu.bo.sumajflow.controller.comercializadora;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ucb.edu.bo.sumajflow.bl.comercializadora.ConcentradoComercializadoraBl;
import ucb.edu.bo.sumajflow.dto.ingenio.*;
import ucb.edu.bo.sumajflow.utils.HttpUtils;
import ucb.edu.bo.sumajflow.utils.JwtUtil;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Controlador REST para gestión de Concentrados por la COMERCIALIZADORA
 * Endpoints solo para operaciones que corresponden al rol Comercializadora
 */
@RestController
@RequestMapping("/comercializadora/concentrados")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class ConcentradoComercializadoraController {

    private final ConcentradoComercializadoraBl concentradoComercializadoraBl;
    private final JwtUtil jwtUtil;

    /**
     * Listar concentrados disponibles para venta
     * GET /comercializadora/concentrados
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> listarConcentradosDisponibles(
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

            Page<ConcentradoResponseDto> concentrados = concentradoComercializadoraBl.listarConcentradosDisponibles(
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
     * Obtener detalle de un concentrado
     * GET /comercializadora/concentrados/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> obtenerDetalle(
            @PathVariable Integer id,
            @RequestHeader("Authorization") String token
    ) {
        Map<String, Object> response = new HashMap<>();

        try {
            Integer usuarioId = extractUsuarioId(token);

            ConcentradoResponseDto concentrado = concentradoComercializadoraBl.obtenerDetalle(id, usuarioId);

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
     * Obtener dashboard de ventas
     * GET /comercializadora/concentrados/dashboard
     */
    @GetMapping("/dashboard")
    public ResponseEntity<Map<String, Object>> obtenerDashboard(
            @RequestHeader("Authorization") String token
    ) {
        Map<String, Object> response = new HashMap<>();

        try {
            Integer usuarioId = extractUsuarioId(token);

            Map<String, Object> dashboard = concentradoComercializadoraBl.obtenerDashboard(usuarioId);

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
     * Revisar solicitud de venta (cambiar a "venta_en_revision")
     * PATCH /comercializadora/concentrados/{id}/revisar-venta
     */
    @PatchMapping("/{id}/revisar-venta")
    public ResponseEntity<Map<String, Object>> revisarVenta(
            @PathVariable Integer id,
            @RequestHeader("Authorization") String token,
            HttpServletRequest request
    ) {
        Map<String, Object> response = new HashMap<>();

        try {
            Integer usuarioId = extractUsuarioId(token);
            String ipOrigen = HttpUtils.obtenerIpCliente(request);

            ConcentradoResponseDto concentrado = concentradoComercializadoraBl.revisarVenta(
                    id,
                    usuarioId,
                    ipOrigen
            );

            response.put("success", true);
            response.put("message", "Venta en revisión");
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
     * Aprobar venta (definir precio de compra)
     * POST /comercializadora/concentrados/{id}/aprobar-venta
     */
    @PostMapping("/{id}/aprobar-venta")
    public ResponseEntity<Map<String, Object>> aprobarVenta(
            @PathVariable Integer id,
            @Valid @RequestBody AprobarVentaDto aprobarDto,
            @RequestHeader("Authorization") String token,
            HttpServletRequest request
    ) {
        Map<String, Object> response = new HashMap<>();

        try {
            Integer usuarioId = extractUsuarioId(token);
            String ipOrigen = HttpUtils.obtenerIpCliente(request);

            VentaConcentradoResponseDto venta = concentradoComercializadoraBl.aprobarVenta(
                    id,
                    aprobarDto,
                    usuarioId,
                    ipOrigen
            );

            response.put("success", true);
            response.put("message", "Venta aprobada exitosamente");
            response.put("data", venta);
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
     * Registrar pago de venta al socio
     * POST /comercializadora/concentrados/{id}/registrar-pago-venta
     */
    @PostMapping("/{id}/registrar-pago-venta")
    public ResponseEntity<Map<String, Object>> registrarPagoVenta(
            @PathVariable Integer id,
            @Valid @RequestBody RegistrarPagoVentaDto pagoDto,
            @RequestHeader("Authorization") String token,
            HttpServletRequest request
    ) {
        Map<String, Object> response = new HashMap<>();

        try {
            Integer usuarioId = extractUsuarioId(token);
            String ipOrigen = HttpUtils.obtenerIpCliente(request);

            ConcentradoResponseDto concentrado = concentradoComercializadoraBl.registrarPagoVenta(
                    id,
                    pagoDto,
                    usuarioId,
                    ipOrigen
            );

            response.put("success", true);
            response.put("message", "¡Venta completada! Pago registrado exitosamente");
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
     * Ver historial de ventas realizadas
     * GET /comercializadora/concentrados/historial-ventas
     */
    @GetMapping("/historial-ventas")
    public ResponseEntity<Map<String, Object>> verHistorialVentas(
            @RequestHeader("Authorization") String token
    ) {
        Map<String, Object> response = new HashMap<>();

        try {
            Integer usuarioId = extractUsuarioId(token);

            List<VentaConcentradoResponseDto> ventas = concentradoComercializadoraBl.verHistorialVentas(usuarioId);

            response.put("success", true);
            response.put("data", ventas);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error al obtener historial de ventas: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    // Metodo auxiliar para extraer usuario del token
    private Integer extractUsuarioId(String token) {
        String cleanToken = token.replace("Bearer ", "");
        return jwtUtil.extractUsuarioId(cleanToken);
    }
}