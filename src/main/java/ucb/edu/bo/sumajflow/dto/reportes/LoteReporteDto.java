package ucb.edu.bo.sumajflow.dto.reportes;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoteReporteDto {
    private Integer id;
    private String minaNombre;
    private String socioNombre;
    private String socioCi;
    private List<String> minerales;
    private BigDecimal pesoTotalReal;
    private String tipoMineral;
    private String estado;
}