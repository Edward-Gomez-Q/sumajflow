package ucb.edu.bo.sumajflow.dto.socio;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LiquidacionPagoDto {

    @NotBlank(message = "El método de pago es requerido")
    @Size(max = 50, message = "El método de pago no puede exceder 50 caracteres")
    private String metodoPago;

    @NotBlank(message = "El número de comprobante es requerido")
    @Size(max = 100, message = "El número de comprobante no puede exceder 100 caracteres")
    private String numeroComprobante;

    @NotBlank(message = "La URL del comprobante es requerida")
    @Size(max = 500, message = "La URL del comprobante no puede exceder 500 caracteres")
    private String urlComprobante;

    @Size(max = 500, message = "Las observaciones no pueden exceder 500 caracteres")
    private String observaciones;
}