package ucb.edu.bo.sumajflow.dto.tracking;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor; /**
 * DTO para respuesta de actualización de ubicación
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActualizacionUbicacionResponseDto {
    private Boolean success;
    private String mensaje;
    private UbicacionDto ubicacionRegistrada;
    private GeofencingStatusDto geofencingStatus;
    private String nuevoEstadoViaje;
    private Boolean requiereAccion;  // Si debe mostrar algún diálogo al usuario
    private String accionRequerida;  // "registrar_llegada", "registrar_pesaje", etc.
}
