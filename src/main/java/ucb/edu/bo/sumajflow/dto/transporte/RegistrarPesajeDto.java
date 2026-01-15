package ucb.edu.bo.sumajflow.dto.transporte;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal; /**
 * DTO para registrar pesaje (cooperativa o destino)
 */

@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RegistrarPesajeDto extends EventoUbicacionBaseDto {
    @NotNull(message = "El peso bruto es obligatorio")
    @DecimalMin(value = "0.01", message = "El peso bruto debe ser mayor a 0")
    @Digits(integer = 10, fraction = 2, message = "Formato de peso bruto inválido")
    private BigDecimal pesoBrutoKg;

    @NotNull(message = "El peso tara es obligatorio")
    @DecimalMin(value = "0.01", message = "El peso tara debe ser mayor a 0")
    @Digits(integer = 10, fraction = 2, message = "Formato de peso tara inválido")
    private BigDecimal pesoTaraKg;

    @NotNull(message = "Debe especificar el tipo de pesaje")
    @Pattern(regexp = "cooperativa|destino", message = "Tipo de pesaje debe ser 'cooperativa' o 'destino'")
    private String tipoPesaje;

    @Size(max = 500, message = "La URL del ticket no puede exceder 500 caracteres")
    private String ticketPesajeUrl;
}
