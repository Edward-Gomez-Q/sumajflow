package ucb.edu.bo.sumajflow.dto.ingenio;

import lombok.*;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProcesoPlantaDto {

    private Integer id;
    private Integer procesoId;
    private String nombreProceso;
    private String descripcionProceso;
    private Integer orden;
    private String estado; // pendiente, en_proceso, completado
    private LocalDateTime fechaInicio;
    private LocalDateTime fechaFin;
    private String observaciones;
}