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

/**
 * Documento MongoDB para tracking de ubicaciones en tiempo real
 * Almacena la ubicación actual e historial de un camión durante un viaje
 */
@Document(collection = "tracking_ubicaciones")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@CompoundIndex(name = "lote_transportista_idx", def = "{'loteId': 1, 'transportistaId': 1}")
public class TrackingUbicacion {

    @Id
    private String id;

    // Referencias a PostgreSQL (denormalizadas para queries rápidos)
    @Indexed(unique = true)
    private Integer asignacionCamionId;

    @Indexed
    private Integer loteId;

    @Indexed
    private Integer transportistaId;

    // Información del viaje (denormalizada)
    private String codigoLote;
    private String placaVehiculo;
    private String nombreTransportista;

    // Ubicación actual con índice geoespacial
    private UbicacionActual ubicacionActual;

    // Historial de ubicaciones (array que crece durante el viaje)
    @Builder.Default
    private List<PuntoUbicacion> historialUbicaciones = new ArrayList<>();

    // Estado del viaje
    private String estadoViaje;

    @Builder.Default
    private String estadoConexion = "online";

    private LocalDateTime ultimaSincronizacion;

    // Puntos de control para geofencing
    @Builder.Default
    private List<PuntoControl> puntosControl = new ArrayList<>();

    // Métricas calculadas del viaje
    @Builder.Default
    private MetricasViaje metricas = new MetricasViaje();

    // Datos de sincronización offline
    @Builder.Default
    private List<PuntoUbicacion> ubicacionesPendientesSincronizar = new ArrayList<>();

    // ==================== NUEVO: Eventos de estado ====================
    @Builder.Default
    private List<EventoEstado> eventosEstado = new ArrayList<>();
    // ==================================================================

    // Timestamps
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    private LocalDateTime updatedAt;

    /**
     * Ubicación actual con soporte para índice geoespacial
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UbicacionActual {
        // Formato GeoJSON para índice 2dsphere
        @GeoSpatialIndexed(type = GeoSpatialIndexType.GEO_2DSPHERE)
        private GeoJsonPoint location;

        private Double lat;
        private Double lng;
        private LocalDateTime timestamp;
        private Double precision;      // metros
        private Double velocidad;      // km/h
        private Double rumbo;          // grados 0-360
        private Double altitud;        // metros sobre nivel del mar
    }

    /**
     * Punto de ubicación para el historial
     */
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
        private Boolean sincronizado;  // Para tracking offline

        // ==================== NUEVO ====================
        private Boolean esOffline;     // true si se capturó sin conexión
        // ===============================================
    }

    /**
     * Punto de control para geofencing
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PuntoControl {
        private String tipo;           // mina, balanza_cooperativa, balanza_ingenio, almacen, etc.
        private String nombre;
        private Double lat;
        private Double lng;
        private Integer radio;         // metros para geofencing
        private Integer orden;         // orden en la ruta
        private Boolean requerido;     // si es obligatorio pasar por este punto
        private LocalDateTime llegada;
        private LocalDateTime salida;
        private String estado;         // pendiente, en_punto, completado, omitido
    }

    /**
     * Métricas calculadas del viaje
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MetricasViaje {
        @Builder.Default
        private Double distanciaRecorrida = 0.0;      // km
        @Builder.Default
        private Long tiempoEnMovimiento = 0L;          // segundos
        @Builder.Default
        private Long tiempoDetenido = 0L;              // segundos
        @Builder.Default
        private Double velocidadPromedio = 0.0;        // km/h
        @Builder.Default
        private Double velocidadMaxima = 0.0;          // km/h
        private LocalDateTime inicioViaje;
        private LocalDateTime finViaje;
    }

    // ==================== NUEVO: Evento de cambio de estado ====================
    /**
     * Evento de cambio de estado del viaje
     * Registra cada transición de estado con timestamp, ubicación y tipo
     */
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
        private String tipoEvento;  // INICIO_VIAJE, LLEGADA_MINA, FIN_CARGUIO, etc.
    }
    // ===========================================================================

    /**
     * Clase auxiliar para formato GeoJSON
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GeoJsonPoint {
        @Builder.Default
        private String type = "Point";
        private double[] coordinates;  // [lng, lat] - MongoDB usa este orden

        public static GeoJsonPoint of(double lat, double lng) {
            return GeoJsonPoint.builder()
                    .type("Point")
                    .coordinates(new double[]{lng, lat})
                    .build();
        }
    }
}