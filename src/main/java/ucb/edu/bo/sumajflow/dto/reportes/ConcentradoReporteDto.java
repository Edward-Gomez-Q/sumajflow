package ucb.edu.bo.sumajflow.dto.reportes;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConcentradoReporteDto {
    private Integer id;
    private String codigoConcentrado;
    private String ingenioNombre;
    private String socioNombre;
    private BigDecimal pesoFinal;
    private String mineralPrincipal;
    private String estado;
}