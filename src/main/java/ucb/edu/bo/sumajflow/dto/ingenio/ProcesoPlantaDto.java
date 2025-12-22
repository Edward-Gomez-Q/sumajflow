package ucb.edu.bo.sumajflow.dto.ingenio;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProcesoPlantaDto {
    private Integer id;
    private Integer procesoId;
    private String procesoNombre;
    private Integer orden;
    private String estado; // "pendiente", "en_proceso", "completado"
    private LocalDateTime fechaInicio;
    private LocalDateTime fechaFin;
    private String observaciones;
}