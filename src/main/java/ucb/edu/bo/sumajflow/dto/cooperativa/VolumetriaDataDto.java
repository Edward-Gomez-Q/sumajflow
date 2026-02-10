package ucb.edu.bo.sumajflow.dto.cooperativa;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VolumetriaDataDto {
    private BigDecimal pesoTotalDespachadoMes; // en kg
    private Integer camionesDespachadosMes;
    private BigDecimal promedioKgPorCamion;
    private BigDecimal comparativoMesAnterior; // porcentaje
}
