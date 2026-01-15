package ucb.edu.bo.sumajflow.dto.transporte;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * DTO para confirmar llegada a mina
 */

@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConfirmarLlegadaMinaDto extends EventoUbicacionBaseDto {
    @NotNull(message = "Debe indicar si la pala está operativa")
    private Boolean palaOperativa;

    @NotNull(message = "Debe indicar si el mineral es visible")
    private Boolean mineralVisible;

    @Size(max = 500, message = "La URL de la foto no puede exceder 500 caracteres")
    private String fotoReferenciaUrl;
}
