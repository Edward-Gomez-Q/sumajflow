package ucb.edu.bo.sumajflow.controller.cooperativa;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ucb.edu.bo.sumajflow.bl.cooperativa.LotesCooperativaBl;
import ucb.edu.bo.sumajflow.dto.cooperativa.*;
import ucb.edu.bo.sumajflow.dto.socio.LoteDetalleDto;
import ucb.edu.bo.sumajflow.utils.HttpUtils;
import ucb.edu.bo.sumajflow.utils.JwtUtil;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/cooperativa/lotes")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class LotesCooperativaController {

    private final LotesCooperativaBl lotesCooperativaBl;
    private final JwtUtil jwtUtil;

    /**
     * Obtener lotes pendientes de aprobación por cooperativa
     * GET /cooperativa/lotes/pendientes
     */
    @GetMapping("/pendientes")
    public ResponseEntity<Map<String, Object>> getLotesPendientes(
            @RequestHeader("Authorization") String token
    ) {
        Map<String, Object> response = new HashMap<>();

        try {
            Integer usuarioId = extractUsuarioId(token);
            List<LotePendienteDto> lotes = lotesCooperativaBl.getLotesPendientesCooperativa(usuarioId);

            response.put("success", true);
            response.put("data", lotes);
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
     * Obtener detalle completo de un lote - AHORA RETORNA LoteDetalleDto
     * GET /cooperativa/lotes/pendientes/{id}/detalle
     */
    @GetMapping("/pendientes/{id}/detalle")
    public ResponseEntity<Map<String, Object>> getDetalleLote(
            @PathVariable Integer id,
            @RequestHeader("Authorization") String token
    ) {
        Map<String, Object> response = new HashMap<>();

        try {
            Integer usuarioId = extractUsuarioId(token);
            LoteDetalleDto lote = lotesCooperativaBl.getDetalleLote(id, usuarioId);

            response.put("success", true);
            response.put("data", lote);
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
     * Obtener transportistas disponibles
     * GET /cooperativa/lotes/transportistas-disponibles
     */
    @GetMapping("/transportistas-disponibles")
    public ResponseEntity<Map<String, Object>> getTransportistasDisponibles(
            @RequestHeader("Authorization") String token
    ) {
        Map<String, Object> response = new HashMap<>();

        try {
            Integer usuarioId = extractUsuarioId(token);
            List<TransportistaDisponibleDto> transportistas =
                    lotesCooperativaBl.getTransportistasDisponibles(usuarioId);

            response.put("success", true);
            response.put("data", transportistas);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error interno del servidor: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * Aprobar lote y asignar transportistas - AHORA RETORNA LoteDetalleDto
     * PUT /cooperativa/lotes/{id}/aprobar
     */
    @PutMapping("/{id}/aprobar")
    public ResponseEntity<Map<String, Object>> aprobarLote(
            @PathVariable Integer id,
            @RequestBody LoteAprobacionDto aprobacionDto,
            @RequestHeader("Authorization") String token,
            HttpServletRequest request
    ) {
        Map<String, Object> response = new HashMap<>();

        try {
            Integer usuarioId = extractUsuarioId(token);

            // Extraer contexto HTTP
            String ipOrigen = HttpUtils.obtenerIpCliente(request);
            String metodoHttp = request.getMethod();
            String endpoint = request.getRequestURI();

            LoteDetalleDto lote = lotesCooperativaBl.aprobarLote(
                    id,
                    aprobacionDto,
                    usuarioId,
                    ipOrigen,
                    metodoHttp,
                    endpoint
            );

            response.put("success", true);
            response.put("message", "Lote aprobado y transportistas asignados exitosamente");
            response.put("data", lote);
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
     * Rechazar lote
     * PUT /cooperativa/lotes/{id}/rechazar
     */
    @PutMapping("/{id}/rechazar")
    public ResponseEntity<Map<String, Object>> rechazarLote(
            @PathVariable Integer id,
            @RequestBody LoteRechazoDto rechazoDto,
            @RequestHeader("Authorization") String token,
            HttpServletRequest request
    ) {
        Map<String, Object> response = new HashMap<>();

        try {
            Integer usuarioId = extractUsuarioId(token);

            // Extraer contexto HTTP
            String ipOrigen = HttpUtils.obtenerIpCliente(request);
            String metodoHttp = request.getMethod();
            String endpoint = request.getRequestURI();

            lotesCooperativaBl.rechazarLote(
                    id,
                    rechazoDto,
                    usuarioId,
                    ipOrigen,
                    metodoHttp,
                    endpoint
            );

            response.put("success", true);
            response.put("message", "Lote rechazado exitosamente");
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
     * Obtener todos los lotes de la cooperativa con filtros y paginación
     * GET /cooperativa/lotes
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getLotesCooperativa(
            @RequestParam(required = false) String estado,
            @RequestParam(required = false) String tipoOperacion,
            @RequestParam(required = false) String tipoMineral,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fechaDesde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fechaHasta,
            @RequestParam(required = false) Integer socioId,
            @RequestParam(required = false) Integer minaId,
            @RequestParam(required = false) Integer sectorId,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(defaultValue = "fechaCreacion") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            @RequestHeader("Authorization") String token
    ) {
        Map<String, Object> response = new HashMap<>();

        try {
            Integer usuarioId = extractUsuarioId(token);

            // Construir filtros
            LoteFiltrosCooperativaDto filtros = new LoteFiltrosCooperativaDto();
            filtros.setEstado(estado);
            filtros.setTipoOperacion(tipoOperacion);
            filtros.setTipoMineral(tipoMineral);
            filtros.setFechaDesde(fechaDesde);
            filtros.setFechaHasta(fechaHasta);
            filtros.setSocioId(socioId);
            filtros.setMinaId(minaId);
            filtros.setSectorId(sectorId);
            filtros.setPage(page);
            filtros.setSize(size);
            filtros.setSortBy(sortBy);
            filtros.setSortDir(sortDir);

            // Obtener lotes paginados
            LotesCooperativaPaginadosDto lotes = lotesCooperativaBl.getLotesCooperativaPaginados(usuarioId, filtros);

            response.put("success", true);
            response.put("data", lotes);
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
     * Obtener detalle completo de un lote (cualquier estado) - AHORA RETORNA LoteDetalleDto
     * GET /cooperativa/lotes/{id}/detalle
     */
    @GetMapping("/{id}/detalle")
    public ResponseEntity<Map<String, Object>> getLoteDetalleCompleto(
            @PathVariable Integer id,
            @RequestHeader("Authorization") String token
    ) {
        Map<String, Object> response = new HashMap<>();

        try {
            Integer usuarioId = extractUsuarioId(token);
            LoteDetalleDto lote = lotesCooperativaBl.getLoteDetalleCompleto(id, usuarioId);

            response.put("success", true);
            response.put("data", lote);
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

    // Metodo auxiliar para extraer el usuario del token
    private Integer extractUsuarioId(String token) {
        String cleanToken = token.replace("Bearer ", "");
        return jwtUtil.extractUsuarioId(cleanToken);
    }
}