package ucb.edu.bo.sumajflow.dto.transportista;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IniciarOnboardingDto {

    @NotBlank(message = "El token es obligatorio")
    private String token;
}