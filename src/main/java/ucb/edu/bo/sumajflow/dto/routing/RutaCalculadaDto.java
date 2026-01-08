package ucb.edu.bo.sumajflow.dto.routing;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RutaCalculadaDto {
    private Double distanciaKm;
    private Double tiempoHoras;
    private Boolean exitosa; // true si usó OSRM, false si usó línea recta
    private String metodoCalculo; // "osrm" o "linea_recta"
}