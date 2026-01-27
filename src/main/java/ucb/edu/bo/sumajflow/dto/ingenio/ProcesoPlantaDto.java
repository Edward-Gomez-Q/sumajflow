package ucb.edu.bo.sumajflow.dto.ingenio;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProcesoPlantaDto {
    private Integer id;
    private Integer procesoId;
    private String nombreProceso;
    private Integer orden;
    private String estado; // "pendiente", "en_proceso", "completado"
    private LocalDateTime fechaInicio;
    private LocalDateTime fechaFin;
    private String observaciones;
}