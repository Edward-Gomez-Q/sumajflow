package ucb.edu.bo.sumajflow.dto.tracking;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor; /**
 * DTO para iniciar tracking de un viaje
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IniciarTrackingDto {

    @NotNull(message = "La asignación de camión es requerida")
    private Integer asignacionCamionId;

    // Ubicación inicial (opcional)
    private Double latInicial;
    private Double lngInicial;
}
