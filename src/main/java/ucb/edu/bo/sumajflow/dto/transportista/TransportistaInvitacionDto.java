package ucb.edu.bo.sumajflow.dto.transportista;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para crear invitación con QR
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransportistaInvitacionDto {

    @NotBlank(message = "El primer nombre es obligatorio")
    @Size(min = 2, max = 100, message = "El primer nombre debe tener entre 2 y 100 caracteres")
    private String primerNombre;

    @Size(max = 100, message = "El segundo nombre no puede exceder 100 caracteres")
    private String segundoNombre;

    @NotBlank(message = "El primer apellido es obligatorio")
    @Size(min = 2, max = 100, message = "El primer apellido debe tener entre 2 y 100 caracteres")
    private String primerApellido;

    @Size(max = 100, message = "El segundo apellido no puede exceder 100 caracteres")
    private String segundoApellido;

    @NotBlank(message = "El número de celular es obligatorio")
    @Pattern(regexp = "^\\+591\\d{8}$",
            message = "Formato inválido. Debe ser: +591XXXXXXXX")
    private String numeroCelular;
}