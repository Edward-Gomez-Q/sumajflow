package ucb.edu.bo.sumajflow.dto.tracking;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime; /**
 * DTO para métricas del viaje
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MetricasViajeDto {
    private Double distanciaRecorrida;
    private Long tiempoEnMovimiento;
    private Long tiempoDetenido;
    private Double velocidadPromedio;
    private Double velocidadMaxima;
    private LocalDateTime inicioViaje;
    private LocalDateTime finViaje;
    private String tiempoTranscurrido;  // Formato legible "2h 30m"
}
