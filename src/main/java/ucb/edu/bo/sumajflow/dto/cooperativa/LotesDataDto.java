package ucb.edu.bo.sumajflow.dto.cooperativa;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LotesDataDto {
    private Integer pendienteAprobacion;
    private Integer aprobadosHoy;
    private Integer rechazadosHoy;
    private BigDecimal tasaAprobacionMes; // porcentaje
    private BigDecimal tiempoPromedioAprobacion; // horas
}
