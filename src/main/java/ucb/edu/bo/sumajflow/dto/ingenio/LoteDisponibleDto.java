package ucb.edu.bo.sumajflow.dto.ingenio;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoteDisponibleDto {
    private Integer id;
    private String codigo;
    private String socioNombre;
    private String tipoMineral;
    private BigDecimal pesoReal; // kg
    private List<String> minerales;
    private LocalDateTime fechaLlegada;
    private Long diasEspera;
    private String prioridad; // "alta", "media", "baja"
    private String motivoPrioridad;
    private Integer concentradosEstimados;
    private BigDecimal tiempoProcesamientoEstimado; // horas
}
