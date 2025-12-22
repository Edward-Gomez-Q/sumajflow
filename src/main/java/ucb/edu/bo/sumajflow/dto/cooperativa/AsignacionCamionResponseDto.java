package ucb.edu.bo.sumajflow.dto.cooperativa;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AsignacionCamionResponseDto {
    private Integer id;
    private Integer loteId;
    private Integer transportistaId;
    private String transportistaNombre;
    private String placaVehiculo;
    private Integer numeroCamion;
    private String estado;
    private LocalDateTime fechaAsignacion;
}