package ucb.edu.bo.sumajflow.dto.tracking;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List; /**
 * DTO para sincronización offline
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SincronizacionResponseDto {
    private Boolean success;
    private Integer ubicacionesSincronizadas;
    private Integer ubicacionesFallidas;
    private List<String> errores;
    private LocalDateTime ultimaSincronizacion;
}
