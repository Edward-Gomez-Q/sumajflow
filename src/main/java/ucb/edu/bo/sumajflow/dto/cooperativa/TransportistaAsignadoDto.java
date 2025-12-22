package ucb.edu.bo.sumajflow.dto.cooperativa;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TransportistaAsignadoDto {
    private Integer asignacionId;
    private Integer transportistaId;
    private String nombreCompleto;
    private String placaVehiculo;
    private Integer numeroCamion;
    private String estado;
}