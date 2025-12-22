package ucb.edu.bo.sumajflow.controller.reportes;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ucb.edu.bo.sumajflow.bl.reportes.ReporteQuimicoBl;
import ucb.edu.bo.sumajflow.dto.reportes.ReporteQuimicoCreateDto;
import ucb.edu.bo.sumajflow.dto.reportes.ReporteQuimicoDetalleDto;
import ucb.edu.bo.sumajflow.dto.reportes.ReporteQuimicoResponseDto;
import ucb.edu.bo.sumajflow.utils.HttpUtils;
import ucb.edu.bo.sumajflow.utils.JwtUtil;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/reportes-quimicos")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class ReporteQuimicoController {

    private final ReporteQuimicoBl reporteQuimicoBl;
    private final JwtUtil jwtUtil;

    /**
     * Crear un nuevo reporte químico
     * POST /reportes-quimicos
     *
     * Body ejemplo:
     * {
     *   "numeroReporte": "RQ-2025-001",
     *   "laboratorio": "Lab Minerales S.A.",
     *   "fechaAnalisis": "2025-01-15",
     *   "leyAg": 850.50,
     *   "leyPb": 12.35,
     *   "leyZn": 18.20,
     *   "humedad": 3.5,
     *   "tipoAnalisis": "pre_venta",
     *   "urlPdf": "https://storage.com/reportes/rq-2025-001.pdf",
     *   "loteId": 5
     * }
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> crearReporte(
            @RequestBody ReporteQuimicoCreateDto dto,
            @RequestHeader("Authorization") String token,
            HttpServletRequest request
    ) {
        Map<String, Object> response = new HashMap<>();

        try {
            Integer usuarioId = extractUsuarioId(token);

            // Extraer contexto HTTP
            String ipOrigen = HttpUtils.obtenerIpCliente(request);

            ReporteQuimicoResponseDto reporte = reporteQuimicoBl.crearReporte(dto, usuarioId, ipOrigen);

            response.put("success", true);
            response.put("message", "Reporte químico creado exitosamente");
            response.put("data", reporte);
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
     * Obtener reportes químicos con filtros opcionales
     * GET /reportes-quimicos?tipoAnalisis=pre_venta&laboratorio=Lab&fechaInicio=2025-01-01&fechaFin=2025-12-31&limit=20
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getReportes(
            @RequestParam(required = false) String tipoAnalisis,
            @RequestParam(required = false) String laboratorio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFin,
            @RequestParam(required = false) Integer limit,
            @RequestHeader("Authorization") String token
    ) {
        Map<String, Object> response = new HashMap<>();

        try {
            List<ReporteQuimicoResponseDto> reportes = reporteQuimicoBl.getReportes(
                    tipoAnalisis,
                    laboratorio,
                    fechaInicio,
                    fechaFin,
                    limit
            );

            response.put("success", true);
            response.put("data", reportes);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error interno del servidor: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * Obtener detalle completo de un reporte
     * GET /reportes-quimicos/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getDetalleReporte(
            @PathVariable Integer id,
            @RequestHeader("Authorization") String token
    ) {
        Map<String, Object> response = new HashMap<>();

        try {
            ReporteQuimicoDetalleDto reporte = reporteQuimicoBl.getDetalleReporte(id);

            response.put("success", true);
            response.put("data", reporte);
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
     * Buscar reporte por número
     * GET /reportes-quimicos/buscar?numero=RQ-2025-001
     */
    @GetMapping("/buscar")
    public ResponseEntity<Map<String, Object>> buscarPorNumero(
            @RequestParam String numero,
            @RequestHeader("Authorization") String token
    ) {
        Map<String, Object> response = new HashMap<>();

        try {
            ReporteQuimicoResponseDto reporte = reporteQuimicoBl.getReportePorNumero(numero);

            response.put("success", true);
            response.put("data", reporte);
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
     * Actualizar reporte químico
     * PUT /reportes-quimicos/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> actualizarReporte(
            @PathVariable Integer id,
            @RequestBody ReporteQuimicoCreateDto dto,
            @RequestHeader("Authorization") String token,
            HttpServletRequest request
    ) {
        Map<String, Object> response = new HashMap<>();

        try {
            Integer usuarioId = extractUsuarioId(token);

            // Extraer contexto HTTP
            String ipOrigen = HttpUtils.obtenerIpCliente(request);

            ReporteQuimicoResponseDto reporte = reporteQuimicoBl.actualizarReporte(
                    id,
                    dto,
                    usuarioId,
                    ipOrigen
            );

            response.put("success", true);
            response.put("message", "Reporte químico actualizado exitosamente");
            response.put("data", reporte);
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
     * Eliminar reporte químico
     * DELETE /reportes-quimicos/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> eliminarReporte(
            @PathVariable Integer id,
            @RequestHeader("Authorization") String token,
            HttpServletRequest request
    ) {
        Map<String, Object> response = new HashMap<>();

        try {
            Integer usuarioId = extractUsuarioId(token);

            // Extraer contexto HTTP
            String ipOrigen = HttpUtils.obtenerIpCliente(request);

            reporteQuimicoBl.eliminarReporte(id, usuarioId, ipOrigen);

            response.put("success", true);
            response.put("message", "Reporte químico eliminado exitosamente");
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
     * Obtener estadísticas de reportes químicos
     * GET /reportes-quimicos/estadisticas
     */
    @GetMapping("/estadisticas")
    public ResponseEntity<Map<String, Object>> getEstadisticas(
            @RequestHeader("Authorization") String token
    ) {
        Map<String, Object> response = new HashMap<>();

        try {
            Map<String, Object> estadisticas = reporteQuimicoBl.getEstadisticas();

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

    // Método auxiliar para extraer el usuario del token
    private Integer extractUsuarioId(String token) {
        String cleanToken = token.replace("Bearer ", "");
        return jwtUtil.extractUsuarioId(cleanToken);
    }
}