package ucb.edu.bo.sumajflow.dto.ingenio;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LiquidacionServicioResponseDto {

    private Integer id;
    private Integer concentradoId;
    private String codigoConcentrado;
    private Integer socioId;
    private String socioNombres;
    private String socioApellidos;
    private String tipoLiquidacion;
    private LocalDateTime fechaLiquidacion;
    private String moneda;
    private BigDecimal pesoLiquidado;
    private BigDecimal valorBruto;
    private BigDecimal valorNeto;
    private String estado;
    private String urlDocumentoLiquidacion;
    private String urlComprobantePago;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}