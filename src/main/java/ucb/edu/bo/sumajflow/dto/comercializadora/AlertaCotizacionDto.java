package ucb.edu.bo.sumajflow.dto.comercializadora;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AlertaCotizacionDto {
    private String mineral;
    private String tipo; // "maximo_alcanzado", "minimo_alcanzado", "volatilidad_alta"
    private String mensaje;
    private String recomendacion;
}
