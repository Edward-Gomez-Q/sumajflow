package ucb.edu.bo.sumajflow.dto.comercializadora;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ResumenCarteraDto {
    private Integer totalConcentrados;
    private BigDecimal pesoTotal; // kg
    private BigDecimal valorCompraTotal; // USD
    private BigDecimal valorizacionActual; // USD
    private BigDecimal gananciaNoRealizada; // USD
    private BigDecimal rentabilidadPromedio; // porcentaje
}
