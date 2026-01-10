package ucb.edu.bo.sumajflow.dto.transporte;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map; /**
 * DTO de respuesta unificada para transiciones
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransicionEstadoResponseDto {
    private Boolean success;
    private String message;
    private String estadoAnterior;
    private String estadoNuevo;
    private String proximoPaso;
    private ProximoPuntoControlDto proximoPuntoControl;
    private Map<String, Object> metadata;
}
