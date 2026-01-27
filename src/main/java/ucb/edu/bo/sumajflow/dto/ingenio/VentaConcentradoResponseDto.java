package ucb.edu.bo.sumajflow.dto.ingenio;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VentaConcentradoResponseDto {

    private Integer id;
    private Integer concentradoId;
    private String codigoConcentrado;
    private Integer socioId;
    private String socioNombres;
    private String socioApellidos;
    private Integer comercializadoraId;
    private String comercializadoraNombre;
    private String tipoLiquidacion;
    private LocalDateTime fechaLiquidacion;
    private String moneda;
    private BigDecimal pesoVendido;
    private BigDecimal valorBruto;
    private BigDecimal valorNeto;
    private String estado;
    private String urlContrato;
    private String urlComprobantePago;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}