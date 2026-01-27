package ucb.edu.bo.sumajflow.dto.liquidacion;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LiquidacionDetalleDto {
    private Integer id;
    private Integer socioId;
    private String socioNombre;
    private String socioCi;
    private String socioTelefono;
    private String tipoLiquidacion;
    private LocalDateTime fechaLiquidacion;
    private String moneda;
    private BigDecimal pesoLiquidado;
    private BigDecimal valorBruto;
    private BigDecimal valorNeto;
    private String estado;
    private String observaciones;

    // Cotizaciones
    private List<CotizacionDto> cotizaciones;

    // Deducciones
    private List<DeduccionDto> deducciones;

    // Cálculo detallado
    private LiquidacionCalculoDto calculo;

    // Información relacionada
    private LoteRelacionadoDto lote;
    private ConcentradoRelacionadoDto concentrado;
    private ReporteQuimicoRelacionadoDto reporteQuimico;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}