package ucb.edu.bo.sumajflow.dto.comercializadora;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class HistoricoCotizacionDto {
    private String fecha; // "03 Feb"
    private BigDecimal Pb;
    private BigDecimal Zn;
    private BigDecimal Ag;
}
