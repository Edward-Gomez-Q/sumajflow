package ucb.edu.bo.sumajflow.dto.ingenio;

import jakarta.validation.constraints.Size;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProcesoFinalizarDto {

    @Size(max = 500, message = "Las observaciones de fin de proceso no pueden exceder 500 caracteres")
    private String observacionesFinProceso;

    @Size(max = 1000, message = "Las observaciones generales no pueden exceder 1000 caracteres")
    private String observacionesGenerales;
}