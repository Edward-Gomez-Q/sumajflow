package ucb.edu.bo.sumajflow.dto.reportes;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReporteQuimicoResponseDto {
    private Integer id;
    private String numeroReporte;
    private String laboratorio;
    private LocalDate fechaAnalisis;

    // Leyes de minerales
    private BigDecimal leyAg;
    private BigDecimal leyPb;
    private BigDecimal leyZn;
    private BigDecimal humedad;

    private String tipoAnalisis;
    private String urlPdf;

    // Información relacionada
    private Integer loteId;
    private String loteInfo; // "Mina: X, Socio: Y"
    private Integer concentradoId;
    private String concentradoInfo; // "Código: X, Ingenio: Y"

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}