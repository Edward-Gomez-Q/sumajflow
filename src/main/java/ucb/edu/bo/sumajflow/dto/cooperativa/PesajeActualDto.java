package ucb.edu.bo.sumajflow.dto.cooperativa;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PesajeActualDto {
    private Integer asignacionId;
    private Integer loteId;
    private String placaVehiculo;
    private LocalDateTime horaInicio;
    private String tipoPesaje; // "origen"
}
