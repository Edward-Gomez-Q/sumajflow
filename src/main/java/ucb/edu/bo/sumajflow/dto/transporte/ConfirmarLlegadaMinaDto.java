package ucb.edu.bo.sumajflow.dto.transporte;

import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.SuperBuilder;

/**
 * DTO para confirmar llegada a la mina
 */
@EqualsAndHashCode(callSuper = true)
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class ConfirmarLlegadaMinaDto extends TransicionEstadoBaseDto {
    @NotNull(message = "La asignación es requerida")
    private Integer asignacionCamionId;

    // Checklist de validación
    private Boolean palaOperativa;
    private Boolean mineralVisible;
    private Boolean espacioParaCarga;
}
