package ucb.edu.bo.sumajflow.dto.cooperativa;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LotePendienteDto {
    private Integer id;
    private String minaNombre;
    private String socioNombre;
    private String socioCi;
    private List<String> minerales;
    private Integer camionlesSolicitados;
    private String tipoOperacion;
    private String tipoMineral;
    private String destinoNombre;
    private String destinoTipo;
    private BigDecimal pesoTotalEstimado;
    private String estado;
    private LocalDateTime fechaCreacion;
    private String observaciones;
}