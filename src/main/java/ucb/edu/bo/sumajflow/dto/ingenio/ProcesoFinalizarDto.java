package ucb.edu.bo.sumajflow.dto.ingenio;

import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProcesoFinalizarDto {

    @Size(max = 500, message = "Las observaciones de fin de proceso no pueden exceder 500 caracteres")
    private String observacionesFinProceso;

    @Size(max = 1000, message = "Las observaciones generales no pueden exceder 1000 caracteres")
    private String observacionesGenerales;

    // ===== NUEVOS CAMPOS OBLIGATORIOS =====

    @NotNull(message = "El peso TMH es obligatorio")
    @DecimalMin(value = "0.01", message = "El peso TMH debe ser mayor a 0")
    private BigDecimal pesoTmh;

    @NotNull(message = "El peso TMS es obligatorio")
    @DecimalMin(value = "0.01", message = "El peso TMS debe ser mayor a 0")
    private BigDecimal pesoTms;

    @NotNull(message = "El número de sacos es obligatorio")
    @Min(value = 1, message = "El número de sacos debe ser al menos 1")
    private Integer numeroSacos;
}