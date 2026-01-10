package ucb.edu.bo.sumajflow.dto.transporte;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor; /**
 * DTO para registrar pesaje
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegistrarPesajeDto {
    @NotNull(message = "La asignación es requerida")
    private Integer asignacionCamionId;

    @NotNull(message = "El tipo de pesaje es requerido")
    @Pattern(regexp = "cooperativa|destino", message = "Tipo de pesaje inválido")
    private String tipoPesaje;

    @NotNull(message = "El peso bruto es requerido")
    @DecimalMin(value = "0.0", message = "Peso bruto debe ser positivo")
    private Double pesoBrutoKg;

    @NotNull(message = "El peso tara es requerido")
    @DecimalMin(value = "0.0", message = "Peso tara debe ser positivo")
    private Double pesoTaraKg;

    private Double lat;
    private Double lng;
    private String ticketPesajeUrl;
    private String observaciones;
}
