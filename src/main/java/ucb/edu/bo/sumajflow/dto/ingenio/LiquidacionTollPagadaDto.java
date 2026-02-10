package ucb.edu.bo.sumajflow.dto.ingenio;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LiquidacionTollPagadaDto {
    private Integer id;
    private String socioNombre;
    private BigDecimal montoBob;
    private LocalDateTime fechaPago;
    private String metodoPago;
}
