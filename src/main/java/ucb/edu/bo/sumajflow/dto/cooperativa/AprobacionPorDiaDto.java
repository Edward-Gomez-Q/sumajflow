package ucb.edu.bo.sumajflow.dto.cooperativa;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AprobacionPorDiaDto {
    private String fecha; // "04 Feb"
    private Integer aprobados;
    private Integer rechazados;
    private BigDecimal tasaAprobacion; // porcentaje
}
