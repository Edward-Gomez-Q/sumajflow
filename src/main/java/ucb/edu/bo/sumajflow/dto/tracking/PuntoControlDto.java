package ucb.edu.bo.sumajflow.dto.tracking;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime; /**
 * DTO para punto de control
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PuntoControlDto {
    private String tipo;
    private String nombre;
    private Double lat;
    private Double lng;
    private Integer radio;
    private Integer orden;
    private Boolean requerido;
    private LocalDateTime llegada;
    private LocalDateTime salida;
    private String estado;
    private Double distanciaActual;  // Distancia desde ubicación actual
}
