package ucb.edu.bo.sumajflow.dto.socio;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SocioEstadoDto {
    private Integer socioId;
    private String estado;
    private LocalDateTime fechaEnvio;
    private LocalDate fechaAfiliacion;
    private String solicitudId;
    private CooperativaInfoDto cooperativa;
    private String mensaje;
}