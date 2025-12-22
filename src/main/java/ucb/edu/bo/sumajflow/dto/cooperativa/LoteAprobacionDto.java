package ucb.edu.bo.sumajflow.dto.cooperativa;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoteAprobacionDto {
    private List<AsignacionTransportistaDto> asignaciones; // Lista de transportistas asignados
    private LocalDate fechaAsignacion; // Fecha tentativa para iniciar transporte
    private String observaciones; // Observaciones de la cooperativa
}