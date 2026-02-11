package ucb.edu.bo.sumajflow.dto.socio;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class IngresoMensualDto {
    private String mes;
    private BigDecimal pagoToll;
    private BigDecimal ingresoVentaConcentrado;
    private BigDecimal ingresoVentaComplejo;
}
