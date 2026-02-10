package ucb.edu.bo.sumajflow.dto.ingenio;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OperacionesDataDto {
    private Integer concentradosEnProceso;
    private Integer concentradosCompletadosHoy;
    private BigDecimal pesoTotalProcesamientoMes; // toneladas
    private BigDecimal capacidadUtilizada; // porcentaje
}
