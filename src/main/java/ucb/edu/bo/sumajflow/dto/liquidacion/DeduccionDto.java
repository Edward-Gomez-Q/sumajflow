package ucb.edu.bo.sumajflow.dto.liquidacion;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DeduccionDto {
    private Integer id; // Para actualización
    private String concepto; // "Regalías cooperativa", "Caja de salud", etc.
    private BigDecimal monto; // Monto fijo
    private BigDecimal porcentaje; // Porcentaje sobre valor bruto
    private String tipoDeduccion; // "porcentaje", "monto_fijo"
}