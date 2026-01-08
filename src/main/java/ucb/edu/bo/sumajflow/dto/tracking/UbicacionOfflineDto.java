package ucb.edu.bo.sumajflow.dto.tracking;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime; /**
 * DTO para cada ubicación offline individual
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UbicacionOfflineDto {

    @NotNull(message = "La latitud es requerida")
    private Double lat;

    @NotNull(message = "La longitud es requerida")
    private Double lng;

    @NotNull(message = "El timestamp es requerido")
    private LocalDateTime timestamp;

    private Double precision;
    private Double velocidad;
    private Double rumbo;
    private Double altitud;
}


