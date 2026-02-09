package ucb.edu.bo.sumajflow.bl.socio;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ucb.edu.bo.sumajflow.bl.*;
import ucb.edu.bo.sumajflow.dto.ingenio.LiquidacionTollResponseDto;
import ucb.edu.bo.sumajflow.dto.socio.LiquidacionPagoDto;
import ucb.edu.bo.sumajflow.entity.*;
import ucb.edu.bo.sumajflow.repository.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Servicio de Liquidación de Toll para el ROL SOCIO
 * Responsabilidades:
 * - Listar sus liquidaciones de Toll
 * - Ver detalle de liquidación
 * - Registrar pago de liquidación
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LiquidacionTollSocioBl {

    private final LiquidacionTollBl liquidacionTollBl;
    private final LiquidacionRepository liquidacionRepository;
    private final ConcentradoRepository concentradoRepository;
    private final UsuariosRepository usuariosRepository;
    private final SocioRepository socioRepository;
    private final NotificacionBl notificacionBl;
    private final ConcentradoBl concentradoBl;
    private final LiquidacionVentaBl liquidacionVentaBl;

    // ==================== LISTAR LIQUIDACIONES ====================

    /**
     * Listar liquidaciones de Toll del socio - CON VALIDACIONES DE SOCIO
     */
    @Transactional(readOnly = true)
    public Page<LiquidacionTollResponseDto> listarLiquidacionesToll(
            Integer usuarioId,
            String estado,
            LocalDateTime fechaDesde,
            LocalDateTime fechaHasta,
            int page,
            int size
    ) {
        log.debug("Listando liquidaciones de Toll para socio - Usuario ID: {}", usuarioId);

        Socio socio = obtenerSocioDelUsuario(usuarioId);

        // Obtener liquidaciones del socio
        List<Liquidacion> liquidacionesSocio = liquidacionRepository
                .findBySocioIdAndTipoLiquidacionOrderByCreatedAtDesc(socio, "toll");

        // Llamar al servicio general para filtrar y paginar
        return liquidacionTollBl.listarLiquidaciones(
                liquidacionesSocio, estado, fechaDesde, fechaHasta, page, size
        );
    }

    // ==================== OBTENER DETALLE ====================

    /**
     * Obtener detalle de liquidación - CON VALIDACIONES DE SOCIO
     */
    @Transactional(readOnly = true)
    public LiquidacionTollResponseDto obtenerDetalleLiquidacion(
            Integer liquidacionId,
            Integer usuarioId
    ) {
        log.debug("Obteniendo detalle de liquidación de Toll ID: {} para socio", liquidacionId);

        Socio socio = obtenerSocioDelUsuario(usuarioId);
        Liquidacion liquidacion = obtenerLiquidacionConPermisos(liquidacionId, socio);

        // Llamar al servicio general para convertir a DTO
        return liquidacionTollBl.convertirADto(liquidacion);
    }

    // ==================== REGISTRAR PAGO ====================

    /**
     * Registrar pago de liquidación
     */
    public LiquidacionTollResponseDto registrarPago(
            Integer liquidacionId,
            LiquidacionPagoDto pagoDto,
            Integer usuarioId,
            String ipOrigen
    ) {
        log.info("Registrando pago de liquidación de Toll ID: {} por socio", liquidacionId);

        // 1. Validaciones
        Socio socio = obtenerSocioDelUsuario(usuarioId);
        Liquidacion liquidacion = obtenerLiquidacionConPermisos(liquidacionId, socio);

        validarEstadoParaPago(liquidacion);
        validarDatosPago(pagoDto);

        // 2. Registrar pago
        liquidacion.setEstado("pagado");
        liquidacion.setFechaPago(LocalDateTime.now());
        liquidacion.setMetodoPago(pagoDto.getMetodoPago());
        liquidacion.setNumeroComprobante(pagoDto.getNumeroComprobante());
        liquidacion.setUrlComprobante(pagoDto.getUrlComprobante());

        if (pagoDto.getObservaciones() != null && !pagoDto.getObservaciones().isBlank()) {
            liquidacionVentaBl.agregarObservacion(
                    liquidacion,
                    "pagado",
                    "Pago registrado por el socio",
                    pagoDto.getObservaciones(),
                    "socio",
                    "esperando_pago",
                    Map.of(
                            "metodo_pago", pagoDto.getMetodoPago(),
                            "numero_comprobante", pagoDto.getNumeroComprobante()
                    )
            );
        }

        liquidacionRepository.save(liquidacion);

        // 3. Actualizar estado de concentrados a "listo_para_venta"
        actualizarEstadoConcentrados(liquidacion);

        // 4. Notificar al ingenio
        notificarPagoRealizado(liquidacion);
        //Notificar que se ha realizado el pago para el primer concentrado encontrado
        liquidacion.getLiquidacionLoteList().stream()
                .findFirst()
                .map(LiquidacionLote::getLotesId)
                .map(Lotes::getLoteConcentradoRelacionList)
                .flatMap(relaciones -> relaciones.stream().findFirst())
                .map(LoteConcentradoRelacion::getConcentradoId)
                .ifPresent(concentrado ->

                        //Mostrar al ingeino
                        concentradoBl.publicarEventoWebSocketIngenio(concentrado, "listo_para_venta")
                );

        log.info("✅ Pago registrado exitosamente - Liquidación ID: {}", liquidacionId);

        return liquidacionTollBl.convertirADto(liquidacion);
    }

    // ==================== ESTADÍSTICAS ====================

    /**
     * Obtener estadísticas de liquidaciones del socio
     */
    @Transactional(readOnly = true)
    public Map<String, Object> obtenerEstadisticas(Integer usuarioId) {
        log.debug("Obteniendo estadísticas de liquidaciones de Toll para socio");

        Socio socio = obtenerSocioDelUsuario(usuarioId);

        List<Liquidacion> todas = liquidacionRepository
                .findBySocioIdAndTipoLiquidacionOrderByCreatedAtDesc(socio, "toll");

        Map<String, Object> stats = new HashMap<>();
        stats.put("total", todas.size());
        stats.put("pendienteProcesamiento", todas.stream()
                .filter(l -> "pendiente_procesamiento".equals(l.getEstado()))
                .count());
        stats.put("esperandoPago", todas.stream()
                .filter(l -> "esperando_pago".equals(l.getEstado()))
                .count());
        stats.put("pagadas", todas.stream()
                .filter(l -> "pagado".equals(l.getEstado()))
                .count());
        stats.put("totalPagadoBob", todas.stream()
                .filter(l -> "pagado".equals(l.getEstado()))
                .map(Liquidacion::getValorNetoBob)
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add));
        stats.put("totalPendienteBob", todas.stream()
                .filter(l -> "esperando_pago".equals(l.getEstado()))
                .map(Liquidacion::getValorNetoBob)
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add));

        return stats;
    }

    // ==================== MÉTODOS PRIVADOS ====================

    private Socio obtenerSocioDelUsuario(Integer usuarioId) {
        Usuarios usuario = usuariosRepository.findById(usuarioId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        return socioRepository.findByUsuariosId(usuario)
                .orElseThrow(() -> new IllegalArgumentException("Socio no encontrado"));
    }

    private Liquidacion obtenerLiquidacionConPermisos(Integer liquidacionId, Socio socio) {
        Liquidacion liquidacion = liquidacionTollBl.obtenerLiquidacion(liquidacionId);

        // Validar tipo
        if (!"toll".equals(liquidacion.getTipoLiquidacion())) {
            throw new IllegalArgumentException("Esta no es una liquidación de tipo Toll");
        }

        // Validar permisos
        if (!liquidacion.getSocioId().getId().equals(socio.getId())) {
            throw new IllegalArgumentException("No tienes permiso para acceder a esta liquidación");
        }

        return liquidacion;
    }

    private void validarEstadoParaPago(Liquidacion liquidacion) {
        if (!"esperando_pago".equals(liquidacion.getEstado())) {
            throw new IllegalArgumentException(
                    "La liquidación debe estar en estado 'esperando_pago' para registrar el pago. " +
                            "Estado actual: " + liquidacion.getEstado()
            );
        }
    }

    private void validarDatosPago(LiquidacionPagoDto pagoDto) {
        if (pagoDto.getMetodoPago() == null || pagoDto.getMetodoPago().isBlank()) {
            throw new IllegalArgumentException("El método de pago es requerido");
        }

        if (pagoDto.getNumeroComprobante() == null || pagoDto.getNumeroComprobante().isBlank()) {
            throw new IllegalArgumentException("El número de comprobante es requerido");
        }

        if (pagoDto.getUrlComprobante() == null || pagoDto.getUrlComprobante().isBlank()) {
            throw new IllegalArgumentException("La URL del comprobante es requerida");
        }
    }

    private void actualizarEstadoConcentrados(Liquidacion liquidacion) {
        liquidacion.getLiquidacionLoteList().forEach(ll -> {
            Lotes lote = ll.getLotesId();

            List<Concentrado> concentrados = concentradoRepository.findAll().stream()
                    .filter(c -> c.getLoteConcentradoRelacionList().stream()
                            .anyMatch(rel -> rel.getLoteComplejoId().getId().equals(lote.getId())))
                    .filter(c -> "esperando_pago".equals(c.getEstado()))
                    .toList();

            concentrados.forEach(c -> {
                c.setEstado("listo_para_venta");
                concentradoRepository.save(c);
                log.info("✅ Concentrado ID: {} actualizado a 'listo_para_venta'", c.getId());
            });
        });
    }

    private void notificarPagoRealizado(Liquidacion liquidacion) {
        // Obtener ingenio (a través de los lotes)
        Integer ingenioUsuarioId = liquidacion.getLiquidacionLoteList().stream()
                .findFirst().flatMap(ll -> ll.getLotesId().getLoteIngenioList().stream()
                        .findFirst()
                        .map(li -> li.getIngenioMineroId().getUsuariosId().getId()))
                .orElse(null);

        if (ingenioUsuarioId == null) {
            log.warn("No se pudo obtener el ingenio para notificar el pago");
            return;
        }

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("liquidacionId", liquidacion.getId());
        metadata.put("tipoLiquidacion", "toll");
        metadata.put("valorNetoBob", liquidacion.getValorNetoBob());
        metadata.put("socioNombre", liquidacion.getSocioId().getUsuariosId().getPersona().getNombres());

        notificacionBl.crearNotificacion(
                ingenioUsuarioId,
                "success",
                "Pago de Toll recibido",
                String.format("El socio ha pagado la liquidación de servicio de procesamiento por %.2f BOB",
                        liquidacion.getValorNetoBob()),
                metadata
        );
    }
}