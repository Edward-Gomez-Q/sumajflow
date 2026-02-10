package ucb.edu.bo.sumajflow.controller.ingenio;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import ucb.edu.bo.sumajflow.bl.ingenio.DashboardIngenioBl;
import ucb.edu.bo.sumajflow.dto.ingenio.DashboardIngenioDto;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/ingenio/dashboard")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class DashboardIngenioController {

    private final DashboardIngenioBl dashboardIngenioBl;

    @GetMapping
    public ResponseEntity<Map<String, Object>> obtenerDashboard(Authentication authentication) {
        Map<String, Object> response = new HashMap<>();

        try {
            Integer usuarioId = (Integer) authentication.getDetails();

            DashboardIngenioDto dashboard = dashboardIngenioBl.obtenerDashboard(usuarioId);

            response.put("success", true);
            response.put("data", dashboard);

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            log.error("Error de validación en dashboard ingenio: {}", e.getMessage());
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);

        } catch (Exception e) {
            log.error("Error interno en dashboard ingenio: ", e);
            response.put("success", false);
            response.put("message", "Error interno del servidor");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}