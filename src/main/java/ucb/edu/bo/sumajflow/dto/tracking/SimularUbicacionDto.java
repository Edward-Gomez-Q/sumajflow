package ucb.edu.bo.sumajflow.dto.tracking;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor; /**
 * DTO para simular ubicación (modo desarrollo)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SimularUbicacionDto {

    @NotNull(message = "La asignación de camión es requerida")
    private Integer asignacionCamionId;

    @NotNull(message = "La latitud es requerida")
    @Min(value = -90, message = "Latitud inválida")
    @Max(value = 90, message = "Latitud inválida")
    private Double lat;

    @NotNull(message = "La longitud es requerida")
    @Min(value = -180, message = "Longitud inválida")
    @Max(value = 180, message = "Longitud inválida")
    private Double lng;

    private Double velocidad;  // km/h (opcional, default 0)
    private Double rumbo;      // grados 0-360 (opcional)
}
