package ucb.edu.bo.sumajflow.dto.ingenio;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LotesPendientesDataDto {
    private Integer pendienteAprobacion;
    private Integer transporteCompleto;
}
