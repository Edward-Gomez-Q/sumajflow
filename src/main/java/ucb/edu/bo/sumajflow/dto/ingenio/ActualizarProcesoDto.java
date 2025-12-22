package ucb.edu.bo.sumajflow.dto.ingenio;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ActualizarProcesoDto {
    private String nuevoEstado; // "pendiente", "en_proceso", "completado"
    private String observaciones;
}