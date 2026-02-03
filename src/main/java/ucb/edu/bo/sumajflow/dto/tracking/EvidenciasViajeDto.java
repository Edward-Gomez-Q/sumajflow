package ucb.edu.bo.sumajflow.dto.tracking;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EvidenciasViajeDto {

    private Integer asignacionCamionId;
    private Integer loteId;
    private String codigoLote;
    private String estadoViaje;
    private Integer numeroCamion;
    private String placaVehiculo;
    private String nombreTransportista;

    // Evidencias por punto de control
    private EvidenciaInicioViaje inicioViaje;
    private EvidenciaLlegadaMina llegadaMina;
    private EvidenciaCarguioCompleto carguioCompleto;
    private EvidenciaPesaje pesajeOrigen;
    private EvidenciaPesaje pesajeDestino;
    private EvidenciaLlegadaAlmacen llegadaAlmacen;
    private EvidenciaDescargaIniciada descargaIniciada;
    private EvidenciaRutaFinalizada rutaFinalizada;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EvidenciaInicioViaje {
        private Double lat;
        private Double lng;
        private LocalDateTime timestamp;
        private Integer usuarioId;
        private String dispositivo;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EvidenciaLlegadaMina {
        private Double lat;
        private Double lng;
        private LocalDateTime timestamp;
        private String observaciones;
        private Boolean palaOperativa;
        private Boolean mineralVisible;
        private String fotoReferenciaUrl;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EvidenciaCarguioCompleto {
        private Double lat;
        private Double lng;
        private LocalDateTime timestamp;
        private String fotoCamionCargadoUrl;
        private Boolean mineralCargadoCompletamente;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EvidenciaPesaje {
        private Double lat;
        private Double lng;
        private LocalDateTime timestamp;
        private Double pesoBrutoKg;
        private Double pesoTaraKg;
        private Double pesoNetoKg;
        private String ticketPesajeUrl;
        private String observaciones;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EvidenciaLlegadaAlmacen {
        private Double lat;
        private Double lng;
        private LocalDateTime timestamp;
        private String observaciones;
        private Boolean confirmacionLlegada;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EvidenciaDescargaIniciada {
        private Double lat;
        private Double lng;
        private LocalDateTime timestamp;
        private String observaciones;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EvidenciaRutaFinalizada {
        private Double lat;
        private Double lng;
        private LocalDateTime timestamp;
        private String observacionesFinales;
    }
}