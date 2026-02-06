package ucb.edu.bo.sumajflow.bl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ucb.edu.bo.sumajflow.dto.ingenio.ConcentradoCreateDto;
import ucb.edu.bo.sumajflow.dto.ingenio.LiquidacionTollResponseDto;
import ucb.edu.bo.sumajflow.dto.ingenio.LoteSimpleDto;
import ucb.edu.bo.sumajflow.entity.*;
import ucb.edu.bo.sumajflow.repository.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Servicio GENERAL de Liquidación de Toll
 * Lógica común compartida por todos los roles
 * Sin validaciones de permisos - solo lógica pura
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LiquidacionTollBl {

    private final LiquidacionRepository liquidacionRepository;
    private final LiquidacionLoteRepository liquidacionLoteRepository;
    private final AsignacionCamionRepository asignacionCamionRepository;
    private final PersonaRepository personaRepository;
    private final LotesRepository lotesRepository;
    private final NotificacionBl notificacionBl;
    private final ObjectMapper objectMapper;

    // Constantes de costos
    private static final BigDecimal COSTO_RETROEXCAVADORA_GRANDE = new BigDecimal("500.00");
    private static final BigDecimal COSTO_RETROEXCAVADORA_PEQUENA = new BigDecimal("300.00");
    private static final BigDecimal COSTO_USO_BALANZA = new BigDecimal("20.00");
    private static final BigDecimal KG_A_TONELADAS = new BigDecimal("1000");

    // ==================== CREAR LIQUIDACIÓN ====================

    /**
     * Crear liquidación de Toll - MeTODO GENERAL
     * Llamado desde LiquidacionTollIngenioBl al crear concentrados
     */
    @Transactional
    public Liquidacion crearLiquidacionToll(
            List<Lotes> lotes,
            Socio socio,
            IngenioMinero ingenio,
            BigDecimal costoPorTonelada,
            ConcentradoCreateDto createDto
    ) {
        log.info("Creando liquidación de Toll para {} lotes del socio ID: {}", lotes.size(), socio.getId());

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
                .observaciones("Liquidación de servicio de procesamiento (Toll) - " + lotes.size() + " lotes. " +
                        "Esperando finalización de procesamiento.")
                .build();

        liquidacion = liquidacionRepository.save(liquidacion);

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
     * Activar liquidación para pago - MeTODO GENERAL
     * Llamado desde KanbanIngenioBl cuando finaliza el procesamiento
     */
    @Transactional
    public Liquidacion activarLiquidacionParaPago(List<Lotes> lotes) {
        log.info("Activando liquidación de Toll para pago - {} lotes", lotes.size());

        Liquidacion liquidacion = buscarLiquidacionPorLotes(lotes);

        if (liquidacion == null) {
            throw new IllegalStateException("No se encontró liquidación de Toll para los lotes procesados");
        }

        if (!"pendiente_procesamiento".equals(liquidacion.getEstado())) {
            log.warn("La liquidación ID: {} no está en estado pendiente_procesamiento. Estado actual: {}",
                    liquidacion.getId(), liquidacion.getEstado());
            return liquidacion;
        }

        // Cambiar estado a esperando_pago
        liquidacion.setEstado("esperando_pago");
        liquidacion.setObservaciones(liquidacion.getObservaciones() +
                " | ACTIVADA PARA PAGO: " + LocalDateTime.now());

        liquidacionRepository.save(liquidacion);

        // Notificar al socio
        notificarSocioParaPago(liquidacion);

        log.info("✅ Liquidación activada para pago - ID: {}", liquidacion.getId());

        return liquidacion;
    }

    // ==================== REGISTRAR PAGO ====================

    /**
     * Registrar pago de liquidación - MeTODO GENERAL
     * Llamado desde LiquidacionTollSocioBl
     */
    @Transactional
    public Liquidacion registrarPago(
            Liquidacion liquidacion,
            String metodoPago,
            String numeroComprobante,
            String urlComprobante,
            String observaciones
    ) {
        log.info("Registrando pago de liquidación ID: {}", liquidacion.getId());

        liquidacion.setEstado("pagado");
        liquidacion.setFechaPago(LocalDateTime.now());
        liquidacion.setMetodoPago(metodoPago);
        liquidacion.setNumeroComprobante(numeroComprobante);
        liquidacion.setUrlComprobante(urlComprobante);

        if (observaciones != null && !observaciones.isBlank()) {
            String obsActuales = liquidacion.getObservaciones() != null ? liquidacion.getObservaciones() : "";
            liquidacion.setObservaciones(obsActuales + " | PAGO: " + observaciones);
        }

        liquidacionRepository.save(liquidacion);

        log.info("✅ Pago registrado - Liquidación ID: {}", liquidacion.getId());

        return liquidacion;
    }

    // ==================== LISTAR LIQUIDACIONES ====================

    /**
     * Listar liquidaciones con filtros y paginación - MeTODO GENERAL
     */
    @Transactional(readOnly = true)
    public Page<LiquidacionTollResponseDto> listarLiquidaciones(
            List<Liquidacion> liquidacionesBase,
            String estado,
            LocalDateTime fechaDesde,
            LocalDateTime fechaHasta,
            int page,
            int size
    ) {
        log.debug("Aplicando filtros a {} liquidaciones", liquidacionesBase.size());

        // Aplicar filtros
        List<Liquidacion> liquidacionesFiltradas = liquidacionesBase.stream()
                .filter(l -> aplicarFiltros(l, estado, fechaDesde, fechaHasta))
                .toList();

        // Paginar
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), liquidacionesFiltradas.size());

        List<LiquidacionTollResponseDto> liquidacionesDto = liquidacionesFiltradas
                .subList(start, end)
                .stream()
                .map(this::convertirADto)
                .collect(Collectors.toList());

        return new PageImpl<>(liquidacionesDto, pageable, liquidacionesFiltradas.size());
    }

    // ==================== BÚSQUEDA ====================

    /**
     * Buscar liquidación por lote - MeTODO GENERAL
     */
    @Transactional(readOnly = true)
    public LiquidacionTollResponseDto buscarLiquidacionPorLote(Integer loteId) {
        log.debug("Buscando liquidación de Toll para lote ID: {}", loteId);

        Lotes lote = lotesRepository.findById(loteId)
                .orElseThrow(() -> new IllegalArgumentException("Lote no encontrado"));

        List<LiquidacionLote> liquidacionesLote = liquidacionLoteRepository.findByLotesId(lote);

        if (liquidacionesLote.isEmpty()) {
            return null;
        }

        Liquidacion liquidacion = liquidacionesLote.stream()
                .map(LiquidacionLote::getLiquidacionId)
                .filter(liq -> "toll".equals(liq.getTipoLiquidacion()))
                .findFirst()
                .orElse(null);

        if (liquidacion == null) {
            return null;
        }

        return convertirADto(liquidacion);
    }

    /**
     * Buscar liquidación por múltiples lotes - METODO GENERAL
     */
    @Transactional(readOnly = true)
    public Liquidacion buscarLiquidacionPorLotes(List<Lotes> lotes) {
        if (lotes.isEmpty()) {
            return null;
        }

        List<LiquidacionLote> liquidacionesLote = liquidacionLoteRepository.findByLotesId(lotes.getFirst());

        return liquidacionesLote.stream()
                .map(LiquidacionLote::getLiquidacionId)
                .filter(liq -> "toll".equals(liq.getTipoLiquidacion()))
                .findFirst()
                .orElse(null);
    }

    /**
     * Obtener liquidación por ID - METODO GENERAL
     */
    @Transactional(readOnly = true)
    public Liquidacion obtenerLiquidacion(Integer liquidacionId) {
        return liquidacionRepository.findById(liquidacionId)
                .orElseThrow(() -> new IllegalArgumentException("Liquidación no encontrada"));
    }

    // ==================== CONVERSIÓN A DTO ====================

    /**
     * Convertir a DTO - METODO GENERAL
     */
    public LiquidacionTollResponseDto convertirADto(Liquidacion liquidacion) {
        Socio socio = liquidacion.getSocioId();
        Persona persona = personaRepository.findByUsuariosId(socio.getUsuariosId()).orElse(null);

        List<LoteSimpleDto> lotesDto = liquidacion.getLiquidacionLoteList().stream()
                .map(ll -> LoteSimpleDto.builder()
                        .id(ll.getLotesId().getId())
                        .minaNombre(ll.getLotesId().getMinasId().getNombre())
                        .tipoMineral(ll.getLotesId().getTipoMineral())
                        .pesoTotalReal(ll.getPesoEntrada())
                        .estado(ll.getLotesId().getEstado())
                        .build())
                .collect(Collectors.toList());

        Map<String, Object> serviciosAdicionales = null;
        try {
            if (liquidacion.getServiciosAdicionales() != null) {
                serviciosAdicionales = objectMapper.readValue(
                        liquidacion.getServiciosAdicionales(),
                        Map.class
                );
            }
        } catch (Exception e) {
            log.warn("Error al parsear servicios adicionales", e);
        }

        int totalCamiones = 0;
        if (serviciosAdicionales != null && serviciosAdicionales.containsKey("uso_balanza")) {
            Map<String, Object> usoBalanza = (Map<String, Object>) serviciosAdicionales.get("uso_balanza");
            totalCamiones = (Integer) usoBalanza.get("cantidad_camiones");
        }

        return LiquidacionTollResponseDto.builder()
                .id(liquidacion.getId())
                .tipoLiquidacion(liquidacion.getTipoLiquidacion())
                .estado(liquidacion.getEstado())
                .socioId(socio.getId())
                .socioNombres(persona != null ? persona.getNombres() : null)
                .socioApellidos(persona != null ?
                        persona.getPrimerApellido() +
                                (persona.getSegundoApellido() != null ? " " + persona.getSegundoApellido() : "") : null)
                .socioCi(persona != null ? persona.getCi() : null)
                .pesoTotalEntradaKg(liquidacion.getPesoTotalEntrada())
                .pesoTotalToneladas(liquidacion.getPesoTmh())
                .costoPorTonelada(liquidacion.getCostoPorTonelada())
                .costoProcesamientoTotal(liquidacion.getCostoProcesamientoTotal())
                .serviciosAdicionales(serviciosAdicionales)
                .totalServiciosAdicionales(liquidacion.getTotalServiciosAdicionales())
                .valorBrutoUsd(liquidacion.getValorBrutoUsd())
                .valorNetoUsd(liquidacion.getValorNetoUsd())
                .tipoCambio(liquidacion.getTipoCambio())
                .valorNetoBob(liquidacion.getValorNetoBob())
                .moneda(liquidacion.getMoneda())
                .lotes(lotesDto)
                .totalLotes(lotesDto.size())
                .totalCamiones(totalCamiones)
                .fechaAprobacion(liquidacion.getFechaAprobacion())
                .fechaPago(liquidacion.getFechaPago())
                .metodoPago(liquidacion.getMetodoPago())
                .numeroComprobante(liquidacion.getNumeroComprobante())
                .urlComprobante(liquidacion.getUrlComprobante())
                .observaciones(liquidacion.getObservaciones())
                .createdAt(liquidacion.getCreatedAt())
                .updatedAt(liquidacion.getUpdatedAt())
                .build();
    }

    // ==================== MeTODOS AUXILIARES PRIVADOS ====================

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

    private boolean aplicarFiltros(
            Liquidacion l,
            String estado,
            LocalDateTime fechaDesde,
            LocalDateTime fechaHasta
    ) {
        if (estado != null && !estado.isEmpty() && !estado.equals(l.getEstado())) {
            return false;
        }
        if (fechaDesde != null && l.getCreatedAt().isBefore(fechaDesde)) {
            return false;
        }
        return fechaHasta == null || !l.getCreatedAt().isAfter(fechaHasta);
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
}