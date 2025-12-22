package ucb.edu.bo.sumajflow.dto.liquidacion;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConcentradoRelacionadoDto {
    private Integer id;
    private String codigoConcentrado;
    private String ingenioNombre;
    private BigDecimal pesoFinal;
    private String mineralPrincipal;
}