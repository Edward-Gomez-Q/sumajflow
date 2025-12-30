// src/main/java/ucb/edu/bo/sumajflow/dto/socio/AsignacionCamionSimpleDto.java
package ucb.edu.bo.sumajflow.dto.socio;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AsignacionCamionSimpleDto {
    private Integer id;
    private Integer numeroCamion;
    private String estado;
    private LocalDateTime fechaAsignacion;

    // Info del transportista
    private Integer transportistaId;
    private String transportistaNombre;
    private String transportistaPlaca;
    private String transportistaTelefono;
}