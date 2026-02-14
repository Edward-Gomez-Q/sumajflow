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
import ucb.edu.bo.sumajflow.dto.venta.VentaLiquidacionDetalleDto;
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

    public CalculoVentaResult calcularVentaConDeduccionesEspecificasComplejo(
            BigDecimal valorBrutoPb,
            BigDecimal valorBrutoZn,
            BigDecimal valorBrutoAg,
            List<DeduccionInput> deducciones,
            BigDecimal tipoCambio
    ) {
        BigDecimal valorBrutoTotal = valorBrutoPb.add(valorBrutoZn).add(valorBrutoAg);

        List<DeduccionResult> deduccionesResult = new ArrayList<>();
        BigDecimal totalDeduccionesUsd = BigDecimal.ZERO;

        for (DeduccionInput ded : deducciones) {
            BigDecimal baseCalculo = switch (ded.baseCalculo()) {
                case "valor_bruto_pb" -> valorBrutoPb;
                case "valor_bruto_zn" -> valorBrutoZn;
                case "valor_bruto_ag" -> valorBrutoAg;
                case "valor_bruto_total" -> valorBrutoTotal;
                default -> {
                    log.warn("⚠️ Base de cálculo '{}' no reconocida, usando valor_bruto_total", ded.baseCalculo());
                    yield valorBrutoTotal;
                }
            };

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

        return new CalculoVentaResult(
                null, // precioAjustado - no aplica en lote complejo
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


    /**
     * Convertir liquidación a DTO DETALLADO con toda la información de cálculos
     */
    public VentaLiquidacionDetalleDto convertirADtoDetallado(Liquidacion liquidacion) {
        log.debug("Convirtiendo liquidación ID: {} a DTO detallado", liquidacion.getId());

        VentaLiquidacionDetalleDto dto = VentaLiquidacionDetalleDto.builder().build();

        // ========== INFORMACIÓN BÁSICA ==========
        dto.setId(liquidacion.getId());
        dto.setTipoLiquidacion(liquidacion.getTipoLiquidacion());
        dto.setEstado(liquidacion.getEstado());
        dto.setCreatedAt(liquidacion.getCreatedAt());
        dto.setUpdatedAt(liquidacion.getUpdatedAt());

        // ========== PARTES INVOLUCRADAS ==========
        dto.setSocio(mapearSocioInfo(liquidacion.getSocioId()));
        if (liquidacion.getComercializadoraId() != null) {
            dto.setComercializadora(mapearComercializadoraInfo(liquidacion.getComercializadoraId()));
        }

        // ========== ITEMS EN VENTA ==========
        if (TIPO_VENTA_CONCENTRADO.equals(liquidacion.getTipoLiquidacion())) {
            dto.setConcentrados(mapearConcentradosDetalle(liquidacion));
        } else {
            dto.setLotes(mapearLotesDetalle(liquidacion));
        }

        // ========== PESOS ==========
        dto.setPesos(mapearPesos(liquidacion));

        // ========== REPORTES QUÍMICOS ==========
        dto.setReportesQuimicos(mapearReportesQuimicosDetalle(liquidacion));

        // ========== COTIZACIONES ==========
        dto.setCotizaciones(mapearCotizacionesDetalle(liquidacion));

        // ========== VALORACIÓN ==========
        dto.setValoracion(mapearValoracionDetalle(liquidacion));

        // ========== DEDUCCIONES ==========
        dto.setDeducciones(mapearDeduccionesDetalle(liquidacion));

        // ========== RESULTADO FINAL ==========
        dto.setResultadoFinal(mapearResultadoFinal(liquidacion));

        // ========== PAGO ==========
        dto.setPago(mapearPagoInfo(liquidacion));

        // ========== HISTORIAL OBSERVACIONES ==========
        dto.setHistorialObservaciones(mapearHistorialObservaciones(liquidacion));

        return dto;
    }

// ========== MÉTODOS AUXILIARES DE MAPEO ==========

    private VentaLiquidacionDetalleDto.SocioInfoDto mapearSocioInfo(Socio socio) {
        Persona persona = personaRepository.findByUsuariosId(socio.getUsuariosId()).orElse(null);

        return VentaLiquidacionDetalleDto.SocioInfoDto.builder()
                .id(socio.getId())
                .nombres(persona != null ? persona.getNombres() : null)
                .apellidos(persona != null ?
                        persona.getPrimerApellido() +
                                (persona.getSegundoApellido() != null ? " " + persona.getSegundoApellido() : "")
                        : null)
                .ci(persona != null ? persona.getCi() : null)
                .build();
    }

    private VentaLiquidacionDetalleDto.ComercializadoraInfoDto mapearComercializadoraInfo(Comercializadora comercializadora) {
        return VentaLiquidacionDetalleDto.ComercializadoraInfoDto.builder()
                .id(comercializadora.getId())
                .razonSocial(comercializadora.getRazonSocial())
                .nit(comercializadora.getNit())
                .departamento(comercializadora.getDepartamento())
                .municipio(comercializadora.getMunicipio())
                .build();
    }

    private List<VentaLiquidacionDetalleDto.ConcentradoDetalleDto> mapearConcentradosDetalle(Liquidacion liquidacion) {
        // Deduplicar concentrados
        return liquidacion.getLiquidacionConcentradoList().stream()
                .map(LiquidacionConcentrado::getConcentradoId)
                .distinct()
                .map(c -> VentaLiquidacionDetalleDto.ConcentradoDetalleDto.builder()
                        .id(c.getId())
                        .codigoConcentrado(c.getCodigoConcentrado())
                        .mineralPrincipal(c.getMineralPrincipal())
                        .numeroSacos(c.getNumeroSacos())
                        .pesoInicial(c.getPesoInicial())
                        .pesoFinal(c.getPesoFinal())
                        .pesoTmh(c.getPesoTmh())
                        .pesoTms(c.getPesoTms())
                        .merma(c.getMerma())
                        .porcentajeMerma(c.getPorcentajeMerma())
                        .estado(c.getEstado())
                        .ingenioNombre(c.getIngenioMineroId() != null ? c.getIngenioMineroId().getRazonSocial() : null)
                        .build())
                .collect(Collectors.toList());
    }

    private List<VentaLiquidacionDetalleDto.LoteDetalleDto> mapearLotesDetalle(Liquidacion liquidacion) {
        return liquidacion.getLiquidacionLoteList().stream()
                .map(LiquidacionLote::getLotesId)
                .distinct()
                .map(l -> VentaLiquidacionDetalleDto.LoteDetalleDto.builder()
                        .id(l.getId())
                        .minaNombre(l.getMinasId().getNombre())
                        .tipoMineral(l.getTipoMineral())
                        .pesoTotalReal(l.getPesoTotalReal())
                        .estado(l.getEstado())
                        .fechaCreacion(l.getFechaCreacion())
                        .build())
                .collect(Collectors.toList());
    }

    private VentaLiquidacionDetalleDto.PesosDto mapearPesos(Liquidacion liquidacion) {
        Map<String, Object> extras = parsearJson(liquidacion.getServiciosAdicionales());

        BigDecimal porcentajeHumedad = null;
        String pesoUsado = "Peso Final TMS";

        // Obtener % humedad del reporte acordado
        if (extras.containsKey("reporte_acordado")) {
            Map<String, Object> acordado = (Map<String, Object>) extras.get("reporte_acordado");
            if (acordado.containsKey("porcentaje_h2o")) {
                porcentajeHumedad = toBigDecimal(acordado.get("porcentaje_h2o"));
            }
        }

        // Determinar qué peso se usó
        if (liquidacion.getPesoFinalTms() != null && liquidacion.getPesoFinalTms().compareTo(BigDecimal.ZERO) > 0) {
            pesoUsado = "Peso Final TMS (con merma aplicada)";
        } else if (liquidacion.getPesoTms() != null && liquidacion.getPesoTms().compareTo(BigDecimal.ZERO) > 0) {
            pesoUsado = "Peso TMS (sin merma final)";
        } else if (liquidacion.getPesoTmh() != null) {
            pesoUsado = "Peso TMH (toneladas húmedas)";
        }

        return VentaLiquidacionDetalleDto.PesosDto.builder()
                .pesoTotalEntrada(liquidacion.getPesoTotalEntrada())
                .pesoTmh(liquidacion.getPesoTmh())
                .pesoTms(liquidacion.getPesoTms())
                .pesoFinalTms(liquidacion.getPesoFinalTms())
                .porcentajeHumedad(porcentajeHumedad)
                .pesoUsadoEnCalculo(pesoUsado)
                .build();
    }

    private VentaLiquidacionDetalleDto.ReportesQuimicosDto mapearReportesQuimicosDetalle(Liquidacion liquidacion) {
        VentaLiquidacionDetalleDto.ReporteQuimicoDetalleDto reporteSocio = null;
        VentaLiquidacionDetalleDto.ReporteQuimicoDetalleDto reporteCom = null;
        VentaLiquidacionDetalleDto.ReporteQuimicoDetalleDto reporteAcordado = null;

        // Obtener reportes de socio y comercializadora
        if (TIPO_VENTA_CONCENTRADO.equals(liquidacion.getTipoLiquidacion())) {
            for (LiquidacionConcentrado lc : liquidacion.getLiquidacionConcentradoList()) {
                ReporteQuimico rq = lc.getReporteQuimicoId();
                if (rq != null) {
                    if (Boolean.TRUE.equals(rq.getSubidoPorSocio()) && reporteSocio == null) {
                        reporteSocio = mapearReporteQuimicoDetalle(rq);
                    } else if (Boolean.TRUE.equals(rq.getSubidoPorComercializadora()) && reporteCom == null) {
                        reporteCom = mapearReporteQuimicoDetalle(rq);
                    }
                }
            }
        } else {
            for (LiquidacionLote ll : liquidacion.getLiquidacionLoteList()) {
                ReporteQuimico rq = ll.getReporteQuimicoId();
                if (rq != null) {
                    if (Boolean.TRUE.equals(rq.getSubidoPorSocio()) && reporteSocio == null) {
                        reporteSocio = mapearReporteQuimicoDetalle(rq);
                    } else if (Boolean.TRUE.equals(rq.getSubidoPorComercializadora()) && reporteCom == null) {
                        reporteCom = mapearReporteQuimicoDetalle(rq);
                    }
                }
            }
        }

        // Obtener reporte acordado del JSON
        Map<String, Object> extras = parsearJson(liquidacion.getServiciosAdicionales());
        if (extras.containsKey("reporte_acordado")) {
            reporteAcordado = mapearReporteAcordado(extras, liquidacion.getTipoLiquidacion());
        }

        // Calcular diferencias
        VentaLiquidacionDetalleDto.DiferenciasReportesDto diferencias = null;
        if (reporteSocio != null && reporteCom != null && reporteAcordado != null) {
            diferencias = calcularDiferenciasReportes(reporteSocio, reporteCom, reporteAcordado);
        }

        return VentaLiquidacionDetalleDto.ReportesQuimicosDto.builder()
                .reporteSocio(reporteSocio)
                .reporteComercializadora(reporteCom)
                .reporteAcordado(reporteAcordado)
                .diferencias(diferencias)
                .build();
    }

    private VentaLiquidacionDetalleDto.ReporteQuimicoDetalleDto mapearReporteQuimicoDetalle(ReporteQuimico rq) {
        String origen = Boolean.TRUE.equals(rq.getSubidoPorSocio()) ? "socio" : "comercializadora";

        return VentaLiquidacionDetalleDto.ReporteQuimicoDetalleDto.builder()
                .id(rq.getId())
                .numeroReporte(rq.getNumeroReporte())
                .origen(origen)
                .laboratorio(rq.getLaboratorio())
                .fechaEmpaquetado(rq.getFechaEmpaquetado())
                .fechaRecepcionLaboratorio(rq.getFechaRecepcionLaboratorio())
                .fechaSalidaLaboratorio(rq.getFechaSalidaLaboratorio())
                .fechaAnalisis(rq.getFechaAnalisis())
                .leyMineralPrincipal(rq.getLeyMineralPrincipal())
                .leyAgGmt(rq.getLeyAgGmt())
                .leyAgDm(rq.getLeyAgDm())
                .leyPb(rq.getLeyPb())
                .leyZn(rq.getLeyZn())
                .porcentajeH2o(rq.getPorcentajeH2o())
                .numeroSacos(rq.getNumeroSacos())
                .pesoPorSaco(rq.getPesoPorSaco())
                .tipoEmpaque(rq.getTipoEmpaque())
                .urlPdf(rq.getUrlPdf())
                .observacionesLaboratorio(rq.getObservacionesLaboratorio())
                .estado(rq.getEstado())
                .build();
    }

    private VentaLiquidacionDetalleDto.ReporteQuimicoDetalleDto mapearReporteAcordado(
            Map<String, Object> extras,
            String tipoVenta
    ) {
        Map<String, Object> acordado = (Map<String, Object>) extras.get("reporte_acordado");

        return VentaLiquidacionDetalleDto.ReporteQuimicoDetalleDto.builder()
                .origen("acordado")
                .leyMineralPrincipal(toBigDecimal(acordado.get("ley_mineral_principal")))
                .leyAgGmt(toBigDecimal(acordado.get("ley_ag_gmt")))
                .leyAgDm(toBigDecimal(acordado.get("ley_ag_dm")))
                .leyPb(toBigDecimal(acordado.get("ley_pb")))
                .leyZn(toBigDecimal(acordado.get("ley_zn")))
                .porcentajeH2o(toBigDecimal(acordado.get("porcentaje_h2o")))
                .estado("acordado")
                .build();
    }

    private VentaLiquidacionDetalleDto.DiferenciasReportesDto calcularDiferenciasReportes(
            VentaLiquidacionDetalleDto.ReporteQuimicoDetalleDto socio,
            VentaLiquidacionDetalleDto.ReporteQuimicoDetalleDto com,
            VentaLiquidacionDetalleDto.ReporteQuimicoDetalleDto acordado
    ) {
        BigDecimal difPrincipal = BigDecimal.ZERO;
        BigDecimal difAg = BigDecimal.ZERO;
        BigDecimal difHumedad = BigDecimal.ZERO;

        if (socio.getLeyMineralPrincipal() != null && com.getLeyMineralPrincipal() != null) {
            difPrincipal = socio.getLeyMineralPrincipal().subtract(com.getLeyMineralPrincipal()).abs();
        } else if (socio.getLeyPb() != null && com.getLeyPb() != null) {
            difPrincipal = socio.getLeyPb().subtract(com.getLeyPb()).abs();
        }

        if (socio.getLeyAgGmt() != null && com.getLeyAgGmt() != null) {
            difAg = socio.getLeyAgGmt().subtract(com.getLeyAgGmt()).abs();
        } else if (socio.getLeyAgDm() != null && com.getLeyAgDm() != null) {
            difAg = socio.getLeyAgDm().subtract(com.getLeyAgDm()).abs();
        }

        if (socio.getPorcentajeH2o() != null && com.getPorcentajeH2o() != null) {
            difHumedad = socio.getPorcentajeH2o().subtract(com.getPorcentajeH2o()).abs();
        }

        boolean requiereRevision = difPrincipal.compareTo(new BigDecimal("5")) > 0;
        String mensaje = requiereRevision
                ? String.format("Diferencia de %.2f%% en ley principal excede el límite de 5%%", difPrincipal)
                : "Reportes dentro del rango aceptable";

        return VentaLiquidacionDetalleDto.DiferenciasReportesDto.builder()
                .diferenciaLeyPrincipal(difPrincipal)
                .diferenciaLeyAg(difAg)
                .diferenciaHumedad(difHumedad)
                .requiereRevision(requiereRevision)
                .mensaje(mensaje)
                .build();
    }

    private List<VentaLiquidacionDetalleDto.CotizacionDetalleDto> mapearCotizacionesDetalle(Liquidacion liquidacion) {
        return liquidacion.getCotizacionesList().stream()
                .map(cot -> VentaLiquidacionDetalleDto.CotizacionDetalleDto.builder()
                        .mineral(cot.getMineral())
                        .cotizacion(cot.getCotizacionUsd())
                        .unidad(cot.getUnidad())
                        .fuente(cot.getFuente())
                        .fecha(cot.getFechaCotizacion() != null ?
                                cot.getFechaCotizacion().atStartOfDay() : null)
                        .build())
                .collect(Collectors.toList());
    }

    private VentaLiquidacionDetalleDto.ValoracionDetalleDto mapearValoracionDetalle(Liquidacion liquidacion) {
        Map<String, Object> extras = parsearJson(liquidacion.getServiciosAdicionales());
        String tipoVenta = liquidacion.getTipoLiquidacion();
        String mineralPrincipal = (String) extras.get("mineral_principal");

        VentaLiquidacionDetalleDto.ValoracionDetalleDto valoracion =
                VentaLiquidacionDetalleDto.ValoracionDetalleDto.builder()
                        .tipoVenta(tipoVenta)
                        .mineralPrincipal(mineralPrincipal)
                        .build();

        if (TIPO_VENTA_CONCENTRADO.equals(tipoVenta)) {
            // VENTA_CONCENTRADO
            if (extras.containsKey("valor_principal_usd_ton")) {
                valoracion.setValoracionMineralPrincipal(construirValoracionMineral(
                        mineralPrincipal,
                        toBigDecimal(extras.get("ley_mineral_principal_promedio")),
                        liquidacion.getCostoPorTonelada(),
                        toBigDecimal(extras.get("valor_principal_usd_ton")),
                        liquidacion.getPesoFinalTms() != null ? liquidacion.getPesoFinalTms() :
                                liquidacion.getPesoTms() != null ? liquidacion.getPesoTms() : liquidacion.getPesoTmh(),
                        toBigDecimal(extras.get("valor_bruto_principal_usd"))
                ));
            }

            if (extras.containsKey("valor_ag_usd_ton")) {
                valoracion.setValoracionPlata(construirValoracionPlata(
                        toBigDecimal(extras.get("ley_ag_gmt")),
                        "g/MT",
                        toBigDecimal(extras.get("contenido_ag_oz_ton")),
                        obtenerCotizacionAg(liquidacion),
                        null,
                        toBigDecimal(extras.get("valor_ag_usd_ton")),
                        liquidacion.getPesoFinalTms() != null ? liquidacion.getPesoFinalTms() :
                                liquidacion.getPesoTms() != null ? liquidacion.getPesoTms() : liquidacion.getPesoTmh(),
                        toBigDecimal(extras.get("valor_bruto_ag_usd"))
                ));
            }

            valoracion.setValorTotalUsdPorTon(toBigDecimal(extras.get("valor_total_usd_ton")));

        } else {
            // VENTA_LOTE_COMPLEJO
            BigDecimal pesoTon = liquidacion.getPesoTmh();

            // ✅ Validar que existan los valores antes de construir la valoración
            BigDecimal valorBrutoPb = toBigDecimal(extras.get("valor_bruto_pb"));
            BigDecimal valorBrutoZn = toBigDecimal(extras.get("valor_bruto_zn"));
            BigDecimal valorBrutoAg = toBigDecimal(extras.get("valor_bruto_ag"));

            // ✅ Solo construir si los datos existen (liquidación cerrada)
            if (valorBrutoPb != null && extras.containsKey("ley_pb") && extras.containsKey("precio_por_ton_pb")) {
                valoracion.setValoracionPb(construirValoracionMineralLote(
                        "Pb",
                        toBigDecimal(extras.get("ley_pb")),
                        toBigDecimal(extras.get("precio_unitario_pb")),
                        toBigDecimal(extras.get("precio_por_ton_pb")),
                        pesoTon,
                        valorBrutoPb
                ));
            }

            if (valorBrutoZn != null && extras.containsKey("ley_zn") && extras.containsKey("precio_por_ton_zn")) {
                valoracion.setValoracionZn(construirValoracionMineralLote(
                        "Zn",
                        toBigDecimal(extras.get("ley_zn")),
                        toBigDecimal(extras.get("precio_unitario_zn")),
                        toBigDecimal(extras.get("precio_por_ton_zn")),
                        pesoTon,
                        valorBrutoZn
                ));
            }

            if (valorBrutoAg != null && extras.containsKey("ley_ag_dm") && extras.containsKey("precio_por_ton_ag")) {
                valoracion.setValoracionAgDm(construirValoracionPlataLote(
                        toBigDecimal(extras.get("ley_ag_dm")),
                        toBigDecimal(extras.get("precio_unitario_ag")),
                        toBigDecimal(extras.get("precio_por_ton_ag")),
                        pesoTon,
                        valorBrutoAg
                ));
            }

            // ✅ Solo calcular valor total si existen los valores
            if (valorBrutoPb != null && valorBrutoZn != null && valorBrutoAg != null && pesoTon != null && pesoTon.compareTo(BigDecimal.ZERO) > 0) {
                valoracion.setValorTotalUsdPorTon(
                        valorBrutoPb.add(valorBrutoZn).add(valorBrutoAg).divide(pesoTon, 4, RoundingMode.HALF_UP)
                );
            }
        }

        valoracion.setValorBrutoTotalUsd(liquidacion.getValorBrutoUsd());

        return valoracion;
    }
    private VentaLiquidacionDetalleDto.ValoracionMineralDto construirValoracionMineral(
            String mineral,
            BigDecimal ley,
            BigDecimal cotizacion,
            BigDecimal valorUsdPorTon,
            BigDecimal pesoTon,
            BigDecimal valorBruto
    ) {
        String formula = String.format(
                "(%s USD/ton × %s%%) ÷ 100 = %s USD/ton × %s ton = %s USD",
                cotizacion, ley, valorUsdPorTon, pesoTon, valorBruto
        );

        return VentaLiquidacionDetalleDto.ValoracionMineralDto.builder()
                .mineral(mineral)
                .ley(ley)
                .cotizacionInternacional(cotizacion)
                .valorUsdPorTon(valorUsdPorTon)
                .pesoToneladas(pesoTon)
                .valorBrutoUsd(valorBruto)
                .formulaAplicada(formula)
                .build();
    }

    private VentaLiquidacionDetalleDto.ValoracionMineralDto construirValoracionMineralLote(
            String mineral,
            BigDecimal ley,
            BigDecimal precioUnitario,
            BigDecimal precioPorTon,
            BigDecimal pesoTon,
            BigDecimal valorBruto
    ) {
        String formula = String.format(
                "%s USD × %s%% = %s USD/ton × %s ton = %s USD",
                precioUnitario, ley, precioPorTon, pesoTon, valorBruto
        );

        return VentaLiquidacionDetalleDto.ValoracionMineralDto.builder()
                .mineral(mineral)
                .ley(ley)
                .cotizacionInternacional(precioUnitario)
                .valorUsdPorTon(precioPorTon)
                .pesoToneladas(pesoTon)
                .valorBrutoUsd(valorBruto)
                .formulaAplicada(formula)
                .build();
    }

    private VentaLiquidacionDetalleDto.ValoracionPlataDto construirValoracionPlata(
            BigDecimal leyAg,
            String unidad,
            BigDecimal contenidoOz,
            BigDecimal cotizacionOz,
            BigDecimal cotizacionDm,
            BigDecimal valorUsdPorTon,
            BigDecimal pesoTon,
            BigDecimal valorBruto
    ) {
        String formula;
        if ("g/MT".equals(unidad)) {
            formula = String.format(
                    "%s g/MT ÷ 31.1035 = %s oz/ton × %s USD/oz = %s USD/ton × %s ton = %s USD",
                    leyAg, contenidoOz, cotizacionOz, valorUsdPorTon, pesoTon, valorBruto
            );
        } else {
            formula = String.format(
                    "%s DM × %s USD/DM = %s USD/ton × %s ton = %s USD",
                    leyAg, cotizacionDm, valorUsdPorTon, pesoTon, valorBruto
            );
        }

        return VentaLiquidacionDetalleDto.ValoracionPlataDto.builder()
                .leyAg(leyAg)
                .unidadLey(unidad)
                .contenidoOzPorTon(contenidoOz)
                .cotizacionUsdPorOz(cotizacionOz)
                .cotizacionUsdPorDm(cotizacionDm)
                .valorUsdPorTon(valorUsdPorTon)
                .pesoToneladas(pesoTon)
                .valorBrutoUsd(valorBruto)
                .formulaAplicada(formula)
                .build();
    }

    private VentaLiquidacionDetalleDto.ValoracionPlataDto construirValoracionPlataLote(
            BigDecimal leyAgDm,
            BigDecimal precioUnitario,
            BigDecimal precioPorTon,
            BigDecimal pesoTon,
            BigDecimal valorBruto
    ) {
        String formula = String.format(
                "%s DM × %s USD/DM = %s USD/ton × %s ton = %s USD",
                leyAgDm, precioUnitario, precioPorTon, pesoTon, valorBruto
        );

        return VentaLiquidacionDetalleDto.ValoracionPlataDto.builder()
                .leyAg(leyAgDm)
                .unidadLey("DM")
                .cotizacionUsdPorDm(precioUnitario)
                .valorUsdPorTon(precioPorTon)
                .pesoToneladas(pesoTon)
                .valorBrutoUsd(valorBruto)
                .formulaAplicada(formula)
                .build();
    }

    private BigDecimal obtenerCotizacionAg(Liquidacion liquidacion) {
        return liquidacion.getCotizacionesList().stream()
                .filter(c -> "Ag".equals(c.getMineral()))
                .findFirst()
                .map(LiquidacionCotizacion::getCotizacionUsd)
                .orElse(BigDecimal.ZERO);
    }

    private VentaLiquidacionDetalleDto.DeduccionesDetalleDto mapearDeduccionesDetalle(Liquidacion liquidacion) {
        List<VentaLiquidacionDetalleDto.DeduccionItemDto> items = liquidacion.getDeduccionesList().stream()
                .sorted(Comparator.comparingInt(d -> d.getOrden() != null ? d.getOrden() : 0))
                .map(d -> {
                    BigDecimal montoBob = d.getMontoDeducido() != null && liquidacion.getTipoCambio() != null
                            ? d.getMontoDeducido().multiply(liquidacion.getTipoCambio()).setScale(4, RoundingMode.HALF_UP)
                            : BigDecimal.ZERO;

                    // Determinar monto base según base_calculo
                    BigDecimal montoBase = determinarMontoBase(liquidacion, d.getBaseCalculo());

                    String formula = String.format(
                            "%s × %s%% = %s USD",
                            montoBase, d.getPorcentaje(), d.getMontoDeducido()
                    );

                    return VentaLiquidacionDetalleDto.DeduccionItemDto.builder()
                            .orden(d.getOrden() != null ? d.getOrden() : 0)
                            .concepto(d.getConcepto())
                            .tipoDeduccion(d.getTipoDeduccion())
                            .porcentaje(d.getPorcentaje())
                            .baseCalculo(d.getBaseCalculo())
                            .montoBaseUsd(montoBase)
                            .montoDeducidoUsd(d.getMontoDeducido())
                            .montoDeducidoBob(montoBob)
                            .descripcion(d.getDescripcion())
                            .formulaAplicada(formula)
                            .build();
                })
                .collect(Collectors.toList());

        BigDecimal totalUsd = items.stream()
                .map(VentaLiquidacionDetalleDto.DeduccionItemDto::getMontoDeducidoUsd)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalBob = items.stream()
                .map(VentaLiquidacionDetalleDto.DeduccionItemDto::getMontoDeducidoBob)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal porcentajeTotal = items.stream()
                .map(VentaLiquidacionDetalleDto.DeduccionItemDto::getPorcentaje)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return VentaLiquidacionDetalleDto.DeduccionesDetalleDto.builder()
                .deducciones(items)
                .totalDeduccionesUsd(totalUsd)
                .totalDeduccionesBob(totalBob)
                .porcentajeTotal(porcentajeTotal)
                .build();
    }

    private BigDecimal determinarMontoBase(Liquidacion liquidacion, String baseCalculo) {
        if (baseCalculo == null) return liquidacion.getValorBrutoUsd() != null ? liquidacion.getValorBrutoUsd() : BigDecimal.ZERO;

        Map<String, Object> extras = parsearJson(liquidacion.getServiciosAdicionales());

        return switch (baseCalculo) {
            // ✅ Para concentrados
            case "valor_bruto_principal" -> {
                BigDecimal valor = toBigDecimal(extras.get("valor_bruto_principal_usd"));
                yield valor != null ? valor : BigDecimal.ZERO;
            }

            // ✅ Para plata (funciona en AMBOS tipos)
            case "valor_bruto_ag" -> {
                // Primero intenta lote complejo
                BigDecimal valorAg = toBigDecimal(extras.get("valor_bruto_ag"));
                // Si no existe, intenta concentrado
                if (valorAg == null || valorAg.compareTo(BigDecimal.ZERO) == 0) {
                    valorAg = toBigDecimal(extras.get("valor_bruto_ag_usd"));
                }
                yield valorAg != null ? valorAg : BigDecimal.ZERO;
            }

            // ✅ Para lotes complejos
            case "valor_bruto_pb" -> {
                BigDecimal valor = toBigDecimal(extras.get("valor_bruto_pb"));
                yield valor != null ? valor : BigDecimal.ZERO;
            }

            case "valor_bruto_zn" -> {
                BigDecimal valor = toBigDecimal(extras.get("valor_bruto_zn"));
                yield valor != null ? valor : BigDecimal.ZERO;
            }

            // ✅ Valor total
            case "valor_bruto_total" -> {
                BigDecimal valor = liquidacion.getValorBrutoUsd();
                yield valor != null ? valor : BigDecimal.ZERO;
            }

            // ✅ Fallback
            default -> {
                log.warn("⚠️ Base de cálculo '{}' no reconocida, usando valor_bruto_total", baseCalculo);
                BigDecimal valor = liquidacion.getValorBrutoUsd();
                yield valor != null ? valor : BigDecimal.ZERO;
            }
        };
    }
    private VentaLiquidacionDetalleDto.ResultadoFinalDto mapearResultadoFinal(Liquidacion liquidacion) {
        BigDecimal porcentajeDed = BigDecimal.ZERO;
        if (liquidacion.getValorBrutoUsd() != null && liquidacion.getValorBrutoUsd().compareTo(BigDecimal.ZERO) > 0) {
            porcentajeDed = liquidacion.getTotalServiciosAdicionales() != null
                    ? liquidacion.getTotalServiciosAdicionales()
                    .multiply(BigDecimal.valueOf(100))
                    .divide(liquidacion.getValorBrutoUsd(), 2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;
        }

        BigDecimal porcentajePago = BigDecimal.valueOf(100).subtract(porcentajeDed);

        return VentaLiquidacionDetalleDto.ResultadoFinalDto.builder()
                .valorBrutoUsd(liquidacion.getValorBrutoUsd())
                .totalDeduccionesUsd(liquidacion.getTotalServiciosAdicionales())
                .valorNetoUsd(liquidacion.getValorNetoUsd())
                .tipoCambio(liquidacion.getTipoCambio())
                .valorNetoBob(liquidacion.getValorNetoBob())
                .moneda(liquidacion.getMoneda())
                .porcentajeDeduccionTotal(porcentajeDed)
                .porcentajePagoSocio(porcentajePago)
                .build();
    }

    private VentaLiquidacionDetalleDto.PagoInfoDto mapearPagoInfo(Liquidacion liquidacion) {
        Map<String, Object> extras = parsearJson(liquidacion.getServiciosAdicionales());
        LocalDateTime fechaCierre = null;

        if (extras.containsKey("fecha_cierre")) {
            try {
                fechaCierre = LocalDateTime.parse(extras.get("fecha_cierre").toString());
            } catch (Exception e) {
                log.warn("Error parseando fecha_cierre", e);
            }
        }

        return VentaLiquidacionDetalleDto.PagoInfoDto.builder()
                .fechaAprobacion(liquidacion.getFechaAprobacion())
                .fechaCierre(fechaCierre)
                .fechaPago(liquidacion.getFechaPago())
                .metodoPago(liquidacion.getMetodoPago())
                .numeroComprobante(liquidacion.getNumeroComprobante())
                .urlComprobante(liquidacion.getUrlComprobante())
                .build();
    }

    private List<VentaLiquidacionDetalleDto.ObservacionDto> mapearHistorialObservaciones(Liquidacion liquidacion) {
        List<Map<String, Object>> historial = obtenerHistorialObservaciones(liquidacion);

        return historial.stream()
                .map(obs -> {
                    LocalDateTime timestamp = null;
                    if (obs.containsKey("timestamp")) {
                        try {
                            timestamp = LocalDateTime.parse(obs.get("timestamp").toString());
                        } catch (Exception e) {
                            log.warn("Error parseando timestamp de observación", e);
                        }
                    }

                    return VentaLiquidacionDetalleDto.ObservacionDto.builder()
                            .estado((String) obs.get("estado"))
                            .descripcion((String) obs.get("descripcion"))
                            .observaciones((String) obs.get("observaciones"))
                            .usuarioId((Integer) obs.get("usuario_id"))
                            .tipoUsuario((String) obs.get("tipo_usuario"))
                            .timestamp(timestamp)
                            .estadoAnterior((String) obs.get("estado_anterior"))
                            .metadataAdicional(obs.containsKey("metadata") ?
                                    (Map<String, Object>) obs.get("metadata") : null)
                            .build();
                })
                .collect(Collectors.toList());
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
            liquidacion.getCotizacionesList().stream()
                    .max(Comparator.comparing(LiquidacionCotizacion::getFechaCotizacion)).ifPresent(cot -> dto.setCotizacionInternacionalUsd(cot.getCotizacionUsd()));

        }
    }

    public void agregarObservacion(
            Liquidacion liquidacion,
            String estado,
            String descripcion,
            String observaciones,
            String tipoUsuario,
            String estadoAnterior,
            Map<String, Object> metadataAdicional
    ) {
        List<Map<String, Object>> historial = obtenerHistorialObservaciones(liquidacion);

        Map<String, Object> nuevaObservacion = new HashMap<>();
        nuevaObservacion.put("estado", estado);
        nuevaObservacion.put("descripcion", descripcion);
        nuevaObservacion.put("observaciones", observaciones);
        nuevaObservacion.put("tipo_usuario", tipoUsuario);
        nuevaObservacion.put("timestamp", LocalDateTime.now().toString());

        if (estadoAnterior != null) {
            nuevaObservacion.put("estado_anterior", estadoAnterior);
        }

        if (metadataAdicional != null && !metadataAdicional.isEmpty()) {
            nuevaObservacion.put("metadata", metadataAdicional);
        }

        historial.add(nuevaObservacion);
        liquidacion.setObservaciones(convertirHistorialAJson(historial));
    }

    /**
     * Obtener historial de observaciones desde JSONB
     */
    public List<Map<String, Object>> obtenerHistorialObservaciones(Liquidacion liquidacion) {
        if (liquidacion.getObservaciones() == null || liquidacion.getObservaciones().isBlank()) {
            return new ArrayList<>();
        }

        try {
            return objectMapper.readValue(
                    liquidacion.getObservaciones(),
                    new TypeReference<List<Map<String, Object>>>() {}
            );
        } catch (JsonProcessingException e) {
            log.error("Error al parsear historial de observaciones de liquidación ID: {}", liquidacion.getId(), e);
            return new ArrayList<>();
        }
    }

    /**
     * Convertir historial a JSON
     */
    private String convertirHistorialAJson(List<Map<String, Object>> historial) {
        try {
            return objectMapper.writeValueAsString(historial);
        } catch (JsonProcessingException e) {
            log.error("Error al convertir historial a JSON", e);
            return "[]";
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