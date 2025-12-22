package ucb.edu.bo.sumajflow.dto.transporte;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PesajeResponseDto {
    private Integer id;
    private Integer asignacionCamionId;
    private String tipoPesaje;
    private BigDecimal pesoBruto;
    private BigDecimal pesoTara;
    private BigDecimal pesoNeto;
    private LocalDateTime fechaPesaje;
    private String observaciones;
}