package ucb.edu.bo.sumajflow.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeduccionConfiguracionDto {
    private String codigo;
    private String concepto;
    private String descripcion;
    private String tipoDeduccion;
    private String categoria;
    private String aplicaAMineral;
    private BigDecimal porcentaje;
    private String baseCalculo;
    private Integer orden;
}