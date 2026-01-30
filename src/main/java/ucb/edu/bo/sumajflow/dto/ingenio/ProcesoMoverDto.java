package ucb.edu.bo.sumajflow.dto.ingenio;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProcesoMoverDto {

    @NotNull(message = "El ID del proceso destino es requerido")
    private Integer procesoDestinoId;

    @Size(max = 500, message = "Las observaciones de fin de proceso no pueden exceder 500 caracteres")
    private String observacionesFinProceso;

    @Size(max = 500, message = "Las observaciones de inicio de proceso no pueden exceder 500 caracteres")
    private String observacionesInicioProceso;
}