package ucb.edu.bo.sumajflow.dto.socio;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConcentradoResumenDto {
    private Integer id;
    private String codigo;
    private String mineralPrincipal;
    private BigDecimal pesoFinal;
    private String estado;
    private ProgresoConcentradoDto progreso;
    private LiquidacionTollDto liquidacionToll; // null si no aplica
}
