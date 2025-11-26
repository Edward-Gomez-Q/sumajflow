package ucb.edu.bo.sumajflow.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import ucb.edu.bo.sumajflow.bl.CooperativaBl;
import ucb.edu.bo.sumajflow.dto.socio.SocioAprobacionDto;
import ucb.edu.bo.sumajflow.dto.socio.SocioResponseDto;
import ucb.edu.bo.sumajflow.dto.socio.SociosPaginadosDto;

import java.util.HashMap;
import java.util.Map;

/**
 * Controlador para operaciones de cooperativa
 */
@RestController
@RequestMapping("/cooperativa")
@CrossOrigin(origins = "*")
public class CooperativaController {

    private final CooperativaBl cooperativaBl;

    public CooperativaController(CooperativaBl cooperativaBl) {
        this.cooperativaBl = cooperativaBl;
    }

    /**
     * Endpoint para listar todos los socios de la cooperativa con paginación y filtros
     * GET /cooperativa/socios
     *
     * Parámetros query:
     * - estado: "aprobado", "pendiente", "rechazado" (opcional)
     * - busqueda: búsqueda por nombre, apellido o CI (opcional)
     * - ordenarPor: "nombre", "fechaAfiliacion", "fechaEnvio" (default: "fechaAfiliacion")
     * - direccion: "asc", "desc" (default: "desc")
     * - pagina: número de página (default: 0)
     * - tamanoPagina: elementos por página (default: 10)
     */
    @GetMapping("/socios")
    public ResponseEntity<Map<String, Object>> listarSocios(
            Authentication authentication,
            @RequestParam(required = false) String estado,
            @RequestParam(required = false) String busqueda,
            @RequestParam(defaultValue = "fechaAfiliacion") String ordenarPor,
            @RequestParam(defaultValue = "desc") String direccion,
            @RequestParam(defaultValue = "0") Integer pagina,
            @RequestParam(defaultValue = "10") Integer tamanoPagina
    ) {
        Map<String, Object> response = new HashMap<>();

        try {
            // Obtener ID del usuario autenticado
            Integer usuarioId = (Integer) authentication.getDetails();

            // Validar parámetros
            if (tamanoPagina < 1 || tamanoPagina > 100) {
                response.put("success", false);
                response.put("message", "El tamaño de página debe estar entre 1 y 100");
                return ResponseEntity.badRequest().body(response);
            }

            if (pagina < 0) {
                response.put("success", false);
                response.put("message", "El número de página debe ser mayor o igual a 0");
                return ResponseEntity.badRequest().body(response);
            }

            // Obtener lista de socios
            SociosPaginadosDto socios = cooperativaBl.listarSocios(
                    usuarioId,
                    estado,
                    busqueda,
                    ordenarPor,
                    direccion,
                    pagina,
                    tamanoPagina
            );

            response.put("success", true);
            response.put("data", socios);

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error interno del servidor: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Endpoint para obtener el detalle completo de un socio
     * GET /cooperativa/socios/{cooperativaSocioId}
     */
    @GetMapping("/socios/{cooperativaSocioId}")
    public ResponseEntity<Map<String, Object>> obtenerDetalleSocio(
            Authentication authentication,
            @PathVariable Integer cooperativaSocioId
    ) {
        Map<String, Object> response = new HashMap<>();

        try {
            Integer usuarioId = (Integer) authentication.getDetails();

            SocioResponseDto socio = cooperativaBl.obtenerDetalleSocio(usuarioId, cooperativaSocioId);

            response.put("success", true);
            response.put("data", socio);

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);

        } catch (SecurityException e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error interno del servidor: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Endpoint para aprobar o rechazar una solicitud de socio
     * PUT /cooperativa/socios/procesar
     */
    @PutMapping("/socios/procesar")
    public ResponseEntity<Map<String, Object>> procesarSolicitud(
            Authentication authentication,
            @RequestBody SocioAprobacionDto aprobacionDto
    ) {
        Map<String, Object> response = new HashMap<>();

        try {
            // Validar datos
            if (aprobacionDto.getCooperativaSocioId() == null) {
                response.put("success", false);
                response.put("message", "El ID de la solicitud es requerido");
                return ResponseEntity.badRequest().body(response);
            }

            if (aprobacionDto.getEstado() == null || aprobacionDto.getEstado().isEmpty()) {
                response.put("success", false);
                response.put("message", "El estado es requerido");
                return ResponseEntity.badRequest().body(response);
            }

            Integer usuarioId = (Integer) authentication.getDetails();

            // Procesar solicitud
            cooperativaBl.procesarSolicitud(usuarioId, aprobacionDto);

            response.put("success", true);
            response.put("message", "Solicitud procesada exitosamente");

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);

        } catch (SecurityException e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error interno del servidor: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Endpoint para obtener estadísticas rápidas de socios
     * GET /cooperativa/socios/estadisticas
     */
    @GetMapping("/socios/estadisticas")
    public ResponseEntity<Map<String, Object>> obtenerEstadisticas(Authentication authentication) {
        Map<String, Object> response = new HashMap<>();

        try {
            Integer usuarioId = (Integer) authentication.getDetails();

            // Obtener datos con paginación de 1 elemento solo para las estadísticas
            SociosPaginadosDto datos = cooperativaBl.listarSocios(
                    usuarioId, null, null, "fechaAfiliacion", "desc", 0, 1
            );

            Map<String, Object> estadisticas = new HashMap<>();
            estadisticas.put("totalAprobados", datos.getTotalAprobados());
            estadisticas.put("totalPendientes", datos.getTotalPendientes());
            estadisticas.put("totalRechazados", datos.getTotalRechazados());
            estadisticas.put("totalSocios", datos.getTotalElementos());

            response.put("success", true);
            response.put("data", estadisticas);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error interno del servidor: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Endpoint de prueba
     * GET /cooperativa/health
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "OK");
        response.put("service", "Cooperativa Service");
        return ResponseEntity.ok(response);
    }
}