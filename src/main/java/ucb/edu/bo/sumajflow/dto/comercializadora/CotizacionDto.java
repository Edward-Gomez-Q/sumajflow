package ucb.edu.bo.sumajflow.dto.comercializadora;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CotizacionDto {
    private String mineral; // "Pb", "Zn", "Ag"
    private BigDecimal valor; // USD
    private String unidad; // "USD/ton" o "USD/oz"
    private BigDecimal variacion24h; // porcentaje
    private BigDecimal variacion7d;
    private BigDecimal variacion30d;
    private String tendencia; // "up", "down", "stable"
    private BigDecimal minimo30d;
    private BigDecimal maximo30d;
    private BigDecimal promedioMovil;
}
