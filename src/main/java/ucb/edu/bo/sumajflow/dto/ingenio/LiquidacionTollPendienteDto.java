package ucb.edu.bo.sumajflow.dto.ingenio;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LiquidacionTollPendienteDto {
    private Integer id;
    private String socioNombre;
    private List<Integer> lotes;
    private BigDecimal pesoTotal; // kg
    private BigDecimal costoProcesamiento; // BOB
    private BigDecimal serviciosAdicionales; // BOB
    private BigDecimal totalBob;
    private String estado;
    private Long diasPendiente;
}
