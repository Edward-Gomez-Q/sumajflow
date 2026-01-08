package ucb.edu.bo.sumajflow.dto.tracking;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List; /**
 * DTO para resumen de ruta
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RutaResumenDto {
    private UbicacionDto puntoInicio;
    private UbicacionDto puntoFin;
    private Double distanciaTotal;
    private Long duracionTotal;
    private List<PuntoControlDto> puntosVisitados;
}
