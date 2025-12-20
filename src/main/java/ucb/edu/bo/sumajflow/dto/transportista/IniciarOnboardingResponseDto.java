package ucb.edu.bo.sumajflow.dto.transportista;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IniciarOnboardingResponseDto {
    private Integer invitacionId;
    private String primerNombre;
    private String segundoNombre;
    private String primerApellido;
    private String segundoApellido;
    private String numeroCelular;
    private String cooperativaNombre;
    private Boolean codigoEnviado;
    private String mensaje;
}