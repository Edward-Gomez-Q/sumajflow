package ucb.edu.bo.sumajflow.dto.transporte;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * DTO unificado para registrar eventos del viaje
 * POST /transportista/viaje/{asignacionId}/evento
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegistrarEventoDto {

    /**
     * Tipo de evento a registrar:
     * INICIO_VIAJE, LLEGADA_MINA, INICIO_CARGUIO, FIN_CARGUIO,
     * PESAJE_BALANZA_COOP, PESAJE_BALANZA_DESTINO,
     * INICIO_DESCARGA, FIN_DESCARGA
     */
    @NotNull(message = "El tipo de evento es obligatorio")
    private String tipoEvento;

    // Ubicación GPS
    private Double lat;
    private Double lng;
    private Double precision;
    private Double altitud;

    // Comentario/observaciones
    private String comentario;

    // Evidencias fotográficas (URLs de MinIO)
    private List<String> evidencias;

    // Datos específicos de pesaje
    private DatosPesajeDto datosPesaje;

    // Metadatos extra del evento
    private Map<String, Object> metadatosExtra;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DatosPesajeDto {
        private Double pesoBruto;
        private Double pesoTara;
        private String numeroTicket;
        private String observacionesBalanza;
    }
}