package ucb.edu.bo.sumajflow.dto.comercializadora;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PipelineEtapaDto {
    private String nombre;
    private String estado;
    private String color;
    private List<LiquidacionPipelineDto> liquidaciones;
    private MetricasEtapaDto metricas;
}
