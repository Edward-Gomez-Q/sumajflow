package ucb.edu.bo.sumajflow.dto.ingenio;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReporteQuimicoResponseDto {

    private Integer id;
    private String numeroReporte;
    private String laboratorio;
    private LocalDate fechaAnalisis;
    private BigDecimal leyAg;
    private BigDecimal leyPb;
    private BigDecimal leyZn;
    private BigDecimal humedad;
    private String tipoAnalisis;
    private String urlPdf;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}