package ucb.edu.bo.sumajflow.dto.comercializadora;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CompraPorMesDto {
    private String mes;
    private Integer cantidadLiquidaciones;
    private BigDecimal pesoTotal; // kg
    private BigDecimal inversionTotal; // USD
    private BigDecimal precioPromedioTon; // USD/ton
}
