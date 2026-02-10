package ucb.edu.bo.sumajflow.dto.ingenio;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LiquidacionesTollDto {
    private List<LiquidacionTollPendienteDto> pendientes;
    private List<LiquidacionTollPagadaDto> pagadasRecientes;
    private EstadisticasTollDto estadisticas;
}
