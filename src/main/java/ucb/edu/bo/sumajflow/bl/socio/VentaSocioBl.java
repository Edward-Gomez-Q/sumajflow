package ucb.edu.bo.sumajflow.bl.socio;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ucb.edu.bo.sumajflow.bl.CotizacionMineralBl;
import ucb.edu.bo.sumajflow.bl.LiquidacionVentaBl;
import ucb.edu.bo.sumajflow.bl.LiquidacionVentaBl.*;
import ucb.edu.bo.sumajflow.bl.NotificacionBl;
import ucb.edu.bo.sumajflow.dto.CotizacionMineralDto;
import ucb.edu.bo.sumajflow.dto.venta.*;
import ucb.edu.bo.sumajflow.entity.*;
import ucb.edu.bo.sumajflow.repository.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Servicio de Venta de Concentrados/Lotes para el ROL SOCIO
 * Responsabilidades:
 * 1. Crear liquidación de venta (seleccionar concentrados/lotes + comercializadora)
 * 2. Subir reporte químico
 * 3. Cerrar venta (cuando la cotización le conviene)
 * 4. Listar y consultar sus ventas
 * 5. Estadísticas
 * 6. Consultas auxiliares (concentrados disponibles, comercializadoras, etc.)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VentaSocioBl {

    private final LiquidacionVentaBl liquidacionVentaBl;
    private final LiquidacionRepository liquidacionRepository;
    private final LiquidacionConcentradoRepository liquidacionConcentradoRepository;
    private final LiquidacionLoteRepository liquidacionLoteRepository;
    private final LiquidacionDeduccionRepository liquidacionDeduccionRepository;
    private final LiquidacionCotizacionRepository liquidacionCotizacionRepository;
    private final ConcentradoRepository concentradoRepository;
    private final LotesRepository lotesRepository;
    private final ComercializadoraRepository comercializadoraRepository;
    private final ReporteQuimicoRepository reporteQuimicoRepository;
    private final UsuariosRepository usuariosRepository;
    private final SocioRepository socioRepository;
    private final NotificacionBl notificacionBl;
    private final CotizacionMineralBl cotizacionMineralBl;
    private final DeduccionConfiguracionRepository deduccionConfiguracionRepository;

    // ==================== 1. CREAR VENTA DE CONCENTRADO ====================

    /**
     * Crear liquidación de venta de concentrado(s).
     * Estado inicial: pendiente_aprobacion
     */
    @Transactional
    public VentaLiquidacionResponseDto crearVentaConcentrado(
            VentaCreateDto createDto,
            Integer usuarioId
    ) {
        log.info("Socio creando venta de concentrado - Usuario ID: {}", usuarioId);

        Socio socio = obtenerSocioDelUsuario(usuarioId);

        Comercializadora comercializadora = comercializadoraRepository.findById(createDto.getComercializadoraId())
                .orElseThrow(() -> new IllegalArgumentException("Comercializadora no encontrada"));

        if (createDto.getConcentradosIds() == null || createDto.getConcentradosIds().isEmpty()) {
            throw new IllegalArgumentException("Debe seleccionar al menos un concentrado para vender");
        }

        List<Concentrado> concentrados = new ArrayList<>();
        String mineralPrincipal = null;
        BigDecimal pesoTotalFinal = BigDecimal.ZERO;
        BigDecimal pesoTotalTmh = BigDecimal.ZERO;
        BigDecimal pesoTotalTms = BigDecimal.ZERO;

        for (Integer concId : createDto.getConcentradosIds()) {
            Concentrado concentrado = concentradoRepository.findById(concId)
                    .orElseThrow(() -> new IllegalArgumentException("Concentrado no encontrado - ID: " + concId));

            if (!concentrado.getSocioPropietarioId().getId().equals(socio.getId())) {
                throw new IllegalArgumentException("El concentrado ID " + concId + " no te pertenece");
            }

            if (!"listo_para_venta".equals(concentrado.getEstado())) {
                throw new IllegalArgumentException(
                        "El concentrado ID " + concId + " no está listo para venta. Estado: " + concentrado.getEstado());
            }

            // Todos deben ser del mismo mineral principal
            if (mineralPrincipal == null) {
                mineralPrincipal = concentrado.getMineralPrincipal();
            } else if (!mineralPrincipal.equals(concentrado.getMineralPrincipal())) {
                throw new IllegalArgumentException("Todos los concentrados deben ser del mismo mineral principal");
            }

            if (concentrado.getPesoFinal() != null) pesoTotalFinal = pesoTotalFinal.add(concentrado.getPesoFinal());
            if (concentrado.getPesoTmh() != null) pesoTotalTmh = pesoTotalTmh.add(concentrado.getPesoTmh());
            if (concentrado.getPesoTms() != null) pesoTotalTms = pesoTotalTms.add(concentrado.getPesoTms());

            concentrados.add(concentrado);
        }

        // Crear liquidación
        Liquidacion liquidacion = Liquidacion.builder()
                .socioId(socio)
                .comercializadoraId(comercializadora)
                .tipoLiquidacion(LiquidacionVentaBl.TIPO_VENTA_CONCENTRADO)
                .pesoTmh(pesoTotalTmh)
                .pesoTms(pesoTotalTms)
                .pesoFinalTms(pesoTotalFinal)
                .estado("pendiente_aprobacion")
                .moneda("BOB")
                .observaciones(createDto.getObservaciones() != null ? createDto.getObservaciones() : "")
                .build();

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("mineral_principal", mineralPrincipal);
        metadata.put("cantidad_concentrados", concentrados.size());
        liquidacion.setServiciosAdicionales(liquidacionVentaBl.convertirAJson(metadata));

        liquidacion = liquidacionRepository.save(liquidacion);

        for (Concentrado concentrado : concentrados) {
            LiquidacionConcentrado lc = LiquidacionConcentrado.builder()
                    .liquidacionId(liquidacion)
                    .concentradoId(concentrado)
                    .pesoLiquidado(concentrado.getPesoFinal())
                    .build();
            liquidacion.addLiquidacionConcentrado(lc);
            liquidacionConcentradoRepository.save(lc);

            concentrado.setEstado("en_venta");
            concentradoRepository.save(concentrado);
        }

        notificarComercializadora(liquidacion, comercializadora,
                "Nueva solicitud de venta",
                String.format("El socio solicita vender %d concentrado(s) de %s. Peso total: %.2f TMS",
                        concentrados.size(), mineralPrincipal, pesoTotalFinal),
                "info");

        log.info("✅ Venta de concentrado creada - Liquidación ID: {}", liquidacion.getId());
        return liquidacionVentaBl.convertirADto(liquidacion);
    }

    // ==================== CREAR VENTA DE LOTE COMPLEJO ====================

    /**
     * Crear liquidación de venta de lote(s) complejo(s).
     * Para lotes que van directo de mina a comercializadora.
     */
    @Transactional
    public VentaLiquidacionResponseDto crearVentaLoteComplejo(
            VentaCreateDto createDto,
            Integer usuarioId
    ) {
        log.info("Socio creando venta de lote complejo - Usuario ID: {}", usuarioId);

        Socio socio = obtenerSocioDelUsuario(usuarioId);

        Comercializadora comercializadora = comercializadoraRepository.findById(createDto.getComercializadoraId())
                .orElseThrow(() -> new IllegalArgumentException("Comercializadora no encontrada"));

        if (createDto.getLotesIds() == null || createDto.getLotesIds().isEmpty()) {
            throw new IllegalArgumentException("Debe seleccionar al menos un lote para vender");
        }

        List<Lotes> lotes = new ArrayList<>();
        BigDecimal pesoTotal = BigDecimal.ZERO;

        for (Integer loteId : createDto.getLotesIds()) {
            Lotes lote = lotesRepository.findById(loteId)
                    .orElseThrow(() -> new IllegalArgumentException("Lote no encontrado - ID: " + loteId));

            if (!lote.getMinasId().getSocioId().getId().equals(socio.getId())) {
                throw new IllegalArgumentException("El lote ID " + loteId + " no te pertenece");
            }

            if (!"Transporte completo".equals(lote.getEstado())) {
                throw new IllegalArgumentException(
                        "El lote ID " + loteId + " debe estar en 'Transporte completo'. Estado: " + lote.getEstado());
            }

            if (lote.getPesoTotalReal() != null) pesoTotal = pesoTotal.add(lote.getPesoTotalReal());
            lotes.add(lote);
        }

        BigDecimal pesoToneladas = pesoTotal.divide(BigDecimal.valueOf(1000), 4, RoundingMode.HALF_UP);

        Liquidacion liquidacion = Liquidacion.builder()
                .socioId(socio)
                .comercializadoraId(comercializadora)
                .tipoLiquidacion(LiquidacionVentaBl.TIPO_VENTA_LOTE_COMPLEJO)
                .pesoTotalEntrada(pesoTotal)
                .pesoTmh(pesoToneladas)
                .estado("pendiente_aprobacion")
                .moneda("BOB")
                .observaciones(createDto.getObservaciones() != null ? createDto.getObservaciones() : "")
                .build();

        Map<String, Object> metadataMap = new HashMap<>();
        metadataMap.put("cantidad_lotes", lotes.size());
        liquidacion.setServiciosAdicionales(liquidacionVentaBl.convertirAJson(metadataMap));

        liquidacion = liquidacionRepository.save(liquidacion);

        for (Lotes lote : lotes) {
            LiquidacionLote ll = LiquidacionLote.builder()
                    .liquidacionId(liquidacion)
                    .lotesId(lote)
                    .pesoEntrada(lote.getPesoTotalReal())
                    .build();
            liquidacion.addLiquidacionLote(ll);
            liquidacionLoteRepository.save(ll);

            lote.setEstado("En venta");
            lotesRepository.save(lote);
        }

        notificarComercializadora(liquidacion, comercializadora,
                "Nueva solicitud de venta de lotes",
                String.format("El socio solicita vender %d lote(s). Peso total: %.2f kg", lotes.size(), pesoTotal),
                "info");

        log.info("✅ Venta de lote complejo creada - Liquidación ID: {}", liquidacion.getId());
        return liquidacionVentaBl.convertirADto(liquidacion);
    }

    // ==================== 2. SUBIR REPORTE QUÍMICO ====================

// ==================== METODO MEJORADO: SUBIR REPORTE QUÍMICO SOCIO ====================

    /**
     * Socio sube su reporte químico.
     * Ambos (socio y comercializadora) pueden subir en cualquier orden.
     * Si el otro ya subió, se procede al promedio automático.
     * IMPORTANTE: El reporte es del MISMO concentrado/lote sellado a vista de ambos.
     */
    @Transactional
    public VentaLiquidacionResponseDto subirReporteQuimico(
            ReporteQuimicoUploadDto uploadDto,
            Integer usuarioId
    ) {
        log.info("Socio subiendo reporte químico - Liquidación ID: {}", uploadDto.getLiquidacionId());

        Socio socio = obtenerSocioDelUsuario(usuarioId);
        Liquidacion liquidacion = obtenerLiquidacionConPermisos(uploadDto.getLiquidacionId(), socio);

        // Validar estado
        if (!"aprobado".equals(liquidacion.getEstado()) && !"esperando_reportes".equals(liquidacion.getEstado())) {
            throw new IllegalArgumentException(
                    "La liquidación debe estar en estado 'aprobado' o 'esperando_reportes'. Estado actual: " + liquidacion.getEstado());
        }

        // Verificar que no haya subido ya un reporte
        if (yaSubioReporteSocio(liquidacion)) {
            throw new IllegalArgumentException("Ya has subido un reporte químico para esta liquidación");
        }

        // Asignar tipo de venta al DTO para validaciones
        uploadDto.setTipoVenta(liquidacion.getTipoLiquidacion());

        // Validar campos según tipo de venta
        try {
            uploadDto.validarSegunTipoVenta();
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Error en validación: " + e.getMessage());
        }

        // Crear reporte químico
        String numeroReporte = String.format("RQ-SOC-%d-%d",
                liquidacion.getId(),
                System.currentTimeMillis() % 10000);

        ReporteQuimico reporte = ReporteQuimico.builder()
                .numeroReporte(numeroReporte)
                .tipoReporte("socio")
                .tipoVenta(liquidacion.getTipoLiquidacion())
                .laboratorio(uploadDto.getLaboratorio())

                // Fechas
                .fechaEmpaquetado(uploadDto.getFechaEmpaquetado())
                .fechaRecepcionLaboratorio(uploadDto.getFechaRecepcionLaboratorio())
                .fechaSalidaLaboratorio(uploadDto.getFechaSalidaLaboratorio())
                .fechaAnalisis(uploadDto.getFechaAnalisis())

                // Leyes según tipo de venta
                .leyMineralPrincipal(uploadDto.getLeyMineralPrincipal()) // Solo venta_concentrado
                .leyAgGmt(uploadDto.getLeyAgGmt()) // Solo venta_concentrado
                .leyAgDm(uploadDto.getLeyAgDm()) // Solo venta_lote_complejo
                .leyPb(uploadDto.getLeyPb()) // Solo lote_complejo
                .leyZn(uploadDto.getLeyZn()) // Solo lote_complejo
                .porcentajeH2o(uploadDto.getPorcentajeH2o()) // Solo venta_concentrado

                // Empaquetado (solo venta_concentrado)
                .numeroSacos(uploadDto.getNumeroSacos())
                .pesoPorSaco(uploadDto.getPesoPorSaco())
                .tipoEmpaque(uploadDto.getTipoEmpaque())

                // Documentación
                .urlPdf(uploadDto.getUrlPdf())
                .observacionesLaboratorio(uploadDto.getObservacionesLaboratorio())

                // Validación
                .estado("validado")
                .subidoPorSocio(true)
                .fechaSubidaSocio(LocalDateTime.now())
                .build();

        // Validar campos según tipo de venta (seguridad adicional)
        reporte.validarCamposSegunTipoVenta();

        reporte = reporteQuimicoRepository.save(reporte);

        // Asociar reporte a liquidación
        asociarReporteALiquidacion(liquidacion, reporte, uploadDto.getReferenciaId());

        // Cambiar estado si es necesario
        if ("aprobado".equals(liquidacion.getEstado())) {
            liquidacion.setEstado("esperando_reportes");
            liquidacionRepository.save(liquidacion);
        }

        // Si la comercializadora ya subió su reporte, procesar acuerdo
        if (yaSubioReporteComercializadora(liquidacion)) {
            procesarAcuerdoQuimico(liquidacion);
        } else {
            // Notificar a comercializadora que el socio ya subió
            notificarComercializadora(liquidacion, liquidacion.getComercializadoraId(),
                    "Reporte del socio subido",
                    "El socio ha subido su reporte químico. Por favor sube el tuyo para continuar.",
                    "info");
        }

        log.info("✅ Reporte químico del socio subido exitosamente - Liquidación ID: {}", uploadDto.getLiquidacionId());
        return liquidacionVentaBl.convertirADto(liquidacion);
    }
    /**
     * Verifica si el socio ya subió su reporte
     */
    private boolean yaSubioReporteSocio(Liquidacion liquidacion) {
        if (LiquidacionVentaBl.TIPO_VENTA_CONCENTRADO.equals(liquidacion.getTipoLiquidacion())) {
            return liquidacion.getLiquidacionConcentradoList().stream()
                    .anyMatch(lc -> {
                        ReporteQuimico rq = lc.getReporteQuimicoId();
                        return rq != null && Boolean.TRUE.equals(rq.getSubidoPorSocio());
                    });
        } else {
            return liquidacion.getLiquidacionLoteList().stream()
                    .anyMatch(ll -> {
                        ReporteQuimico rq = ll.getReporteQuimicoId();
                        return rq != null && Boolean.TRUE.equals(rq.getSubidoPorSocio());
                    });
        }
    }

    /**
     * Verifica si la comercializadora ya subió su reporte
     */
    private boolean yaSubioReporteComercializadora(Liquidacion liquidacion) {
        if (LiquidacionVentaBl.TIPO_VENTA_CONCENTRADO.equals(liquidacion.getTipoLiquidacion())) {
            return liquidacion.getLiquidacionConcentradoList().stream()
                    .anyMatch(lc -> {
                        ReporteQuimico rq = lc.getReporteQuimicoId();
                        return rq != null && Boolean.TRUE.equals(rq.getSubidoPorComercializadora());
                    });
        } else {
            return liquidacion.getLiquidacionLoteList().stream()
                    .anyMatch(ll -> {
                        ReporteQuimico rq = ll.getReporteQuimicoId();
                        return rq != null && Boolean.TRUE.equals(rq.getSubidoPorComercializadora());
                    });
        }
    }

    /**
     * Cuando ambos reportes están subidos, calcular promedio y avanzar estado.
     * El promedio se guarda en serviciosAdicionales como JSON.
     */
    private void procesarAcuerdoQuimico(Liquidacion liquidacion) {
        log.info("Ambos reportes subidos - Procesando acuerdo químico - Liquidación ID: {}", liquidacion.getId());

        ReporteQuimico reporteSocio = null;
        ReporteQuimico reporteComercializadora = null;

        // Obtener ambos reportes
        if (LiquidacionVentaBl.TIPO_VENTA_CONCENTRADO.equals(liquidacion.getTipoLiquidacion())) {
            for (LiquidacionConcentrado lc : liquidacion.getLiquidacionConcentradoList()) {
                ReporteQuimico rq = lc.getReporteQuimicoId();
                if (rq != null) {
                    if (Boolean.TRUE.equals(rq.getSubidoPorSocio())) reporteSocio = rq;
                    if (Boolean.TRUE.equals(rq.getSubidoPorComercializadora())) reporteComercializadora = rq;
                }
            }
        } else {
            for (LiquidacionLote ll : liquidacion.getLiquidacionLoteList()) {
                ReporteQuimico rq = ll.getReporteQuimicoId();
                if (rq != null) {
                    if (Boolean.TRUE.equals(rq.getSubidoPorSocio())) reporteSocio = rq;
                    if (Boolean.TRUE.equals(rq.getSubidoPorComercializadora())) reporteComercializadora = rq;
                }
            }
        }

        if (reporteSocio == null || reporteComercializadora == null) {
            log.warn("No se encontraron ambos reportes para procesar acuerdo");
            return;
        }

        // Promediar reportes según tipo de venta
        Map<String, Object> reporteAcordado = new HashMap<>();
        boolean requiereRevision = false;
        BigDecimal diferencia = BigDecimal.ZERO;

        if (LiquidacionVentaBl.TIPO_VENTA_CONCENTRADO.equals(liquidacion.getTipoLiquidacion())) {
            BigDecimal leyMineralPromedio = promedio(reporteSocio.getLeyMineralPrincipal(), reporteComercializadora.getLeyMineralPrincipal());
            BigDecimal leyAgGmtPromedio = promedio(reporteSocio.getLeyAgGmt(), reporteComercializadora.getLeyAgGmt());
            BigDecimal leyPbPromedio = promedio(reporteSocio.getLeyPb(), reporteComercializadora.getLeyPb());
            BigDecimal h2oPromedio = promedio(reporteSocio.getPorcentajeH2o(), reporteComercializadora.getPorcentajeH2o());

            // Calcular diferencia en ley mineral principal
            diferencia = reporteSocio.getLeyMineralPrincipal()
                    .subtract(reporteComercializadora.getLeyMineralPrincipal())
                    .abs();
            requiereRevision = diferencia.compareTo(new BigDecimal("5")) > 0;

            reporteAcordado.put("ley_mineral_principal", leyMineralPromedio);
            reporteAcordado.put("ley_ag_gmt", leyAgGmtPromedio);
            reporteAcordado.put("ley_pb", leyPbPromedio);
            reporteAcordado.put("porcentaje_h2o", h2oPromedio);

        } else {
            // Para lote complejo: promediar ley_ag_dm, ley_pb, ley_zn (NO hay ley_mineral_principal)

            BigDecimal leyAgDmPromedio = promedio(reporteSocio.getLeyAgDm(), reporteComercializadora.getLeyAgDm());
            BigDecimal leyPbPromedio = promedio(reporteSocio.getLeyPb(), reporteComercializadora.getLeyPb());
            BigDecimal leyZnPromedio = promedio(reporteSocio.getLeyZn(), reporteComercializadora.getLeyZn());

            // Calcular diferencia en Pb (principal metal en lote complejo)
            diferencia = reporteSocio.getLeyPb()
                    .subtract(reporteComercializadora.getLeyPb())
                    .abs();
            requiereRevision = diferencia.compareTo(new BigDecimal("3")) > 0; // 3% para lote complejo

            reporteAcordado.put("ley_ag_dm", leyAgDmPromedio);
            reporteAcordado.put("ley_pb", leyPbPromedio);
            reporteAcordado.put("ley_zn", leyZnPromedio);
        }

        reporteAcordado.put("requiere_revision", requiereRevision);
        reporteAcordado.put("diferencia", diferencia);

        // Guardar en serviciosAdicionales
        Map<String, Object> extras = liquidacionVentaBl.parsearJson(liquidacion.getServiciosAdicionales());
        extras.put("reporte_acordado", reporteAcordado);
        liquidacion.setServiciosAdicionales(liquidacionVentaBl.convertirAJson(extras));

        // Cambiar estado
        liquidacion.setEstado("esperando_cierre_venta");
        liquidacionRepository.save(liquidacion);

        // Notificar al socio
        Integer socioUsuarioId = liquidacion.getSocioId().getUsuariosId().getId();
        Map<String, Object> metadataNotif = new HashMap<>();
        metadataNotif.put("liquidacionId", liquidacion.getId());
        metadataNotif.put("requiereRevision", requiereRevision);

        notificacionBl.crearNotificacion(
                socioUsuarioId,
                requiereRevision ? "warning" : "info",
                "Reportes químicos procesados",
                requiereRevision
                        ? String.format("Diferencia de %.2f%% detectada. Revisa antes de cerrar la venta.", diferencia)
                        : "Ambos reportes procesados correctamente. Cierra la venta cuando la cotización te convenga.",
                metadataNotif
        );

        log.info("✅ Acuerdo químico procesado - Liquidación ID: {}, Requiere revisión: {}",
                liquidacion.getId(), requiereRevision);
    }

    /**
     * Calcula el promedio de dos valores
     */
    private BigDecimal promedio(BigDecimal a, BigDecimal b) {
        if (a == null && b == null) return null;
        if (a == null) return b;
        if (b == null) return a;
        return a.add(b).divide(BigDecimal.valueOf(2), 4, RoundingMode.HALF_UP);
    }

    // ==================== 3. CERRAR VENTA ====================

    /**
     * Socio cierra la venta cuando la cotización internacional le conviene.
     * Se calculan: precio ajustado, valor bruto, deducciones, valor neto.
     * Se persiste la cotización en LiquidacionCotizacion y las deducciones en LiquidacionDeduccion.
     * Estado: esperando_cierre_venta → cerrado
     */
    @Transactional
    public VentaLiquidacionResponseDto cerrarVenta(
            Integer liquidacionId,
            VentaCierreDto cierreDto,
            Integer usuarioId
    ) {
        log.info("========== INICIO CIERRE DE VENTA ==========");
        log.info("Socio cerrando venta - Liquidación ID: {}, Usuario ID: {}", liquidacionId, usuarioId);

        Socio socio = obtenerSocioDelUsuario(usuarioId);
        Liquidacion liquidacion = obtenerLiquidacionConPermisos(liquidacionId, socio);

        if (!"esperando_cierre_venta".equals(liquidacion.getEstado())) {
            throw new IllegalArgumentException(
                    "Debe estar en estado 'esperando_cierre_venta'. Estado actual: " + liquidacion.getEstado());
        }

        // ========== 1. OBTENER COTIZACIONES Y DEDUCCIONES ACTUALES ==========
        log.info("========== PASO 1: Obtener Cotizaciones y Deducciones ==========");

        Map<String, CotizacionMineralDto> cotizaciones = cotizacionMineralBl.obtenerCotizacionesActuales();
        BigDecimal tipoCambio = cotizacionMineralBl.obtenerDolarOficial();
        LocalDate hoy = LocalDate.now();

        log.info("✅ Cotizaciones obtenidas:");
        log.info("   - Pb: {} USD/ton ({})", cotizaciones.get("Pb").getCotizacionUsdTon(), cotizaciones.get("Pb").getFuente());
        log.info("   - Zn: {} USD/ton ({})", cotizaciones.get("Zn").getCotizacionUsdTon(), cotizaciones.get("Zn").getFuente());
        log.info("   - Ag: {} USD/oz ({})", cotizaciones.get("Ag").getCotizacionUsdOz(), cotizaciones.get("Ag").getFuente());
        log.info("   - Tipo de cambio: {} BOB/USD", tipoCambio);

        // ========== 2. OBTENER REPORTE ACORDADO ==========
        log.info("========== PASO 2: Obtener Reporte Acordado ==========");

        Map<String, Object> extras = liquidacionVentaBl.parsearJson(liquidacion.getServiciosAdicionales());
        if (!extras.containsKey("reporte_acordado")) {
            throw new IllegalStateException("No se encontró el reporte acordado");
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> acordado = (Map<String, Object>) extras.get("reporte_acordado");

        BigDecimal leyMineralPrincipal = null;
        BigDecimal leyAgGmt = BigDecimal.ZERO;
        BigDecimal leyPb = null;
        BigDecimal leyZn = null;

        if (LiquidacionVentaBl.TIPO_VENTA_CONCENTRADO.equals(liquidacion.getTipoLiquidacion())) {
            // Concentrado: ley_mineral_principal + ley_ag_gmt
            leyMineralPrincipal = new BigDecimal(acordado.get("ley_mineral_principal").toString());
            if (acordado.containsKey("ley_ag_gmt") && acordado.get("ley_ag_gmt") != null) {
                leyAgGmt = new BigDecimal(acordado.get("ley_ag_gmt").toString());
            }
            log.info("✅ Tipo: VENTA_CONCENTRADO");
            log.info("   - Ley mineral principal: {}%", leyMineralPrincipal);
            log.info("   - Ley Ag: {} g/MT", leyAgGmt);
        } else {
            // Lote complejo: ley_ag_dm, ley_pb, ley_zn
            if (acordado.containsKey("ley_ag_dm") && acordado.get("ley_ag_dm") != null) {
                leyAgGmt = new BigDecimal(acordado.get("ley_ag_dm").toString());
            }
            if (acordado.containsKey("ley_pb") && acordado.get("ley_pb") != null) {
                leyPb = new BigDecimal(acordado.get("ley_pb").toString());
            }
            if (acordado.containsKey("ley_zn") && acordado.get("ley_zn") != null) {
                leyZn = new BigDecimal(acordado.get("ley_zn").toString());
            }
            log.info("✅ Tipo: VENTA_LOTE_COMPLEJO");
            log.info("   - Ley Ag: {} g/MT", leyAgGmt);
            log.info("   - Ley Pb: {}%", leyPb);
            log.info("   - Ley Zn: {}%", leyZn);
        }

        // ========== 3. DETERMINAR PESO ==========
        log.info("========== PASO 3: Determinar Peso Oficial ==========");

        BigDecimal pesoFinal = liquidacion.getPesoFinalTms();
        String tipoPeso = "Peso Final TMS";

        if (pesoFinal == null || pesoFinal.compareTo(BigDecimal.ZERO) <= 0) {
            pesoFinal = liquidacion.getPesoTms();
            tipoPeso = "Peso TMS (sin merma final)";
        }
        if (pesoFinal == null || pesoFinal.compareTo(BigDecimal.ZERO) <= 0) {
            pesoFinal = liquidacion.getPesoTmh();
            tipoPeso = "Peso TMH (fallback)";
        }
        if (pesoFinal == null || pesoFinal.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalStateException("No se puede calcular la venta sin peso válido");
        }

        log.info("✅ Peso oficial determinado: {} ton ({})", pesoFinal, tipoPeso);

        // ========== 4. IDENTIFICAR MINERAL PRINCIPAL Y OBTENER COTIZACIÓN ==========
        log.info("========== PASO 4: Identificar Mineral Principal ==========");

        String mineralPrincipal = extras.containsKey("mineral_principal")
                ? (String) extras.get("mineral_principal")
                : null;

        CotizacionMineralDto cotizacionPrincipal;
        BigDecimal cotizacionUsdPrincipal;

        if (LiquidacionVentaBl.TIPO_VENTA_CONCENTRADO.equals(liquidacion.getTipoLiquidacion())) {
            // Para concentrado: usar mineral principal (Pb o Zn)
            if ("Pb".equalsIgnoreCase(mineralPrincipal)) {
                cotizacionPrincipal = cotizaciones.get("Pb");
            } else if ("Zn".equalsIgnoreCase(mineralPrincipal)) {
                cotizacionPrincipal = cotizaciones.get("Zn");
            } else {
                throw new IllegalArgumentException("Mineral principal no reconocido: " + mineralPrincipal);
            }
            cotizacionUsdPrincipal = cotizacionPrincipal.getCotizacionUsdTon();
        } else {
            // Para lote complejo: usar Pb como referencia
            cotizacionPrincipal = cotizaciones.get("Pb");
            cotizacionUsdPrincipal = cotizacionPrincipal.getCotizacionUsdTon();

            // Si no hay leyPb, usar leyZn
            if (leyPb == null || leyPb.compareTo(BigDecimal.ZERO) == 0) {
                if (leyZn != null && leyZn.compareTo(BigDecimal.ZERO) > 0) {
                    cotizacionPrincipal = cotizaciones.get("Zn");
                    cotizacionUsdPrincipal = cotizacionPrincipal.getCotizacionUsdTon();
                    leyMineralPrincipal = leyZn;
                    mineralPrincipal = "Zn";
                } else {
                    throw new IllegalStateException("No hay ley de Pb ni Zn en el lote complejo");
                }
            } else {
                leyMineralPrincipal = leyPb;
                mineralPrincipal = "Pb";
            }
        }

        log.info("✅ Mineral principal identificado: {}", mineralPrincipal);
        log.info("   - Cotización: {} USD/ton", cotizacionUsdPrincipal);
        log.info("   - Ley: {}%", leyMineralPrincipal);

        // ========== 5. CALCULAR VALOR DEL MINERAL PRINCIPAL (USD/ton) ==========
        log.info("========== PASO 5: Calcular Valoración Mineral Principal ==========");

        // valorPrincipalUsdTon = (cotizacionUsdTon * leyMineral%) / 100
        BigDecimal valorPrincipalUsdTon = cotizacionUsdPrincipal
                .multiply(leyMineralPrincipal)
                .divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);

        log.info("✅ Cálculo: ({} USD/ton × {}%) / 100 = {} USD/ton",
                cotizacionUsdPrincipal, leyMineralPrincipal, valorPrincipalUsdTon);

        // ========== 6. CALCULAR VALOR DE PLATA (USD/ton) ==========
        log.info("========== PASO 6: Calcular Valoración Plata ==========");

        BigDecimal valorAgUsdTon = BigDecimal.ZERO;
        BigDecimal contenidoAgOzTon = BigDecimal.ZERO;
        CotizacionMineralDto cotizacionAg = cotizaciones.get("Ag");

        if (leyAgGmt.compareTo(BigDecimal.ZERO) > 0) {
            // Constante de conversión
            BigDecimal GRAMOS_POR_ONZA_TROY = new BigDecimal("31.1034768");

            // contenidoAgOzTon = leyAgGmt / GRAMOS_POR_ONZA_TROY
            contenidoAgOzTon = leyAgGmt.divide(GRAMOS_POR_ONZA_TROY, 6, RoundingMode.HALF_UP);

            // valorAgUsdTon = contenidoAgOzTon * cotizacionUsdOz
            valorAgUsdTon = contenidoAgOzTon
                    .multiply(cotizacionAg.getCotizacionUsdOz())
                    .setScale(4, RoundingMode.HALF_UP);

            log.info("✅ Cálculo Ag:");
            log.info("   - Ley Ag: {} g/MT", leyAgGmt);
            log.info("   - Contenido: {} g/MT ÷ {} g/oz = {} oz/ton",
                    leyAgGmt, GRAMOS_POR_ONZA_TROY, contenidoAgOzTon);
            log.info("   - Valoración: {} oz/ton × {} USD/oz = {} USD/ton",
                    contenidoAgOzTon, cotizacionAg.getCotizacionUsdOz(), valorAgUsdTon);
        } else {
            log.info("⚠️  No hay contenido de plata (ley = 0)");
        }

        // ========== 7. CALCULAR VALOR TOTAL POR TONELADA ==========
        log.info("========== PASO 7: Calcular Valor Total USD/ton ==========");

        // valorTotalUsdTon = valorPrincipalUsdTon + valorAgUsdTon
        BigDecimal valorTotalUsdTon = valorPrincipalUsdTon.add(valorAgUsdTon);

        log.info("✅ Valor total por tonelada: {} + {} = {} USD/ton",
                valorPrincipalUsdTon, valorAgUsdTon, valorTotalUsdTon);

        // ========== 8. CALCULAR VALOR BRUTO TOTAL ==========
        log.info("========== PASO 8: Calcular Valor Bruto Total ==========");

        // valorBrutoTotal = valorTotalUsdTon * pesoFinal
        BigDecimal valorBrutoTotal = valorTotalUsdTon
                .multiply(pesoFinal)
                .setScale(4, RoundingMode.HALF_UP);

        // Para las deducciones también necesitamos los valores brutos separados
        BigDecimal valorBrutoPrincipal = valorPrincipalUsdTon
                .multiply(pesoFinal)
                .setScale(4, RoundingMode.HALF_UP);

        BigDecimal valorBrutoAg = valorAgUsdTon
                .multiply(pesoFinal)
                .setScale(4, RoundingMode.HALF_UP);

        log.info("✅ Valores brutos calculados:");
        log.info("   - Mineral principal: {} USD/ton × {} ton = {} USD",
                valorPrincipalUsdTon, pesoFinal, valorBrutoPrincipal);
        log.info("   - Plata: {} USD/ton × {} ton = {} USD",
                valorAgUsdTon, pesoFinal, valorBrutoAg);
        log.info("   - TOTAL BRUTO: {} USD", valorBrutoTotal);

        // ========== 9. OBTENER Y APLICAR DEDUCCIONES DESDE BD ==========
        log.info("========== PASO 9: Obtener Deducciones desde BD ==========");

        List<DeduccionConfiguracion> deduccionesConfig = deduccionConfiguracionRepository
                .findDeduccionesAplicables(hoy, liquidacion.getTipoLiquidacion());

        log.info("✅ Se encontraron {} deducciones configuradas para tipo '{}'",
                deduccionesConfig.size(), liquidacion.getTipoLiquidacion());

        List<DeduccionInput> deduccionesInput = construirDeduccionesDesdeConfig(
                deduccionesConfig,
                mineralPrincipal,
                valorBrutoPrincipal,
                valorBrutoAg
        );

        log.info("✅ Se aplicarán {} deducciones después del filtrado", deduccionesInput.size());

        // ========== 10. CALCULAR DEDUCCIONES Y VALOR NETO ==========
        log.info("========== PASO 10: Calcular Deducciones y Valor Neto ==========");

        CalculoVentaResult calculo = liquidacionVentaBl.calcularVentaConDeduccionesEspecificas(
                valorBrutoPrincipal,
                valorBrutoAg,
                deduccionesInput,
                tipoCambio
        );

        log.info("✅ Cálculo de deducciones completado:");
        for (DeduccionResult ded : calculo.deducciones()) {
            log.info("   - {}: {}% sobre {} = {} USD",
                    ded.concepto(), ded.porcentaje(), ded.baseCalculo(), ded.montoDeducidoUsd());
        }
        log.info("   - Total deducciones: {} USD", calculo.totalDeduccionesUsd());
        log.info("   - Valor neto: {} USD ({} BOB)",
                calculo.valorNetoUsd(), calculo.valorNetoBob());

        // ========== 11. ACTUALIZAR LIQUIDACIÓN ==========
        log.info("========== PASO 11: Actualizar Liquidación ==========");

        liquidacion.setCostoPorTonelada(cotizacionUsdPrincipal);
        liquidacion.setValorBrutoUsd(calculo.valorBrutoUsd());
        liquidacion.setValorNetoUsd(calculo.valorNetoUsd());
        liquidacion.setTipoCambio(tipoCambio);
        liquidacion.setValorNetoBob(calculo.valorNetoBob());
        liquidacion.setTotalServiciosAdicionales(calculo.totalDeduccionesUsd());

        // Guardar datos adicionales en JSON
        extras.put("valor_principal_usd_ton", valorPrincipalUsdTon);
        extras.put("valor_ag_usd_ton", valorAgUsdTon);
        extras.put("contenido_ag_oz_ton", contenidoAgOzTon);
        extras.put("valor_total_usd_ton", valorTotalUsdTon);
        extras.put("valor_bruto_principal_usd", valorBrutoPrincipal);
        extras.put("valor_bruto_ag_usd", valorBrutoAg);
        extras.put("ley_mineral_principal_promedio", leyMineralPrincipal);
        extras.put("ley_ag_gmt", leyAgGmt);
        extras.put("fecha_cierre", LocalDateTime.now().toString());
        liquidacion.setServiciosAdicionales(liquidacionVentaBl.convertirAJson(extras));

        liquidacion.setEstado("cerrado");

        if (cierreDto.getObservaciones() != null && !cierreDto.getObservaciones().isBlank()) {
            String obs = liquidacion.getObservaciones() != null ? liquidacion.getObservaciones() : "";
            liquidacion.setObservaciones(obs + " | CIERRE: " + cierreDto.getObservaciones());
        }

        liquidacionRepository.save(liquidacion);
        log.info("✅ Liquidación actualizada y guardada");

        // ========== 12. PERSISTIR COTIZACIONES ==========
        log.info("========== PASO 12: Persistir Cotizaciones ==========");

        // Cotización mineral principal
        LiquidacionCotizacion cotizacionPrincipalEntity = LiquidacionCotizacion.builder()
                .liquidacionId(liquidacion)
                .mineral(cotizacionPrincipal.getNomenclatura())
                .cotizacionUsd(cotizacionUsdPrincipal)
                .unidad(cotizacionPrincipal.getUnidad())
                .fuente(cotizacionPrincipal.getFuente())
                .fechaCotizacion(cotizacionPrincipal.getFecha())
                .build();
        liquidacion.addCotizacion(cotizacionPrincipalEntity);
        liquidacionCotizacionRepository.save(cotizacionPrincipalEntity);
        log.info("✅ Cotización {} guardada: {} {}",
                mineralPrincipal, cotizacionUsdPrincipal, cotizacionPrincipal.getUnidad());

        // Cotización plata (si aplica)
        if (valorBrutoAg.compareTo(BigDecimal.ZERO) > 0) {
            LiquidacionCotizacion cotizacionAgEntity = LiquidacionCotizacion.builder()
                    .liquidacionId(liquidacion)
                    .mineral("Ag")
                    .cotizacionUsd(cotizacionAg.getCotizacionUsdOz())
                    .unidad(cotizacionAg.getUnidad())
                    .fuente(cotizacionAg.getFuente())
                    .fechaCotizacion(cotizacionAg.getFecha())
                    .build();
            liquidacion.addCotizacion(cotizacionAgEntity);
            liquidacionCotizacionRepository.save(cotizacionAgEntity);
            log.info("✅ Cotización Ag guardada: {} {}",
                    cotizacionAg.getCotizacionUsdOz(), cotizacionAg.getUnidad());
        }

        // ========== 13. PERSISTIR DEDUCCIONES ==========
        log.info("========== PASO 13: Persistir Deducciones ==========");

        for (DeduccionResult ded : calculo.deducciones()) {
            LiquidacionDeduccion deduccion = LiquidacionDeduccion.builder()
                    .liquidacionId(liquidacion)
                    .concepto(ded.concepto())
                    .porcentaje(ded.porcentaje())
                    .tipoDeduccion(ded.tipoDeduccion())
                    .montoDeducido(ded.montoDeducidoUsd())
                    .baseCalculo(ded.baseCalculo())
                    .orden(ded.orden())
                    .moneda("USD")
                    .descripcion(ded.descripcion())
                    .build();
            liquidacion.addDeduccion(deduccion);
            liquidacionDeduccionRepository.save(deduccion);
        }
        log.info("✅ {} deducciones guardadas", calculo.deducciones().size());

        // ========== 14. NOTIFICAR COMERCIALIZADORA ==========
        log.info("========== PASO 14: Notificar Comercializadora ==========");

        Comercializadora comercializadora = liquidacion.getComercializadoraId();
        notificarComercializadora(liquidacion, comercializadora,
                "Venta cerrada - Pendiente de pago",
                String.format("El socio ha cerrado la venta. Monto a pagar: %.2f BOB (%.2f USD)",
                        calculo.valorNetoBob(), calculo.valorNetoUsd()),
                "warning");
        log.info("✅ Notificación enviada a comercializadora ID: {}", comercializadora.getId());

        // ========== RESUMEN FINAL ==========
        log.info("========== RESUMEN CIERRE DE VENTA ==========");
        log.info("Liquidación ID: {}", liquidacionId);
        log.info("Tipo: {}", liquidacion.getTipoLiquidacion());
        log.info("Mineral principal: {} (ley: {}%)", mineralPrincipal, leyMineralPrincipal);
        log.info("Plata: {} g/MT ({} oz/ton)", leyAgGmt, contenidoAgOzTon);
        log.info("Peso: {} ton", pesoFinal);
        log.info("Valoración USD/ton: {} ({} + {} Ag)",
                valorTotalUsdTon, valorPrincipalUsdTon, valorAgUsdTon);
        log.info("Valor bruto: {} USD", calculo.valorBrutoUsd());
        log.info("Deducciones: {} USD", calculo.totalDeduccionesUsd());
        log.info("Valor neto: {} USD / {} BOB", calculo.valorNetoUsd(), calculo.valorNetoBob());
        log.info("Tipo de cambio: {}", tipoCambio);
        log.info("========== FIN CIERRE DE VENTA ==========");

        return liquidacionVentaBl.convertirADto(liquidacion);
    }

    /**
     * Construir deducciones desde configuración de BD
     */
    private List<DeduccionInput> construirDeduccionesDesdeConfig(
            List<DeduccionConfiguracion> configs,
            String mineralPrincipal,
            BigDecimal valorBrutoPrincipal,
            BigDecimal valorBrutoAg
    ) {
        log.info("   Filtrando deducciones para mineral: {}", mineralPrincipal);

        List<DeduccionInput> deducciones = new ArrayList<>();

        for (DeduccionConfiguracion config : configs) {
            String aplicaA = config.getAplicaAMineral();

            // Log de evaluación
            log.debug("   Evaluando deducción: {} (aplica a: {})",
                    config.getConcepto(), aplicaA != null ? aplicaA : "todos");

            // Filtrar deducciones según mineral
            if (aplicaA != null && !"todos".equalsIgnoreCase(aplicaA)) {

                // Regalía de mineral principal (Pb o Zn)
                if (aplicaA.equalsIgnoreCase(mineralPrincipal)) {
                    deducciones.add(new DeduccionInput(
                            config.getConcepto(),
                            config.getPorcentaje(),
                            config.getTipoDeduccion(),
                            config.getDescripcion(),
                            config.getBaseCalculo(),
                            config.getOrden()
                    ));
                    log.info("   ✓ Aplicando: {} (mineral principal {})",
                            config.getConcepto(), mineralPrincipal);
                }
                // Regalía de plata (solo si hay contenido de Ag)
                else if ("Ag".equalsIgnoreCase(aplicaA)) {
                    if (valorBrutoAg.compareTo(BigDecimal.ZERO) > 0) {
                        deducciones.add(new DeduccionInput(
                                config.getConcepto(),
                                config.getPorcentaje(),
                                config.getTipoDeduccion(),
                                config.getDescripcion(),
                                config.getBaseCalculo(),
                                config.getOrden()
                        ));
                        log.info("   ✓ Aplicando: {} (hay contenido de Ag)", config.getConcepto());
                    } else {
                        log.debug("   ✗ Omitiendo: {} (sin contenido de Ag)", config.getConcepto());
                    }
                } else {
                    log.debug("   ✗ Omitiendo: {} (no aplica a {})",
                            config.getConcepto(), mineralPrincipal);
                }
            }
            // Deducciones que aplican a todos los minerales
            else {
                deducciones.add(new DeduccionInput(
                        config.getConcepto(),
                        config.getPorcentaje(),
                        config.getTipoDeduccion(),
                        config.getDescripcion(),
                        config.getBaseCalculo(),
                        config.getOrden()
                ));
                log.info("   ✓ Aplicando: {} (aplica a todos)", config.getConcepto());
            }
        }

        log.info("   Total deducciones después del filtrado: {}", deducciones.size());
        return deducciones;
    }
    /**
     * Construir deducciones fijas por ley boliviana
     */
    private List<DeduccionInput> construirDeduccionesFijas(
            String mineralPrincipal,
            BigDecimal valorBrutoPrincipal,
            BigDecimal valorBrutoAg
    ) {
        List<DeduccionInput> deducciones = new ArrayList<>();
        int orden = 1;

        // ========== REGALÍAS MINERAS (sobre cada mineral específico) ==========

        // Regalía Minera - Mineral Principal (Zn o Pb): 3%
        if ("Zn".equalsIgnoreCase(mineralPrincipal)) {
            deducciones.add(new DeduccionInput(
                    "Regalía Minera - Zinc",
                    new BigDecimal("3.0"),
                    "regalia",
                    "Regalía minera por Zinc según ley aplicable",
                    "valor_bruto_principal",
                    orden++
            ));
        } else if ("Pb".equalsIgnoreCase(mineralPrincipal)) {
            deducciones.add(new DeduccionInput(
                    "Regalía Minera - Plomo",
                    new BigDecimal("3.0"),
                    "regalia",
                    "Regalía minera por Plomo según ley aplicable",
                    "valor_bruto_principal",
                    orden++
            ));
        }

        // Regalía Minera - Plata: 3.6% (solo si hay contenido de Ag)
        if (valorBrutoAg.compareTo(BigDecimal.ZERO) > 0) {
            deducciones.add(new DeduccionInput(
                    "Regalía Minera - Plata",
                    new BigDecimal("3.6"),
                    "regalia",
                    "Regalía minera por Plata según ley aplicable",
                    "valor_bruto_ag",
                    orden++
            ));
        }

        // ========== APORTES Y CONTRIBUCIONES (sobre valor bruto total) ==========

        // Aporte a la Cooperativa: 3%
        deducciones.add(new DeduccionInput(
                "Aporte a la Cooperativa",
                new BigDecimal("3.0"),
                "aporte",
                "Aporte obligatorio a la cooperativa minera",
                "valor_bruto_total",
                orden++
        ));

        // C.N.S (Caja Nacional de Salud): 1.8%
        deducciones.add(new DeduccionInput(
                "C.N.S",
                new BigDecimal("1.8"),
                "aporte",
                "Caja Nacional de Salud",
                "valor_bruto_total",
                orden++
        ));

        // COMIBOL (Corporación Minera de Bolivia): 1%
        deducciones.add(new DeduccionInput(
                "COMIBOL",
                new BigDecimal("1.0"),
                "aporte",
                "Corporación Minera de Bolivia",
                "valor_bruto_total",
                orden++
        ));

        // FEDECOMIN (Federación de Cooperativas Mineras): 1%
        deducciones.add(new DeduccionInput(
                "FEDECOMIN",
                new BigDecimal("1.0"),
                "aporte",
                "Federación de Cooperativas Mineras",
                "valor_bruto_total",
                orden++
        ));

        // FENCOMIN (Federación Nacional de Cooperativas Mineras): 0.4%
        deducciones.add(new DeduccionInput(
                "FENCOMIN",
                new BigDecimal("0.4"),
                "aporte",
                "Federación Nacional de Cooperativas Mineras",
                "valor_bruto_total",
                orden++
        ));

        return deducciones;
    }

    // ==================== 4. LISTAR Y CONSULTAR ====================

    @Transactional(readOnly = true)
    public Page<VentaLiquidacionResponseDto> listarVentas(
            Integer usuarioId,
            String estado,
            String tipoLiquidacion,
            LocalDateTime fechaDesde,
            LocalDateTime fechaHasta,
            int page,
            int size
    ) {
        Socio socio = obtenerSocioDelUsuario(usuarioId);
        List<Liquidacion> ventas = liquidacionRepository
                .findBySocioIdAndTipoLiquidacionInOrderByCreatedAtDesc(socio, LiquidacionVentaBl.TIPOS_VENTA);
        return liquidacionVentaBl.listarLiquidaciones(
                ventas, estado, tipoLiquidacion, fechaDesde, fechaHasta, page, size);
    }

    @Transactional(readOnly = true)
    public VentaLiquidacionResponseDto obtenerDetalleVenta(Integer liquidacionId, Integer usuarioId) {
        Socio socio = obtenerSocioDelUsuario(usuarioId);
        Liquidacion liquidacion = obtenerLiquidacionConPermisos(liquidacionId, socio);
        return liquidacionVentaBl.convertirADto(liquidacion);
    }

    // ==================== 5. ESTADÍSTICAS ====================

    @Transactional(readOnly = true)
    public Map<String, Object> obtenerEstadisticas(Integer usuarioId) {
        Socio socio = obtenerSocioDelUsuario(usuarioId);
        List<Liquidacion> todas = liquidacionRepository
                .findBySocioIdAndTipoLiquidacionInOrderByCreatedAtDesc(socio, LiquidacionVentaBl.TIPOS_VENTA);

        Map<String, Object> stats = new HashMap<>();
        stats.put("total", todas.size());
        stats.put("pendienteAprobacion", contarPorEstado(todas, "pendiente_aprobacion"));
        stats.put("esperandoReportes", contarPorEstado(todas, "esperando_reportes"));
        stats.put("esperandoCierreVenta", contarPorEstado(todas, "esperando_cierre_venta"));
        stats.put("cerradas", contarPorEstado(todas, "cerrado"));
        stats.put("pagadas", contarPorEstado(todas, "pagado"));
        stats.put("rechazadas", contarPorEstado(todas, "rechazado"));

        stats.put("totalCobradoBob", todas.stream()
                .filter(l -> "pagado".equals(l.getEstado()))
                .map(Liquidacion::getValorNetoBob).filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        stats.put("totalPendienteBob", todas.stream()
                .filter(l -> "cerrado".equals(l.getEstado()))
                .map(Liquidacion::getValorNetoBob).filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add));

        return stats;
    }

    // ==================== 6. CONSULTAS AUXILIARES ====================

    @Transactional(readOnly = true)
    public List<Map<String, Object>> obtenerConcentradosDisponibles(Integer usuarioId) {
        Socio socio = obtenerSocioDelUsuario(usuarioId);
        // Usa findBySocioPropietarioIdOrderByCreatedAtDesc y filtra por estado en Java
        // ya que findBySocioPropietarioIdAndEstado puede no existir aún en tu repo
        return concentradoRepository.findBySocioPropietarioIdOrderByCreatedAtDesc(socio).stream()
                .filter(c -> "listo_para_venta".equals(c.getEstado()))
                .map(c -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", c.getId());
                    map.put("codigoConcentrado", c.getCodigoConcentrado());
                    map.put("mineralPrincipal", c.getMineralPrincipal());
                    map.put("pesoInicial", c.getPesoInicial());
                    map.put("pesoFinal", c.getPesoFinal());
                    map.put("pesoTmh", c.getPesoTmh());
                    map.put("pesoTms", c.getPesoTms());
                    map.put("numeroSacos", c.getNumeroSacos());
                    // IngenioMinero entity - ajustar getter si el campo se llama diferente
                    IngenioMinero ingenio = c.getIngenioMineroId();
                    map.put("ingenioNombre", ingenio != null ? ingenio.getRazonSocial() : null);
                    return map;
                }).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> obtenerLotesDisponiblesParaVenta(Integer usuarioId) {
        Socio socio = obtenerSocioDelUsuario(usuarioId);
        return lotesRepository.findAll().stream()
                .filter(l -> l.getMinasId().getSocioId().getId().equals(socio.getId()))
                .filter(l -> "Transporte completo".equals(l.getEstado()))
                .map(l -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", l.getId());
                    map.put("minaNombre", l.getMinasId().getNombre());
                    map.put("tipoMineral", l.getTipoMineral());
                    map.put("pesoTotalReal", l.getPesoTotalReal());
                    map.put("estado", l.getEstado());
                    return map;
                }).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> obtenerComercializadorasDisponibles() {
        return comercializadoraRepository.findAll().stream()
                .map(c -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", c.getId());
                    map.put("razonSocial", c.getRazonSocial());
                    map.put("nit", c.getNit());
                    map.put("departamento", c.getDepartamento());
                    map.put("municipio", c.getMunicipio());
                    return map;
                }).collect(Collectors.toList());
    }

    // ==================== MÉTODOS PRIVADOS ====================

    private Socio obtenerSocioDelUsuario(Integer usuarioId) {
        Usuarios usuario = usuariosRepository.findById(usuarioId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));
        return socioRepository.findByUsuariosId(usuario)
                .orElseThrow(() -> new IllegalArgumentException("Socio no encontrado"));
    }

    private Liquidacion obtenerLiquidacionConPermisos(Integer liquidacionId, Socio socio) {
        Liquidacion liquidacion = liquidacionVentaBl.obtenerLiquidacion(liquidacionId);

        if (!LiquidacionVentaBl.TIPOS_VENTA.contains(liquidacion.getTipoLiquidacion())) {
            throw new IllegalArgumentException("Esta no es una liquidación de venta");
        }
        if (!liquidacion.getSocioId().getId().equals(socio.getId())) {
            throw new IllegalArgumentException("No tienes permiso para acceder a esta liquidación");
        }
        return liquidacion;
    }


    private void asociarReporteALiquidacion(Liquidacion liquidacion, ReporteQuimico reporte, Integer referenciaId) {
        if (LiquidacionVentaBl.TIPO_VENTA_CONCENTRADO.equals(liquidacion.getTipoLiquidacion())) {
            LiquidacionConcentrado lc = liquidacion.getLiquidacionConcentradoList().stream()
                    .filter(rel -> rel.getConcentradoId().getId().equals(referenciaId))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Concentrado ID " + referenciaId + " no está en esta liquidación"));

            if (lc.getReporteQuimicoId() != null) {
                // El otro lado ya subió, crear nuevo registro
                LiquidacionConcentrado lcNuevo = LiquidacionConcentrado.builder()
                        .liquidacionId(liquidacion)
                        .concentradoId(lc.getConcentradoId())
                        .pesoLiquidado(lc.getPesoLiquidado())
                        .reporteQuimicoId(reporte)
                        .build();
                liquidacion.addLiquidacionConcentrado(lcNuevo);
                liquidacionConcentradoRepository.save(lcNuevo);
            } else {
                lc.setReporteQuimicoId(reporte);
                liquidacionConcentradoRepository.save(lc);
            }
        } else {
            LiquidacionLote ll = liquidacion.getLiquidacionLoteList().stream()
                    .filter(rel -> rel.getLotesId().getId().equals(referenciaId))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Lote ID " + referenciaId + " no está en esta liquidación"));

            if (ll.getReporteQuimicoId() != null) {
                LiquidacionLote llNuevo = LiquidacionLote.builder()
                        .liquidacionId(liquidacion)
                        .lotesId(ll.getLotesId())
                        .pesoEntrada(ll.getPesoEntrada())
                        .reporteQuimicoId(reporte)
                        .build();
                liquidacion.addLiquidacionLote(llNuevo);
                liquidacionLoteRepository.save(llNuevo);
            } else {
                ll.setReporteQuimicoId(reporte);
                liquidacionLoteRepository.save(ll);
            }
        }
    }

    private void notificarComercializadora(Liquidacion liquidacion, Comercializadora comercializadora,
                                           String titulo, String mensaje, String tipo) {
        Integer usuarioIdCom = comercializadora.getUsuariosId().getId();
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("liquidacionId", liquidacion.getId());
        metadata.put("tipoLiquidacion", liquidacion.getTipoLiquidacion());
        notificacionBl.crearNotificacion(usuarioIdCom, tipo, titulo, mensaje, metadata);
    }

    private long contarPorEstado(List<Liquidacion> liquidaciones, String estado) {
        return liquidaciones.stream().filter(l -> estado.equals(l.getEstado())).count();
    }
}