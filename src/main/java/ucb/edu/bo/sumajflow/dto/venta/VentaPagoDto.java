package ucb.edu.bo.sumajflow.dto.venta;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

/**
 * DTO para que la comercializadora confirme el pago
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VentaPagoDto {

    @NotBlank(message = "El método de pago es requerido")
    private String metodoPago;

    @NotBlank(message = "El número de comprobante es requerido")
    private String numeroComprobante;

    @NotBlank(message = "La URL del comprobante es requerida")
    private String urlComprobante;

    private String observaciones;
}