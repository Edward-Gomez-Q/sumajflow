package ucb.edu.bo.sumajflow.document;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.GeoSpatialIndexType;
import org.springframework.data.mongodb.core.index.GeoSpatialIndexed;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Document(collection = "tracking_ubicaciones")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@CompoundIndex(name = "lote_transportista_idx", def = "{'loteId': 1, 'transportistaId': 1}")
public class TrackingUbicacion {

    @Id
    private String id;

    @Indexed(unique = true)
    private Integer asignacionCamionId;

    @Indexed
    private Integer loteId;

    @Indexed
    private Integer transportistaId;

    private String codigoLote;
    private String placaVehiculo;
    private String nombreTransportista;

    private UbicacionActual ubicacionActual;

    @Builder.Default
    private List<PuntoUbicacion> historialUbicaciones = new ArrayList<>();

    private String estadoViaje;

    @Builder.Default
    private String estadoConexion = "online";

    private LocalDateTime ultimaSincronizacion;

    @Builder.Default
    private List<PuntoControl> puntosControl = new ArrayList<>();

    @Builder.Default
    private MetricasViaje metricas = new MetricasViaje();

    @Builder.Default
    private List<EventoEstado> eventosEstado = new ArrayList<>();

    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    private LocalDateTime updatedAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UbicacionActual {
        @GeoSpatialIndexed(type = GeoSpatialIndexType.GEO_2DSPHERE)
        private GeoJsonPoint location;

        private Double lat;
        private Double lng;
        private LocalDateTime timestamp;
        private Double precision;
        private Double velocidad;
        private Double rumbo;
        private Double altitud;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PuntoUbicacion {
        private Double lat;
        private Double lng;
        private LocalDateTime timestamp;
        private Double precision;
        private Double velocidad;
        private Double rumbo;
        private Double altitud;
        private Boolean sincronizado;
        private Boolean esOffline;
        private String estadoViaje;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PuntoControl {
        private String tipo;
        private String nombre;
        private Double lat;
        private Double lng;
        private Integer radio;
        private Integer orden;
        private Boolean requerido;
        private LocalDateTime llegada;
        private LocalDateTime salida;
        private String estado;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MetricasViaje {
        @Builder.Default
        private Double distanciaRecorrida = 0.0;
        @Builder.Default
        private Long tiempoEnMovimiento = 0L;
        @Builder.Default
        private Long tiempoDetenido = 0L;
        @Builder.Default
        private Double velocidadPromedio = 0.0;
        @Builder.Default
        private Double velocidadMaxima = 0.0;
        private LocalDateTime inicioViaje;
        private LocalDateTime finViaje;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EventoEstado {
        private LocalDateTime timestamp;
        private String estadoAnterior;
        private String estadoNuevo;
        private Double lat;
        private Double lng;
        private String tipoEvento;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GeoJsonPoint {
        @Builder.Default
        private String type = "Point";
        private double[] coordinates;

        public static GeoJsonPoint of(double lat, double lng) {
            return GeoJsonPoint.builder()
                    .type("Point")
                    .coordinates(new double[]{lng, lat})
                    .build();
        }
    }
}