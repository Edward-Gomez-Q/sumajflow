package ucb.edu.bo.sumajflow.dto.comercializadora;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LiquidacionPipelineDto {
    private Integer id;
    private String socioNombre;
    private String tipo; // "venta_concentrado" o "venta_lote_complejo"
    private BigDecimal peso; // kg
    private BigDecimal valorEstimado; // USD
    private Long diasEnEtapa;
    private String prioridad; // "alta", "media", "baja"
}
