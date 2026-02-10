package ucb.edu.bo.sumajflow.dto.cooperativa;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SociosDataDto {
    private Integer totalSocios;
    private Integer sociosActivos;
    private Integer nuevosEsteMes;
    private Integer minasRegistradas;
    private List<MinasPorSectorDto> minasPorSector;
}
