package ucb.edu.bo.sumajflow.dto.cooperativa;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DeduccionVentaDto {
    private String concepto;
    private String tipoDeduccion;
    private BigDecimal porcentaje;
    private BigDecimal montoDeducidoUsd;
    private BigDecimal montoDeducidoBob;
    private String baseCalculo;
    private Integer orden;
    private String descripcion;
}