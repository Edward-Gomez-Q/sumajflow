package ucb.edu.bo.sumajflow.dto.transporte;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * DTO para confirmar llegada a almacén destino
 */

@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConfirmarLlegadaAlmacenDto extends EventoUbicacionBaseDto {
    @NotNull(message = "Debe confirmar la llegada al destino")
    private Boolean confirmacionLlegada;
}
