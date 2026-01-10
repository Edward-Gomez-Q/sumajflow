package ucb.edu.bo.sumajflow.dto.transporte;

import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.SuperBuilder;

/**
 * DTO para confirmar descarga
 */
@EqualsAndHashCode(callSuper = true)
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class ConfirmarDescargaDto extends TransicionEstadoBaseDto {
    @NotNull(message = "La asignación es requerida")
    private Integer asignacionCamionId;

    private String firmaReceptor;
}
