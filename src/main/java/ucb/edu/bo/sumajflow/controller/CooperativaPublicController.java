package ucb.edu.bo.sumajflow.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ucb.edu.bo.sumajflow.dto.CooperativaPublicDto;
import ucb.edu.bo.sumajflow.entity.Cooperativa;
import ucb.edu.bo.sumajflow.entity.Minerales;
import ucb.edu.bo.sumajflow.entity.Procesos;
import ucb.edu.bo.sumajflow.repository.CooperativaRepository;
import ucb.edu.bo.sumajflow.repository.MineralesRepository;
import ucb.edu.bo.sumajflow.repository.ProcesosRepository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


@RestController
@RequestMapping("/public")
@CrossOrigin(origins = "*")
public class CooperativaPublicController {

    private final CooperativaRepository cooperativaRepository;
    private final ProcesosRepository procesosRepository;
    private final MineralesRepository mineralesRepository;

    public CooperativaPublicController(CooperativaRepository cooperativaRepository, ProcesosRepository procesosRepository, MineralesRepository mineralesRepository) {
        this.cooperativaRepository = cooperativaRepository;
        this.procesosRepository = procesosRepository;
        this.mineralesRepository = mineralesRepository;
    }

    /**
     * Obtiene lista de todas las cooperativas (solo ID y razón social)
     * GET /public/cooperativas
     */
    @GetMapping("/cooperativas")
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

    /**
     * Obtiene lista de todos los procesos (solo ID y nombre)
     * GET /public/procesos
     */
    @GetMapping("/procesos")
    public ResponseEntity<Map<String, Object>> getAllProcesos() {
        Map<String, Object> response = new HashMap<>();
        try {
            List<Procesos> procesos = procesosRepository.findAll();

            List<Map<String, Object>> procesosDto = procesos.stream()
                    .map(p -> {
                        Map<String, Object> dto = new HashMap<>();
                        dto.put("id", p.getId());
                        dto.put("nombre", p.getNombre());
                        return dto;
                    })
                    .collect(Collectors.toList());

            response.put("success", true);
            response.put("data", procesosDto);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error al obtener procesos: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }
    /**
     * Obtiene lista de todos los minerales (id, nombre y nomenclatura)
     * GET /public/minerales
     */
    @GetMapping("/minerales")
    public ResponseEntity<Map<String, Object>> getAllMinerales() {
        Map<String, Object> response = new HashMap<>();
        try {
            List<Minerales> minerales = mineralesRepository.findAll();

            List<Map<String, Object>> mineralesDto = minerales.stream()
                    .map(m -> {
                        Map<String, Object> dto = new HashMap<>();
                        dto.put("id", m.getId());
                        dto.put("nombre", m.getNombre());
                        dto.put("nomenclatura", m.getNomenclatura());
                        return dto;
                    })
                    .collect(Collectors.toList());

            response.put("success", true);
            response.put("data", mineralesDto);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error al obtener minerales: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }
}