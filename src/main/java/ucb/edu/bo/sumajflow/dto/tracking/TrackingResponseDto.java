package ucb.edu.bo.sumajflow.dto.tracking;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO de respuesta con información completa del tracking
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrackingResponseDto {

    private String id;
    private Integer asignacionCamionId;
    private Integer loteId;
    private Integer transportistaId;

    // Info del viaje
    private String codigoLote;
    private String placaVehiculo;
    private String nombreTransportista;

    // Ubicación actual
    private UbicacionDto ubicacionActual;

    // Estado
    private String estadoViaje;
    private String estadoConexion;
    private LocalDateTime ultimaSincronizacion;

    // Puntos de control
    private List<PuntoControlDto> puntosControl;

    // Métricas
    private MetricasViajeDto metricas;

    // Geofencing
    private GeofencingStatusDto geofencingStatus;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

