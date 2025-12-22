package ucb.edu.bo.sumajflow.dto.transporte;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoteTransporteDto {
    private Integer id;
    private String minaNombre;
    private String destinoNombre;
    private String tipoOperacion;
    private String tipoMineral;
    private String estado;
    private Integer camionlesSolicitados;
    private BigDecimal pesoTotalEstimado;
    private LocalDateTime fechaCreacion;

    // Asignaciones de camiones
    private List<AsignacionCamionDetalleDto> asignaciones;

    // Estadísticas de progreso
    private Integer camionesCompletados;
    private BigDecimal pesoTotalReal;
}