package ucb.edu.bo.sumajflow.bl.ingenio;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ucb.edu.bo.sumajflow.bl.LiquidacionTollBl;
import ucb.edu.bo.sumajflow.bl.LiquidacionVentaBl;
import ucb.edu.bo.sumajflow.bl.NotificacionBl;
import ucb.edu.bo.sumajflow.dto.ingenio.ConcentradoCreateDto;
import ucb.edu.bo.sumajflow.dto.ingenio.LiquidacionTollResponseDto;
import ucb.edu.bo.sumajflow.entity.*;
import ucb.edu.bo.sumajflow.repository.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Servicio de Liquidación de Toll para el ROL INGENIO
 * Responsabilidades:
 * - Crear liquidación al generar concentrados
 * - Activar liquidación para pago al finalizar procesamiento
 * - Listar liquidaciones del ingenio
 * - Ver detalle de liquidación
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LiquidacionTollIngenioBl {

    private final LiquidacionTollBl liquidacionTollBl;
    private final LiquidacionRepository liquidacionRepository;
    private final LiquidacionLoteRepository liquidacionLoteRepository;
    private final AsignacionCamionRepository asignacionCamionRepository;
    private final IngenioMineroRepository ingenioMineroRepository;
    private final UsuariosRepository usuariosRepository;
    private final NotificacionBl notificacionBl;
    private final ObjectMapper objectMapper;
    private final LiquidacionVentaBl liquidacionVentaBl;

    // Constantes de costos
    private static final BigDecimal COSTO_RETROEXCAVADORA_GRANDE = new BigDecimal("500.00");
    private static final BigDecimal COSTO_RETROEXCAVADORA_PEQUENA = new BigDecimal("300.00");
    private static final BigDecimal COSTO_USO_BALANZA = new BigDecimal("20.00");
    private static final BigDecimal KG_A_TONELADAS = new BigDecimal("1000");

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

        // 1. Calcular peso total en kilogramos
        BigDecimal pesoTotalKg = lotes.stream()
                .map(Lotes::getPesoTotalReal)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);

        // 2. Convertir a toneladas
        BigDecimal pesoToneladas = pesoTotalKg.divide(KG_A_TONELADAS, 4, RoundingMode.HALF_UP);

        log.info("Peso total: {} kg ({} toneladas)", pesoTotalKg, pesoToneladas);

        // 3. Calcular costo de procesamiento
        BigDecimal costoProcesamientoTotal = pesoToneladas
                .multiply(costoPorTonelada)
                .setScale(4, RoundingMode.HALF_UP);

        // 4. Calcular servicios adicionales
        Map<String, Object> serviciosAdicionalesMap = calcularServiciosAdicionales(lotes, createDto);
        BigDecimal totalServiciosBob = (BigDecimal) serviciosAdicionalesMap.get("total_bob");
        BigDecimal totalServiciosUsd = totalServiciosBob.divide(
                createDto.getTipoCambio(), 4, RoundingMode.HALF_UP
        );
        serviciosAdicionalesMap.put("total_usd", totalServiciosUsd);

        log.info("Servicios adicionales: {} BOB ({} USD)", totalServiciosBob, totalServiciosUsd);

        // 5. Calcular totales en USD
        BigDecimal valorBrutoUsd = costoProcesamientoTotal.add(totalServiciosUsd);
        BigDecimal valorNetoUsd = valorBrutoUsd;
        BigDecimal valorNetoBob = valorNetoUsd.multiply(createDto.getTipoCambio()).setScale(4, RoundingMode.HALF_UP);

        log.info("Total: {} USD ({} BOB)", valorNetoUsd, valorNetoBob);

        // 6. Crear liquidación
        Liquidacion liquidacion = Liquidacion.builder()
                .socioId(socio)
                .tipoLiquidacion("toll")
                .pesoTotalEntrada(pesoTotalKg)
                .pesoTmh(pesoToneladas)
                .costoPorTonelada(costoPorTonelada)
                .costoProcesamientoTotal(costoProcesamientoTotal)
                .serviciosAdicionales(convertirAJson(serviciosAdicionalesMap))
                .totalServiciosAdicionales(totalServiciosUsd)
                .valorBrutoUsd(valorBrutoUsd)
                .valorNetoUsd(valorNetoUsd)
                .tipoCambio(createDto.getTipoCambio())
                .valorNetoBob(valorNetoBob)
                .moneda("BOB")
                .estado("pendiente_procesamiento")
                .build();

        liquidacion = liquidacionRepository.save(liquidacion);
        liquidacionVentaBl.agregarObservacion(
                liquidacion,
                "pendiente_procesamiento",
                "Liquidación de Toll creada",
                "Liquidación de servicio de procesamiento - " + lotes.size() + " lotes. Esperando finalización de procesamiento.",
                "ingenio",
                null,
                Map.of(
                        "cantidad_lotes", lotes.size(),
                        "peso_total_kg", pesoTotalKg,
                        "costo_procesamiento_total", costoProcesamientoTotal
                )
        );

        // 7. Crear relaciones con lotes
        for (Lotes lote : lotes) {
            LiquidacionLote liquidacionLote = LiquidacionLote.builder()
                    .liquidacionId(liquidacion)
                    .lotesId(lote)
                    .pesoEntrada(lote.getPesoTotalReal())
                    .build();

            liquidacion.addLiquidacionLote(liquidacionLote);
            liquidacionLoteRepository.save(liquidacionLote);
        }

        log.info("✅ Liquidación de Toll creada - ID: {}, Estado: pendiente_procesamiento, Total: {} BOB",
                liquidacion.getId(), valorNetoBob);

        return liquidacion;
    }

    // ==================== ACTIVAR PARA PAGO ====================

    /**
     * Activar liquidación para pago
     */
    @Transactional
    public void activarLiquidacionParaPago(List<Lotes> lotes, Integer usuarioId) {
        log.info("Activando liquidación de Toll para pago - {} lotes", lotes.size());

        // Validar que el usuario sea del ingenio
        IngenioMinero ingenio = obtenerIngenioDelUsuario(usuarioId);

        Liquidacion liquidacion = liquidacionTollBl.buscarLiquidacionPorLotes(lotes);

        if (liquidacion == null) {
            throw new IllegalStateException("No se encontró liquidación de Toll para los lotes procesados");
        }

        // Validar que la liquidación pertenece al ingenio
        if (!perteneceAlIngenio(liquidacion, ingenio)) {
            throw new IllegalArgumentException("No tienes permiso para activar esta liquidación");
        }

        if (!"pendiente_procesamiento".equals(liquidacion.getEstado())) {
            log.warn("La liquidación ID: {} no está en estado pendiente_procesamiento. Estado actual: {}",
                    liquidacion.getId(), liquidacion.getEstado());
            return;
        }

        // Cambiar estado a esperando_pago
        liquidacion.setEstado("esperando_pago");
        liquidacionVentaBl.agregarObservacion(
                liquidacion,
                "esperando_pago",
                "Liquidación activada para pago",
                "Procesamiento finalizado. Listo para que el socio realice el pago.",
                "ingenio",
                "pendiente_procesamiento",
                Map.of(
                        "valor_neto_bob", liquidacion.getValorNetoBob()
                )
        );

        liquidacionRepository.save(liquidacion);

        // Notificar al socio
        notificarSocioParaPago(liquidacion);

        log.info("✅ Liquidación activada para pago - ID: {}", liquidacion.getId());

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

    private Map<String, Object> calcularServiciosAdicionales(List<Lotes> lotes, ConcentradoCreateDto createDto) {
        Map<String, Object> serviciosAdicionalesMap = new HashMap<>();
        BigDecimal totalServiciosBob = BigDecimal.ZERO;

        // Retroexcavadoras grandes
        if (createDto.getCantidadRetroexcavadoraGrande() != null && createDto.getCantidadRetroexcavadoraGrande() > 0) {
            BigDecimal costoRetroGrande = COSTO_RETROEXCAVADORA_GRANDE
                    .multiply(new BigDecimal(createDto.getCantidadRetroexcavadoraGrande()));

            serviciosAdicionalesMap.put("retroexcavadora_grande", Map.of(
                    "cantidad", createDto.getCantidadRetroexcavadoraGrande(),
                    "costo_unitario", COSTO_RETROEXCAVADORA_GRANDE,
                    "costo_total", costoRetroGrande,
                    "moneda", "BOB"
            ));

            totalServiciosBob = totalServiciosBob.add(costoRetroGrande);
        }

        // Retroexcavadoras pequeñas
        if (createDto.getCantidadRetroexcavadoraPequena() != null && createDto.getCantidadRetroexcavadoraPequena() > 0) {
            BigDecimal costoRetroPequena = COSTO_RETROEXCAVADORA_PEQUENA
                    .multiply(new BigDecimal(createDto.getCantidadRetroexcavadoraPequena()));

            serviciosAdicionalesMap.put("retroexcavadora_pequena", Map.of(
                    "cantidad", createDto.getCantidadRetroexcavadoraPequena(),
                    "costo_unitario", COSTO_RETROEXCAVADORA_PEQUENA,
                    "costo_total", costoRetroPequena,
                    "moneda", "BOB"
            ));

            totalServiciosBob = totalServiciosBob.add(costoRetroPequena);
        }

        // Uso de balanza
        int totalCamiones = calcularTotalCamiones(lotes);
        BigDecimal costoBalanzas = COSTO_USO_BALANZA.multiply(new BigDecimal(totalCamiones));

        serviciosAdicionalesMap.put("uso_balanza", Map.of(
                "cantidad_camiones", totalCamiones,
                "costo_unitario", COSTO_USO_BALANZA,
                "costo_total", costoBalanzas,
                "moneda", "BOB"
        ));

        totalServiciosBob = totalServiciosBob.add(costoBalanzas);
        serviciosAdicionalesMap.put("total_bob", totalServiciosBob);

        return serviciosAdicionalesMap;
    }

    private int calcularTotalCamiones(List<Lotes> lotes) {
        return lotes.stream()
                .mapToInt(asignacionCamionRepository::countByLotesId)
                .sum();
    }

    private String convertirAJson(Map<String, Object> mapa) {
        try {
            return objectMapper.writeValueAsString(mapa);
        } catch (JsonProcessingException e) {
            log.error("Error al convertir mapa a JSON", e);
            return "{}";
        }
    }

    private void notificarSocioParaPago(Liquidacion liquidacion) {
        Integer socioUsuarioId = liquidacion.getSocioId().getUsuariosId().getId();

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("liquidacionId", liquidacion.getId());
        metadata.put("tipoLiquidacion", "toll");
        metadata.put("valorNetoBob", liquidacion.getValorNetoBob());

        notificacionBl.crearNotificacion(
                socioUsuarioId,
                "warning",
                "Pago de Toll pendiente",
                String.format("El procesamiento de tu mineral ha finalizado. Debes realizar el pago de %.2f BOB " +
                        "para que tu concentrado este listo para la venta.", liquidacion.getValorNetoBob()),
                metadata
        );
    }

    public boolean verificarLiquidacionPagadaEnConcentradosDelMismoLote(Concentrado concentrado) {
        return concentrado.getLoteConcentradoRelacionList().stream()
                .map(LoteConcentradoRelacion::getLoteComplejoId)
                .anyMatch(lote -> {
                    Liquidacion liquidacion = liquidacionTollBl.buscarLiquidacionPorLotes(List.of(lote));
                    log.info("Verificando liquidación de Toll para lote ID: {} - Liquidación encontrada: {}",
                            lote.getId(), liquidacion != null ? liquidacion.getId() : "Ninguna");
                    return liquidacion != null && "pagado".equals(liquidacion.getEstado());
                });
    }
}