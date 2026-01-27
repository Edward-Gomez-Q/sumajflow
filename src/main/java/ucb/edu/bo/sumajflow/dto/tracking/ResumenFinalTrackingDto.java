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
public class ResumenFinalTrackingDto {
    private Integer asignacionCamionId;
    private Integer loteId;
    private String codigoLote;
    private String placaVehiculo;
    private String nombreTransportista;
    private String estadoViaje;

    // Ubicación final
    private UbicacionDto ubicacionFinal;

    // Métricas del viaje
    private MetricasViajeDto metricas;

    // Puntos de control
    private List<PuntoControlDto> puntosControl;

    // Eventos del viaje
    private List<EventoEstadoDto> eventosEstado;

    // Timestamps
    private LocalDateTime inicioViaje;
    private LocalDateTime finViaje;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}