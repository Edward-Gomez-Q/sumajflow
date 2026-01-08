package ucb.edu.bo.sumajflow.dto.tracking;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor; /**
 * DTO para registrar llegada/salida de punto de control
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegistrarPuntoControlDto {

    @NotNull(message = "La asignación de camión es requerida")
    private Integer asignacionCamionId;

    @NotNull(message = "El tipo de punto es requerido")
    private String tipoPunto;  // mina, balanza_cooperativa, balanza_ingenio, almacen

    @NotNull(message = "La acción es requerida")
    private String accion;     // llegada, salida

    private Double lat;
    private Double lng;
    private String observaciones;
}
