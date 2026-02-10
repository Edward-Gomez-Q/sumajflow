package ucb.edu.bo.sumajflow.dto.cooperativa;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BalanzaMonitorDto {
    private Integer id;
    private String tipo; // "cooperativa"
    private String nombre;
    private String estado; // "disponible", "en_uso", "mantenimiento"
    private PesajeActualDto pesajeActual; // null si no está en uso
    private Integer pesajesHoy;
    private Integer tiempoPromedioEspera; // minutos
    private List<ProximoCamionDto> proximosCamiones;
}
