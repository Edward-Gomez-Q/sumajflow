package ucb.edu.bo.sumajflow.dto.tracking;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO para detalle completo del lote para iniciar viaje
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoteDetalleViajeDto {
    private Integer asignacionId;
    private Integer loteId;
    private String codigoLote;

    // Socio
    private String socioNombre;
    private String socioTelefono;

    // Mina (Punto 1)
    private String minaNombre;
    private Double minaLat;
    private Double minaLng;

    // Viaje
    private String tipoOperacion;
    private String tipoMineral;
    private List<String> mineralTags;
    private String destinoNombre;
    private String destinoTipo;

    // Ruta - Distancia y tiempo
    private Double distanciaEstimadaKm;
    private Double tiempoEstimadoHoras;
    private Boolean rutaCalculadaConExito; // true si usó OSRM, false si es línea recta
    private String metodoCalculo; // "osrm" o "linea_recta"

    // Waypoints de la ruta (para mostrar en el mapa de Flutter)
    private WaypointDto puntoOrigen;           // Mina
    private WaypointDto puntoBalanzaCoop;      // Balanza Cooperativa
    private WaypointDto puntoBalanzaDestino;   // Balanza Destino (Ingenio/Comercializadora)
    private WaypointDto puntoAlmacenDestino;   // Almacén Final

    // Estado
    private String estado;
    private Integer numeroCamion;
    private Integer totalCamiones;

    /**
     * DTO interno para representar un waypoint/punto de la ruta
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WaypointDto {
        private String nombre;
        private String tipo; // "mina", "balanza_coop", "balanza_destino", "almacen"
        private Double latitud;
        private Double longitud;
        private String color; // Color para el marcador en el mapa
        private Integer orden; // 1, 2, 3, 4
    }
}