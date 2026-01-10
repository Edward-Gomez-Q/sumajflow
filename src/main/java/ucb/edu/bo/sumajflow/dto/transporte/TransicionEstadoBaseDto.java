package ucb.edu.bo.sumajflow.dto.transporte;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.List;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class TransicionEstadoBaseDto {

    @NotNull(message = "La latitud es requerida")
    @DecimalMin(value = "-90.0", message = "Latitud inválida")
    @DecimalMax(value = "90.0", message = "Latitud inválida")
    private Double lat;

    @NotNull(message = "La longitud es requerida")
    @DecimalMin(value = "-180.0", message = "Longitud inválida")
    @DecimalMax(value = "180.0", message = "Longitud inválida")
    private Double lng;

    private String observaciones;
    private List<String> fotosUrls;
}
