package ucb.edu.bo.sumajflow.dto.transporte;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * DTO para iniciar viaje
 */
@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
public class IniciarViajeDto extends EventoUbicacionBaseDto {
    // Hereda lat, lng, observaciones
}
