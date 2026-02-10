package ucb.edu.bo.sumajflow.dto.socio;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AlertaDto {
    private Integer id;
    private String tipo; // "critico", "advertencia", "info"
    private String categoria; // "financiero", "cotizacion", "transporte"
    private String titulo;
    private String descripcion;
    private String accionRecomendada;
    private LocalDateTime timestamp;
}
