package ucb.edu.bo.sumajflow.dto.socio;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class IngresoPorMineralDto {
    private String mineral; // "Pb", "Zn", "Ag"
    private BigDecimal ingreso;
    private BigDecimal porcentaje;
}
