package ucb.edu.bo.sumajflow.dto.reportes;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReporteQuimicoDetalleDto {
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

    // Información completa del lote (si aplica)
    private LoteReporteDto lote;

    // Información completa del concentrado (si aplica)
    private ConcentradoReporteDto concentrado;

    // Análisis adicional
    private Map<String, BigDecimal> otrosElementos; // Elementos adicionales analizados

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}