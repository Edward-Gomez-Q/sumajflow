package ucb.edu.bo.sumajflow.controller.ingenio;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ucb.edu.bo.sumajflow.bl.ingenio.KanbanIngenioBl;
import ucb.edu.bo.sumajflow.dto.ingenio.*;
import ucb.edu.bo.sumajflow.utils.HttpUtils;
import ucb.edu.bo.sumajflow.utils.JwtUtil;

import java.util.HashMap;
import java.util.Map;

/**
 * Controlador REST para gestión del Kanban de procesos de planta
 * Endpoints para visualización y avance de procesos mediante drag & drop
 */
@RestController
@RequestMapping("/ingenio/kanban")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class KanbanIngenioController {

    private final KanbanIngenioBl kanbanIngenioBl;
    private final JwtUtil jwtUtil;

    /**
     * Obtener procesos del Kanban de un concentrado
     * GET /ingenio/kanban/concentrados/{concentradoId}/procesos
     */
    @GetMapping("/concentrados/{concentradoId}/procesos")
    public ResponseEntity<Map<String, Object>> obtenerProcesos(
            @PathVariable Integer concentradoId,
            @RequestHeader("Authorization") String token
    ) {
        Map<String, Object> response = new HashMap<>();

        try {
            Integer usuarioId = extractUsuarioId(token);
            ProcesosConcentradoResponseDto procesos = kanbanIngenioBl.obtenerProcesos(concentradoId, usuarioId);

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
     * Iniciar procesamiento (primer proceso)
     * POST /ingenio/kanban/concentrados/{concentradoId}/iniciar
     */
    @PostMapping("/concentrados/{concentradoId}/iniciar")
    public ResponseEntity<Map<String, Object>> iniciarProcesamiento(
            @PathVariable Integer concentradoId,
            @Valid @RequestBody ProcesoIniciarDto iniciarDto,
            @RequestHeader("Authorization") String token,
            HttpServletRequest request
    ) {
        Map<String, Object> response = new HashMap<>();

        try {
            Integer usuarioId = extractUsuarioId(token);
            String ipOrigen = HttpUtils.obtenerIpCliente(request);

            ProcesosConcentradoResponseDto procesos = kanbanIngenioBl.iniciarProcesamiento(
                    concentradoId, iniciarDto, usuarioId, ipOrigen
            );

            response.put("success", true);
            response.put("message", "Procesamiento iniciado exitosamente");
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
     * Mover concentrado entre procesos intermedios
     * POST /ingenio/kanban/concentrados/{concentradoId}/mover-a-proceso
     */
    @PostMapping("/concentrados/{concentradoId}/mover-a-proceso")
    public ResponseEntity<Map<String, Object>> moverAProceso(
            @PathVariable Integer concentradoId,
            @Valid @RequestBody ProcesoMoverDto moverDto,
            @RequestHeader("Authorization") String token,
            HttpServletRequest request
    ) {
        Map<String, Object> response = new HashMap<>();

        try {
            Integer usuarioId = extractUsuarioId(token);
            String ipOrigen = HttpUtils.obtenerIpCliente(request);

            ProcesosConcentradoResponseDto procesos = kanbanIngenioBl.moverAProceso(
                    concentradoId, moverDto, usuarioId, ipOrigen
            );

            response.put("success", true);
            response.put("message", "Concentrado movido exitosamente");
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
     * Finalizar procesamiento completo
     * POST /ingenio/kanban/concentrados/{concentradoId}/finalizar
     */
    @PostMapping("/concentrados/{concentradoId}/finalizar")
    public ResponseEntity<Map<String, Object>> finalizarProcesamiento(
            @PathVariable Integer concentradoId,
            @Valid @RequestBody ProcesoFinalizarDto finalizarDto,
            @RequestHeader("Authorization") String token,
            HttpServletRequest request
    ) {
        Map<String, Object> response = new HashMap<>();

        try {
            Integer usuarioId = extractUsuarioId(token);
            String ipOrigen = HttpUtils.obtenerIpCliente(request);

            ProcesosConcentradoResponseDto procesos = kanbanIngenioBl.finalizarProcesamiento(
                    concentradoId, finalizarDto, usuarioId, ipOrigen
            );

            response.put("success", true);
            response.put("message", "Procesamiento finalizado exitosamente");
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

    private Integer extractUsuarioId(String token) {
        String cleanToken = token.replace("Bearer ", "");
        return jwtUtil.extractUsuarioId(cleanToken);
    }
}