package ucb.edu.bo.sumajflow.dto.ingenio;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EstadisticasTollDto {
    private BigDecimal totalPendienteCobro;
    private BigDecimal promedioTiempoCobranza; // días
    private BigDecimal tasaCobranza; // porcentaje
    private BigDecimal ingresosUltimos30Dias;
}
