package ucb.edu.bo.sumajflow.dto.comercializadora;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ComprasDataDto {
    private Integer pendienteAprobacion;
    private Integer aprobadas;
    private Integer esperandoCierre;
    private Integer esperandoPago;
}
