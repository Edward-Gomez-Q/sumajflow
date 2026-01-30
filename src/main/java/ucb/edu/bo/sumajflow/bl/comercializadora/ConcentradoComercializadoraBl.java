package ucb.edu.bo.sumajflow.bl.comercializadora;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ucb.edu.bo.sumajflow.bl.ConcentradoBl;
import ucb.edu.bo.sumajflow.bl.NotificacionBl;
import ucb.edu.bo.sumajflow.dto.ingenio.*;
import ucb.edu.bo.sumajflow.entity.*;
import ucb.edu.bo.sumajflow.repository.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Servicio de Concentrados para el ROL COMERCIALIZADORA
 * Responsabilidades:
 * - Ver concentrados disponibles para venta (estado "venta_solicitada" hacia adelante)
 * - Revisar solicitud de venta
 * - Aprobar/rechazar venta (definir precio)
 * - Registrar pago de venta al socio
 * - Ver historial de ventas realizadas
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ConcentradoComercializadoraBl {

    // Repositorios
    private final ConcentradoRepository concentradoRepository;
    private final UsuariosRepository usuariosRepository;
    private final ComercializadoraRepository comercializadoraRepository;
    private final PersonaRepository personaRepository;
    private final LiquidacionRepository liquidacionRepository;
    private final LiquidacionConcentradoRepository liquidacionConcentradoRepository;

    // Servicios
    private final ConcentradoBl concentradoBl;
    private final NotificacionBl notificacionBl;

    // ==================== LISTAR CONCENTRADOS DISPONIBLES PARA VENTA ====================

    /**
     * Listar concentrados disponibles para venta (en estados de venta)
     */
    @Transactional(readOnly = true)
    public Page<ConcentradoResponseDto> listarConcentradosDisponibles(
            Integer usuarioId,
            String estado,
            String mineralPrincipal,
            LocalDateTime fechaDesde,
            LocalDateTime fechaHasta,
            int page,
            int size
    ) {
        log.debug("Listando concentrados disponibles para venta - Usuario ID: {}", usuarioId);

        Comercializadora comercializadora = obtenerComercializadoraDelUsuario(usuarioId);

        // Obtener todos los concentrados en estados de venta
        List<String> estadosVenta = Arrays.asList(
                "venta_solicitada",
                "venta_en_revision",
                "venta_liquidada",
                "vendido"
        );

        List<Concentrado> concentradosVenta = concentradoRepository.findAll().stream()
                .filter(c -> estadosVenta.contains(c.getEstado()))
                .collect(Collectors.toList());

        return concentradoBl.listarConcentradosConFiltros(
                concentradosVenta,
                estado,
                mineralPrincipal,
                fechaDesde,
                fechaHasta,
                page,
                size
        );
    }

    /**
     * Obtener detalle de un concentrado
     */
    @Transactional(readOnly = true)
    public ConcentradoResponseDto obtenerDetalle(Integer concentradoId, Integer usuarioId) {
        log.debug("Obteniendo detalle del concentrado ID: {}", concentradoId);

        Comercializadora comercializadora = obtenerComercializadoraDelUsuario(usuarioId);

        return concentradoBl.obtenerDetalle(concentradoId);
    }

    /**
     * Obtener dashboard de ventas de la comercializadora
     */
    @Transactional(readOnly = true)
    public Map<String, Object> obtenerDashboard(Integer usuarioId) {
        log.debug("Obteniendo dashboard de ventas - Usuario ID: {}", usuarioId);

        Comercializadora comercializadora = obtenerComercializadoraDelUsuario(usuarioId);

        // Obtener liquidaciones de tipo venta
        List<Liquidacion> liquidaciones = liquidacionRepository.findByTipo("venta_concentrado");

        Map<String, Object> dashboard = new HashMap<>();
        dashboard.put("totalVentas", liquidaciones.size());
        dashboard.put("ventasPendientes", liquidaciones.stream().filter(l -> "pendiente_pago".equals(l.getEstado())).count());
        dashboard.put("ventasPagadas", liquidaciones.stream().filter(l -> "pagado".equals(l.getEstado())).count());

        // Concentrados en proceso de venta
        List<Concentrado> todosConcentrados = concentradoRepository.findAll();
        dashboard.put("solicitudesPendientes", todosConcentrados.stream().filter(c -> "venta_solicitada".equals(c.getEstado())).count());
        dashboard.put("enRevision", todosConcentrados.stream().filter(c -> "venta_en_revision".equals(c.getEstado())).count());

        return dashboard;
    }

    // ==================== REVISAR SOLICITUD DE VENTA ====================

    /**
     * Revisar solicitud de venta (cambiar estado a "venta_en_revision")
     */
    @Transactional
    public ConcentradoResponseDto revisarVenta(
            Integer concentradoId,
            Integer usuarioId,
            String ipOrigen
    ) {
        log.info("Revisando venta de concentrado - Concentrado ID: {}", concentradoId);

        Comercializadora comercializadora = obtenerComercializadoraDelUsuario(usuarioId);

        Concentrado concentrado = concentradoBl.obtenerConcentrado(concentradoId);

        // Validar estado
        concentradoBl.validarEstado(concentrado, "venta_solicitada");

        // Transicionar estado
        concentradoBl.transicionarEstado(
                concentrado,
                "venta_en_revision",
                "Comercializadora está revisando la solicitud de venta",
                null,
                usuarioId,
                ipOrigen
        );

        // WebSocket
        concentradoBl.publicarEventoWebSocket(concentrado, "venta_en_revision");

        return concentradoBl.convertirAResponseDto(concentrado);
    }

    // ==================== APROBAR VENTA ====================

    /**
     * Aprobar venta (definir precio de compra)
     */
    @Transactional
    public VentaConcentradoResponseDto aprobarVenta(
            Integer concentradoId,
            AprobarVentaDto aprobarDto,
            Integer usuarioId,
            String ipOrigen
    ) {
        log.info("Aprobando venta de concentrado - Concentrado ID: {}", concentradoId);

        Comercializadora comercializadora = obtenerComercializadoraDelUsuario(usuarioId);

        Concentrado concentrado = concentradoBl.obtenerConcentrado(concentradoId);

        // Validar estado
        concentradoBl.validarEstado(concentrado, "venta_en_revision");

        // Crear registro de liquidación de venta
        Liquidacion liquidacion = Liquidacion.builder()
                .socioId(concentrado.getSocioPropietarioId())
                .tipoLiquidacion("venta_concentrado")
                .fechaLiquidacion(LocalDateTime.now())
                .moneda("BOB")
                .pesoLiquidado(concentrado.getPesoInicial())
                .valorBruto(aprobarDto.getPrecioVenta())
                .valorNeto(aprobarDto.getPrecioVenta())
                .estado("pendiente_pago")
                .build();

        liquidacion = liquidacionRepository.save(liquidacion);

        // Asociar liquidación al concentrado
        LiquidacionConcentrado liquidacionConcentrado = LiquidacionConcentrado.builder()
                .concentradoId(concentrado)
                .liquidacionId(liquidacion)
                .build();

        concentrado.addLiquidacionConcentrado(liquidacionConcentrado);
        liquidacionConcentradoRepository.save(liquidacionConcentrado);

        // Transicionar estado
        concentradoBl.transicionarEstado(
                concentrado,
                "venta_liquidada",
                "Venta aprobada por comercializadora - Precio: " + aprobarDto.getPrecioVenta() + " BOB",
                aprobarDto.getObservaciones(),
                usuarioId,
                ipOrigen
        );

        // Registrar auditoría
        List<Map<String, Object>> historial = concentradoBl.obtenerHistorial(concentrado);
        Map<String, Object> registro = new HashMap<>();
        registro.put("accion", "APROBAR_VENTA");
        registro.put("liquidacion_id", liquidacion.getId());
        registro.put("comercializadora_id", comercializadora.getId());
        registro.put("comercializadora_nombre", comercializadora.getRazonSocial());
        registro.put("precio_venta", aprobarDto.getPrecioVenta());
        registro.put("url_contrato", aprobarDto.getUrlContrato());
        registro.put("usuario_id", usuarioId);
        registro.put("ip_origen", ipOrigen);
        registro.put("timestamp", LocalDateTime.now().toString());
        historial.add(registro);
        concentrado.setObservaciones(concentradoBl.convertirHistorialAJson(historial));
        concentradoRepository.save(concentrado);

        // Notificar al socio e ingenio
        notificarVentaAprobada(concentrado, liquidacion, comercializadora);

        // WebSocket
        concentradoBl.publicarEventoWebSocket(concentrado, "venta_liquidada");

        return convertirVentaADto(liquidacion, concentrado, comercializadora);
    }

    // ==================== REGISTRAR PAGO DE VENTA ====================

    /**
     * Registrar pago de venta al socio
     */
    @Transactional
    public ConcentradoResponseDto registrarPagoVenta(
            Integer concentradoId,
            RegistrarPagoVentaDto pagoDto,
            Integer usuarioId,
            String ipOrigen
    ) {
        log.info("Registrando pago de venta - Concentrado ID: {}", concentradoId);

        Comercializadora comercializadora = obtenerComercializadoraDelUsuario(usuarioId);

        Concentrado concentrado = concentradoBl.obtenerConcentrado(concentradoId);

        // Validar estado
        concentradoBl.validarEstado(concentrado, "venta_liquidada");

        // Obtener liquidación de venta
        LiquidacionConcentrado liquidacionConcentrado = concentrado.getLiquidacionConcentradoList().stream()
                .filter(lc -> "venta_concentrado".equals(lc.getLiquidacionId().getTipoLiquidacion()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No se encontró liquidación de venta"));

        Liquidacion liquidacion = liquidacionConcentrado.getLiquidacionId();

        // Actualizar liquidación con datos de pago
        liquidacion.setEstado("pagado");
        liquidacionRepository.save(liquidacion);

        // Transicionar concentrado a "vendido" (estado final)
        concentradoBl.transicionarEstado(
                concentrado,
                "vendido",
                "Venta completada - Pago registrado: " + pagoDto.getMontoPagado() + " BOB",
                pagoDto.getObservaciones(),
                usuarioId,
                ipOrigen
        );

        // Actualizar fecha fin del concentrado
        concentrado.setFechaFin(LocalDateTime.now());
        concentradoRepository.save(concentrado);

        // Registrar auditoría del pago
        List<Map<String, Object>> historial = concentradoBl.obtenerHistorial(concentrado);
        Map<String, Object> registro = new HashMap<>();
        registro.put("accion", "REGISTRAR_PAGO_VENTA");
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

        // Notificar a todos los involucrados
        notificarVentaCompletada(concentrado);

        // WebSocket
        concentradoBl.publicarEventoWebSocket(concentrado, "concentrado_vendido");

        log.info("Venta completada - Concentrado ID: {} VENDIDO", concentradoId);

        return concentradoBl.convertirAResponseDto(concentrado);
    }

    // ==================== VER HISTORIAL DE VENTAS ====================

    /**
     * Obtener historial de ventas de la comercializadora
     */
    @Transactional(readOnly = true)
    public List<VentaConcentradoResponseDto> verHistorialVentas(Integer usuarioId) {
        log.debug("Obteniendo historial de ventas - Usuario ID: {}", usuarioId);

        Comercializadora comercializadora = obtenerComercializadoraDelUsuario(usuarioId);

        // Obtener todas las liquidaciones de tipo venta
        List<Liquidacion> liquidaciones = liquidacionRepository.findByTipoLiquidacionOrderByFechaLiquidacionDesc("venta_concentrado");

        return liquidaciones.stream()
                .map(liquidacion -> {
                    LiquidacionConcentrado lc = liquidacion.getLiquidacionConcentradoList().isEmpty()
                            ? null
                            : liquidacion.getLiquidacionConcentradoList().get(0);

                    if (lc != null) {
                        return convertirVentaADto(liquidacion, lc.getConcentradoId(), comercializadora);
                    }
                    return null;
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    // ==================== METODOS AUXILIARES PRIVADOS ====================

    private Comercializadora obtenerComercializadoraDelUsuario(Integer usuarioId) {
        Usuarios usuario = usuariosRepository.findById(usuarioId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        return comercializadoraRepository.findByUsuariosId(usuario)
                .orElseThrow(() -> new IllegalArgumentException("Comercializadora no encontrada"));
    }

    private void notificarVentaAprobada(Concentrado concentrado, Liquidacion liquidacion, Comercializadora comercializadora) {
        // Notificar al socio
        Integer socioUsuarioId = concentrado.getSocioPropietarioId().getUsuariosId().getId();

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("concentradoId", concentrado.getId());
        metadata.put("codigoConcentrado", concentrado.getCodigoConcentrado());
        metadata.put("liquidacionId", liquidacion.getId());
        metadata.put("montoVenta", liquidacion.getValorNeto());
        metadata.put("comercializadoraNombre", comercializadora.getRazonSocial());

        notificacionBl.crearNotificacion(
                socioUsuarioId,
                "success",
                "Venta de concentrado aprobada",
                "La venta del concentrado " + concentrado.getCodigoConcentrado() + " ha sido aprobada por " +
                        comercializadora.getRazonSocial() + ". Precio de venta: " + liquidacion.getValorNeto() +
                        " BOB. Esperando confirmación del pago.",
                metadata
        );

        // Notificar al ingenio
        Integer ingenioUsuarioId = concentrado.getIngenioMineroId().getUsuariosId().getId();

        notificacionBl.crearNotificacion(
                ingenioUsuarioId,
                "success",
                "Venta aprobada",
                "La venta del concentrado " + concentrado.getCodigoConcentrado() + " ha sido aprobada por " +
                        comercializadora.getRazonSocial(),
                metadata
        );
    }

    private void notificarVentaCompletada(Concentrado concentrado) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("concentradoId", concentrado.getId());
        metadata.put("codigoConcentrado", concentrado.getCodigoConcentrado());
        metadata.put("estado", "vendido");

        // Notificar al socio
        Integer socioUsuarioId = concentrado.getSocioPropietarioId().getUsuariosId().getId();

        notificacionBl.crearNotificacion(
                socioUsuarioId,
                "success",
                "¡Venta completada!",
                "La venta del concentrado " + concentrado.getCodigoConcentrado() + " ha sido completada exitosamente. El pago ha sido registrado.",
                metadata
        );

        // Notificar al ingenio
        Integer ingenioUsuarioId = concentrado.getIngenioMineroId().getUsuariosId().getId();

        notificacionBl.crearNotificacion(
                ingenioUsuarioId,
                "success",
                "Venta completada",
                "El concentrado " + concentrado.getCodigoConcentrado() + " ha sido vendido exitosamente.",
                metadata
        );
    }

    private VentaConcentradoResponseDto convertirVentaADto(
            Liquidacion liquidacion,
            Concentrado concentrado,
            Comercializadora comercializadora
    ) {
        VentaConcentradoResponseDto dto = new VentaConcentradoResponseDto();
        dto.setId(liquidacion.getId());
        dto.setConcentradoId(concentrado.getId());
        dto.setCodigoConcentrado(concentrado.getCodigoConcentrado());
        dto.setSocioId(liquidacion.getSocioId().getId());

        Persona persona = personaRepository.findByUsuariosId(liquidacion.getSocioId().getUsuariosId()).orElse(null);
        if (persona != null) {
            dto.setSocioNombres(persona.getNombres());
            dto.setSocioApellidos(persona.getPrimerApellido() +
                    (persona.getSegundoApellido() != null ? " " + persona.getSegundoApellido() : ""));
        }

        dto.setComercializadoraId(comercializadora.getId());
        dto.setComercializadoraNombre(comercializadora.getRazonSocial());
        dto.setTipoLiquidacion(liquidacion.getTipoLiquidacion());
        dto.setFechaLiquidacion(liquidacion.getFechaLiquidacion());
        dto.setMoneda(liquidacion.getMoneda());
        dto.setPesoVendido(liquidacion.getPesoLiquidado());
        dto.setValorBruto(liquidacion.getValorBruto());
        dto.setValorNeto(liquidacion.getValorNeto());
        dto.setEstado(liquidacion.getEstado());
        dto.setCreatedAt(liquidacion.getCreatedAt());
        dto.setUpdatedAt(liquidacion.getUpdatedAt());

        return dto;
    }
}