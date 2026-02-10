package ucb.edu.bo.sumajflow.dto.socio;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FinancialDataDto {
    private BigDecimal totalPendienteCobro;
    private BigDecimal totalCobradoMesActual;
    private BigDecimal proyeccionMesActual;
    private BigDecimal comparativoMesAnterior; // porcentaje
}
