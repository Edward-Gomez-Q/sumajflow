package ucb.edu.bo.sumajflow.dto.tracking;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List; /**
 * DTO para historial de ubicaciones
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HistorialUbicacionesDto {
    private Integer asignacionCamionId;
    private Integer totalPuntos;
    private List<UbicacionDto> ubicaciones;
    private RutaResumenDto resumen;
}
