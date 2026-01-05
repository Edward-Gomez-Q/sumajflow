package ucb.edu.bo.sumajflow.dto.profile;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para cambiar contraseña
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdatePasswordDto {

    @NotBlank(message = "La contraseña actual es requerida")
    private String contrasenaActual;

    @NotBlank(message = "La nueva contraseña es requerida")
    @Size(min = 6, message = "La contraseña debe tener al menos 6 caracteres")
    private String nuevaContrasena;

    @NotBlank(message = "La confirmación de contraseña es requerida")
    private String confirmarContrasena;
}