package ucb.edu.bo.sumajflow.dto.ingenio;

import lombok.*;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoteSimpleDto {

    private Integer id;
    private String minaNombre;
    private String tipoMineral;
    private BigDecimal pesoTotalReal;
    private String estado;
}