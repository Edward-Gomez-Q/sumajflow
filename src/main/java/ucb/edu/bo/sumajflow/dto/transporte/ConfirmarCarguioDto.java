package ucb.edu.bo.sumajflow.dto.transporte;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * DTO para confirmar carguío completado
 */

@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConfirmarCarguioDto extends EventoUbicacionBaseDto {
    @NotNull(message = "Debe confirmar si el mineral fue cargado completamente")
    private Boolean mineralCargadoCompletamente;

    @Size(max = 500, message = "La URL de la foto no puede exceder 500 caracteres")
    private String fotoCamionCargadoUrl;
}
