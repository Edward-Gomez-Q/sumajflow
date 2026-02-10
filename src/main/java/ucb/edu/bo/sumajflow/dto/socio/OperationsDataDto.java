package ucb.edu.bo.sumajflow.dto.socio;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OperationsDataDto {
    private Integer lotesActivos;
    private Integer lotesEnTransporte;
    private Integer lotesEnProceso;
    private Integer concentradosListosVenta;
    private Integer concentradosEnVenta;
}
