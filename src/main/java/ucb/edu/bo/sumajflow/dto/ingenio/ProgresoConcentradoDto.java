package ucb.edu.bo.sumajflow.dto.ingenio;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProgresoConcentradoDto {
    private String etapaActual;
    private Integer porcentaje;
    private Integer etapasCompletadas;
    private Integer etapasTotal;
}
