package ucb.edu.bo.sumajflow.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ucb.edu.bo.sumajflow.dto.CooperativaPublicDto;
import ucb.edu.bo.sumajflow.entity.Cooperativa;
import ucb.edu.bo.sumajflow.repository.CooperativaRepository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Controlador público para obtener información básica de cooperativas
 */
@RestController
@RequestMapping("/public/cooperativas")
@CrossOrigin(origins = "*")
public class CooperativaPublicController {

    private final CooperativaRepository cooperativaRepository;

    public CooperativaPublicController(CooperativaRepository cooperativaRepository) {
        this.cooperativaRepository = cooperativaRepository;
    }

    /**
     * Obtiene lista de todas las cooperativas (solo ID y razón social)
     * GET /public/cooperativas
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getAllCooperativas() {
        Map<String, Object> response = new HashMap<>();

        try {
            List<Cooperativa> cooperativas = cooperativaRepository.findAll();

            List<CooperativaPublicDto> cooperativasDto = cooperativas.stream()
                    .map(c -> new CooperativaPublicDto(c.getId(), c.getRazonSocial()))
                    .collect(Collectors.toList());

            response.put("success", true);
            response.put("data", cooperativasDto);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error al obtener cooperativas: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }
}