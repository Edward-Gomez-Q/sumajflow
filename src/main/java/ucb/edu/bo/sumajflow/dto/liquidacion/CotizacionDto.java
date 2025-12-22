package ucb.edu.bo.sumajflow.dto.liquidacion;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CotizacionDto {
    private Integer id; // Para actualización
    private String mineral; // "Ag", "Pb", "Zn"
    private BigDecimal cotizacionUsd; // Cotización en USD
    private String unidad; // "oz/TM", "lb/TM", etc.
}