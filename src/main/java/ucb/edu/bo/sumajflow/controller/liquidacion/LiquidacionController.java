package ucb.edu.bo.sumajflow.controller.liquidacion;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ucb.edu.bo.sumajflow.bl.liquidacion.LiquidacionBl;
import ucb.edu.bo.sumajflow.dto.liquidacion.LiquidacionCreateDto;
import ucb.edu.bo.sumajflow.dto.liquidacion.LiquidacionDetalleDto;
import ucb.edu.bo.sumajflow.dto.liquidacion.LiquidacionResponseDto;
import ucb.edu.bo.sumajflow.utils.HttpUtils;
import ucb.edu.bo.sumajflow.utils.JwtUtil;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Controlador REST para gestión de Liquidaciones
 * Maneja ventas directas, ventas de concentrado y cobros de ingenio
 */
@RestController
@RequestMapping("/liquidaciones")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class LiquidacionController {

    private final LiquidacionBl liquidacionBl;
    private final JwtUtil jwtUtil;

    /**
     * Crear una nueva liquidación
     * POST /liquidaciones
     *
     * Body ejemplo para venta directa:
     * {
     *   "socioId": 1,
     *   "tipoLiquidacion": "venta_directa",
     *   "fechaLiquidacion": "2025-01-20",
     *   "moneda": "USD",
     *   "pesoLiquidado": 1500.50,
     *   "loteId": 5,
     *   "reporteQuimicoId": 3,
     *   "cotizaciones": [
     *     {
     *       "mineral": "Ag",
     *       "cotizacionUsd": 25.50,
     *       "unidad": "oz/TM"
     *     },
     *     {
     *       "mineral": "Pb",
     *       "cotizacionUsd": 0.95,
     *       "unidad": "lb/TM"
     *     }
     *   ],
     *   "deducciones": [
     *     {
     *       "concepto": "Regalías cooperativa",
     *       "porcentaje": 3.0,
     *       "tipoDeduccion": "porcentaje"
     *     },
     *     {
     *       "concepto": "Caja de salud",
     *       "porcentaje": 1.5,
     *       "tipoDeduccion": "porcentaje"
     *     },
     *     {
     *       "concepto": "Transporte",
     *       "monto": 500.00,
     *       "tipoDeduccion": "monto_fijo"
     *     }
     *   ],
     *   "observaciones": "Venta directa a comercializadora"
     * }
     *
     * Body ejemplo para venta de concentrado:
     * {
     *   "socioId": 1,
     *   "tipoLiquidacion": "venta_concentrado",
     *   "fechaLiquidacion": "2025-01-20",
     *   "moneda": "USD",
     *   "pesoLiquidado": 3200.75,
     *   "concentradoId": 2,
     *   "reporteQuimicoId": 8,
     *   "cotizaciones": [...],
     *   "deducciones": [...]
     * }
     *
     * Body ejemplo para cobro de ingenio:
     * {
     *   "socioId": 1,
     *   "tipoLiquidacion": "cobro_ingenio",
     *   "fechaLiquidacion": "2025-01-20",
     *   "moneda": "BOB",
     *   "pesoLiquidado": 3200.75,
     *   "concentradoId": 2,
     *   "deducciones": [
     *     {
     *       "concepto": "Procesamiento en planta",
     *       "monto": 8000.00,
     *       "tipoDeduccion": "monto_fijo"
     *     }
     *   ]
     * }
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> crearLiquidacion(
            @RequestBody LiquidacionCreateDto dto,
            @RequestHeader("Authorization") String token,
            HttpServletRequest request
    ) {
        Map<String, Object> response = new HashMap<>();

        try {
            Integer usuarioId = extractUsuarioId(token);

            // Extraer contexto HTTP
            String ipOrigen = HttpUtils.obtenerIpCliente(request);

            LiquidacionResponseDto liquidacion = liquidacionBl.crearLiquidacion(
                    dto,
                    usuarioId,
                    ipOrigen
            );

            response.put("success", true);
            response.put("message", "Liquidación creada exitosamente en estado borrador");
            response.put("data", liquidacion);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (IllegalArgumentException e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error interno del servidor: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * Obtener liquidaciones con filtros opcionales
     * GET /liquidaciones?socioId=1&tipo=venta_directa&estado=pagado&fechaInicio=2025-01-01&fechaFin=2025-12-31
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getLiquidaciones(
            @RequestParam(required = false) Integer socioId,
            @RequestParam(required = false) String tipo,
            @RequestParam(required = false) String estado,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFin,
            @RequestHeader("Authorization") String token
    ) {
        Map<String, Object> response = new HashMap<>();

        try {
            List<LiquidacionResponseDto> liquidaciones = liquidacionBl.getLiquidaciones(
                    socioId,
                    tipo,
                    estado,
                    fechaInicio,
                    fechaFin
            );

            response.put("success", true);
            response.put("data", liquidaciones);
            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error interno del servidor: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * Obtener detalle completo de una liquidación
     * GET /liquidaciones/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getDetalleLiquidacion(
            @PathVariable Integer id,
            @RequestHeader("Authorization") String token
    ) {
        Map<String, Object> response = new HashMap<>();

        try {
            LiquidacionDetalleDto liquidacion = liquidacionBl.getDetalleLiquidacion(id);

            response.put("success", true);
            response.put("data", liquidacion);
            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error interno del servidor: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * Actualizar liquidación (solo si está en borrador)
     * PUT /liquidaciones/{id}
     *
     * Body: Mismo formato que POST
     */
    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> actualizarLiquidacion(
            @PathVariable Integer id,
            @RequestBody LiquidacionCreateDto dto,
            @RequestHeader("Authorization") String token,
            HttpServletRequest request
    ) {
        Map<String, Object> response = new HashMap<>();

        try {
            Integer usuarioId = extractUsuarioId(token);

            // Extraer contexto HTTP
            String ipOrigen = HttpUtils.obtenerIpCliente(request);

            LiquidacionResponseDto liquidacion = liquidacionBl.actualizarLiquidacion(
                    id,
                    dto,
                    usuarioId,
                    ipOrigen
            );

            response.put("success", true);
            response.put("message", "Liquidación actualizada exitosamente");
            response.put("data", liquidacion);
            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error interno del servidor: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * Finalizar liquidación (cambiar de borrador a pendiente_pago)
     * PUT /liquidaciones/{id}/finalizar
     *
     * No requiere body
     */
    @PutMapping("/{id}/finalizar")
    public ResponseEntity<Map<String, Object>> finalizarLiquidacion(
            @PathVariable Integer id,
            @RequestHeader("Authorization") String token,
            HttpServletRequest request
    ) {
        Map<String, Object> response = new HashMap<>();

        try {
            Integer usuarioId = extractUsuarioId(token);

            // Extraer contexto HTTP
            String ipOrigen = HttpUtils.obtenerIpCliente(request);

            LiquidacionResponseDto liquidacion = liquidacionBl.finalizarLiquidacion(
                    id,
                    usuarioId,
                    ipOrigen
            );

            response.put("success", true);
            response.put("message", "Liquidación finalizada. Ahora está pendiente de pago");
            response.put("data", liquidacion);
            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error interno del servidor: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * Marcar liquidación como pagada
     * PUT /liquidaciones/{id}/pagar
     *
     * No requiere body
     */
    @PutMapping("/{id}/pagar")
    public ResponseEntity<Map<String, Object>> marcarComoPagada(
            @PathVariable Integer id,
            @RequestHeader("Authorization") String token,
            HttpServletRequest request
    ) {
        Map<String, Object> response = new HashMap<>();

        try {
            Integer usuarioId = extractUsuarioId(token);

            // Extraer contexto HTTP
            String ipOrigen = HttpUtils.obtenerIpCliente(request);

            LiquidacionResponseDto liquidacion = liquidacionBl.marcarComoPagada(
                    id,
                    usuarioId,
                    ipOrigen
            );

            response.put("success", true);
            response.put("message", "Liquidación marcada como pagada exitosamente");
            response.put("data", liquidacion);
            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error interno del servidor: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * Obtener estadísticas de liquidaciones
     * GET /liquidaciones/estadisticas?socioId=1
     */
    @GetMapping("/estadisticas")
    public ResponseEntity<Map<String, Object>> getEstadisticas(
            @RequestParam(required = false) Integer socioId,
            @RequestHeader("Authorization") String token
    ) {
        Map<String, Object> response = new HashMap<>();

        try {
            Map<String, Object> estadisticas = liquidacionBl.getEstadisticas(socioId);

            response.put("success", true);
            response.put("data", estadisticas);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error interno del servidor: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * Obtener liquidaciones de un socio
     * GET /liquidaciones/socio/{socioId}
     */
    @GetMapping("/socio/{socioId}")
    public ResponseEntity<Map<String, Object>> getLiquidacionesPorSocio(
            @PathVariable Integer socioId,
            @RequestHeader("Authorization") String token
    ) {
        Map<String, Object> response = new HashMap<>();

        try {
            List<LiquidacionResponseDto> liquidaciones = liquidacionBl.getLiquidaciones(
                    socioId,
                    null,
                    null,
                    null,
                    null
            );

            response.put("success", true);
            response.put("data", liquidaciones);
            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error interno del servidor: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * Obtener liquidaciones por tipo
     * GET /liquidaciones/tipo/{tipo}
     */
    @GetMapping("/tipo/{tipo}")
    public ResponseEntity<Map<String, Object>> getLiquidacionesPorTipo(
            @PathVariable String tipo,
            @RequestHeader("Authorization") String token
    ) {
        Map<String, Object> response = new HashMap<>();

        try {
            // Validar tipo
            if (!List.of("venta_directa", "venta_concentrado", "cobro_ingenio").contains(tipo)) {
                response.put("success", false);
                response.put("message", "Tipo de liquidación inválido");
                return ResponseEntity.badRequest().body(response);
            }

            List<LiquidacionResponseDto> liquidaciones = liquidacionBl.getLiquidaciones(
                    null,
                    tipo,
                    null,
                    null,
                    null
            );

            response.put("success", true);
            response.put("data", liquidaciones);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error interno del servidor: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * Obtener liquidaciones pendientes de pago
     * GET /liquidaciones/pendientes
     */
    @GetMapping("/pendientes")
    public ResponseEntity<Map<String, Object>> getLiquidacionesPendientes(
            @RequestHeader("Authorization") String token
    ) {
        Map<String, Object> response = new HashMap<>();

        try {
            List<LiquidacionResponseDto> liquidaciones = liquidacionBl.getLiquidaciones(
                    null,
                    null,
                    "pendiente_pago",
                    null,
                    null
            );

            response.put("success", true);
            response.put("data", liquidaciones);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error interno del servidor: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * Obtener liquidaciones en borrador
     * GET /liquidaciones/borradores
     */
    @GetMapping("/borradores")
    public ResponseEntity<Map<String, Object>> getLiquidacionesBorrador(
            @RequestHeader("Authorization") String token
    ) {
        Map<String, Object> response = new HashMap<>();

        try {
            List<LiquidacionResponseDto> liquidaciones = liquidacionBl.getLiquidaciones(
                    null,
                    null,
                    "borrador",
                    null,
                    null
            );

            response.put("success", true);
            response.put("data", liquidaciones);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error interno del servidor: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(response);
        }
    }

    // Método auxiliar para extraer el usuario del token
    private Integer extractUsuarioId(String token) {
        String cleanToken = token.replace("Bearer ", "");
        return jwtUtil.extractUsuarioId(cleanToken);
    }
}