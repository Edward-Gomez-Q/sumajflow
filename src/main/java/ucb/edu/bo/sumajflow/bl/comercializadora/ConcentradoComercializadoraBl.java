package ucb.edu.bo.sumajflow.bl.comercializadora;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ucb.edu.bo.sumajflow.bl.NotificacionBl;
import ucb.edu.bo.sumajflow.dto.ingenio.*;
import ucb.edu.bo.sumajflow.dto.socio.MineralInfoDto;
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
    private final LoteMineralesRepository loteMineralesRepository;
    private final LiquidacionRepository liquidacionRepository;
    private final LiquidacionConcentradoRepository liquidacionConcentradoRepository;

    // Servicios
    private final NotificacionBl notificacionBl;
    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper;

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

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        // Obtener todos los concentrados en estados de venta
        List<String> estadosVenta = Arrays.asList(
                "venta_solicitada",
                "venta_en_revision",
                "venta_liquidada",
                "vendido"
        );

        List<Concentrado> todosLosConcentrados = concentradoRepository.findAll().stream()
                .filter(c -> estadosVenta.contains(c.getEstado()))
                .toList();

        // Aplicar filtros
        List<Concentrado> concentradosFiltrados = todosLosConcentrados.stream()
                .filter(c -> {
                    if (estado != null && !estado.isEmpty() && !estado.equals(c.getEstado())) {
                        return false;
                    }
                    if (mineralPrincipal != null && !mineralPrincipal.isEmpty() && !mineralPrincipal.equals(c.getMineralPrincipal())) {
                        return false;
                    }
                    if (fechaDesde != null && c.getCreatedAt().isBefore(fechaDesde)) {
                        return false;
                    }
                    if (fechaHasta != null && c.getCreatedAt().isAfter(fechaHasta)) {
                        return false;
                    }
                    return true;
                })
                .collect(Collectors.toList());

        // Aplicar paginación
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), concentradosFiltrados.size());

        List<ConcentradoResponseDto> paginaActual = concentradosFiltrados.subList(start, end).stream()
                .map(this::convertirAResponseDto)
                .collect(Collectors.toList());

        return new PageImpl<>(paginaActual, pageable, concentradosFiltrados.size());
    }

    /**
     * Obtener detalle de un concentrado
     */
    @Transactional(readOnly = true)
    public ConcentradoResponseDto obtenerDetalle(Integer concentradoId, Integer usuarioId) {
        log.debug("Obteniendo detalle del concentrado ID: {}", concentradoId);

        Comercializadora comercializadora = obtenerComercializadoraDelUsuario(usuarioId);

        Concentrado concentrado = concentradoRepository.findById(concentradoId)
                .orElseThrow(() -> new IllegalArgumentException("Concentrado no encontrado"));

        return convertirAResponseDto(concentrado);
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

        Concentrado concentrado = concentradoRepository.findById(concentradoId)
                .orElseThrow(() -> new IllegalArgumentException("Concentrado no encontrado"));

        // Validar estado
        if (!"venta_solicitada".equals(concentrado.getEstado())) {
            throw new IllegalArgumentException(
                    "El concentrado no tiene solicitud de venta pendiente. Estado actual: " + concentrado.getEstado()
            );
        }

        // Transicionar estado
        transicionarEstado(
                concentrado,
                "venta_en_revision",
                "Comercializadora está revisando la solicitud de venta",
                null,
                usuarioId,
                ipOrigen
        );

        // WebSocket
        publicarEventoWebSocket(concentrado, "venta_en_revision");

        return convertirAResponseDto(concentrado);
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

        Concentrado concentrado = concentradoRepository.findById(concentradoId)
                .orElseThrow(() -> new IllegalArgumentException("Concentrado no encontrado"));

        // Validar estado
        if (!"venta_en_revision".equals(concentrado.getEstado())) {
            throw new IllegalArgumentException(
                    "La venta no está en revisión. Estado actual: " + concentrado.getEstado()
            );
        }

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
        transicionarEstado(
                concentrado,
                "venta_liquidada",
                "Venta aprobada por comercializadora - Precio: " + aprobarDto.getPrecioVenta() + " BOB",
                aprobarDto.getObservaciones(),
                usuarioId,
                ipOrigen
        );

        // Registrar auditoría
        List<Map<String, Object>> historial = obtenerHistorial(concentrado);
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
        concentrado.setObservaciones(convertirHistorialAJson(historial));
        concentradoRepository.save(concentrado);

        // Notificar al socio e ingenio
        notificarVentaAprobada(concentrado, liquidacion, comercializadora);

        // WebSocket
        publicarEventoWebSocket(concentrado, "venta_liquidada");

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

        Concentrado concentrado = concentradoRepository.findById(concentradoId)
                .orElseThrow(() -> new IllegalArgumentException("Concentrado no encontrado"));

        // Validar estado
        if (!"venta_liquidada".equals(concentrado.getEstado())) {
            throw new IllegalArgumentException(
                    "La venta no está liquidada. Estado actual: " + concentrado.getEstado()
            );
        }

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
        transicionarEstado(
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
        List<Map<String, Object>> historial = obtenerHistorial(concentrado);
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
        concentrado.setObservaciones(convertirHistorialAJson(historial));
        concentradoRepository.save(concentrado);

        // Notificar a todos los involucrados
        notificarVentaCompletada(concentrado);

        // WebSocket
        publicarEventoWebSocket(concentrado, "concentrado_vendido");

        log.info("Venta completada - Concentrado ID: {} VENDIDO", concentradoId);

        return convertirAResponseDto(concentrado);
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

    // ==================== MÉTODOS AUXILIARES PRIVADOS ====================

    private Comercializadora obtenerComercializadoraDelUsuario(Integer usuarioId) {
        Usuarios usuario = usuariosRepository.findById(usuarioId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        return comercializadoraRepository.findByUsuariosId(usuario)
                .orElseThrow(() -> new IllegalArgumentException("Comercializadora no encontrada"));
    }

    private void transicionarEstado(
            Concentrado concentrado,
            String nuevoEstado,
            String descripcion,
            String observacionesAdicionales,
            Integer usuarioId,
            String ipOrigen
    ) {
        String estadoAnterior = concentrado.getEstado();
        concentrado.setEstado(nuevoEstado);

        List<Map<String, Object>> historial = obtenerHistorial(concentrado);
        Map<String, Object> registro = crearRegistroHistorial(
                nuevoEstado,
                descripcion,
                observacionesAdicionales,
                usuarioId,
                ipOrigen
        );
        registro.put("estado_anterior", estadoAnterior);
        historial.add(registro);

        concentrado.setObservaciones(convertirHistorialAJson(historial));
        concentradoRepository.save(concentrado);
    }

    private Map<String, Object> crearRegistroHistorial(
            String estado,
            String descripcion,
            String observaciones,
            Integer usuarioId,
            String ipOrigen
    ) {
        Map<String, Object> registro = new HashMap<>();
        registro.put("estado", estado);
        registro.put("descripcion", descripcion);
        registro.put("observaciones", observaciones);
        registro.put("usuario_id", usuarioId);
        registro.put("ip_origen", ipOrigen);
        registro.put("timestamp", LocalDateTime.now().toString());
        return registro;
    }

    private List<Map<String, Object>> obtenerHistorial(Concentrado concentrado) {
        if (concentrado.getObservaciones() == null || concentrado.getObservaciones().isBlank()) {
            return new ArrayList<>();
        }

        try {
            return objectMapper.readValue(
                    concentrado.getObservaciones(),
                    new TypeReference<List<Map<String, Object>>>() {}
            );
        } catch (JsonProcessingException e) {
            log.error("Error al parsear historial del concentrado ID: {}", concentrado.getId(), e);
            return new ArrayList<>();
        }
    }

    private String convertirHistorialAJson(List<Map<String, Object>> historial) {
        try {
            return objectMapper.writeValueAsString(historial);
        } catch (JsonProcessingException e) {
            log.error("Error al convertir historial a JSON", e);
            return "[]";
        }
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

    private void publicarEventoWebSocket(Concentrado concentrado, String evento) {
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("evento", evento);
            payload.put("concentradoId", concentrado.getId());
            payload.put("codigoConcentrado", concentrado.getCodigoConcentrado());
            payload.put("estado", concentrado.getEstado());
            payload.put("timestamp", LocalDateTime.now().toString());

            // Canal del ingenio
            String canalIngenio = "/topic/ingenio/" + concentrado.getIngenioMineroId().getId() + "/concentrados";
            messagingTemplate.convertAndSend(canalIngenio, payload);

            // Canal del socio
            String canalSocio = "/topic/socio/" + concentrado.getSocioPropietarioId().getId() + "/concentrados";
            messagingTemplate.convertAndSend(canalSocio, payload);

        } catch (Exception e) {
            log.error("Error al publicar evento WebSocket", e);
        }
    }

    private ConcentradoResponseDto convertirAResponseDto(Concentrado concentrado) {
        ConcentradoResponseDto dto = new ConcentradoResponseDto();
        dto.setId(concentrado.getId());
        dto.setCodigoConcentrado(concentrado.getCodigoConcentrado());
        dto.setEstado(concentrado.getEstado());
        dto.setPesoInicial(concentrado.getPesoInicial());
        dto.setPesoFinal(concentrado.getPesoFinal());
        dto.setMerma(concentrado.getMerma());
        dto.setMineralPrincipal(concentrado.getMineralPrincipal());
        dto.setNumeroSacos(concentrado.getNumeroSacos());
        dto.setIngenioId(concentrado.getIngenioMineroId().getId());
        dto.setIngenioNombre(concentrado.getIngenioMineroId().getRazonSocial());

        Socio socio = concentrado.getSocioPropietarioId();
        if (socio != null) {
            dto.setSocioId(socio.getId());
            Persona persona = personaRepository.findByUsuariosId(socio.getUsuariosId()).orElse(null);
            if (persona != null) {
                dto.setSocioNombres(persona.getNombres());
                dto.setSocioApellidos(persona.getPrimerApellido() + (persona.getSegundoApellido() != null ? " " + persona.getSegundoApellido() : ""));
                dto.setSocioCi(persona.getCi());
            }
        }

        List<LoteSimpleDto> lotesDto = concentrado.getLoteConcentradoRelacionList().stream()
                .map(relacion -> {
                    Lotes lote = relacion.getLoteComplejoId();
                    return LoteSimpleDto.builder()
                            .id(lote.getId())
                            .minaNombre(lote.getMinasId().getNombre())
                            .tipoMineral(lote.getTipoMineral())
                            .pesoTotalReal(lote.getPesoTotalReal())
                            .estado(lote.getEstado())
                            .build();
                })
                .collect(Collectors.toList());
        dto.setLotes(lotesDto);

        if (!concentrado.getLoteConcentradoRelacionList().isEmpty()) {
            Lotes primerLote = concentrado.getLoteConcentradoRelacionList().get(0).getLoteComplejoId();
            List<LoteMinerales> loteMinerales = loteMineralesRepository.findByLotesId(primerLote);
            dto.setMinerales(
                    loteMinerales.stream()
                            .map(lm -> new MineralInfoDto(lm.getMineralesId().getId(), lm.getMineralesId().getNombre(), lm.getMineralesId().getNomenclatura()))
                            .collect(Collectors.toList())
            );
        }

        dto.setFechaInicio(concentrado.getFechaInicio());
        dto.setFechaFin(concentrado.getFechaFin());
        dto.setCreatedAt(concentrado.getCreatedAt());
        dto.setUpdatedAt(concentrado.getUpdatedAt());

        try {
            List<Map<String, Object>> historial = objectMapper.readValue(concentrado.getObservaciones(), new TypeReference<List<Map<String, Object>>>() {});
            dto.setObservaciones(historial.isEmpty() ? null : historial.get(historial.size() - 1));
        } catch (Exception e) {
            log.warn("Error al parsear observaciones del concentrado ID: {}", concentrado.getId());
        }

        return dto;
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
            dto.setSocioApellidos(persona.getPrimerApellido() + (persona.getSegundoApellido() != null ? " " + persona.getSegundoApellido() : ""));
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