package ucb.edu.bo.sumajflow.dto.ingenio;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReporteQuimicoSimpleDto {

    private Integer id;
    private String numeroReporte;
    private String laboratorio;
    private LocalDate fechaAnalisis;
    private BigDecimal leyAg;
    private BigDecimal leyPb;
    private BigDecimal leyZn;
    private BigDecimal humedad;
    private String urlPdf;
}