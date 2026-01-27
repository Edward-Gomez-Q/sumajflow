package ucb.edu.bo.sumajflow.dto.profile;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateTransportistaDataDto {

    @Size(max = 10, message = "La categoría de licencia no puede superar los 10 caracteres")
    private String categoriaLicencia;

    private LocalDate fechaVencimientoLicencia;

    @Size(max = 200, message = "La URL de la licencia no puede superar los 200 caracteres")
    private String licenciaConducir;

    // Datos del vehículo (SOLO no críticos)
    @Size(max = 30, message = "El color no puede superar los 30 caracteres")
    private String colorVehiculo;
}