package ucb.edu.bo.sumajflow.dto.cooperativa;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ValidacionDto {
    private String nombre;
    private String estado; // "ok", "advertencia", "error"
    private String mensaje;
}
