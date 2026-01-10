package ucb.edu.bo.sumajflow.dto.transporte;

import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.SuperBuilder;

/**
 * DTO para confirmar carguío completo
 */
@EqualsAndHashCode(callSuper = true)
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class ConfirmarCarguioDto extends TransicionEstadoBaseDto {
    @NotNull(message = "La asignación es requerida")
    private Integer asignacionCamionId;

    private Double pesoEstimadoKg;
}
