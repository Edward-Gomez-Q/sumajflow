package ucb.edu.bo.sumajflow.dto.comercializadora;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CompraPorSocioDto {
    private String socioNombre;
    private Integer cantidadCompras;
    private BigDecimal pesoTotal; // kg
    private BigDecimal montoTotal; // USD
    private BigDecimal precioPromedio; // USD/ton
    private Integer confiabilidad; // 0-100
}
