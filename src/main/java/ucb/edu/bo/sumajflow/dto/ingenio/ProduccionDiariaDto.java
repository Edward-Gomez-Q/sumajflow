package ucb.edu.bo.sumajflow.dto.ingenio;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProduccionDiariaDto {
    private String fecha; // "04 Feb"
    private Integer concentradosCreados;
    private Integer concentradosFinalizados;
    private BigDecimal pesoTotalProcesado; // toneladas
}
