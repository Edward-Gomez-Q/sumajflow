package ucb.edu.bo.sumajflow.dto.transportista;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
/**
 * DTO para reenviar código
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReenviarCodigoDto {

    @NotBlank(message = "El token es obligatorio")
    private String token;
}