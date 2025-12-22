package ucb.edu.bo.sumajflow.dto.ingenio;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoteDisponibleConcentradoDto {
    private Integer id;
    private String minaNombre;
    private String socioNombre;
    private List<String> minerales;
    private BigDecimal pesoTotalReal;
    private LocalDateTime fechaLlegada;
    private String estado;
}