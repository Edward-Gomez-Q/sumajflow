package ucb.edu.bo.sumajflow.dto.ingenio;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConcentradoKanbanDto {
    private Integer id;
    private String codigo;
    private String mineralPrincipal;
    private BigDecimal pesoInicial;
    private String socioNombre;
    private LocalDateTime fechaCreacion;
    private ProgresoConcentradoDto progreso;
    private AlertaConcentradoDto alertas;
}
