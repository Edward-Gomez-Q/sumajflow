package ucb.edu.bo.sumajflow.dto.tracking;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HistorialPorEstadoDto {
    private Integer asignacionCamionId;
    private Integer loteId;
    private String codigoLote;
    private String placaVehiculo;
    private String nombreTransportista;
    private Integer totalUbicaciones;
    private List<EstadoHistorialDto> estadosHistorial;
}
