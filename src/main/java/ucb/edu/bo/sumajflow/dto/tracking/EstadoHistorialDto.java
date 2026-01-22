package ucb.edu.bo.sumajflow.dto.tracking;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.List;
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EstadoHistorialDto {
    private String estadoViaje;
    private LocalDateTime inicioEstado;
    private LocalDateTime finEstado;
    private Long duracionSegundos;
    private Integer totalUbicaciones;
    private Double distanciaRecorridaKm;
    private Double velocidadPromedioKmH;
    private Double velocidadMaximaKmH;
    private Integer ubicacionesOffline;
    private List<UbicacionDto> ubicaciones;
}
