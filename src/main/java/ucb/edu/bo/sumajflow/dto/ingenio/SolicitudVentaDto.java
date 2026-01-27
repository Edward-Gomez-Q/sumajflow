package ucb.edu.bo.sumajflow.dto.ingenio;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SolicitudVentaDto {

    @NotNull(message = "La comercializadora es requerida")
    private Integer comercializadoraId;

    @Size(max = 500, message = "Las observaciones no pueden exceder 500 caracteres")
    private String observaciones;
}