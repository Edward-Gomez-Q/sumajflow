package ucb.edu.bo.sumajflow.bl;

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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ucb.edu.bo.sumajflow.dto.ingenio.LoteSimpleDto;
import ucb.edu.bo.sumajflow.dto.venta.VentaLiquidacionResponseDto;
import ucb.edu.bo.sumajflow.dto.venta.VentaLiquidacionResponseDto.*;
import ucb.edu.bo.sumajflow.entity.*;
import ucb.edu.bo.sumajflow.repository.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Servicio GENERAL de Liquidación de Venta
 * Contiene lógica común compartida por socio y comercializadora:
 * - Conversión a DTO
 * - Filtrado y paginación
 * - Cálculos de venta (cotización, deducciones, valor neto)
 * - Promedio de reportes químicos
 * Tipos soportados: venta_concentrado, venta_lote_complejo
 * ESTADOS:
 * pendiente_aprobacion → aprobado → esperando_reportes → esperando_cierre_venta
 * → cerrado → pagado
 * (rechazado en cualquier punto antes de cerrado)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LiquidacionVentaBl {

    private final LiquidacionRepository liquidacionRepository;
    private final LiquidacionConcentradoRepository liquidacionConcentradoRepository;
    private final LiquidacionLoteRepository liquidacionLoteRepository;
    private final PersonaRepository personaRepository;
    private final ReporteQuimicoRepository reporteQuimicoRepository;
    private final ObjectMapper objectMapper;

    // Tipos de liquidación de venta
    public static final String TIPO_VENTA_CONCENTRADO = "venta_concentrado";
    public static final String TIPO_VENTA_LOTE_COMPLEJO = "venta_lote_complejo";
    public static final List<String> TIPOS_VENTA = List.of(TIPO_VENTA_CONCENTRADO, TIPO_VENTA_LOTE_COMPLEJO);

    // ==================== OBTENER ====================

    @Transactional(readOnly = true)
    public Liquidacion obtenerLiquidacion(Integer liquidacionId) {
        return liquidacionRepository.findById(liquidacionId)
                .orElseThrow(() -> new IllegalArgumentException("Liquidación de venta no encontrada"));
    }

    // ==================== LISTAR CON FILTROS ====================

    @Transactional(readOnly = true)
    public Page<VentaLiquidacionResponseDto> listarLiquidaciones(
            List<Liquidacion> liquidacionesBase,
            String estado,
            String tipoLiquidacion,
            LocalDateTime fechaDesde,
            LocalDateTime fechaHasta,
            int page,
            int size
    ) {
        log.debug("Aplicando filtros a {} liquidaciones de venta", liquidacionesBase.size());

        List<Liquidacion> filtradas = liquidacionesBase.stream()
                .filter(l -> aplicarFiltros(l, estado, tipoLiquidacion, fechaDesde, fechaHasta))
                .toList();

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), filtradas.size());

        List<VentaLiquidacionResponseDto> dtos = filtradas.subList(start, end).stream()
                .map(this::convertirADto)
                .collect(Collectors.toList());

        return new PageImpl<>(dtos, pageable, filtradas.size());
    }

    private boolean aplicarFiltros(
            Liquidacion l, String estado, String tipoLiquidacion,
            LocalDateTime fechaDesde, LocalDateTime fechaHasta
    ) {
        if (estado != null && !estado.isEmpty() && !estado.equals(l.getEstado())) {
            return false;
        }
        if (tipoLiquidacion != null && !tipoLiquidacion.isEmpty() && !tipoLiquidacion.equals(l.getTipoLiquidacion())) {
            return false;
        }
        if (fechaDesde != null && l.getCreatedAt().isBefore(fechaDesde)) {
            return false;
        }
        return fechaHasta == null || !l.getCreatedAt().isAfter(fechaHasta);
    }
    /**
     * Calcular venta con deducciones aplicadas sobre diferentes bases de cálculo
     * @param valorBrutoPrincipal - Valor del mineral principal (Pb o Zn)
     * @param valorBrutoAg - Valor de la plata (Ag)
     * @param deducciones - Lista de deducciones con sus bases de cálculo
     * @param tipoCambio - Tipo de cambio USD a BOB
     */
    public CalculoVentaResult calcularVentaConDeduccionesEspecificas(
            BigDecimal valorBrutoPrincipal,
            BigDecimal valorBrutoAg,
            List<DeduccionInput> deducciones,
            BigDecimal tipoCambio
    ) {
        BigDecimal valorBrutoTotal = valorBrutoPrincipal.add(valorBrutoAg);

        List<DeduccionResult> deduccionesResult = new ArrayList<>();
        BigDecimal totalDeduccionesUsd = BigDecimal.ZERO;

        for (DeduccionInput ded : deducciones) {
            BigDecimal baseCalculo = switch (ded.baseCalculo()) {
                case "valor_bruto_principal" -> valorBrutoPrincipal;
                case "valor_bruto_ag" -> valorBrutoAg;
                default -> valorBrutoTotal;
            };

            // Determinar base de cálculo

            BigDecimal montoDeducido = baseCalculo
                    .multiply(ded.porcentaje())
                    .divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);

            BigDecimal montoDeducidoBob = montoDeducido
                    .multiply(tipoCambio)
                    .setScale(4, RoundingMode.HALF_UP);

            deduccionesResult.add(new DeduccionResult(
                    ded.concepto(),
                    ded.porcentaje(),
                    ded.tipoDeduccion(),
                    montoDeducido,
                    montoDeducidoBob,
                    ded.descripcion(),
                    ded.baseCalculo(),
                    ded.orden()
            ));

            totalDeduccionesUsd = totalDeduccionesUsd.add(montoDeducido);
        }

        BigDecimal valorNetoUsd = valorBrutoTotal.subtract(totalDeduccionesUsd)
                .setScale(4, RoundingMode.HALF_UP);
        BigDecimal valorNetoBob = valorNetoUsd.multiply(tipoCambio)
                .setScale(4, RoundingMode.HALF_UP);

        // Precio ajustado = valorBrutoPrincipal / pesoTms (se calcula afuera)
        return new CalculoVentaResult(
                null, // precioAjustado - se calcula en VentaSocioBl
                valorBrutoTotal,
                totalDeduccionesUsd,
                valorNetoUsd,
                valorNetoBob,
                deduccionesResult
        );
    }

    // ==================== CÁLCULOS DE VENTA ====================


    /**
     * Input para cada deducción (viene del frontend via VentaCierreDto)
     * Usa los mismos nombres de campo que LiquidacionDeduccion entity
     */
    public record DeduccionInput(
            String concepto,
            BigDecimal porcentaje,
            String tipoDeduccion,
            String descripcion,
            String baseCalculo,
            Integer orden
    ) {}

    /**
     * Resultado calculado de cada deducción
     */
    public record DeduccionResult(
            String concepto,
            BigDecimal porcentaje,
            String tipoDeduccion,
            BigDecimal montoDeducidoUsd,
            BigDecimal montoDeducidoBob,
            String descripcion,
            String baseCalculo,
            int orden
    ) {}

    /**
     * Resultado completo del cálculo de venta
     */
    public record CalculoVentaResult(
            BigDecimal precioAjustadoUsd,
            BigDecimal valorBrutoUsd,
            BigDecimal totalDeduccionesUsd,
            BigDecimal valorNetoUsd,
            BigDecimal valorNetoBob,
            List<DeduccionResult> deducciones
    ) {}

    // ==================== PROMEDIO DE REPORTES QUÍMICOS ====================

    /**
     * Promediar reportes químicos según tipo de venta
     * Para venta_concentrado: usa ley_mineral_principal
     * Para venta_lote_complejo: usa ley_pb como referencia
     */
    public ReporteQuimicoPromedio promediarReportes(
            ReporteQuimico reporteSocio,
            ReporteQuimico reporteComercializadora,
            String tipoVenta
    ) {
        BigDecimal leyMineralPromedio = promedio(reporteSocio.getLeyMineralPrincipal(), reporteComercializadora.getLeyMineralPrincipal());
        BigDecimal leyAgGmtPromedio = promedio(reporteSocio.getLeyAgGmt(), reporteComercializadora.getLeyAgGmt());
        BigDecimal leyAgDmPromedio = promedio(reporteSocio.getLeyAgDm(), reporteComercializadora.getLeyAgDm());
        BigDecimal h2oPromedio = promedio(reporteSocio.getPorcentajeH2o(), reporteComercializadora.getPorcentajeH2o());
        BigDecimal leyPbPromedio = promedio(reporteSocio.getLeyPb(), reporteComercializadora.getLeyPb());
        BigDecimal leyZnPromedio = promedio(reporteSocio.getLeyZn(), reporteComercializadora.getLeyZn());

        BigDecimal diferencia;
        boolean requiereRevision;

        if (TIPO_VENTA_CONCENTRADO.equals(tipoVenta)) {
            // Para concentrado: comparar ley_mineral_principal
            diferencia = reporteSocio.getLeyMineralPrincipal()
                    .subtract(reporteComercializadora.getLeyMineralPrincipal())
                    .abs();
            requiereRevision = diferencia.compareTo(new BigDecimal("5")) > 0; // 5% tolerancia
        } else {
            // Para lote complejo: comparar ley_pb
            diferencia = reporteSocio.getLeyPb()
                    .subtract(reporteComercializadora.getLeyPb())
                    .abs();
            requiereRevision = diferencia.compareTo(new BigDecimal("3")) > 0; // 3% tolerancia
        }

        return new ReporteQuimicoPromedio(
                leyMineralPromedio, leyAgGmtPromedio, leyAgDmPromedio, h2oPromedio,
                leyPbPromedio, leyZnPromedio, requiereRevision, diferencia
        );
    }

    public record ReporteQuimicoPromedio(
            BigDecimal leyMineralPrincipal,
            BigDecimal leyAgGmt,
            BigDecimal leyAgDm,
            BigDecimal porcentajeH2o,
            BigDecimal leyPb,
            BigDecimal leyZn,
            boolean requiereRevision,
            BigDecimal diferencia
    ) {}

    private BigDecimal promedio(BigDecimal a, BigDecimal b) {
        if (a == null && b == null) return null;
        if (a == null) return b;
        if (b == null) return a;
        return a.add(b).divide(BigDecimal.valueOf(2), 4, RoundingMode.HALF_UP);
    }

    // ==================== CONVERSIÓN A DTO ====================

    public VentaLiquidacionResponseDto convertirADto(Liquidacion liquidacion) {
        VentaLiquidacionResponseDto dto = VentaLiquidacionResponseDto.builder().build();

        dto.setId(liquidacion.getId());
        dto.setTipoLiquidacion(liquidacion.getTipoLiquidacion());
        dto.setEstado(liquidacion.getEstado());

        // Socio
        Socio socio = liquidacion.getSocioId();
        dto.setSocioId(socio.getId());
        Persona personaSocio = personaRepository.findByUsuariosId(socio.getUsuariosId()).orElse(null);
        if (personaSocio != null) {
            dto.setSocioNombres(personaSocio.getNombres());
            dto.setSocioApellidos(personaSocio.getPrimerApellido() +
                    (personaSocio.getSegundoApellido() != null ? " " + personaSocio.getSegundoApellido() : ""));
            dto.setSocioCi(personaSocio.getCi());
        }

        // Comercializadora
        Comercializadora comercializadora = liquidacion.getComercializadoraId();
        if (comercializadora != null) {
            dto.setComercializadoraId(comercializadora.getId());
            dto.setComercializadoraNombre(comercializadora.getRazonSocial());
            dto.setComercializadoraNit(comercializadora.getNit());
        }

        // Según tipo
        if (TIPO_VENTA_CONCENTRADO.equals(liquidacion.getTipoLiquidacion())) {
            mapearConcentradosVenta(liquidacion, dto);
        } else if (TIPO_VENTA_LOTE_COMPLEJO.equals(liquidacion.getTipoLiquidacion())) {
            mapearLotesVenta(liquidacion, dto);
        }

        // Pesos
        dto.setPesoTmh(liquidacion.getPesoTmh());
        dto.setPesoTms(liquidacion.getPesoTms());
        dto.setPesoFinalTms(liquidacion.getPesoFinalTms());

        // Cálculos de venta
        dto.setCotizacionInternacionalUsd(liquidacion.getCostoPorTonelada());
        dto.setValorBrutoUsd(liquidacion.getValorBrutoUsd());
        dto.setValorNetoUsd(liquidacion.getValorNetoUsd());
        dto.setTipoCambio(liquidacion.getTipoCambio());
        dto.setValorNetoBob(liquidacion.getValorNetoBob());
        dto.setMoneda(liquidacion.getMoneda());

        // Deducciones
        mapearDeducciones(liquidacion, dto);

        // Cotización
        mapearCotizacion(liquidacion, dto);

        // Reportes químicos
        mapearReportesQuimicos(liquidacion, dto);

        // Pago y fechas
        dto.setFechaAprobacion(liquidacion.getFechaAprobacion());
        dto.setFechaPago(liquidacion.getFechaPago());
        dto.setMetodoPago(liquidacion.getMetodoPago());
        dto.setNumeroComprobante(liquidacion.getNumeroComprobante());
        dto.setUrlComprobante(liquidacion.getUrlComprobante());
        dto.setObservaciones(liquidacion.getObservaciones());
        dto.setCreatedAt(liquidacion.getCreatedAt());
        dto.setUpdatedAt(liquidacion.getUpdatedAt());

        return dto;
    }

    // ==================== MAPEOS PRIVADOS ====================

    private void mapearConcentradosVenta(Liquidacion liquidacion, VentaLiquidacionResponseDto dto) {
        List<ConcentradoVentaDto> concentradosDto = new ArrayList<>(liquidacion.getLiquidacionConcentradoList().stream()
                .map(lc -> {
                    Concentrado c = lc.getConcentradoId();
                    return ConcentradoVentaDto.builder()
                            .id(c.getId())
                            .codigoConcentrado(c.getCodigoConcentrado())
                            .mineralPrincipal(c.getMineralPrincipal())
                            .pesoInicial(c.getPesoInicial())
                            .pesoFinal(c.getPesoFinal())
                            .pesoTmh(c.getPesoTmh())
                            .pesoTms(c.getPesoTms())
                            .estado(c.getEstado())
                            .numeroSacos(c.getNumeroSacos())
                            .build();
                })
                // Deduplicar (puede haber 2 registros por concentrado: 1 reporte socio + 1 comercializadora)
                .collect(Collectors.toMap(
                        ConcentradoVentaDto::getId,
                        c -> c,
                        (existing, replacement) -> existing
                ))
                .values());

        dto.setConcentrados(concentradosDto);

        if (!concentradosDto.isEmpty()) {
            dto.setMineralPrincipal(concentradosDto.getFirst().getMineralPrincipal());
        }
    }

    private void mapearLotesVenta(Liquidacion liquidacion, VentaLiquidacionResponseDto dto) {
        List<LoteSimpleDto> lotesDto = new ArrayList<>(liquidacion.getLiquidacionLoteList().stream()
                .map(ll -> LoteSimpleDto.builder()
                        .id(ll.getLotesId().getId())
                        .minaNombre(ll.getLotesId().getMinasId().getNombre())
                        .tipoMineral("Pb,Zn,Ag")
                        .pesoTotalReal(ll.getPesoEntrada())
                        .estado(ll.getLotesId().getEstado())
                        .build())
                .collect(Collectors.toMap(
                        LoteSimpleDto::getId,
                        l -> l,
                        (existing, replacement) -> existing
                ))
                .values());
        dto.setLotes(lotesDto);
    }

    /**
     * Mapear deducciones usando los campos REALES de LiquidacionDeduccion:
     * concepto, porcentaje, montoDeducido, tipoDeduccion, descripcion, orden, moneda
     */
    private void mapearDeducciones(Liquidacion liquidacion, VentaLiquidacionResponseDto dto) {
        if (liquidacion.getDeduccionesList() != null && !liquidacion.getDeduccionesList().isEmpty()) {
            List<DeduccionDetalleDto> deduccionesDto = liquidacion.getDeduccionesList().stream()
                    .sorted(Comparator.comparingInt(d -> d.getOrden() != null ? d.getOrden() : 0))
                    .map(d -> {
                        BigDecimal montoUsd = d.getMontoDeducido();
                        BigDecimal montoBob = null;
                        if (montoUsd != null && liquidacion.getTipoCambio() != null) {
                            montoBob = montoUsd.multiply(liquidacion.getTipoCambio())
                                    .setScale(4, RoundingMode.HALF_UP);
                        }

                        return DeduccionDetalleDto.builder()
                                .nombre(d.getConcepto())
                                .porcentaje(d.getPorcentaje())
                                .montoUsd(montoUsd)
                                .montoBob(montoBob)
                                .descripcion(d.getDescripcion())
                                .build();
                    })
                    .collect(Collectors.toList());

            dto.setDeducciones(deduccionesDto);

            BigDecimal totalDedUsd = deduccionesDto.stream()
                    .map(DeduccionDetalleDto::getMontoUsd)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            dto.setTotalDeduccionesUsd(totalDedUsd);
        }

        // Datos extra desde serviciosAdicionales JSON
        try {
            if (liquidacion.getServiciosAdicionales() != null) {
                Map<String, Object> extras = objectMapper.readValue(
                        liquidacion.getServiciosAdicionales(), Map.class);
                if (extras.containsKey("precio_ajustado_usd")) {
                    dto.setPrecioAjustadoUsd(new BigDecimal(extras.get("precio_ajustado_usd").toString()));
                }
                if (extras.containsKey("ley_mineral_principal_promedio")) {
                    dto.setLeyMineralPrincipalPromedio(new BigDecimal(extras.get("ley_mineral_principal_promedio").toString()));
                }
                if (extras.containsKey("mineral_principal")) {
                    dto.setMineralPrincipal((String) extras.get("mineral_principal"));
                }
            }
        } catch (Exception e) {
            log.warn("Error al parsear datos adicionales de venta", e);
        }
    }

    /**
     * Mapear cotización internacional desde LiquidacionCotizacion
     */
    private void mapearCotizacion(Liquidacion liquidacion, VentaLiquidacionResponseDto dto) {
        if (liquidacion.getCotizacionesList() != null && !liquidacion.getCotizacionesList().isEmpty()) {
            LiquidacionCotizacion cot = liquidacion.getCotizacionesList().stream()
                    .max(Comparator.comparing(LiquidacionCotizacion::getFechaCotizacion))
                    .orElse(null);

            if (cot != null) {
                dto.setCotizacionInternacionalUsd(cot.getCotizacionUsd());
            }
        }
    }

    private void mapearReportesQuimicos(Liquidacion liquidacion, VentaLiquidacionResponseDto dto) {
        if (TIPO_VENTA_CONCENTRADO.equals(liquidacion.getTipoLiquidacion())) {
            for (LiquidacionConcentrado lc : liquidacion.getLiquidacionConcentradoList()) {
                ReporteQuimico rq = lc.getReporteQuimicoId();
                if (rq == null) continue;
                asignarReporteAlDto(rq, dto);
            }
        } else {
            for (LiquidacionLote ll : liquidacion.getLiquidacionLoteList()) {
                ReporteQuimico rq = ll.getReporteQuimicoId();
                if (rq == null) continue;
                asignarReporteAlDto(rq, dto);
            }
        }

        // Reporte acordado (guardado en servicios_adicionales JSON)
        try {
            if (liquidacion.getServiciosAdicionales() != null) {
                Map<String, Object> extras = objectMapper.readValue(
                        liquidacion.getServiciosAdicionales(), Map.class);
                if (extras.containsKey("reporte_acordado")) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> acordado = (Map<String, Object>) extras.get("reporte_acordado");
                    dto.setReporteAcordado(ReporteQuimicoResumenDto.builder()
                            .origen("acordado")
                            .leyMineralPrincipal(toBigDecimal(acordado.get("ley_mineral_principal")))
                            .leyAgGmt(toBigDecimal(acordado.get("ley_ag_gmt")))
                            .leyAgDm(toBigDecimal(acordado.get("ley_ag_dm")))
                            .porcentajeH2o(toBigDecimal(acordado.get("porcentaje_h2o")))
                            .leyPb(toBigDecimal(acordado.get("ley_pb")))
                            .leyZn(toBigDecimal(acordado.get("ley_zn")))
                            .estado("acordado")
                            .build());
                }
            }
        } catch (Exception e) {
            log.warn("Error al parsear reporte acordado", e);
        }
    }

    private void asignarReporteAlDto(ReporteQuimico rq, VentaLiquidacionResponseDto dto) {
        ReporteQuimicoResumenDto resumen = mapearReporteResumen(rq);
        if (Boolean.TRUE.equals(rq.getSubidoPorSocio())) {
            dto.setReporteSocio(resumen);
        } else if (Boolean.TRUE.equals(rq.getSubidoPorComercializadora())) {
            dto.setReporteComercializadora(resumen);
        }
    }

    private ReporteQuimicoResumenDto mapearReporteResumen(ReporteQuimico rq) {
        String origen = Boolean.TRUE.equals(rq.getSubidoPorSocio()) ? "socio" : "comercializadora";
        return ReporteQuimicoResumenDto.builder()
                .id(rq.getId())
                .origen(origen)
                .leyMineralPrincipal(rq.getLeyMineralPrincipal())
                .leyAgGmt(rq.getLeyAgGmt())
                .leyAgDm(rq.getLeyAgDm())
                .porcentajeH2o(rq.getPorcentajeH2o())
                .leyPb(rq.getLeyPb())
                .leyZn(rq.getLeyZn())
                .laboratorio(rq.getLaboratorio())
                .urlPdf(rq.getUrlPdf())
                .estado(rq.getEstado())
                .build();
    }

    private BigDecimal toBigDecimal(Object obj) {
        if (obj == null) return null;
        return new BigDecimal(obj.toString());
    }

    // ==================== UTILIDADES DE JSON ====================

    public String convertirAJson(Map<String, Object> mapa) {
        try {
            return objectMapper.writeValueAsString(mapa);
        } catch (JsonProcessingException e) {
            log.error("Error al convertir a JSON", e);
            return "{}";
        }
    }

    public Map<String, Object> parsearJson(String json) {
        if (json == null || json.isBlank()) return new HashMap<>();
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (JsonProcessingException e) {
            log.warn("Error al parsear JSON", e);
            return new HashMap<>();
        }
    }
}