package ucb.edu.bo.sumajflow.dto.liquidacion;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoteRelacionadoDto {
    private Integer id;
    private String minaNombre;
    private List<String> minerales;
    private BigDecimal pesoTotalReal;
    private String tipoMineral;
}