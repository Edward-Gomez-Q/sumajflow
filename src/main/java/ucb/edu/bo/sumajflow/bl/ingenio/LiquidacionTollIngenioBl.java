package ucb.edu.bo.sumajflow.bl.ingenio;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ucb.edu.bo.sumajflow.bl.LiquidacionTollBl;
import ucb.edu.bo.sumajflow.dto.ingenio.ConcentradoCreateDto;
import ucb.edu.bo.sumajflow.dto.ingenio.LiquidacionTollResponseDto;
import ucb.edu.bo.sumajflow.entity.*;
import ucb.edu.bo.sumajflow.repository.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Servicio de Liquidación de Toll para el ROL INGENIO
 * Responsabilidades:
 * - Crear liquidación al generar concentrados
 * - Listar liquidaciones del ingenio
 * - Ver detalle de liquidación
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LiquidacionTollIngenioBl {

    private final LiquidacionTollBl liquidacionTollBl;
    private final LiquidacionRepository liquidacionRepository;
    private final IngenioMineroRepository ingenioMineroRepository;
    private final UsuariosRepository usuariosRepository;

    // ==================== CREAR LIQUIDACIÓN ====================

    /**
     * Crear liquidación de Toll - CON VALIDACIONES DE INGENIO
     * Llamado desde ConcentradoIngenioBl al crear concentrados
     */
    @Transactional
    public Liquidacion crearLiquidacionToll(
            List<Lotes> lotes,
            Socio socio,
            IngenioMinero ingenio,
            BigDecimal costoPorTonelada,
            ConcentradoCreateDto createDto,
            Integer usuarioId
    ) {
        log.info("Ingenio ID: {} creando liquidación de Toll para {} lotes", ingenio.getId(), lotes.size());

        // Validar que el usuario pertenece al ingenio
        validarIngenioDelUsuario(usuarioId, ingenio);

        // Llamar al servicio general
        Liquidacion liquidacion = liquidacionTollBl.crearLiquidacionToll(
                lotes, socio, ingenio, costoPorTonelada, createDto
        );

        log.info("✅ Liquidación de Toll creada por ingenio - ID: {}", liquidacion.getId());

        return liquidacion;
    }

    // ==================== LISTAR LIQUIDACIONES ====================

    /**
     * Listar liquidaciones de Toll del ingenio - CON VALIDACIONES DE INGENIO
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
        log.debug("Listando liquidaciones de Toll para ingenio - Usuario ID: {}", usuarioId);

        IngenioMinero ingenio = obtenerIngenioDelUsuario(usuarioId);

        // Obtener liquidaciones relacionadas con el ingenio (a través de los lotes)
        List<Liquidacion> liquidacionesIngenio = liquidacionRepository
                .findByTipoLiquidacionOrderByCreatedAtDesc("toll")
                .stream()
                .filter(l -> perteneceAlIngenio(l, ingenio))
                .toList();

        // Llamar al servicio general para filtrar y paginar
        return liquidacionTollBl.listarLiquidaciones(
                liquidacionesIngenio, estado, fechaDesde, fechaHasta, page, size
        );
    }

    // ==================== OBTENER DETALLE ====================

    /**
     * Obtener detalle de liquidación - CON VALIDACIONES DE INGENIO
     */
    @Transactional(readOnly = true)
    public LiquidacionTollResponseDto obtenerDetalleLiquidacion(
            Integer liquidacionId,
            Integer usuarioId
    ) {
        log.debug("Obteniendo detalle de liquidación de Toll ID: {} para ingenio", liquidacionId);

        IngenioMinero ingenio = obtenerIngenioDelUsuario(usuarioId);
        Liquidacion liquidacion = obtenerLiquidacionConPermisos(liquidacionId, ingenio);

        // Llamar al servicio general para convertir a DTO
        return liquidacionTollBl.convertirADto(liquidacion);
    }

    // ==================== ESTADÍSTICAS ====================

    /**
     * Obtener estadísticas de liquidaciones del ingenio
     */
    @Transactional(readOnly = true)
    public Map<String, Object> obtenerEstadisticas(Integer usuarioId) {
        log.debug("Obteniendo estadísticas de liquidaciones de Toll para ingenio");

        IngenioMinero ingenio = obtenerIngenioDelUsuario(usuarioId);

        List<Liquidacion> todas = liquidacionRepository
                .findByTipoLiquidacionOrderByCreatedAtDesc("toll")
                .stream()
                .filter(l -> perteneceAlIngenio(l, ingenio))
                .toList();

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

    private IngenioMinero obtenerIngenioDelUsuario(Integer usuarioId) {
        Usuarios usuario = usuariosRepository.findById(usuarioId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        return ingenioMineroRepository.findByUsuariosId(usuario)
                .orElseThrow(() -> new IllegalArgumentException("Ingenio minero no encontrado"));
    }

    private void validarIngenioDelUsuario(Integer usuarioId, IngenioMinero ingenio) {
        IngenioMinero ingenioDelUsuario = obtenerIngenioDelUsuario(usuarioId);

        if (!ingenioDelUsuario.getId().equals(ingenio.getId())) {
            throw new IllegalArgumentException("No tienes permiso para crear liquidaciones para este ingenio");
        }
    }

    private Liquidacion obtenerLiquidacionConPermisos(Integer liquidacionId, IngenioMinero ingenio) {
        Liquidacion liquidacion = liquidacionTollBl.obtenerLiquidacion(liquidacionId);

        // Validar tipo
        if (!"toll".equals(liquidacion.getTipoLiquidacion())) {
            throw new IllegalArgumentException("Esta no es una liquidación de tipo Toll");
        }

        // Validar permisos: la liquidación debe pertenecer al ingenio
        if (!perteneceAlIngenio(liquidacion, ingenio)) {
            throw new IllegalArgumentException("No tienes permiso para acceder a esta liquidación");
        }

        return liquidacion;
    }

    private boolean perteneceAlIngenio(Liquidacion liquidacion, IngenioMinero ingenio) {
        // Verificar si alguno de los lotes de la liquidación pertenece al ingenio
        return liquidacion.getLiquidacionLoteList().stream()
                .anyMatch(ll -> ll.getLotesId().getLoteIngenioList().stream()
                        .anyMatch(li -> li.getIngenioMineroId().getId().equals(ingenio.getId())));
    }
}