package ucb.edu.bo.sumajflow.dto.ingenio;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TurnoActualDto {
    private String turno; // "mañana", "tarde", "noche"
    private LocalDateTime horaInicio;
    private LocalDateTime horaFin;
    private Integer operadores;
    private Integer concentradosProcesados;
}
