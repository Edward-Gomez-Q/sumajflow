package ucb.edu.bo.sumajflow.dto.reportes;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReporteQuimicoCreateDto {
    private String numeroReporte; // Número único del reporte
    private String laboratorio; // Laboratorio que realiza el análisis
    private LocalDate fechaAnalisis;

    // Leyes de minerales (en porcentaje o g/TM según corresponda)
    private BigDecimal leyAg; // Plata
    private BigDecimal leyPb; // Plomo
    private BigDecimal leyZn; // Zinc
    private BigDecimal humedad; // Porcentaje de humedad

    private String tipoAnalisis; // "pre_venta", "post_proceso", etc.
    private String urlPdf; // URL del PDF del reporte
    private String observaciones;

    // Relación con lote o concentrado
    private Integer loteId; // Si es para un lote
    private Integer concentradoId; // Si es para un concentrado
}