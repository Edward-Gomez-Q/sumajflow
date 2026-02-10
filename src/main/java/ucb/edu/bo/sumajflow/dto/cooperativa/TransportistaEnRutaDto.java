package ucb.edu.bo.sumajflow.dto.cooperativa;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TransportistaEnRutaDto {
    private Integer id;
    private String nombreCompleto;
    private String placaVehiculo;
    private String estadoViaje;
    private Integer progreso; // 0-100
    private Integer loteId;
    private UbicacionDto ubicacionActual;
}
