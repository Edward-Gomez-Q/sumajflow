package ucb.edu.bo.sumajflow.dto.socio;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LiquidacionesPendientesDto {
    private Integer tollPendientePago;
    private BigDecimal tollMontoTotal;
    private Integer ventasPendientesCierre;
    private Integer ventasEsperandoPago;
}
