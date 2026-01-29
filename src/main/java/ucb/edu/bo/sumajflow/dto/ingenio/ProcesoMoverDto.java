package ucb.edu.bo.sumajflow.dto.ingenio;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProcesoMoverDto {

    @NotNull(message = "El ID del proceso destino es requerido")
    private Integer procesoDestinoId;

    private String observaciones;

    private Boolean completarIntermedios; // true = completar procesos intermedios automáticamente
}