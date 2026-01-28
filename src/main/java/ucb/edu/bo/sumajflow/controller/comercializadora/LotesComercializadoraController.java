package ucb.edu.bo.sumajflow.controller.comercializadora;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ucb.edu.bo.sumajflow.bl.comercializadora.LotesComercializadoraBl;
import ucb.edu.bo.sumajflow.dto.comercializadora.*;
import ucb.edu.bo.sumajflow.dto.socio.LoteDetalleDto;
import ucb.edu.bo.sumajflow.utils.HttpUtils;
import ucb.edu.bo.sumajflow.utils.JwtUtil;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/comercializadora/lotes")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class LotesComercializadoraController {

    private final LotesComercializadoraBl lotesComercializadoraBl;
    private final JwtUtil jwtUtil;

    /**
     * Obtener lotes con paginación y filtros
     * GET /comercializadora/lotes
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getLotes(
            @RequestParam(required = false) String estado,
            @RequestParam(required = false) String tipoMineral,
            @RequestParam(required = false) String cooperativaNombre,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fechaDesde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fechaHasta,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(defaultValue = "fechaCreacion") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            @RequestHeader("Authorization") String token
    ) {
        Map<String, Object> response = new HashMap<>();

        try {
            Integer usuarioId = extractUsuarioId(token);

            LoteFiltrosComercializadoraDto filtros = new LoteFiltrosComercializadoraDto();
            filtros.setEstado(estado);
            filtros.setTipoMineral(tipoMineral);
            filtros.setCooperativaNombre(cooperativaNombre);
            filtros.setFechaDesde(fechaDesde);
            filtros.setFechaHasta(fechaHasta);
            filtros.setPage(page);
            filtros.setSize(size);
            filtros.setSortBy(sortBy);
            filtros.setSortDir(sortDir);

            LotesComercializadoraPaginadosDto lotes = lotesComercializadoraBl.getLotesComercializadoraPaginados(usuarioId, filtros);

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
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * Obtener detalle completo de un lote - AHORA RETORNA LoteDetalleDto
     * GET /comercializadora/lotes/{id}/detalle
     */
    @GetMapping("/{id}/detalle")
    public ResponseEntity<Map<String, Object>> getDetalleLote(
            @PathVariable Integer id,
            @RequestHeader("Authorization") String token
    ) {
        Map<String, Object> response = new HashMap<>();

        try {
            Integer usuarioId = extractUsuarioId(token);
            LoteDetalleDto lote = lotesComercializadoraBl.getLoteDetalleCompleto(id, usuarioId);

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
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * Aprobar lote desde la comercializadora - AHORA RETORNA LoteDetalleDto
     * PUT /comercializadora/lotes/{id}/aprobar
     */
    @PutMapping("/{id}/aprobar")
    public ResponseEntity<Map<String, Object>> aprobarLote(
            @PathVariable Integer id,
            @RequestBody LoteAprobacionDestinoDto aprobacionDto,
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

            LoteDetalleDto lote = lotesComercializadoraBl.aprobarLote(
                    id,
                    aprobacionDto,
                    usuarioId,
                    ipOrigen,
                    metodoHttp,
                    endpoint
            );

            response.put("success", true);
            response.put("message", "Lote aprobado exitosamente");
            response.put("data", lote);
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
     * Rechazar lote desde la comercializadora
     * PUT /comercializadora/lotes/{id}/rechazar
     */
    @PutMapping("/{id}/rechazar")
    public ResponseEntity<Map<String, Object>> rechazarLote(
            @PathVariable Integer id,
            @RequestBody LoteRechazoDestinoDto rechazoDto,
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

            lotesComercializadoraBl.rechazarLote(
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
            return ResponseEntity.internalServerError().body(response);
        }
    }
    private Integer extractUsuarioId(String token) {
        String cleanToken = token.replace("Bearer ", "");
        return jwtUtil.extractUsuarioId(cleanToken);
    }
}