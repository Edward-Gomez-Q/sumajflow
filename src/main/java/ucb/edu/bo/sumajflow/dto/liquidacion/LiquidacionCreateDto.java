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
public class LiquidacionCreateDto {
    private Integer socioId;
    private String tipoLiquidacion; // "venta_directa", "venta_concentrado", "cobro_ingenio"
    private LocalDateTime fechaLiquidacion;
    private String moneda; // "BOB", "USD"
    private BigDecimal pesoLiquidado;

    // Relaciones
    private Integer loteId; // Para venta directa
    private Integer concentradoId; // Para venta de concentrado o cobro de ingenio
    private Integer reporteQuimicoId; // Reporte químico asociado

    // Cotizaciones
    private List<CotizacionDto> cotizaciones;

    // Deducciones
    private List<DeduccionDto> deducciones;

    private String observaciones;
}