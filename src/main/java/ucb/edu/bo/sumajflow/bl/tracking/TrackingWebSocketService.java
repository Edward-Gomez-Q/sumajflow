package ucb.edu.bo.sumajflow.bl.tracking;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import ucb.edu.bo.sumajflow.document.TrackingUbicacion;
import ucb.edu.bo.sumajflow.dto.tracking.TrackingResponseDto;

/**
 * Servicio para enviar actualizaciones de tracking por WebSocket
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TrackingWebSocketService {

    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper;

    /**
     * Enviar actualización de tracking a todos los suscriptores de un lote
     *
     * @param loteId ID del lote
     * @param trackingDto Datos de tracking actualizados
     */
    public void enviarActualizacionLote(Integer loteId, TrackingResponseDto trackingDto) {
        try {
            String destination = "/topic/tracking/lote/" + loteId;

            log.debug("📤 Enviando actualización de tracking a lote {} - Destino: {}", loteId, destination);

            messagingTemplate.convertAndSend(destination, trackingDto);

            log.debug("✅ Actualización enviada exitosamente al lote {}", loteId);

        } catch (Exception e) {
            log.error("❌ Error al enviar actualización de tracking al lote {}: {}", loteId, e.getMessage(), e);
        }
    }

    /**
     * Enviar actualización específica de un camión
     *
     * @param asignacionCamionId ID de la asignación del camión
     * @param trackingDto Datos de tracking actualizados
     */
    public void enviarActualizacionCamion(Integer asignacionCamionId, TrackingResponseDto trackingDto) {
        try {
            String destination = "/topic/tracking/camion/" + asignacionCamionId;

            log.debug("📤 Enviando actualización de tracking a camión {} - Destino: {}",
                    asignacionCamionId, destination);

            messagingTemplate.convertAndSend(destination, trackingDto);

            log.debug("✅ Actualización enviada exitosamente al camión {}", asignacionCamionId);

        } catch (Exception e) {
            log.error("❌ Error al enviar actualización de tracking al camión {}: {}",
                    asignacionCamionId, e.getMessage(), e);
        }
    }

    /**
     * Enviar actualización tanto al lote como al camión específico
     *
     * @param loteId ID del lote
     * @param asignacionCamionId ID de la asignación del camión
     * @param trackingDto Datos de tracking actualizados
     */
    public void enviarActualizacionCompleta(Integer loteId, Integer asignacionCamionId, TrackingResponseDto trackingDto) {
        log.info("📡 Enviando actualización completa - Lote: {}, Camión: {}", loteId, asignacionCamionId);

        // Enviar al topic del lote (para vista general)
        enviarActualizacionLote(loteId, trackingDto);

        // Enviar al topic del camión (para vista detallada)
        enviarActualizacionCamion(asignacionCamionId, trackingDto);
    }

    /**
     * Enviar notificación de evento importante (llegada a punto, cambio de estado, etc.)
     *
     * @param loteId ID del lote
     * @param asignacionCamionId ID de la asignación
     * @param tipoEvento Tipo de evento
     * @param mensaje Mensaje descriptivo del evento
     */
    public void enviarEventoTracking(Integer loteId, Integer asignacionCamionId, String tipoEvento, String mensaje) {
        try {
            EventoTrackingDto evento = EventoTrackingDto.builder()
                    .loteId(loteId)
                    .asignacionCamionId(asignacionCamionId)
                    .tipoEvento(tipoEvento)
                    .mensaje(mensaje)
                    .timestamp(java.time.LocalDateTime.now())
                    .build();

            String destinationLote = "/topic/tracking/lote/" + loteId + "/eventos";
            String destinationCamion = "/topic/tracking/camion/" + asignacionCamionId + "/eventos";

            messagingTemplate.convertAndSend(destinationLote, evento);
            messagingTemplate.convertAndSend(destinationCamion, evento);

            log.info("📢 Evento de tracking enviado - Tipo: {}, Lote: {}, Camión: {}",
                    tipoEvento, loteId, asignacionCamionId);

        } catch (Exception e) {
            log.error("❌ Error al enviar evento de tracking: {}", e.getMessage(), e);
        }
    }

    // DTO interno para eventos
    @lombok.Data
    @lombok.Builder
    private static class EventoTrackingDto {
        private Integer loteId;
        private Integer asignacionCamionId;
        private String tipoEvento;
        private String mensaje;
        private java.time.LocalDateTime timestamp;
    }
}