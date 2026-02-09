package ucb.edu.bo.sumajflow.controller.socio;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ucb.edu.bo.sumajflow.bl.socio.VentaSocioBl;
import ucb.edu.bo.sumajflow.dto.venta.*;
import ucb.edu.bo.sumajflow.utils.JwtUtil;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Controlador REST para Venta de Concentrados/Lotes - ROL SOCIO
 * Endpoints para crear ventas, subir reportes, cerrar ventas y consultas auxiliares
 */
@RestController
@RequestMapping("/socio/ventas")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class VentaSocioController {

    private final VentaSocioBl ventaSocioBl;
    private final JwtUtil jwtUtil;

    // ==================== CREAR VENTA ====================

    /**
     * Crear venta de concentrado(s)
     * POST /socio/ventas/concentrado
     */
    @PostMapping("/concentrado")
    public ResponseEntity<Map<String, Object>> crearVentaConcentrado(
            @Valid @RequestBody VentaCreateDto createDto,
            @RequestHeader("Authorization") String token
    ) {
        Map<String, Object> response = new HashMap<>();

        try {
            Integer usuarioId = extractUsuarioId(token);
            VentaLiquidacionResponseDto venta = ventaSocioBl.crearVentaConcentrado(createDto, usuarioId);

            response.put("success", true);
            response.put("message", "Solicitud de venta de concentrado creada exitosamente. Pendiente de aprobación por la comercializadora.");
            response.put("data", venta);
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
     * Crear venta de lote(s) complejo(s)
     * POST /socio/ventas/lote-complejo
     */
    @PostMapping("/lote-complejo")
    public ResponseEntity<Map<String, Object>> crearVentaLoteComplejo(
            @Valid @RequestBody VentaCreateDto createDto,
            @RequestHeader("Authorization") String token
    ) {
        Map<String, Object> response = new HashMap<>();

        try {
            Integer usuarioId = extractUsuarioId(token);
            VentaLiquidacionResponseDto venta = ventaSocioBl.crearVentaLoteComplejo(createDto, usuarioId);

            response.put("success", true);
            response.put("message", "Solicitud de venta de lote complejo creada exitosamente. Pendiente de aprobación por la comercializadora.");
            response.put("data", venta);
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

    // ==================== REPORTE QUÍMICO ====================

    /**
     * Subir reporte químico del socio
     * POST /socio/ventas/{id}/reporte-quimico
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
            VentaLiquidacionResponseDto venta = ventaSocioBl.subirReporteQuimico(uploadDto, usuarioId);

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

    // ==================== CERRAR VENTA ====================

    /**
     * Cerrar venta (cuando la cotización le conviene al socio)
     * POST /socio/ventas/{id}/cerrar
     */
    @PostMapping("/{id}/cerrar")
    public ResponseEntity<Map<String, Object>> cerrarVenta(
            @PathVariable Integer id,
            @Valid @RequestBody VentaCierreDto cierreDto,
            @RequestHeader("Authorization") String token
    ) {
        Map<String, Object> response = new HashMap<>();

        try {
            Integer usuarioId = extractUsuarioId(token);
            VentaLiquidacionResponseDto venta = ventaSocioBl.cerrarVenta(id, cierreDto, usuarioId);

            response.put("success", true);
            response.put("message", "Venta cerrada exitosamente. Pendiente de pago por la comercializadora.");
            response.put("data", venta);
            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException | IllegalStateException e) {
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
     * Listar ventas del socio con filtros
     * GET /socio/ventas
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
            Page<VentaLiquidacionResponseDto> ventas = ventaSocioBl.listarVentas(
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
     * GET /socio/ventas/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> obtenerDetalleCompleto(
            @PathVariable Integer id,
            @RequestHeader("Authorization") String token
    ) {
        Map<String, Object> response = new HashMap<>();

        try {
            Integer usuarioId = extractUsuarioId(token);
            VentaLiquidacionDetalleDto detalle = ventaSocioBl.obtenerDetalleCompletoVenta(id, usuarioId);

            response.put("success", true);
            response.put("data", detalle);
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
     * GET /socio/ventas/estadisticas
     */
    @GetMapping("/estadisticas")
    public ResponseEntity<Map<String, Object>> obtenerEstadisticas(
            @RequestHeader("Authorization") String token
    ) {
        Map<String, Object> response = new HashMap<>();

        try {
            Integer usuarioId = extractUsuarioId(token);
            Map<String, Object> estadisticas = ventaSocioBl.obtenerEstadisticas(usuarioId);

            response.put("success", true);
            response.put("data", estadisticas);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error al obtener estadísticas: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    // ==================== CONSULTAS AUXILIARES ====================

    /**
     * Obtener concentrados disponibles para venta (estado: listo_para_venta)
     * GET /socio/ventas/concentrados-disponibles
     */
    @GetMapping("/concentrados-disponibles")
    public ResponseEntity<Map<String, Object>> obtenerConcentradosDisponibles(
            @RequestHeader("Authorization") String token
    ) {
        Map<String, Object> response = new HashMap<>();

        try {
            Integer usuarioId = extractUsuarioId(token);
            List<Map<String, Object>> concentrados = ventaSocioBl.obtenerConcentradosDisponibles(usuarioId);

            response.put("success", true);
            response.put("data", concentrados);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error al obtener concentrados disponibles: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * Obtener lotes disponibles para venta directa (estado: Transporte completo)
     * GET /socio/ventas/lotes-disponibles
     */
    @GetMapping("/lotes-disponibles")
    public ResponseEntity<Map<String, Object>> obtenerLotesDisponibles(
            @RequestHeader("Authorization") String token
    ) {
        Map<String, Object> response = new HashMap<>();

        try {
            Integer usuarioId = extractUsuarioId(token);
            List<Map<String, Object>> lotes = ventaSocioBl.obtenerLotesDisponiblesParaVenta(usuarioId);

            response.put("success", true);
            response.put("data", lotes);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error al obtener lotes disponibles: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * Obtener lista de comercializadoras disponibles
     * GET /socio/ventas/comercializadoras
     */
    @GetMapping("/comercializadoras")
    public ResponseEntity<Map<String, Object>> obtenerComercializadoras(
            @RequestHeader("Authorization") String token
    ) {
        Map<String, Object> response = new HashMap<>();

        try {
            extractUsuarioId(token);
            List<Map<String, Object>> comercializadoras = ventaSocioBl.obtenerComercializadorasDisponibles();

            response.put("success", true);
            response.put("data", comercializadoras);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error al obtener comercializadoras: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    private Integer extractUsuarioId(String token) {
        String cleanToken = token.replace("Bearer ", "");
        return jwtUtil.extractUsuarioId(cleanToken);
    }
}