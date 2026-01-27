package ucb.edu.bo.sumajflow.dto.ingenio;

import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConcentradoCreateDto {

    @NotNull(message = "Los lotes son requeridos")
    @Size(min = 1, message = "Debe seleccionar al menos 1 lote")
    private List<Integer> lotesIds;

    @NotNull(message = "El peso inicial es requerido")
    @DecimalMin(value = "0.01", message = "El peso debe ser mayor a 0")
    private BigDecimal pesoInicial;

    @Size(max = 50, message = "El mineral principal no puede exceder 50 caracteres")
    private String mineralPrincipal;

    @Min(value = 1, message = "El número de sacos debe ser mayor a 0")
    private Integer numeroSacos;

    @Size(max = 500, message = "Las observaciones no pueden exceder 500 caracteres")
    private String observacionesIniciales;
}