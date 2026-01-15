package ucb.edu.bo.sumajflow.dto.transporte;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List; /**
 * DTO para resumen de lote asignado
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoteAsignadoResumenDto {
    private Integer asignacionId;
    private Integer loteId;
    private String codigoLote;
    private String minaNombre;
    private String destinoNombre;
    private String tipoOperacion;
    private String tipoMineral;
    private String estado;
    private Integer numeroCamion;
    private List<String> mineralTags;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime fechaAsignacion;
}
