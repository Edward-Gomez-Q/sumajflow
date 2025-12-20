package ucb.edu.bo.sumajflow.dto.transportista;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class VerificarCodigoLoginDto {
    @NotBlank(message = "El número de celular es requerido")
    private String numeroCelular;

    @NotBlank(message = "El código es requerido")
    @Pattern(regexp = "^[0-9]{6}$", message = "El código debe tener 6 dígitos")
    private String codigo;
}