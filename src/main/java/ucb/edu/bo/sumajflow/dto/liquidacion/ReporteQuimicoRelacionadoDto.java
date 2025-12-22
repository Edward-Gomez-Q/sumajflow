package ucb.edu.bo.sumajflow.dto.liquidacion;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReporteQuimicoRelacionadoDto {
    private Integer id;
    private String numeroReporte;
    private String laboratorio;
    private LocalDate fechaAnalisis;
    private BigDecimal leyAg;
    private BigDecimal leyPb;
    private BigDecimal leyZn;
}