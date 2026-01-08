package ucb.edu.bo.sumajflow.dto.tracking;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List; /**
 * DTO para monitoreo en tiempo real (vista web)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MonitoreoLoteDto {
    private Integer loteId;
    private String codigoLote;
    private String estadoLote;
    private Integer totalCamiones;
    private Integer camionesEnRuta;
    private Integer camionesCompletados;
    private List<CamionEnRutaDto> camiones;
}
