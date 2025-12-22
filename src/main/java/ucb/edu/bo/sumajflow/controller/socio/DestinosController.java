package ucb.edu.bo.sumajflow.controller.socio;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ucb.edu.bo.sumajflow.bl.socio.DestinosBl;
import ucb.edu.bo.sumajflow.dto.socio.ComercializadoraSimpleDto;
import ucb.edu.bo.sumajflow.dto.socio.IngenioSimpleDto;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/socio/destinos")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class DestinosController {

    private final DestinosBl destinosBl;

    /**
     * Obtener lista de ingenios mineros disponibles
     * GET /socio/destinos/ingenios
     */
    @GetMapping("/ingenios")
    public ResponseEntity<Map<String, Object>> getIngenios() {
        Map<String, Object> response = new HashMap<>();

        try {
            List<IngenioSimpleDto> ingenios = destinosBl.getIngeniosDisponibles();

            response.put("success", true);
            response.put("data", ingenios);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error interno del servidor: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * Obtener lista de comercializadoras disponibles
     * GET /socio/destinos/comercializadoras
     */
    @GetMapping("/comercializadoras")
    public ResponseEntity<Map<String, Object>> getComercializadoras() {
        Map<String, Object> response = new HashMap<>();

        try {
            List<ComercializadoraSimpleDto> comercializadoras = destinosBl.getComercializadorasDisponibles();

            response.put("success", true);
            response.put("data", comercializadoras);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error interno del servidor: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(response);
        }
    }
}