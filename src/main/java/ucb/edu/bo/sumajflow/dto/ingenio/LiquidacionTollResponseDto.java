package ucb.edu.bo.sumajflow.dto.ingenio;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LiquidacionTollResponseDto {

    private Integer id;
    private String tipoLiquidacion;
    private String estado;

    // Información del socio
    private Integer socioId;
    private String socioNombres;
    private String socioApellidos;
    private String socioCi;

    // Pesos
    private BigDecimal pesoTotalEntradaKg;
    private BigDecimal pesoTotalToneladas;

    // Costos
    private BigDecimal costoPorTonelada;
    private BigDecimal costoProcesamientoTotal;

    // Servicios adicionales
    private Map<String, Object> serviciosAdicionales;
    private BigDecimal totalServiciosAdicionales;

    // Valores monetarios
    private BigDecimal valorBrutoUsd;
    private BigDecimal valorNetoUsd;
    private BigDecimal tipoCambio;
    private BigDecimal valorNetoBob;
    private String moneda;

    // Lotes relacionados
    private List<LoteSimpleDto> lotes;
    private Integer totalLotes;
    private Integer totalCamiones;

    // Fechas
    private LocalDateTime fechaAprobacion;
    private LocalDateTime fechaPago;

    // Información de pago
    private String metodoPago;
    private String numeroComprobante;
    private String urlComprobante;

    // Observaciones
    private String observaciones;

    // Auditoría
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}