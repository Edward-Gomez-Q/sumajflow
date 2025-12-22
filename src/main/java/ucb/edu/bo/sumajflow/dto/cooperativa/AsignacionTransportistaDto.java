package ucb.edu.bo.sumajflow.dto.cooperativa;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AsignacionTransportistaDto {
    private Integer transportistaId;
    private Integer numeroCamion; // Número de camión en el lote (1, 2, 3, etc.)
}