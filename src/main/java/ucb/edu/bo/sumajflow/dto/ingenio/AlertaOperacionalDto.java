package ucb.edu.bo.sumajflow.dto.ingenio;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AlertaOperacionalDto {
    private String tipo; // "mantenimiento", "capacidad", "critico"
    private String severidad; // "alta", "media", "baja"
    private String mensaje;
    private String accion;
}
