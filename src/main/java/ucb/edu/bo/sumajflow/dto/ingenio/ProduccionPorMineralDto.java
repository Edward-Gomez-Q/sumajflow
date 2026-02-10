package ucb.edu.bo.sumajflow.dto.ingenio;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProduccionPorMineralDto {
    private String mineral; // "Zn", "Pb", "Ag"
    private Integer cantidad;
    private BigDecimal pesoTotal; // kg
    private Integer porcentaje;
}
