package ucb.edu.bo.sumajflow.dto.ingenio;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class KanbanDataDto {
    private Integer porIniciar;
    private Integer enProceso;
    private Integer esperandoPago;
    private Integer procesado;
}
