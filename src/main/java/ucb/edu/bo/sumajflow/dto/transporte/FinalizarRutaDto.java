package ucb.edu.bo.sumajflow.dto.transporte;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * DTO para finalizar ruta
 */

@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FinalizarRutaDto extends EventoUbicacionBaseDto {
    @Size(max = 500, message = "Las observaciones finales no pueden exceder 500 caracteres")
    private String observacionesFinales;
}
