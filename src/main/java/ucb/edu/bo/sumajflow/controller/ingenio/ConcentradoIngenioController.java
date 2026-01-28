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
import java.util.Map;

/**
 * Controlador REST para gestión de Concentrados por el INGENIO
 * Endpoints solo para operaciones que corresponden al rol Ingenio
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
     * Crear un nuevo concentrado
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

            ConcentradoResponseDto concentrado = concentradoIngenioBl.crearConcentrado(
                    createDto,
                    usuarioId,
                    ipOrigen
            );

            response.put("success", true);
            response.put("message", "Concentrado creado exitosamente");
            response.put("data", concentrado);

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
     * Obtener procesos del Kanban de un concentrado
     * GET /ingenio/concentrados/{id}/procesos
     */
    @GetMapping("/{id}/procesos")
    public ResponseEntity<Map<String, Object>> obtenerProcesos(
            @PathVariable Integer id,
            @RequestHeader("Authorization") String token
    ) {
        Map<String, Object> response = new HashMap<>();

        try {
            Integer usuarioId = extractUsuarioId(token);

            ProcesosConcentradoResponseDto procesos = concentradoIngenioBl.obtenerProcesos(id, usuarioId);

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
     * Avanzar proceso del Kanban (iniciar o completar)
     * PATCH /ingenio/concentrados/{id}/procesos/{procesoId}/avanzar
     */
    @PatchMapping("/{id}/procesos/{procesoId}/avanzar")
    public ResponseEntity<Map<String, Object>> avanzarProceso(
            @PathVariable Integer id,
            @PathVariable Integer procesoId,
            @Valid @RequestBody ProcesoAvanzarDto avanzarDto,
            @RequestHeader("Authorization") String token,
            HttpServletRequest request
    ) {
        Map<String, Object> response = new HashMap<>();

        try {
            Integer usuarioId = extractUsuarioId(token);
            String ipOrigen = HttpUtils.obtenerIpCliente(request);

            ProcesosConcentradoResponseDto procesos = concentradoIngenioBl.avanzarProceso(
                    id,
                    procesoId,
                    avanzarDto,
                    usuarioId,
                    ipOrigen
            );

            response.put("success", true);
            response.put("message", "Proceso avanzado exitosamente");
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
     * Registrar reporte químico del laboratorio
     * POST /ingenio/concentrados/{id}/reporte-quimico
     */
    @PostMapping("/{id}/reporte-quimico")
    public ResponseEntity<Map<String, Object>> registrarReporteQuimico(
            @PathVariable Integer id,
            @Valid @RequestBody ReporteQuimicoCreateDto reporteDto,
            @RequestHeader("Authorization") String token,
            HttpServletRequest request
    ) {
        Map<String, Object> response = new HashMap<>();

        try {
            Integer usuarioId = extractUsuarioId(token);
            String ipOrigen = HttpUtils.obtenerIpCliente(request);

            ConcentradoResponseDto concentrado = concentradoIngenioBl.registrarReporteQuimico(
                    id,
                    reporteDto,
                    usuarioId,
                    ipOrigen
            );

            response.put("success", true);
            response.put("message", "Reporte químico registrado exitosamente");
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
     * Validar reporte químico (cambiar estado a "listo_para_liquidacion")
     * PATCH /ingenio/concentrados/{id}/validar-reporte
     */
    @PatchMapping("/{id}/validar-reporte")
    public ResponseEntity<Map<String, Object>> validarReporteQuimico(
            @PathVariable Integer id,
            @RequestHeader("Authorization") String token,
            HttpServletRequest request
    ) {
        Map<String, Object> response = new HashMap<>();

        try {
            Integer usuarioId = extractUsuarioId(token);
            String ipOrigen = HttpUtils.obtenerIpCliente(request);

            ConcentradoResponseDto concentrado = concentradoIngenioBl.validarReporteQuimico(
                    id,
                    usuarioId,
                    ipOrigen
            );

            response.put("success", true);
            response.put("message", "Reporte químico validado exitosamente");
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
     * Revisar solicitud de liquidación de servicio
     * PATCH /ingenio/concentrados/{id}/revisar-liquidacion-servicio
     */
    @PatchMapping("/{id}/revisar-liquidacion-servicio")
    public ResponseEntity<Map<String, Object>> revisarLiquidacionServicio(
            @PathVariable Integer id,
            @RequestHeader("Authorization") String token,
            HttpServletRequest request
    ) {
        Map<String, Object> response = new HashMap<>();

        try {
            Integer usuarioId = extractUsuarioId(token);
            String ipOrigen = HttpUtils.obtenerIpCliente(request);

            ConcentradoResponseDto concentrado = concentradoIngenioBl.revisarLiquidacionServicio(
                    id,
                    usuarioId,
                    ipOrigen
            );

            response.put("success", true);
            response.put("message", "Liquidación en revisión");
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
     * Aprobar liquidación de servicio (definir costo)
     * POST /ingenio/concentrados/{id}/aprobar-liquidacion-servicio
     */
    @PostMapping("/{id}/aprobar-liquidacion-servicio")
    public ResponseEntity<Map<String, Object>> aprobarLiquidacionServicio(
            @PathVariable Integer id,
            @Valid @RequestBody AprobarLiquidacionServicioDto aprobarDto,
            @RequestHeader("Authorization") String token,
            HttpServletRequest request
    ) {
        Map<String, Object> response = new HashMap<>();

        try {
            Integer usuarioId = extractUsuarioId(token);
            String ipOrigen = HttpUtils.obtenerIpCliente(request);

            LiquidacionServicioResponseDto liquidacion = concentradoIngenioBl.aprobarLiquidacionServicio(
                    id,
                    aprobarDto,
                    usuarioId,
                    ipOrigen
            );

            response.put("success", true);
            response.put("message", "Liquidación de servicio aprobada exitosamente");
            response.put("data", liquidacion);
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
     * Registrar pago del servicio recibido del socio
     * POST /ingenio/concentrados/{id}/registrar-pago-servicio
     */
    @PostMapping("/{id}/registrar-pago-servicio")
    public ResponseEntity<Map<String, Object>> registrarPagoServicio(
            @PathVariable Integer id,
            @Valid @RequestBody RegistrarPagoServicioDto pagoDto,
            @RequestHeader("Authorization") String token,
            HttpServletRequest request
    ) {
        Map<String, Object> response = new HashMap<>();

        try {
            Integer usuarioId = extractUsuarioId(token);
            String ipOrigen = HttpUtils.obtenerIpCliente(request);

            ConcentradoResponseDto concentrado = concentradoIngenioBl.registrarPagoServicio(
                    id,
                    pagoDto,
                    usuarioId,
                    ipOrigen
            );

            response.put("success", true);
            response.put("message", "Pago de servicio registrado exitosamente");
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

    private Integer extractUsuarioId(String token) {
        String cleanToken = token.replace("Bearer ", "");
        return jwtUtil.extractUsuarioId(cleanToken);
    }
}