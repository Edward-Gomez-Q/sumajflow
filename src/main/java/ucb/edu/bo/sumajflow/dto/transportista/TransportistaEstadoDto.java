package ucb.edu.bo.sumajflow.dto.transportista;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * DTO para cambiar el estado de un transportista
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransportistaEstadoDto {

    @NotBlank(message = "El estado es requerido")
    @Pattern(regexp = "^(activo|inactivo)$",
            message = "El estado debe ser 'activo' o 'inactivo'")
    private String nuevoEstado;

    private String motivo; // Opcional, para justificar el cambio de estado
}