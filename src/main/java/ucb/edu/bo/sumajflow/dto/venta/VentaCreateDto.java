package ucb.edu.bo.sumajflow.dto.venta;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.List;

/**
 * DTO para crear una liquidación de venta de concentrado o lote complejo
 * El socio selecciona concentrados/lotes y una comercializadora
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VentaCreateDto {

    @NotNull(message = "Debe seleccionar una comercializadora")
    private Integer comercializadoraId;

    /**
     * IDs de concentrados a vender (para venta_concentrado)
     * Al menos uno de concentradosIds o lotesIds debe tener elementos
     */
    private List<Integer> concentradosIds;

    /**
     * IDs de lotes complejos a vender (para venta_lote_complejo)
     */
    private List<Integer> lotesIds;

    private String observaciones;
}