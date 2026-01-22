// src/main/java/ucb/edu/bo/sumajflow/dto/socio/AuditoriaLoteDto.java
package ucb.edu.bo.sumajflow.dto.socio;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuditoriaLoteDto {
    private Integer id;
    private String estadoAnterior;
    private String estadoNuevo;
    private String accion;
    private String descripcion;
    private String observaciones;
    private LocalDateTime fechaRegistro;
    private String tipoUsuario;
    private Map<String, String> metadata;
}