package ucb.edu.bo.sumajflow.dto.ingenio;

import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AprobarLiquidacionServicioDto {

    @NotNull(message = "El costo del servicio es requerido")
    @DecimalMin(value = "0.01", message = "El costo debe ser mayor a 0")
    private BigDecimal costoServicio;

    @Size(max = 500, message = "Las observaciones no pueden exceder 500 caracteres")
    private String observaciones;

    @Size(max = 200, message = "La URL del documento no puede exceder 200 caracteres")
    private String urlDocumentoLiquidacion;
}