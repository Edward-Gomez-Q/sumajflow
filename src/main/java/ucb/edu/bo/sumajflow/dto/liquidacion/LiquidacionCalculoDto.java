package ucb.edu.bo.sumajflow.dto.liquidacion;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LiquidacionCalculoDto {
    private BigDecimal valorBruto;
    private BigDecimal totalDeducciones;
    private BigDecimal valorNeto;

    // Desglose por mineral
    private Map<String, BigDecimal> valorPorMineral;

    // Desglose de deducciones
    private Map<String, BigDecimal> deduccionesPorConcepto;
}