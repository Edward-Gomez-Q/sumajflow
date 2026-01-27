package ucb.edu.bo.sumajflow.dto.tracking;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO para eventos de cambio de estado
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventoEstadoDto {

    private LocalDateTime timestamp;
    private String estadoAnterior;
    private String estadoNuevo;
    private Double lat;
    private Double lng;
    private String tipoEvento;
}