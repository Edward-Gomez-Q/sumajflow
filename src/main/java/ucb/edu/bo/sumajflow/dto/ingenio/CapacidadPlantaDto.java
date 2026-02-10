package ucb.edu.bo.sumajflow.dto.ingenio;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CapacidadPlantaDto {
    private BigDecimal capacidadMaxima; // ton/día
    private BigDecimal procesamientoActual; // ton/día
    private BigDecimal utilizacion; // porcentaje
    private BigDecimal proyeccionDia; // ton
}
