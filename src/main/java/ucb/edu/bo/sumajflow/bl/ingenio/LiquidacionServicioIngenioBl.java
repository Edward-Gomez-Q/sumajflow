package ucb.edu.bo.sumajflow.bl.ingenio;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ucb.edu.bo.sumajflow.bl.ConcentradoBl;
import ucb.edu.bo.sumajflow.bl.NotificacionBl;
import ucb.edu.bo.sumajflow.dto.ingenio.*;
import ucb.edu.bo.sumajflow.entity.*;
import ucb.edu.bo.sumajflow.repository.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Servicio de gestión de Liquidaciones de Servicio
 * Responsabilidades:
 * - Revisar solicitudes de liquidación de servicio
 * - Aprobar liquidaciones (definir costo)
 * - Registrar pagos de servicios
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LiquidacionServicioIngenioBl {

    // Repositorios
    private final ConcentradoRepository concentradoRepository;
    private final IngenioMineroRepository ingenioMineroRepository;
    private final UsuariosRepository usuariosRepository;
    private final LiquidacionRepository liquidacionRepository;
    private final LiquidacionConcentradoRepository liquidacionConcentradoRepository;
    private final PersonaRepository personaRepository;

    // Servicios
    private final ConcentradoBl concentradoBl;
    private final NotificacionBl notificacionBl;

    // ==================== REVISAR LIQUIDACIÓN ====================

    /**
     * Revisar solicitud de liquidación (cambiar a "en_revision")
     */
    @Transactional
    public ConcentradoResponseDto revisarLiquidacionServicio(
            Integer concentradoId,
            Integer usuarioId,
            String ipOrigen
    ) {
        log.info("Revisando liquidación de servicio - Concentrado ID: {}", concentradoId);

        IngenioMinero ingenio = obtenerIngenioDelUsuario(usuarioId);
        Concentrado concentrado = obtenerConcentradoConPermisos(concentradoId, ingenio);

        // Validar estado
        concentradoBl.validarEstado(concentrado, "liquidacion_servicio_solicitada");

        // Cambiar estado
        concentradoBl.transicionarEstado(
                concentrado,
                "liquidacion_servicio_en_revision",
                "Ingenio está revisando la solicitud de liquidación de servicio",
                null,
                usuarioId,
                ipOrigen
        );

        // WebSocket
        concentradoBl.publicarEventoWebSocket(concentrado, "liquidacion_servicio_en_revision");

        return concentradoBl.convertirAResponseDto(concentrado);
    }

    // ==================== APROBAR LIQUIDACIÓN ====================

    /**
     * Aprobar liquidación de servicio (definir costo)
     */
    @Transactional
    public LiquidacionServicioResponseDto aprobarLiquidacionServicio(
            Integer concentradoId,
            AprobarLiquidacionServicioDto aprobarDto,
            Integer usuarioId,
            String ipOrigen
    ) {
        log.info("Aprobando liquidación de servicio - Concentrado ID: {}", concentradoId);

        IngenioMinero ingenio = obtenerIngenioDelUsuario(usuarioId);
        Concentrado concentrado = obtenerConcentradoConPermisos(concentradoId, ingenio);

        // Validar estado
        concentradoBl.validarEstado(concentrado, "liquidacion_servicio_en_revision");

        // Crear liquidación
        Liquidacion liquidacion = Liquidacion.builder()
                .socioId(concentrado.getSocioPropietarioId())
                .tipoLiquidacion("servicio_procesamiento")
                .fechaLiquidacion(LocalDateTime.now())
                .moneda("BOB")
                .pesoLiquidado(concentrado.getPesoInicial())
                .valorBruto(aprobarDto.getCostoServicio())
                .valorNeto(aprobarDto.getCostoServicio())
                .estado("pendiente_pago")
                .build();

        liquidacion = liquidacionRepository.save(liquidacion);

        // Crear relación liquidación-concentrado
        LiquidacionConcentrado liquidacionConcentrado = LiquidacionConcentrado.builder()
                .concentradoId(concentrado)
                .liquidacionId(liquidacion)
                .build();

        concentrado.addLiquidacionConcentrado(liquidacionConcentrado);
        liquidacionConcentradoRepository.save(liquidacionConcentrado);

        // Cambiar estado del concentrado
        concentradoBl.transicionarEstado(
                concentrado,
                "servicio_ingenio_liquidado",
                "Liquidación de servicio aprobada - Monto: " + aprobarDto.getCostoServicio() + " BOB",
                aprobarDto.getObservaciones(),
                usuarioId,
                ipOrigen
        );

        // Registrar auditoría
        List<Map<String, Object>> historial = concentradoBl.obtenerHistorial(concentrado);
        Map<String, Object> registro = new HashMap<>();
        registro.put("accion", "APROBAR_LIQUIDACION_SERVICIO");
        registro.put("liquidacion_id", liquidacion.getId());
        registro.put("costo_servicio", aprobarDto.getCostoServicio());
        registro.put("url_documento", aprobarDto.getUrlDocumentoLiquidacion());
        registro.put("usuario_id", usuarioId);
        registro.put("ip_origen", ipOrigen);
        registro.put("timestamp", LocalDateTime.now().toString());
        historial.add(registro);
        concentrado.setObservaciones(concentradoBl.convertirHistorialAJson(historial));
        concentradoRepository.save(concentrado);

        // Notificaciones
        notificarLiquidacionAprobada(concentrado, liquidacion);

        // WebSocket
        concentradoBl.publicarEventoWebSocket(concentrado, "servicio_ingenio_liquidado");

        return convertirLiquidacionADto(liquidacion, concentrado);
    }

    // ==================== REGISTRAR PAGO ====================

    /**
     * Registrar pago del servicio
     */
    @Transactional
    public ConcentradoResponseDto registrarPagoServicio(
            Integer concentradoId,
            RegistrarPagoServicioDto pagoDto,
            Integer usuarioId,
            String ipOrigen
    ) {
        log.info("Registrando pago de servicio - Concentrado ID: {}", concentradoId);

        IngenioMinero ingenio = obtenerIngenioDelUsuario(usuarioId);
        Concentrado concentrado = obtenerConcentradoConPermisos(concentradoId, ingenio);

        // Validar estado
        concentradoBl.validarEstado(concentrado, "servicio_ingenio_liquidado");

        // Obtener liquidación de servicio
        LiquidacionConcentrado liquidacionConcentrado = concentrado.getLiquidacionConcentradoList().stream()
                .filter(lc -> "servicio_procesamiento".equals(lc.getLiquidacionId().getTipoLiquidacion()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No se encontró liquidación de servicio"));

        Liquidacion liquidacion = liquidacionConcentrado.getLiquidacionId();
        liquidacion.setEstado("pagado");
        liquidacionRepository.save(liquidacion);

        // Cambiar estado del concentrado
        concentradoBl.transicionarEstado(
                concentrado,
                "servicio_ingenio_pagado",
                "Pago de servicio registrado - Monto: " + pagoDto.getMontoPagado() + " BOB",
                pagoDto.getObservaciones(),
                usuarioId,
                ipOrigen
        );

        // Registrar auditoría
        List<Map<String, Object>> historial = concentradoBl.obtenerHistorial(concentrado);
        Map<String, Object> registro = new HashMap<>();
        registro.put("accion", "REGISTRAR_PAGO_SERVICIO");
        registro.put("liquidacion_id", liquidacion.getId());
        registro.put("monto_pagado", pagoDto.getMontoPagado());
        registro.put("fecha_pago", pagoDto.getFechaPago().toString());
        registro.put("metodo_pago", pagoDto.getMetodoPago());
        registro.put("numero_comprobante", pagoDto.getNumeroComprobante());
        registro.put("url_comprobante", pagoDto.getUrlComprobante());
        registro.put("usuario_id", usuarioId);
        registro.put("ip_origen", ipOrigen);
        registro.put("timestamp", LocalDateTime.now().toString());
        historial.add(registro);
        concentrado.setObservaciones(concentradoBl.convertirHistorialAJson(historial));
        concentradoRepository.save(concentrado);

        // Transición final a "listo_para_venta"
        concentradoBl.transicionarEstado(
                concentrado,
                "listo_para_venta",
                "Servicio de procesamiento pagado. Concentrado listo para venta.",
                null,
                usuarioId,
                ipOrigen
        );

        // Notificaciones
        notificarServicioPagado(concentrado);

        // WebSocket
        concentradoBl.publicarEventoWebSocket(concentrado, "listo_para_venta");

        return concentradoBl.convertirAResponseDto(concentrado);
    }

    // ==================== MÉTODOS AUXILIARES PRIVADOS ====================

    private IngenioMinero obtenerIngenioDelUsuario(Integer usuarioId) {
        Usuarios usuario = usuariosRepository.findById(usuarioId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        return ingenioMineroRepository.findByUsuariosId(usuario)
                .orElseThrow(() -> new IllegalArgumentException("Ingenio minero no encontrado"));
    }

    private Concentrado obtenerConcentradoConPermisos(Integer concentradoId, IngenioMinero ingenio) {
        Concentrado concentrado = concentradoBl.obtenerConcentrado(concentradoId);

        if (!concentrado.getIngenioMineroId().getId().equals(ingenio.getId())) {
            throw new IllegalArgumentException("No tienes permiso para acceder a este concentrado");
        }

        return concentrado;
    }

    private LiquidacionServicioResponseDto convertirLiquidacionADto(Liquidacion liquidacion, Concentrado concentrado) {
        LiquidacionServicioResponseDto dto = new LiquidacionServicioResponseDto();
        dto.setId(liquidacion.getId());
        dto.setConcentradoId(concentrado.getId());
        dto.setCodigoConcentrado(concentrado.getCodigoConcentrado());
        dto.setSocioId(liquidacion.getSocioId().getId());

        // Obtener datos del socio
        Persona persona = personaRepository.findByUsuariosId(liquidacion.getSocioId().getUsuariosId()).orElse(null);
        if (persona != null) {
            dto.setSocioNombres(persona.getNombres());
            dto.setSocioApellidos(persona.getPrimerApellido() +
                    (persona.getSegundoApellido() != null ? " " + persona.getSegundoApellido() : ""));
        }

        dto.setTipoLiquidacion(liquidacion.getTipoLiquidacion());
        dto.setFechaLiquidacion(liquidacion.getFechaLiquidacion());
        dto.setMoneda(liquidacion.getMoneda());
        dto.setPesoLiquidado(liquidacion.getPesoLiquidado());
        dto.setValorBruto(liquidacion.getValorBruto());
        dto.setValorNeto(liquidacion.getValorNeto());
        dto.setEstado(liquidacion.getEstado());
        dto.setCreatedAt(liquidacion.getCreatedAt());
        dto.setUpdatedAt(liquidacion.getUpdatedAt());

        return dto;
    }

    private void notificarLiquidacionAprobada(Concentrado concentrado, Liquidacion liquidacion) {
        Integer socioUsuarioId = concentrado.getSocioPropietarioId().getUsuariosId().getId();

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("concentradoId", concentrado.getId());
        metadata.put("codigoConcentrado", concentrado.getCodigoConcentrado());
        metadata.put("liquidacionId", liquidacion.getId());
        metadata.put("montoLiquidacion", liquidacion.getValorNeto());

        notificacionBl.crearNotificacion(
                socioUsuarioId,
                "success",
                "Liquidación de servicio aprobada",
                "La liquidación del servicio de procesamiento para el concentrado " + concentrado.getCodigoConcentrado() +
                        " ha sido aprobada. Monto: " + liquidacion.getValorNeto() + " BOB. Procede a realizar el pago.",
                metadata
        );
    }

    private void notificarServicioPagado(Concentrado concentrado) {
        // Notificar al socio
        Integer socioUsuarioId = concentrado.getSocioPropietarioId().getUsuariosId().getId();

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("concentradoId", concentrado.getId());
        metadata.put("codigoConcentrado", concentrado.getCodigoConcentrado());
        metadata.put("estado", concentrado.getEstado());

        notificacionBl.crearNotificacion(
                socioUsuarioId,
                "success",
                "Servicio pagado - Listo para venta",
                "El pago del servicio de procesamiento ha sido registrado. El concentrado " + concentrado.getCodigoConcentrado() +
                        " está listo para solicitar venta.",
                metadata
        );

        // Notificar al ingenio
        Integer ingenioUsuarioId = concentrado.getIngenioMineroId().getUsuariosId().getId();

        notificacionBl.crearNotificacion(
                ingenioUsuarioId,
                "success",
                "Servicio pagado",
                "El pago del servicio de procesamiento para el concentrado " + concentrado.getCodigoConcentrado() +
                        " ha sido registrado.",
                metadata
        );
    }
}