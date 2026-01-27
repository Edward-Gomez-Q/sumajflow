package ucb.edu.bo.sumajflow.dto.ingenio;

import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReporteQuimicoCreateDto {

    @NotBlank(message = "El número de reporte es requerido")
    @Size(max = 100, message = "El número de reporte no puede exceder 100 caracteres")
    private String numeroReporte;

    @Size(max = 100, message = "El laboratorio no puede exceder 100 caracteres")
    private String laboratorio;

    @NotNull(message = "La fecha de análisis es requerida")
    @PastOrPresent(message = "La fecha de análisis no puede ser futura")
    private LocalDateTime fechaAnalisis;

    @DecimalMin(value = "0.0", message = "La ley de Ag debe ser mayor o igual a 0")
    @DecimalMax(value = "100.0", message = "La ley de Ag debe ser menor o igual a 100")
    private BigDecimal leyAg;

    @DecimalMin(value = "0.0", message = "La ley de Pb debe ser mayor o igual a 0")
    @DecimalMax(value = "100.0", message = "La ley de Pb debe ser menor o igual a 100")
    private BigDecimal leyPb;

    @DecimalMin(value = "0.0", message = "La ley de Zn debe ser mayor o igual a 0")
    @DecimalMax(value = "100.0", message = "La ley de Zn debe ser menor o igual a 100")
    private BigDecimal leyZn;

    @DecimalMin(value = "0.0", message = "La humedad debe ser mayor o igual a 0")
    @DecimalMax(value = "100.0", message = "La humedad debe ser menor o igual a 100")
    private BigDecimal humedad;

    @Size(max = 50, message = "El tipo de análisis no puede exceder 50 caracteres")
    private String tipoAnalisis;

    @Size(max = 200, message = "La URL del PDF no puede exceder 200 caracteres")
    private String urlPdf;

}