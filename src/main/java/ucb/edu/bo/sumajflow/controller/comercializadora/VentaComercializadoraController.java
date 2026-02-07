package ucb.edu.bo.sumajflow.controller.comercializadora;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ucb.edu.bo.sumajflow.bl.comercializadora.VentaComercializadoraBl;
import ucb.edu.bo.sumajflow.dto.venta.*;
import ucb.edu.bo.sumajflow.utils.JwtUtil;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Controlador REST para Venta de Concentrados/Lotes - ROL COMERCIALIZADORA
 * Endpoints para aprobar/rechazar, subir reportes, confirmar pago
 */
@RestController
@RequestMapping("/comercializadora/ventas")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class VentaComercializadoraController {

    private final VentaComercializadoraBl ventaComercializadoraBl;
    private final JwtUtil jwtUtil;

    // ==================== APROBAR / RECHAZAR ====================

    /**
     * Aprobar solicitud de venta
     * POST /comercializadora/ventas/{id}/aprobar
     */
    @PostMapping("/{id}/aprobar")
    public ResponseEntity<Map<String, Object>> aprobarVenta(
            @PathVariable Integer id,
            @RequestHeader("Authorization") String token
    ) {
        Map<String, Object> response = new HashMap<>();

        try {
            Integer usuarioId = extractUsuarioId(token);
            VentaLiquidacionResponseDto venta = ventaComercializadoraBl.aprobarVenta(id, usuarioId);

            response.put("success", true);
            response.put("message", "Venta aprobada exitosamente. Ambas partes deben subir su reporte químico.");
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
     * Rechazar solicitud de venta
     * POST /comercializadora/ventas/{id}/rechazar
     */
    @PostMapping("/{id}/rechazar")
    public ResponseEntity<Map<String, Object>> rechazarVenta(
            @PathVariable Integer id,
            @RequestParam(required = false) String motivo,
            @RequestHeader("Authorization") String token
    ) {
        Map<String, Object> response = new HashMap<>();

        try {
            Integer usuarioId = extractUsuarioId(token);
            VentaLiquidacionResponseDto venta = ventaComercializadoraBl.rechazarVenta(id, motivo, usuarioId);

            response.put("success", true);
            response.put("message", "Venta rechazada. Los concentrados/lotes han sido liberados.");
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

    // ==================== REPORTE QUÍMICO ====================

    /**
     * Subir reporte químico de la comercializadora
     * POST /comercializadora/ventas/{id}/reporte-quimico
     */
    @PostMapping("/{id}/reporte-quimico")
    public ResponseEntity<Map<String, Object>> subirReporteQuimico(
            @PathVariable Integer id,
            @Valid @RequestBody ReporteQuimicoUploadDto uploadDto,
            @RequestHeader("Authorization") String token
    ) {
        Map<String, Object> response = new HashMap<>();

        try {
            Integer usuarioId = extractUsuarioId(token);
            uploadDto.setLiquidacionId(id);
            VentaLiquidacionResponseDto venta = ventaComercializadoraBl.subirReporteQuimico(uploadDto, usuarioId);

            response.put("success", true);
            response.put("message", "Reporte químico subido exitosamente");
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

    // ==================== CONFIRMAR PAGO ====================

    /**
     * Confirmar pago de la venta
     * POST /comercializadora/ventas/{id}/pagar
     */
    @PostMapping("/{id}/pagar")
    public ResponseEntity<Map<String, Object>> confirmarPago(
            @PathVariable Integer id,
            @Valid @RequestBody VentaPagoDto pagoDto,
            @RequestHeader("Authorization") String token
    ) {
        Map<String, Object> response = new HashMap<>();

        try {
            Integer usuarioId = extractUsuarioId(token);
            VentaLiquidacionResponseDto venta = ventaComercializadoraBl.confirmarPago(id, pagoDto, usuarioId);

            response.put("success", true);
            response.put("message", "Pago confirmado exitosamente. La venta ha sido completada.");
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

    // ==================== LISTAR Y CONSULTAR ====================

    /**
     * Listar ventas de la comercializadora con filtros
     * GET /comercializadora/ventas
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> listarVentas(
            @RequestParam(required = false) String estado,
            @RequestParam(required = false) String tipoLiquidacion,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fechaDesde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fechaHasta,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestHeader("Authorization") String token
    ) {
        Map<String, Object> response = new HashMap<>();

        try {
            Integer usuarioId = extractUsuarioId(token);
            Page<VentaLiquidacionResponseDto> ventas = ventaComercializadoraBl.listarVentas(
                    usuarioId, estado, tipoLiquidacion, fechaDesde, fechaHasta, page, size);

            response.put("success", true);
            response.put("data", ventas.getContent());
            response.put("totalElements", ventas.getTotalElements());
            response.put("totalPages", ventas.getTotalPages());
            response.put("currentPage", ventas.getNumber());
            response.put("pageSize", ventas.getSize());
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error al listar ventas: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * Obtener detalle de una venta
     * GET /comercializadora/ventas/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> obtenerDetalle(
            @PathVariable Integer id,
            @RequestHeader("Authorization") String token
    ) {
        Map<String, Object> response = new HashMap<>();

        try {
            Integer usuarioId = extractUsuarioId(token);
            VentaLiquidacionResponseDto venta = ventaComercializadoraBl.obtenerDetalleVenta(id, usuarioId);

            response.put("success", true);
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

    // ==================== ESTADÍSTICAS ====================

    /**
     * Obtener estadísticas de ventas
     * GET /comercializadora/ventas/estadisticas
     */
    @GetMapping("/estadisticas")
    public ResponseEntity<Map<String, Object>> obtenerEstadisticas(
            @RequestHeader("Authorization") String token
    ) {
        Map<String, Object> response = new HashMap<>();

        try {
            Integer usuarioId = extractUsuarioId(token);
            Map<String, Object> estadisticas = ventaComercializadoraBl.obtenerEstadisticas(usuarioId);

            response.put("success", true);
            response.put("data", estadisticas);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error al obtener estadísticas: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    private Integer extractUsuarioId(String token) {
        String cleanToken = token.replace("Bearer ", "");
        return jwtUtil.extractUsuarioId(cleanToken);
    }
}