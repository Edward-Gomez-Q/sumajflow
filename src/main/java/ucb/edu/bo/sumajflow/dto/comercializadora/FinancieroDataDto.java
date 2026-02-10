package ucb.edu.bo.sumajflow.dto.comercializadora;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FinancieroDataDto {
    private BigDecimal totalPendientePago;
    private BigDecimal totalPagadoMes;
    private BigDecimal volumenCompradoMes; // toneladas
}
