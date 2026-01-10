package ucb.edu.bo.sumajflow.dto.transporte;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProximoPuntoControlDto {
    private String tipo;
    private String nombre;
    private Double latitud;
    private Double longitud;
    private Double distanciaMetros;
    private String tiempoEstimado;
}