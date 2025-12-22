package ucb.edu.bo.sumajflow.dto.liquidacion;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LiquidacionResponseDto {
    private Integer id;
    private Integer socioId;
    private String socioNombre;
    private String tipoLiquidacion;
    private LocalDate fechaLiquidacion;
    private String moneda;
    private BigDecimal pesoLiquidado;
    private BigDecimal valorBruto;
    private BigDecimal valorNeto;
    private String estado;

    // Información relacionada
    private Integer loteId;
    private String loteInfo;
    private Integer concentradoId;
    private String concentradoInfo;
    private Integer reporteQuimicoId;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}