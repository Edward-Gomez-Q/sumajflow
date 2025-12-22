package ucb.edu.bo.sumajflow.controller.ingenio;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ucb.edu.bo.sumajflow.bl.ingenio.ConcentradoBl;
import ucb.edu.bo.sumajflow.bl.ingenio.ProcesoPlantaBl;
import ucb.edu.bo.sumajflow.dto.ingenio.*;
import ucb.edu.bo.sumajflow.utils.HttpUtils;
import ucb.edu.bo.sumajflow.utils.JwtUtil;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/ingenio/concentrados")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class ConcentradoController {

    private final ConcentradoBl concentradoBl;
    private final ProcesoPlantaBl procesoPlantaBl;
    private final JwtUtil jwtUtil;

    /**
     * Obtener lotes disponibles para crear concentrado
     * GET /ingenio/concentrados/lotes-disponibles
     */
    @GetMapping("/lotes-disponibles")
    public ResponseEntity<Map<String, Object>> getLotesDisponibles(
            @RequestHeader("Authorization") String token
    ) {
        Map<String, Object> response = new HashMap<>();

        try {
            Integer usuarioId = extractUsuarioId(token);
            List<LoteDisponibleConcentradoDto> lotes = concentradoBl.getLotesDisponibles(usuarioId);

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
     * Crear un nuevo concentrado
     * POST /ingenio/concentrados
     *
     * Body ejemplo:
     * {
     *   "codigoConcentrado": "CONC-2025-001",
     *   "lotes": [
     *     { "loteId": 1, "pesoEntrada": 1500.50 },
     *     { "loteId": 2, "pesoEntrada": 2000.00 }
     *   ],
     *   "mineralPrincipal": "Plata",
     *   "observaciones": "Concentrado de alta ley"
     * }
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> crearConcentrado(
            @RequestBody ConcentradoCreateDto dto,
            @RequestHeader("Authorization") String token,
            HttpServletRequest request
    ) {
        Map<String, Object> response = new HashMap<>();

        try {
            Integer usuarioId = extractUsuarioId(token);

            // Extraer contexto HTTP
            String ipOrigen = HttpUtils.obtenerIpCliente(request);

            ConcentradoResponseDto concentrado = concentradoBl.crearConcentrado(dto, usuarioId, ipOrigen);

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
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * Obtener concentrados del ingenio
     * GET /ingenio/concentrados?estado=creado
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getConcentrados(
            @RequestParam(required = false) String estado,
            @RequestHeader("Authorization") String token
    ) {
        Map<String, Object> response = new HashMap<>();

        try {
            Integer usuarioId = extractUsuarioId(token);
            List<ConcentradoResponseDto> concentrados = concentradoBl.getConcentrados(usuarioId, estado);

            response.put("success", true);
            response.put("data", concentrados);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error interno del servidor: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * Obtener detalle completo de un concentrado
     * GET /ingenio/concentrados/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getDetalleConcentrado(
            @PathVariable Integer id,
            @RequestHeader("Authorization") String token
    ) {
        Map<String, Object> response = new HashMap<>();

        try {
            Integer usuarioId = extractUsuarioId(token);
            ConcentradoDetalleDto concentrado = concentradoBl.getDetalleConcentrado(id, usuarioId);

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
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * Obtener procesos de un concentrado (Kanban)
     * GET /ingenio/concentrados/{id}/procesos
     */
    @GetMapping("/{id}/procesos")
    public ResponseEntity<Map<String, Object>> getProcesosByConcentrado(
            @PathVariable Integer id,
            @RequestHeader("Authorization") String token
    ) {
        Map<String, Object> response = new HashMap<>();

        try {
            Integer usuarioId = extractUsuarioId(token);
            List<ProcesoPlantaDto> procesos = procesoPlantaBl.getProcesosByConcentrado(id, usuarioId);

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
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * Actualizar estado de un proceso (Kanban)
     * PUT /ingenio/concentrados/procesos/{procesoId}
     *
     * Body ejemplo:
     * {
     *   "nuevoEstado": "en_proceso",
     *   "observaciones": "Proceso iniciado"
     * }
     */
    @PutMapping("/procesos/{procesoId}")
    public ResponseEntity<Map<String, Object>> actualizarEstadoProceso(
            @PathVariable Integer procesoId,
            @RequestBody ActualizarProcesoDto dto,
            @RequestHeader("Authorization") String token,
            HttpServletRequest request
    ) {
        Map<String, Object> response = new HashMap<>();

        try {
            Integer usuarioId = extractUsuarioId(token);

            // Extraer contexto HTTP
            String ipOrigen = HttpUtils.obtenerIpCliente(request);

            ProcesoPlantaDto proceso = procesoPlantaBl.actualizarEstadoProceso(
                    procesoId,
                    dto,
                    usuarioId,
                    ipOrigen
            );

            response.put("success", true);
            response.put("message", "Proceso actualizado exitosamente");
            response.put("data", proceso);
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
     * Finalizar concentrado y registrar peso final
     * PUT /ingenio/concentrados/{id}/finalizar
     *
     * Body ejemplo:
     * {
     *   "pesoFinal": 3200.75
     * }
     */
    @PutMapping("/{id}/finalizar")
    public ResponseEntity<Map<String, Object>> finalizarConcentrado(
            @PathVariable Integer id,
            @RequestBody Map<String, BigDecimal> body,
            @RequestHeader("Authorization") String token,
            HttpServletRequest request
    ) {
        Map<String, Object> response = new HashMap<>();

        try {
            Integer usuarioId = extractUsuarioId(token);

            // Extraer contexto HTTP
            String ipOrigen = HttpUtils.obtenerIpCliente(request);

            BigDecimal pesoFinal = body.get("pesoFinal");
            procesoPlantaBl.finalizarConcentrado(id, pesoFinal, usuarioId, ipOrigen);

            response.put("success", true);
            response.put("message", "Concentrado finalizado exitosamente");
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

    // Método auxiliar para extraer el usuario del token
    private Integer extractUsuarioId(String token) {
        String cleanToken = token.replace("Bearer ", "");
        return jwtUtil.extractUsuarioId(cleanToken);
    }
}