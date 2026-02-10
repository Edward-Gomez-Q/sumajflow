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
public class LotePendienteDashboardDto {
    private Integer id;
    private String codigo;
    private String socioNombre;
    private String minaNombre;
    private String tipoOperacion; // "toll" o "venta_directa"
    private Integer camionesSolicitados;
    private BigDecimal pesoEstimado;
    private LocalDateTime fechaCreacion;
    private Long horasEspera;
    private String prioridad; // "alta", "media", "baja"
    private String razonPrioridad;
    private List<ValidacionDto> validaciones;
}
