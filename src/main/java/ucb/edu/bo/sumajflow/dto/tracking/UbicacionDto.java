package ucb.edu.bo.sumajflow.dto.tracking;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime; /**
 * DTO para ubicación actual
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UbicacionDto {
    private Double lat;
    private Double lng;
    private LocalDateTime timestamp;
    private Double precision;
    private Double velocidad;
    private Double rumbo;
    private Double altitud;
}
