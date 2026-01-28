package ucb.edu.bo.sumajflow.controller.socio;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ucb.edu.bo.sumajflow.bl.cooperativa.SectoresBl;
import ucb.edu.bo.sumajflow.dto.cooperativa.SectorResponseDto;
import ucb.edu.bo.sumajflow.utils.JwtUtil;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Controller REST para que los socios puedan visualizar los sectores de su cooperativa
 * NOTA: Los socios solo pueden LEER sectores, no crear/modificar/eliminar
 */
@RestController
@RequestMapping("/socio/sectores")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class SectoresSocioController {

    private final SectoresBl sectoresBl;
    private final JwtUtil jwtUtil;

    /**
     * Obtener todos los sectores ACTIVOS de la cooperativa del socio autenticado
     * GET /socio/sectores
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getSectoresDeMiCooperativa(
            @RequestHeader("Authorization") String token
    ) {
        Map<String, Object> response = new HashMap<>();

        try {
            Integer usuarioId = extractUsuarioId(token);

            List<SectorResponseDto> sectores = sectoresBl.getSectoresByCooperativaParaSocio(usuarioId);

            response.put("success", true);
            response.put("data", sectores);
            response.put("message", "Sectores de tu cooperativa obtenidos exitosamente");
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
     * Obtener un sector específico por ID
     * GET /socio/sectores/{id}
     *
     * El socio solo puede ver sectores activos de su cooperativa
     */
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getSector(
            @PathVariable Integer id,
            @RequestHeader("Authorization") String token
    ) {
        Map<String, Object> response = new HashMap<>();

        try {
            Integer usuarioId = extractUsuarioId(token);

            SectorResponseDto sector = sectoresBl.getSectorByIdParaSocio(id, usuarioId);

            response.put("success", true);
            response.put("data", sector);
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
     * Obtener estadísticas de sectores de la cooperativa del socio
     * GET /socio/sectores/estadisticas
     */
    @GetMapping("/estadisticas")
    public ResponseEntity<Map<String, Object>> getEstadisticas(
            @RequestHeader("Authorization") String token
    ) {
        Map<String, Object> response = new HashMap<>();

        try {
            Integer usuarioId = extractUsuarioId(token);
            Map<String, Object> estadisticas = sectoresBl.getEstadisticasParaSocio(usuarioId);

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

    // Metodo auxiliar para extraer el usuario del token
    private Integer extractUsuarioId(String token) {
        String cleanToken = token.replace("Bearer ", "");
        return jwtUtil.extractUsuarioId(cleanToken);
    }
}