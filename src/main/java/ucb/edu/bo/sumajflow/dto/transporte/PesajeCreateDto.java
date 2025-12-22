package ucb.edu.bo.sumajflow.dto.transporte;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PesajeCreateDto {
    private Integer asignacionCamionId;
    private String tipoPesaje; // "cooperativa", "ingenio", "comercializadora"
    private BigDecimal pesoBruto;
    private BigDecimal pesoTara;
    private String observaciones;
}