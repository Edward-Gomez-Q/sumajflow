package ucb.edu.bo.sumajflow.dto.ingenio;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoteConcentradoRelacionDto {
    private Integer id;
    private Integer loteId;
    private String minaNombre;
    private String socioNombre;
    private BigDecimal pesoEntrada;
    private BigDecimal pesoSalida;
    private BigDecimal porcentajeRecuperacion;
    private LocalDateTime fechaCreacion;
}