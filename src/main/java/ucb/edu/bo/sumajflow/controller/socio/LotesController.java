// src/main/java/ucb/edu/bo/sumajflow/controller/socio/LotesController.java
package ucb.edu.bo.sumajflow.controller.socio;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ucb.edu.bo.sumajflow.bl.socio.LotesBl;
import ucb.edu.bo.sumajflow.dto.socio.LoteCreateDto;
import ucb.edu.bo.sumajflow.dto.socio.LoteDetalleDto;
import ucb.edu.bo.sumajflow.dto.socio.LoteFiltrosDto;
import ucb.edu.bo.sumajflow.dto.socio.LoteResponseDto;
import ucb.edu.bo.sumajflow.dto.socio.LotesPaginadosDto;
import ucb.edu.bo.sumajflow.utils.HttpUtils;
import ucb.edu.bo.sumajflow.utils.JwtUtil;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/socio/lotes")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class LotesController {

    private final LotesBl lotesBl;
    private final JwtUtil jwtUtil;

    @GetMapping
    public ResponseEntity<Map<String, Object>> getLotes(
            @RequestParam(required = false) String estado,
            @RequestParam(required = false) String tipoOperacion,
            @RequestParam(required = false) String tipoMineral,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fechaDesde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fechaHasta,
            @RequestParam(required = false) Integer minaId,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(defaultValue = "fechaCreacion") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            @RequestHeader("Authorization") String token
    ) {
        Map<String, Object> response = new HashMap<>();

        try {
            Integer usuarioId = extractUsuarioId(token);

            LoteFiltrosDto filtros = new LoteFiltrosDto();
            filtros.setEstado(estado);
            filtros.setTipoOperacion(tipoOperacion);
            filtros.setTipoMineral(tipoMineral);
            filtros.setFechaDesde(fechaDesde);
            filtros.setFechaHasta(fechaHasta);
            filtros.setMinaId(minaId);
            filtros.setPage(page);
            filtros.setSize(size);
            filtros.setSortBy(sortBy);
            filtros.setSortDir(sortDir);

            LotesPaginadosDto lotes = lotesBl.getLotesPaginados(usuarioId, filtros);

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
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @GetMapping("/{id}/detalle")
    public ResponseEntity<Map<String, Object>> getLoteDetalle(
            @PathVariable Integer id,
            @RequestHeader("Authorization") String token
    ) {
        Map<String, Object> response = new HashMap<>();

        try {
            Integer usuarioId = extractUsuarioId(token);
            LoteDetalleDto lote = lotesBl.getLoteDetalleCompleto(id, usuarioId);

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
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> createLote(
            @RequestBody LoteCreateDto dto,
            @RequestHeader("Authorization") String token,
            HttpServletRequest request
    ) {
        Map<String, Object> response = new HashMap<>();

        try {
            Integer usuarioId = extractUsuarioId(token);

            String ipOrigen = HttpUtils.obtenerIpCliente(request);
            String metodoHttp = request.getMethod();
            String endpoint = request.getRequestURI();

            log.info("Creando lote - UsuarioID: {}, IP: {}, Método: {}, Endpoint: {}",
                    usuarioId, ipOrigen, metodoHttp, endpoint);

            LoteResponseDto lote = lotesBl.createLote(
                    dto,
                    usuarioId,
                    ipOrigen,
                    metodoHttp,
                    endpoint
            );

            response.put("success", true);
            response.put("message", "Lote creado exitosamente y enviado para aprobación");
            response.put("data", lote);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);

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

    private Integer extractUsuarioId(String token) {
        String cleanToken = token.replace("Bearer ", "");
        return jwtUtil.extractUsuarioId(cleanToken);
    }
}