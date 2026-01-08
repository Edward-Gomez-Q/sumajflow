package ucb.edu.bo.sumajflow.dto.tracking;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor; /**
 * DTO para estado de geofencing
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GeofencingStatusDto {
    private Boolean dentroDeZona;
    private String zonaNombre;
    private String zonaTipo;
    private Double distanciaAZona;
    private Boolean puedeRegistrarLlegada;
    private Boolean puedeRegistrarSalida;
    private String proximoPuntoControl;
    private Double distanciaProximoPunto;
}
