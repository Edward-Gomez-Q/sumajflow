package ucb.edu.bo.sumajflow.dto.tracking;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime; /**
 * DTO para cada camión en monitoreo
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CamionEnRutaDto {
    private Integer asignacionCamionId;
    private Integer numeroCamion;
    private String placaVehiculo;
    private String nombreTransportista;
    private String telefonoTransportista;
    private UbicacionDto ubicacionActual;
    private String estadoViaje;
    private String estadoConexion;
    private LocalDateTime ultimaActualizacion;
    private String proximoPuntoControl;
    private Double distanciaProximoPunto;
    private String tiempoEstimadoLlegada;
}
