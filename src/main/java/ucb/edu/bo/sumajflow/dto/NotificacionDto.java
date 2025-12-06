package ucb.edu.bo.sumajflow.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificacionDto {
    private Integer id;
    private String tipo;
    private String titulo;
    private String mensaje;
    private Boolean leido;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime fechaCreacion;

    private String time; // "Hace 5 minutos"
    private Map<String, Object> metadata;
}