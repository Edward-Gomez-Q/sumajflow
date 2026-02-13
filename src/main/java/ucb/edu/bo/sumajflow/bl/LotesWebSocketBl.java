package ucb.edu.bo.sumajflow.bl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import ucb.edu.bo.sumajflow.dto.socio.LoteDetalleDto;
import ucb.edu.bo.sumajflow.dto.socio.LoteResponseDto;
import ucb.edu.bo.sumajflow.entity.*;
import ucb.edu.bo.sumajflow.repository.LoteComercializadoraRepository;
import ucb.edu.bo.sumajflow.repository.LoteIngenioRepository;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Servicio centralizado para eventos WebSocket de lotes
 *
 * ARQUITECTURA:
 * - /user/{usuarioId}/queue/lotes → Lista de lotes (payload ligero)
 * - /topic/lote/{loteId} → Detalle de lote (payload completo)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LotesWebSocketBl {

    private final SimpMessagingTemplate messagingTemplate;
    private final LoteIngenioRepository loteIngenioRepository;
    private final LoteComercializadoraRepository loteComercializadoraRepository;
    private final NotificacionBl notificacionBl;

    // ==================== CREACIÓN DE LOTE ====================

    /**
     * Evento: LOTE CREADO
     * Notificar a: Cooperativa + Destino
     * NO notificar a: Socio (él lo creó)
     */
    public void publicarCreacionLote(Lotes lote, LoteResponseDto loteDto) {
        try {
            LocalDateTime now = LocalDateTime.now();

            // ========== PAYLOAD LIGERO PARA LISTAS ==========
            Map<String, Object> payloadLigero = new HashMap<>();
            payloadLigero.put("evento", "lote_creado");
            payloadLigero.put("loteId", lote.getId());
            payloadLigero.put("estado", lote.getEstado());
            payloadLigero.put("tipoOperacion", lote.getTipoOperacion());
            payloadLigero.put("tipoMineral", lote.getTipoMineral());
            payloadLigero.put("minaNombre", lote.getMinasId().getNombre());
            payloadLigero.put("timestamp", now.toString());

            // Notificar a la cooperativa
            Integer cooperativaUsuarioId = lote.getMinasId()
                    .getSectoresId()
                    .getCooperativaId()
                    .getUsuariosId()
                    .getId();

            enviarAUsuario(cooperativaUsuarioId, payloadLigero);
            String tipoOperacion = "procesamiento_planta".equals(lote.getTipoOperacion()) ? "Procesamiento en Planta" : "Venta Directa";
            notificacionBl.crearNotificacion(cooperativaUsuarioId, "info", "Nuevo lote creado: " + lote.getId(), "El socio ha creado un nuevo lote con ID " + lote.getId() + " y tipo de operación " + tipoOperacion, payloadLigero);
            log.debug("📤 Notificado cooperativa (usuario {}): lote_creado", cooperativaUsuarioId);

            // Notificar al destino (ingenio o comercializadora)
            Integer destinoUsuarioId = obtenerUsuarioIdDestino(lote);
            if (destinoUsuarioId != null) {
                enviarAUsuario(destinoUsuarioId, payloadLigero);
                notificacionBl.crearNotificacion(destinoUsuarioId, "info", "Nuevo lote creado: " + lote.getId(), "Se ha creado una solicitud de lote con destino al almacen, pendiente de aprobación" , payloadLigero);
                log.debug("📤 Notificado destino (usuario {}): lote_creado", destinoUsuarioId);
            }

            // ========== PAYLOAD COMPLETO PARA DETALLE ==========
            Map<String, Object> payloadCompleto = new HashMap<>();
            payloadCompleto.put("evento", "lote_creado");
            payloadCompleto.put("timestamp", now.toString());
            payloadCompleto.put("lote", loteDto);

            enviarADetalle(lote.getId(), payloadCompleto);

            log.info("✅ WebSocket enviado - lote_creado, ID: {}", lote.getId());

        } catch (Exception e) {
            log.error("❌ Error al publicar creación lote ID: {}", lote.getId(), e);
        }
    }

    // ==================== APROBACIÓN COOPERATIVA ====================

    /**
     * Evento: LOTE APROBADO POR COOPERATIVA
     * Notificar a: Socio + Destino
     * NO notificar a: Cooperativa (ella lo aprobó)
     */
    public void publicarAprobacionCooperativa(Lotes lote, LoteDetalleDto loteDto, Integer cooperativaUsuarioId) {
        try {
            LocalDateTime now = LocalDateTime.now();

            // ========== PAYLOAD LIGERO ==========
            Map<String, Object> payloadLigero = new HashMap<>();
            payloadLigero.put("evento", "lote_aprobado_cooperativa");
            payloadLigero.put("loteId", lote.getId());
            payloadLigero.put("estado", lote.getEstado());
            payloadLigero.put("tipoOperacion", lote.getTipoOperacion());
            payloadLigero.put("minaNombre", lote.getMinasId().getNombre());
            payloadLigero.put("camioneAsignados", loteDto.getCamioneAsignados());
            payloadLigero.put("timestamp", now.toString());

            // Notificar al socio
            Integer socioUsuarioId = lote.getMinasId().getSocioId().getUsuariosId().getId();
            enviarAUsuario(socioUsuarioId, payloadLigero);
            log.debug("📤 Notificado socio (usuario {}): lote_aprobado_cooperativa", socioUsuarioId);

            //Notificar al destino
            Integer destinoUsuarioId = obtenerUsuarioIdDestino(lote);
            if (destinoUsuarioId != null && !destinoUsuarioId.equals(cooperativaUsuarioId)) {
                enviarAUsuario(destinoUsuarioId, payloadLigero);
                log.debug("📤 Notificado destino (usuario {}): lote_aprobado_cooperativa", destinoUsuarioId);
            }

            // ========== PAYLOAD COMPLETO ==========
            Map<String, Object> payloadCompleto = new HashMap<>();
            payloadCompleto.put("evento", "lote_aprobado_cooperativa");
            payloadCompleto.put("timestamp", now.toString());
            payloadCompleto.put("lote", loteDto);

            enviarADetalle(lote.getId(), payloadCompleto);

            log.info("✅ WebSocket enviado - lote_aprobado_cooperativa, ID: {}", lote.getId());

        } catch (Exception e) {
            log.error("❌ Error al publicar aprobación cooperativa lote ID: {}", lote.getId(), e);
        }
    }

    // ==================== RECHAZO COOPERATIVA ====================

    /**
     * Evento: LOTE RECHAZADO POR COOPERATIVA
     * Notificar a: Socio + Destino
     * NO notificar a: Cooperativa (ella lo rechazó)
     */
    public void publicarRechazoCooperativa(Lotes lote, String motivoRechazo, Integer cooperativaUsuarioId) {
        try {
            LocalDateTime now = LocalDateTime.now();

            Map<String, Object> payloadLigero = new HashMap<>();
            payloadLigero.put("evento", "lote_rechazado_cooperativa");
            payloadLigero.put("loteId", lote.getId());
            payloadLigero.put("estado", lote.getEstado());
            payloadLigero.put("motivoRechazo", motivoRechazo);
            payloadLigero.put("minaNombre", lote.getMinasId().getNombre());
            payloadLigero.put("timestamp", now.toString());

            // Notificar al socio
            Integer socioUsuarioId = lote.getMinasId().getSocioId().getUsuariosId().getId();
            enviarAUsuario(socioUsuarioId, payloadLigero);

            //  Notificar al destino
            Integer destinoUsuarioId = obtenerUsuarioIdDestino(lote);
            if (destinoUsuarioId != null && !destinoUsuarioId.equals(cooperativaUsuarioId)) {
                enviarAUsuario(destinoUsuarioId, payloadLigero);
            }

            // Payload completo
            Map<String, Object> payloadCompleto = new HashMap<>();
            payloadCompleto.put("evento", "lote_rechazado_cooperativa");
            payloadCompleto.put("timestamp", now.toString());
            payloadCompleto.put("motivoRechazo", motivoRechazo);

            enviarADetalle(lote.getId(), payloadCompleto);

            log.info("✅ WebSocket enviado - lote_rechazado_cooperativa, ID: {}", lote.getId());

        } catch (Exception e) {
            log.error("❌ Error al publicar rechazo cooperativa lote ID: {}", lote.getId(), e);
        }
    }

    // ==================== APROBACIÓN DESTINO ====================

    /**
     * Evento: LOTE APROBADO POR DESTINO (Ingenio/Comercializadora)
     * Notificar a: Socio + Cooperativa
     * NO notificar a: Destino (él lo aprobó)
     */
    public void publicarAprobacionDestino(Lotes lote, LoteDetalleDto loteDto, Integer destinoUsuarioId) {
        try {
            LocalDateTime now = LocalDateTime.now();

            Map<String, Object> payloadLigero = new HashMap<>();
            payloadLigero.put("evento", "lote_aprobado_destino");
            payloadLigero.put("loteId", lote.getId());
            payloadLigero.put("estado", lote.getEstado());
            payloadLigero.put("tipoOperacion", lote.getTipoOperacion());
            payloadLigero.put("minaNombre", lote.getMinasId().getNombre());
            payloadLigero.put("timestamp", now.toString());

            // Notificar al socio
            Integer socioUsuarioId = lote.getMinasId().getSocioId().getUsuariosId().getId();
            enviarAUsuario(socioUsuarioId, payloadLigero);

            // Notificar a la cooperativa
            Integer cooperativaUsuarioId = lote.getMinasId()
                    .getSectoresId()
                    .getCooperativaId()
                    .getUsuariosId()
                    .getId();
            enviarAUsuario(cooperativaUsuarioId, payloadLigero);

            // Payload completo
            Map<String, Object> payloadCompleto = new HashMap<>();
            payloadCompleto.put("evento", "lote_aprobado_destino");
            payloadCompleto.put("timestamp", now.toString());
            payloadCompleto.put("lote", loteDto);

            enviarADetalle(lote.getId(), payloadCompleto);

            log.info("✅ WebSocket enviado - lote_aprobado_destino, ID: {}", lote.getId());

        } catch (Exception e) {
            log.error("❌ Error al publicar aprobación destino lote ID: {}", lote.getId(), e);
        }
    }

    // ==================== RECHAZO DESTINO ====================

    /**
     * Evento: LOTE RECHAZADO POR DESTINO
     * Notificar a: Socio + Cooperativa
     * NO notificar a: Destino (él lo rechazó)
     */
    public void publicarRechazoDestino(Lotes lote, String motivoRechazo, Integer destinoUsuarioId) {
        try {
            LocalDateTime now = LocalDateTime.now();

            Map<String, Object> payloadLigero = new HashMap<>();
            payloadLigero.put("evento", "lote_rechazado_destino");
            payloadLigero.put("loteId", lote.getId());
            payloadLigero.put("estado", lote.getEstado());
            payloadLigero.put("motivoRechazo", motivoRechazo);
            payloadLigero.put("minaNombre", lote.getMinasId().getNombre());
            payloadLigero.put("timestamp", now.toString());

            // Notificar al socio
            Integer socioUsuarioId = lote.getMinasId().getSocioId().getUsuariosId().getId();
            enviarAUsuario(socioUsuarioId, payloadLigero);

            // Notificar a la cooperativa
            Integer cooperativaUsuarioId = lote.getMinasId()
                    .getSectoresId()
                    .getCooperativaId()
                    .getUsuariosId()
                    .getId();
            enviarAUsuario(cooperativaUsuarioId, payloadLigero);

            // Payload completo
            Map<String, Object> payloadCompleto = new HashMap<>();
            payloadCompleto.put("evento", "lote_rechazado_destino");
            payloadCompleto.put("timestamp", now.toString());
            payloadCompleto.put("motivoRechazo", motivoRechazo);

            enviarADetalle(lote.getId(), payloadCompleto);

            log.info("✅ WebSocket enviado - lote_rechazado_destino, ID: {}", lote.getId());

        } catch (Exception e) {
            log.error("❌ Error al publicar rechazo destino lote ID: {}", lote.getId(), e);
        }
    }

    // ==================== TRANSPORTE ====================

    /**
     * Evento: TRANSPORTE INICIADO
     * Notificar a: Socio + Cooperativa + Destino
     */
    public void publicarInicioTransporte(Lotes lote, Integer numeroCamion) {
        try {
            LocalDateTime now = LocalDateTime.now();

            Map<String, Object> payload = new HashMap<>();
            payload.put("evento", "transporte_iniciado");
            payload.put("loteId", lote.getId());
            payload.put("estado", lote.getEstado());
            payload.put("numeroCamion", numeroCamion);
            payload.put("minaNombre", lote.getMinasId().getNombre());
            payload.put("timestamp", now.toString());

            // Notificar a todos
            notificarATodos(lote, payload);
            enviarADetalle(lote.getId(), payload);

            log.info("✅ WebSocket enviado - transporte_iniciado, Lote: {}, Camión: {}",
                    lote.getId(), numeroCamion);

        } catch (Exception e) {
            log.error("❌ Error al publicar inicio transporte lote ID: {}", lote.getId(), e);
        }
    }

    /**
     * Evento: TRANSPORTE FINALIZADO
     * Notificar a: Socio + Cooperativa + Destino
     */
    public void publicarFinTransporte(Lotes lote, Integer numeroCamion) {
        try {
            LocalDateTime now = LocalDateTime.now();

            Map<String, Object> payload = new HashMap<>();
            payload.put("evento", "transporte_finalizado");
            payload.put("loteId", lote.getId());
            payload.put("estado", lote.getEstado());
            payload.put("numeroCamion", numeroCamion);
            payload.put("minaNombre", lote.getMinasId().getNombre());
            payload.put("timestamp", now.toString());

            // Notificar a todos
            notificarATodos(lote, payload);
            enviarADetalle(lote.getId(), payload);

            log.info("✅ WebSocket enviado - transporte_finalizado, Lote: {}, Camión: {}",
                    lote.getId(), numeroCamion);

        } catch (Exception e) {
            log.error("❌ Error al publicar fin transporte lote ID: {}", lote.getId(), e);
        }
    }

    // ==================== MÉTODOS AUXILIARES ====================

    /**
     * Enviar mensaje a la cola personal de un usuario (para lista)
     */
    private void enviarAUsuario(Integer usuarioId, Map<String, Object> payload) {
        String destination = "/user/" + usuarioId + "/queue/lotes";
        messagingTemplate.convertAndSend(destination, payload);
    }

    /**
     * Enviar mensaje al topic del lote (para detalle)
     */
    private void enviarADetalle(Integer loteId, Map<String, Object> payload) {
        String destination = "/topic/lote/" + loteId;
        messagingTemplate.convertAndSend(destination, payload);
    }

    /**
     * Notificar a socio, cooperativa y destino
     */
    private void notificarATodos(Lotes lote, Map<String, Object> payload) {
        // Socio
        Integer socioUsuarioId = lote.getMinasId().getSocioId().getUsuariosId().getId();
        enviarAUsuario(socioUsuarioId, payload);

        // Cooperativa
        Integer cooperativaUsuarioId = lote.getMinasId()
                .getSectoresId()
                .getCooperativaId()
                .getUsuariosId()
                .getId();
        enviarAUsuario(cooperativaUsuarioId, payload);

        // Destino
        Integer destinoUsuarioId = obtenerUsuarioIdDestino(lote);
        if (destinoUsuarioId != null) {
            enviarAUsuario(destinoUsuarioId, payload);
        }
    }

    /**
     * Obtener el usuario ID del destino (ingenio o comercializadora)
     */
    private Integer obtenerUsuarioIdDestino(Lotes lote) {
        try {
            if ("procesamiento_planta".equals(lote.getTipoOperacion())) {
                // Es ingenio
                return loteIngenioRepository.findByLotesId(lote)
                        .map(li -> li.getIngenioMineroId().getUsuariosId().getId())
                        .orElse(null);
            } else {
                // Es comercializadora
                return loteComercializadoraRepository.findByLotesId(lote)
                        .map(lc -> lc.getComercializadoraId().getUsuariosId().getId())
                        .orElse(null);
            }
        } catch (Exception e) {
            log.warn("⚠️ No se pudo obtener usuario destino del lote ID: {}", lote.getId());
            return null;
        }
    }
}