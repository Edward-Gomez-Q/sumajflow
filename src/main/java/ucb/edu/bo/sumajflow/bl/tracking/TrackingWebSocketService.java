package ucb.edu.bo.sumajflow.bl.tracking;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import ucb.edu.bo.sumajflow.dto.tracking.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Servicio para enviar actualizaciones de tracking en tiempo real via WebSocket
 * Permite que el frontend web vea los camiones moviéndose en el mapa
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TrackingWebSocketService {

    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Notifica actualización de ubicación a todos los suscriptores del lote
     * Canal: /topic/tracking/lote/{loteId}
     */
    public void notificarActualizacionUbicacion(Integer loteId, CamionEnRutaDto camion) {
        try {
            String destino = "/topic/tracking/lote/" + loteId;

            Map<String, Object> mensaje = new HashMap<>();
            mensaje.put("tipo", "UBICACION_ACTUALIZADA");
            mensaje.put("data", camion);
            mensaje.put("timestamp", System.currentTimeMillis());

            messagingTemplate.convertAndSend(destino, mensaje);

            log.debug("Ubicación enviada por WebSocket - Lote: {}, Camión: {}",
                    loteId, camion.getPlacaVehiculo());
        } catch (Exception e) {
            log.error("Error al enviar ubicación por WebSocket: {}", e.getMessage());
        }
    }

    /**
     * Notifica llegada a punto de control
     * Canal: /topic/tracking/lote/{loteId}
     */
    public void notificarLlegadaPuntoControl(Integer loteId, Integer asignacionId,
                                             String tipoPunto, String nombrePunto) {
        try {
            String destino = "/topic/tracking/lote/" + loteId;

            Map<String, Object> mensaje = new HashMap<>();
            mensaje.put("tipo", "LLEGADA_PUNTO_CONTROL");
            mensaje.put("asignacionCamionId", asignacionId);
            mensaje.put("tipoPunto", tipoPunto);
            mensaje.put("nombrePunto", nombrePunto);
            mensaje.put("timestamp", System.currentTimeMillis());

            messagingTemplate.convertAndSend(destino, mensaje);

            log.info("Notificación de llegada enviada - Lote: {}, Punto: {}", loteId, nombrePunto);
        } catch (Exception e) {
            log.error("Error al notificar llegada: {}", e.getMessage());
        }
    }

    /**
     * Notifica salida de punto de control
     */
    public void notificarSalidaPuntoControl(Integer loteId, Integer asignacionId,
                                            String tipoPunto, String nombrePunto) {
        try {
            String destino = "/topic/tracking/lote/" + loteId;

            Map<String, Object> mensaje = new HashMap<>();
            mensaje.put("tipo", "SALIDA_PUNTO_CONTROL");
            mensaje.put("asignacionCamionId", asignacionId);
            mensaje.put("tipoPunto", tipoPunto);
            mensaje.put("nombrePunto", nombrePunto);
            mensaje.put("timestamp", System.currentTimeMillis());

            messagingTemplate.convertAndSend(destino, mensaje);

            log.info("Notificación de salida enviada - Lote: {}, Punto: {}", loteId, nombrePunto);
        } catch (Exception e) {
            log.error("Error al notificar salida: {}", e.getMessage());
        }
    }

    /**
     * Notifica cambio de estado del camión
     */
    public void notificarCambioEstado(Integer loteId, Integer asignacionId,
                                      String estadoAnterior, String estadoNuevo) {
        try {
            String destino = "/topic/tracking/lote/" + loteId;

            Map<String, Object> mensaje = new HashMap<>();
            mensaje.put("tipo", "CAMBIO_ESTADO");
            mensaje.put("asignacionCamionId", asignacionId);
            mensaje.put("estadoAnterior", estadoAnterior);
            mensaje.put("estadoNuevo", estadoNuevo);
            mensaje.put("timestamp", System.currentTimeMillis());

            messagingTemplate.convertAndSend(destino, mensaje);

            log.info("Notificación de cambio de estado - Lote: {}, Estado: {} -> {}",
                    loteId, estadoAnterior, estadoNuevo);
        } catch (Exception e) {
            log.error("Error al notificar cambio de estado: {}", e.getMessage());
        }
    }

    /**
     * Notifica que un camión se ha desconectado (offline)
     */
    public void notificarCamionOffline(Integer loteId, Integer asignacionId, String placaVehiculo) {
        try {
            String destino = "/topic/tracking/lote/" + loteId;

            Map<String, Object> mensaje = new HashMap<>();
            mensaje.put("tipo", "CAMION_OFFLINE");
            mensaje.put("asignacionCamionId", asignacionId);
            mensaje.put("placaVehiculo", placaVehiculo);
            mensaje.put("timestamp", System.currentTimeMillis());

            messagingTemplate.convertAndSend(destino, mensaje);

            log.warn("Camión offline notificado - Lote: {}, Placa: {}", loteId, placaVehiculo);
        } catch (Exception e) {
            log.error("Error al notificar camión offline: {}", e.getMessage());
        }
    }

    /**
     * Notifica que un camión se ha reconectado (online)
     */
    public void notificarCamionOnline(Integer loteId, Integer asignacionId, String placaVehiculo) {
        try {
            String destino = "/topic/tracking/lote/" + loteId;

            Map<String, Object> mensaje = new HashMap<>();
            mensaje.put("tipo", "CAMION_ONLINE");
            mensaje.put("asignacionCamionId", asignacionId);
            mensaje.put("placaVehiculo", placaVehiculo);
            mensaje.put("timestamp", System.currentTimeMillis());

            messagingTemplate.convertAndSend(destino, mensaje);

            log.info("Camión online notificado - Lote: {}, Placa: {}", loteId, placaVehiculo);
        } catch (Exception e) {
            log.error("Error al notificar camión online: {}", e.getMessage());
        }
    }

    /**
     * Notifica viaje completado
     */
    public void notificarViajeCompletado(Integer loteId, Integer asignacionId,
                                         MetricasViajeDto metricas) {
        try {
            String destino = "/topic/tracking/lote/" + loteId;

            Map<String, Object> mensaje = new HashMap<>();
            mensaje.put("tipo", "VIAJE_COMPLETADO");
            mensaje.put("asignacionCamionId", asignacionId);
            mensaje.put("metricas", metricas);
            mensaje.put("timestamp", System.currentTimeMillis());

            messagingTemplate.convertAndSend(destino, mensaje);

            log.info("Viaje completado notificado - Lote: {}, Asignación: {}", loteId, asignacionId);
        } catch (Exception e) {
            log.error("Error al notificar viaje completado: {}", e.getMessage());
        }
    }

    /**
     * Envía el estado completo del monitoreo (útil cuando un cliente se conecta)
     */
    public void enviarEstadoCompleto(Integer loteId, MonitoreoLoteDto monitoreo) {
        try {
            String destino = "/topic/tracking/lote/" + loteId;

            Map<String, Object> mensaje = new HashMap<>();
            mensaje.put("tipo", "ESTADO_COMPLETO");
            mensaje.put("data", monitoreo);
            mensaje.put("timestamp", System.currentTimeMillis());

            messagingTemplate.convertAndSend(destino, mensaje);

            log.debug("Estado completo enviado - Lote: {}", loteId);
        } catch (Exception e) {
            log.error("Error al enviar estado completo: {}", e.getMessage());
        }
    }

    /**
     * Notifica a un usuario específico (para el transportista)
     * Canal: /user/{userId}/queue/tracking
     */
    public void notificarUsuario(Integer usuarioId, String tipo, Object data) {
        try {
            Map<String, Object> mensaje = new HashMap<>();
            mensaje.put("tipo", tipo);
            mensaje.put("data", data);
            mensaje.put("timestamp", System.currentTimeMillis());

            messagingTemplate.convertAndSendToUser(
                    usuarioId.toString(),
                    "/queue/tracking",
                    mensaje
            );

            log.debug("Notificación enviada a usuario: {}", usuarioId);
        } catch (Exception e) {
            log.error("Error al notificar usuario: {}", e.getMessage());
        }
    }
}