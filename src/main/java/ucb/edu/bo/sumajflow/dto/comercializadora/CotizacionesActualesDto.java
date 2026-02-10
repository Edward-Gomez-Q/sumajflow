package ucb.edu.bo.sumajflow.dto.comercializadora;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CotizacionesActualesDto {
    private BigDecimal Pb;
    private BigDecimal Zn;
    private BigDecimal Ag;
    private String tendencia; // "up", "down", "stable"
}
