package ucb.edu.bo.sumajflow.dto.transporte;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map; /**
 * DTO de respuesta para transiciones de estado
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

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime timestamp;
}
