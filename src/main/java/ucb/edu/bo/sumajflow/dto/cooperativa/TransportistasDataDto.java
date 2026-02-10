package ucb.edu.bo.sumajflow.dto.cooperativa;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TransportistasDataDto {
    private Integer totalDisponibles;
    private Integer enRuta;
    private Integer completadosHoy;
    private BigDecimal calificacionPromedio;
    private Integer viajesCompletadosMes;
}
