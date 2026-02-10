package ucb.edu.bo.sumajflow.controller.cooperativa;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import ucb.edu.bo.sumajflow.bl.cooperativa.DashboardCooperativaBl;
import ucb.edu.bo.sumajflow.dto.cooperativa.DashboardCooperativaDto;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/cooperativa/dashboard")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class DashboardCooperativaController {

    private final DashboardCooperativaBl dashboardCooperativaBl;

    @GetMapping
    public ResponseEntity<Map<String, Object>> obtenerDashboard(Authentication authentication) {
        Map<String, Object> response = new HashMap<>();

        try {
            Integer usuarioId = (Integer) authentication.getDetails();

            DashboardCooperativaDto dashboard = dashboardCooperativaBl.obtenerDashboard(usuarioId);

            response.put("success", true);
            response.put("data", dashboard);

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            log.error("Error de validación en dashboard cooperativa: {}", e.getMessage());
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);

        } catch (Exception e) {
            log.error("Error interno en dashboard cooperativa: ", e);
            response.put("success", false);
            response.put("message", "Error interno del servidor");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}