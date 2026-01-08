package ucb.edu.bo.sumajflow.dto.tracking;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoteAsignadoResumenDto {
    private Integer asignacionId;
    private Integer loteId;
    private String codigoLote;
    private String minaNombre;
    private String tipoOperacion;
    private String tipoMineral;
    private String estado;
    private Integer numeroCamion;
    private java.time.LocalDateTime fechaAsignacion;
    private List<String> mineralTags;
}
