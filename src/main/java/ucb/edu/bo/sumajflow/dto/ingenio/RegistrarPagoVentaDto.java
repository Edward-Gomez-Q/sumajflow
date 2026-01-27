package ucb.edu.bo.sumajflow.dto.ingenio;

import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegistrarPagoVentaDto {

    @NotNull(message = "El monto pagado es requerido")
    @DecimalMin(value = "0.01", message = "El monto debe ser mayor a 0")
    private BigDecimal montoPagado;

    @NotNull(message = "La fecha de pago es requerida")
    @PastOrPresent(message = "La fecha de pago no puede ser futura")
    private LocalDate fechaPago;

    @NotBlank(message = "El método de pago es requerido")
    @Size(max = 50, message = "El método de pago no puede exceder 50 caracteres")
    private String metodoPago;

    @Size(max = 100, message = "El número de comprobante no puede exceder 100 caracteres")
    private String numeroComprobante;

    @Size(max = 200, message = "La URL del comprobante no puede exceder 200 caracteres")
    private String urlComprobante;

    @Size(max = 500, message = "Las observaciones no pueden exceder 500 caracteres")
    private String observaciones;
}