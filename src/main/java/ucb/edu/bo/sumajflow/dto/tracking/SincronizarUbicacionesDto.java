package ucb.edu.bo.sumajflow.dto.tracking;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SincronizarUbicacionesDto {

    @NotNull(message = "La asignación de camión es requerida")
    private Integer asignacionCamionId;

    @NotNull(message = "Las ubicaciones son requeridas")
    private List<UbicacionOfflineDto> ubicaciones;
}
