package ucb.edu.bo.sumajflow.dto.comercializadora;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DistribucionCarteraDto {
    private String mineral;
    private Integer cantidad;
    private BigDecimal peso; // kg
    private BigDecimal valorCompra; // USD
    private BigDecimal valorizacionActual; // USD
}
