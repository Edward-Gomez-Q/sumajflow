package ucb.edu.bo.sumajflow.bl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import ucb.edu.bo.sumajflow.dto.ingenio.LiquidacionTollResponseDto;
import ucb.edu.bo.sumajflow.dto.venta.VentaLiquidacionResponseDto;
import ucb.edu.bo.sumajflow.entity.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Servicio centralizado para eventos WebSocket de Liquidaciones
 *
 * ARQUITECTURA:
 * - /user/{usuarioId}/queue/liquidaciones → Lista de liquidaciones (payload ligero)
 * - /topic/liquidacion/{liquidacionId} → Detalle de liquidación (payload completo)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LiquidacionesWebSocketBl {

    private final SimpMessagingTemplate messagingTemplate;
    private final LiquidacionTollBl liquidacionTollBl;
    private final LiquidacionVentaBl liquidacionVentaBl;
    private final ConcentradoBl concentradoBl;

    // ==================== LIQUIDACIÓN TOLL ====================

    /**
     * Evento: LIQUIDACIÓN TOLL CREADA
     * Notificar a: Socio + Ingenio
     */
    public void publicarCreacionToll(Liquidacion liquidacion) {
        try {
            LocalDateTime now = LocalDateTime.now();

            Map<String, Object> payloadLigero = new HashMap<>();
            payloadLigero.put("evento", "toll_creado");
            payloadLigero.put("liquidacionId", liquidacion.getId());
            payloadLigero.put("tipoLiquidacion", liquidacion.getTipoLiquidacion());
            payloadLigero.put("estado", liquidacion.getEstado());
            payloadLigero.put("valorNetoBob", liquidacion.getValorNetoBob());
            payloadLigero.put("timestamp", now.toString());

            // Notificar al socio
            Integer socioUsuarioId = liquidacion.getSocioId().getUsuariosId().getId();
            enviarAUsuario(socioUsuarioId, payloadLigero);
            log.debug("📤 Notificado socio (usuario {}): toll_creado", socioUsuarioId);

            // Notificar al ingenio
            Integer ingenioUsuarioId = obtenerIngenioUsuarioId(liquidacion);
            if (ingenioUsuarioId != null) {
                enviarAUsuario(ingenioUsuarioId, payloadLigero);
                log.debug("📤 Notificado ingenio (usuario {}): toll_creado", ingenioUsuarioId);
            }

            // Payload completo para detalle
            LiquidacionTollResponseDto dtoCompleto = liquidacionTollBl.convertirADto(liquidacion);

            Map<String, Object> payloadCompleto = new HashMap<>();
            payloadCompleto.put("evento", "toll_creado");
            payloadCompleto.put("timestamp", now.toString());
            payloadCompleto.put("liquidacion", dtoCompleto);

            enviarADetalle(liquidacion.getId(), payloadCompleto);

            log.info("✅ WebSocket enviado - toll_creado, ID: {}", liquidacion.getId());

        } catch (Exception e) {
            log.error("❌ Error al publicar creación toll ID: {}", liquidacion.getId(), e);
        }
    }

    /**
     * Evento: PAGO DE TOLL REGISTRADO
     * Notificar a: Socio + Ingenio + Concentrados actualizados
     */
    public void publicarPagoToll(Liquidacion liquidacion) {
        try {
            LocalDateTime now = LocalDateTime.now();

            Map<String, Object> payloadLigero = new HashMap<>();
            payloadLigero.put("evento", "toll_pagado");
            payloadLigero.put("liquidacionId", liquidacion.getId());
            payloadLigero.put("tipoLiquidacion", liquidacion.getTipoLiquidacion());
            payloadLigero.put("estado", liquidacion.getEstado());
            payloadLigero.put("valorNetoBob", liquidacion.getValorNetoBob());
            payloadLigero.put("timestamp", now.toString());

            Integer socioUsuarioId = liquidacion.getSocioId().getUsuariosId().getId();
            enviarAUsuario(socioUsuarioId, payloadLigero);

            Integer ingenioUsuarioId = obtenerIngenioUsuarioId(liquidacion);
            if (ingenioUsuarioId != null) {
                enviarAUsuario(ingenioUsuarioId, payloadLigero);
            }

            LiquidacionTollResponseDto dtoCompleto = liquidacionTollBl.convertirADto(liquidacion);

            Map<String, Object> payloadCompleto = new HashMap<>();
            payloadCompleto.put("evento", "toll_pagado");
            payloadCompleto.put("timestamp", now.toString());
            payloadCompleto.put("liquidacion", dtoCompleto);

            enviarADetalle(liquidacion.getId(), payloadCompleto);

            // Notificar concentrados actualizados
            notificarConcentradosActualizados(liquidacion);

            log.info("✅ WebSocket enviado - toll_pagado, ID: {}", liquidacion.getId());

        } catch (Exception e) {
            log.error("❌ Error al publicar pago toll ID: {}", liquidacion.getId(), e);
        }
    }

    // ==================== LIQUIDACIÓN VENTA ====================

    /**
     * Evento: VENTA CREADA
     * Notificar a: Socio + Comercializadora
     */
    public void publicarCreacionVenta(Liquidacion liquidacion) {
        try {
            LocalDateTime now = LocalDateTime.now();

            Map<String, Object> payloadLigero = new HashMap<>();
            payloadLigero.put("evento", "venta_creada");
            payloadLigero.put("liquidacionId", liquidacion.getId());
            payloadLigero.put("tipoLiquidacion", liquidacion.getTipoLiquidacion());
            payloadLigero.put("estado", liquidacion.getEstado());
            payloadLigero.put("timestamp", now.toString());

            Integer socioUsuarioId = liquidacion.getSocioId().getUsuariosId().getId();
            enviarAUsuario(socioUsuarioId, payloadLigero);

            Integer comercializadoraUsuarioId = liquidacion.getComercializadoraId().getUsuariosId().getId();
            enviarAUsuario(comercializadoraUsuarioId, payloadLigero);

            VentaLiquidacionResponseDto dtoCompleto = liquidacionVentaBl.convertirADto(liquidacion);

            Map<String, Object> payloadCompleto = new HashMap<>();
            payloadCompleto.put("evento", "venta_creada");
            payloadCompleto.put("timestamp", now.toString());
            payloadCompleto.put("liquidacion", dtoCompleto);

            enviarADetalle(liquidacion.getId(), payloadCompleto);

            notificarItemsActualizados(liquidacion);

            log.info("✅ WebSocket enviado - venta_creada, ID: {}", liquidacion.getId());

        } catch (Exception e) {
            log.error("❌ Error al publicar creación venta ID: {}", liquidacion.getId(), e);
        }
    }

    /**
     * Evento: VENTA APROBADA
     */
    public void publicarAprobacionVenta(Liquidacion liquidacion) {
        try {
            LocalDateTime now = LocalDateTime.now();

            Map<String, Object> payloadLigero = new HashMap<>();
            payloadLigero.put("evento", "venta_aprobada");
            payloadLigero.put("liquidacionId", liquidacion.getId());
            payloadLigero.put("tipoLiquidacion", liquidacion.getTipoLiquidacion());
            payloadLigero.put("estado", liquidacion.getEstado());
            payloadLigero.put("timestamp", now.toString());

            Integer socioUsuarioId = liquidacion.getSocioId().getUsuariosId().getId();
            enviarAUsuario(socioUsuarioId, payloadLigero);

            Integer comercializadoraUsuarioId = liquidacion.getComercializadoraId().getUsuariosId().getId();
            enviarAUsuario(comercializadoraUsuarioId, payloadLigero);

            VentaLiquidacionResponseDto dtoCompleto = liquidacionVentaBl.convertirADto(liquidacion);

            Map<String, Object> payloadCompleto = new HashMap<>();
            payloadCompleto.put("evento", "venta_aprobada");
            payloadCompleto.put("timestamp", now.toString());
            payloadCompleto.put("liquidacion", dtoCompleto);

            enviarADetalle(liquidacion.getId(), payloadCompleto);

            log.info("✅ WebSocket enviado - venta_aprobada, ID: {}", liquidacion.getId());

        } catch (Exception e) {
            log.error("❌ Error al publicar aprobación venta ID: {}", liquidacion.getId(), e);
        }
    }

    /**
     * Evento: VENTA RECHAZADA
     */
    public void publicarRechazoVenta(Liquidacion liquidacion, String motivoRechazo) {
        try {
            LocalDateTime now = LocalDateTime.now();

            Map<String, Object> payloadLigero = new HashMap<>();
            payloadLigero.put("evento", "venta_rechazada");
            payloadLigero.put("liquidacionId", liquidacion.getId());
            payloadLigero.put("tipoLiquidacion", liquidacion.getTipoLiquidacion());
            payloadLigero.put("estado", liquidacion.getEstado());
            payloadLigero.put("motivoRechazo", motivoRechazo);
            payloadLigero.put("timestamp", now.toString());

            Integer socioUsuarioId = liquidacion.getSocioId().getUsuariosId().getId();
            enviarAUsuario(socioUsuarioId, payloadLigero);

            Integer comercializadoraUsuarioId = liquidacion.getComercializadoraId().getUsuariosId().getId();
            enviarAUsuario(comercializadoraUsuarioId, payloadLigero);

            VentaLiquidacionResponseDto dtoCompleto = liquidacionVentaBl.convertirADto(liquidacion);

            Map<String, Object> payloadCompleto = new HashMap<>();
            payloadCompleto.put("evento", "venta_rechazada");
            payloadCompleto.put("timestamp", now.toString());
            payloadCompleto.put("motivoRechazo", motivoRechazo);
            payloadCompleto.put("liquidacion", dtoCompleto);

            enviarADetalle(liquidacion.getId(), payloadCompleto);

            notificarItemsActualizados(liquidacion);

            log.info("✅ WebSocket enviado - venta_rechazada, ID: {}", liquidacion.getId());

        } catch (Exception e) {
            log.error("❌ Error al publicar rechazo venta ID: {}", liquidacion.getId(), e);
        }
    }

    /**
     * Evento: REPORTE QUÍMICO SUBIDO
     */
    public void publicarReporteQuimicoSubido(Liquidacion liquidacion, String tipoReporte) {
        try {
            LocalDateTime now = LocalDateTime.now();

            Map<String, Object> payloadLigero = new HashMap<>();
            payloadLigero.put("evento", "reporte_quimico_subido");
            payloadLigero.put("liquidacionId", liquidacion.getId());
            payloadLigero.put("tipoLiquidacion", liquidacion.getTipoLiquidacion());
            payloadLigero.put("estado", liquidacion.getEstado());
            payloadLigero.put("tipoReporte", tipoReporte);
            payloadLigero.put("timestamp", now.toString());

            Integer socioUsuarioId = liquidacion.getSocioId().getUsuariosId().getId();
            Integer comercializadoraUsuarioId = liquidacion.getComercializadoraId().getUsuariosId().getId();

            enviarAUsuario(socioUsuarioId, payloadLigero);
            enviarAUsuario(comercializadoraUsuarioId, payloadLigero);

            VentaLiquidacionResponseDto dtoCompleto = liquidacionVentaBl.convertirADto(liquidacion);

            Map<String, Object> payloadCompleto = new HashMap<>();
            payloadCompleto.put("evento", "reporte_quimico_subido");
            payloadCompleto.put("timestamp", now.toString());
            payloadCompleto.put("tipoReporte", tipoReporte);
            payloadCompleto.put("liquidacion", dtoCompleto);

            enviarADetalle(liquidacion.getId(), payloadCompleto);

            log.info("✅ WebSocket enviado - reporte_quimico_subido ({}), ID: {}", tipoReporte, liquidacion.getId());

        } catch (Exception e) {
            log.error("❌ Error al publicar reporte químico ID: {}", liquidacion.getId(), e);
        }
    }

    /**
     * Evento: VENTA CERRADA
     */
    public void publicarCierreVenta(Liquidacion liquidacion) {
        try {
            LocalDateTime now = LocalDateTime.now();

            Map<String, Object> payloadLigero = new HashMap<>();
            payloadLigero.put("evento", "venta_cerrada");
            payloadLigero.put("liquidacionId", liquidacion.getId());
            payloadLigero.put("tipoLiquidacion", liquidacion.getTipoLiquidacion());
            payloadLigero.put("estado", liquidacion.getEstado());
            payloadLigero.put("valorNetoBob", liquidacion.getValorNetoBob());
            payloadLigero.put("valorNetoUsd", liquidacion.getValorNetoUsd());
            payloadLigero.put("timestamp", now.toString());

            Integer socioUsuarioId = liquidacion.getSocioId().getUsuariosId().getId();
            Integer comercializadoraUsuarioId = liquidacion.getComercializadoraId().getUsuariosId().getId();

            enviarAUsuario(socioUsuarioId, payloadLigero);
            enviarAUsuario(comercializadoraUsuarioId, payloadLigero);

            VentaLiquidacionResponseDto dtoCompleto = liquidacionVentaBl.convertirADto(liquidacion);

            Map<String, Object> payloadCompleto = new HashMap<>();
            payloadCompleto.put("evento", "venta_cerrada");
            payloadCompleto.put("timestamp", now.toString());
            payloadCompleto.put("liquidacion", dtoCompleto);

            enviarADetalle(liquidacion.getId(), payloadCompleto);

            log.info("✅ WebSocket enviado - venta_cerrada, ID: {}", liquidacion.getId());

        } catch (Exception e) {
            log.error("❌ Error al publicar cierre venta ID: {}", liquidacion.getId(), e);
        }
    }

    /**
     * Evento: PAGO DE VENTA CONFIRMADO
     */
    public void publicarPagoVenta(Liquidacion liquidacion) {
        try {
            LocalDateTime now = LocalDateTime.now();

            Map<String, Object> payloadLigero = new HashMap<>();
            payloadLigero.put("evento", "venta_pagada");
            payloadLigero.put("liquidacionId", liquidacion.getId());
            payloadLigero.put("tipoLiquidacion", liquidacion.getTipoLiquidacion());
            payloadLigero.put("estado", liquidacion.getEstado());
            payloadLigero.put("valorNetoBob", liquidacion.getValorNetoBob());
            payloadLigero.put("timestamp", now.toString());

            Integer socioUsuarioId = liquidacion.getSocioId().getUsuariosId().getId();
            Integer comercializadoraUsuarioId = liquidacion.getComercializadoraId().getUsuariosId().getId();

            enviarAUsuario(socioUsuarioId, payloadLigero);
            enviarAUsuario(comercializadoraUsuarioId, payloadLigero);

            VentaLiquidacionResponseDto dtoCompleto = liquidacionVentaBl.convertirADto(liquidacion);

            Map<String, Object> payloadCompleto = new HashMap<>();
            payloadCompleto.put("evento", "venta_pagada");
            payloadCompleto.put("timestamp", now.toString());
            payloadCompleto.put("liquidacion", dtoCompleto);

            enviarADetalle(liquidacion.getId(), payloadCompleto);

            notificarItemsActualizados(liquidacion);

            log.info("✅ WebSocket enviado - venta_pagada, ID: {}", liquidacion.getId());

        } catch (Exception e) {
            log.error("❌ Error al publicar pago venta ID: {}", liquidacion.getId(), e);
        }
    }

    // ==================== MÉTODOS AUXILIARES ====================

    private void enviarAUsuario(Integer usuarioId, Map<String, Object> payload) {
        String destination = "/user/" + usuarioId + "/queue/liquidaciones";
        messagingTemplate.convertAndSend(destination, payload);
    }

    private void enviarADetalle(Integer liquidacionId, Map<String, Object> payload) {
        String destination = "/topic/liquidacion/" + liquidacionId;
        messagingTemplate.convertAndSend(destination, payload);
    }

    private Integer obtenerIngenioUsuarioId(Liquidacion liquidacion) {
        try {
            return liquidacion.getLiquidacionLoteList().stream()
                    .findFirst()
                    .flatMap(ll -> ll.getLotesId().getLoteIngenioList().stream().findFirst())
                    .map(li -> li.getIngenioMineroId().getUsuariosId().getId())
                    .orElse(null);
        } catch (Exception e) {
            log.warn("⚠️ No se pudo obtener usuario del ingenio para liquidación ID: {}", liquidacion.getId());
            return null;
        }
    }

    private void notificarConcentradosActualizados(Liquidacion liquidacion) {
        try {
            liquidacion.getLiquidacionLoteList().forEach(ll -> {
                Lotes lote = ll.getLotesId();
                lote.getLoteConcentradoRelacionList().forEach(relacion -> {
                    Concentrado concentrado = relacion.getConcentradoId();
                    concentradoBl.publicarEventoWebSocket(concentrado, "concentrado_actualizado");
                    log.debug("📤 Concentrado ID {} notificado (toll pagado)", concentrado.getId());
                });
            });
        } catch (Exception e) {
            log.warn("⚠️ Error al notificar concentrados actualizados", e);
        }
    }

    private void notificarItemsActualizados(Liquidacion liquidacion) {
        try {
            if (LiquidacionVentaBl.TIPO_VENTA_CONCENTRADO.equals(liquidacion.getTipoLiquidacion())) {
                liquidacion.getLiquidacionConcentradoList().forEach(lc -> {
                    Concentrado concentrado = lc.getConcentradoId();
                    concentradoBl.publicarEventoWebSocket(concentrado, "concentrado_actualizado");
                    log.debug("📤 Concentrado ID {} notificado (venta actualizada)", concentrado.getId());
                });
            }
        } catch (Exception e) {
            log.warn("⚠️ Error al notificar items actualizados", e);
        }
    }
}