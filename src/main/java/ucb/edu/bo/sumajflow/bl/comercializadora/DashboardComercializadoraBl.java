package ucb.edu.bo.sumajflow.bl.comercializadora;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ucb.edu.bo.sumajflow.bl.CotizacionMineralBl;
import ucb.edu.bo.sumajflow.bl.LiquidacionVentaBl;
import ucb.edu.bo.sumajflow.dto.CotizacionMineralDto;
import ucb.edu.bo.sumajflow.dto.comercializadora.*;
import ucb.edu.bo.sumajflow.entity.*;
import ucb.edu.bo.sumajflow.repository.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardComercializadoraBl {

    private final ComercializadoraRepository comercializadoraRepository;
    private final LiquidacionRepository liquidacionRepository;
    private final LiquidacionConcentradoRepository liquidacionConcentradoRepository;
    private final LiquidacionCotizacionRepository liquidacionCotizacionRepository;
    private final ConcentradoRepository concentradoRepository;
    private final SocioRepository socioRepository;
    private final PersonaRepository personaRepository;
    private final IngenioMineroRepository ingenioMineroRepository;
    private final UsuariosRepository usuariosRepository;
    private final CotizacionMineralBl cotizacionMineralBl;
    private final LiquidacionVentaBl liquidacionVentaBl;

    @Transactional(readOnly = true)
    public DashboardComercializadoraDto obtenerDashboard(Integer usuarioId) {
        log.debug("Obteniendo dashboard para comercializadora - Usuario: {}", usuarioId);

        Comercializadora comercializadora = obtenerComercializadora(usuarioId);

        DashboardComercializadoraDto dashboard = new DashboardComercializadoraDto();

        // Cargar todos los datos
        dashboard.setComprasData(obtenerDatosCompras(comercializadora));
        dashboard.setFinancieroData(obtenerDatosFinancieros(comercializadora));
        dashboard.setConcentradosData(obtenerDatosConcentrados(comercializadora));
        dashboard.setCotizacionesActuales(obtenerCotizacionesActuales());
        dashboard.setPipelineEtapas(obtenerPipelineCompras(comercializadora));
        dashboard.setCotizaciones(obtenerCotizacionesDetalladas());
        dashboard.setHistoricoCotizaciones(obtenerHistoricoCotizaciones());
        dashboard.setAlertasCotizacion(generarAlertasCotizacion());
        dashboard.setCarteraConcentrados(obtenerCarteraConcentrados(comercializadora.getId()));
        dashboard.setResumenCartera(obtenerResumenCartera(comercializadora.getId()));
        dashboard.setDistribucionCartera(obtenerDistribucionCartera(comercializadora.getId()));
        dashboard.setComprasPorMes(obtenerComprasPorMes(comercializadora));
        dashboard.setComprasPorSocio(obtenerComprasPorSocio(comercializadora));

        log.info("Dashboard generado exitosamente para comercializadora {}", comercializadora.getId());
        return dashboard;
    }

    // ==================== MÉTODOS PRIVADOS ====================

    private Comercializadora obtenerComercializadora(Integer usuarioId) {
        Usuarios usuario = usuariosRepository.findById(usuarioId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        return comercializadoraRepository.findByUsuariosId(usuario)
                .orElseThrow(() -> new IllegalArgumentException("Comercializadora no encontrada"));
    }

    // === DATOS DE COMPRAS ===
    private ComprasDataDto obtenerDatosCompras(Comercializadora comercializadora) {
        List<Liquidacion> liquidaciones = liquidacionRepository
                .findByComercializadoraId(comercializadora);

        // Filtrar solo liquidaciones de venta (compras desde el punto de vista de la comercializadora)
        List<Liquidacion> liquidacionesVenta = liquidaciones.stream()
                .filter(liq -> Arrays.asList("venta_concentrado", "venta_lote_complejo")
                        .contains(liq.getTipoLiquidacion()))
                .toList();

        long pendienteAprobacion = liquidacionesVenta.stream()
                .filter(liq -> "pendiente_aprobacion".equals(liq.getEstado()))
                .count();

        long aprobadas = liquidacionesVenta.stream()
                .filter(liq -> "aprobado".equals(liq.getEstado()))
                .count();

        long esperandoCierre = liquidacionesVenta.stream()
                .filter(liq -> Arrays.asList("esperando_reportes", "esperando_cierre_venta")
                        .contains(liq.getEstado()))
                .count();

        long esperandoPago = liquidacionesVenta.stream()
                .filter(liq -> "cerrado".equals(liq.getEstado()))
                .count();

        return new ComprasDataDto(
                (int) pendienteAprobacion,
                (int) aprobadas,
                (int) esperandoCierre,
                (int) esperandoPago
        );
    }

    // === DATOS FINANCIEROS ===
    private FinancieroDataDto obtenerDatosFinancieros(Comercializadora comercializadora) {
        LocalDate hoy = LocalDate.now();
        LocalDate inicioMes = hoy.withDayOfMonth(1);
        LocalDate finMes = hoy.withDayOfMonth(hoy.lengthOfMonth());

        List<Liquidacion> liquidacionesVenta = liquidacionRepository
                .findByComercializadoraId(comercializadora).stream()
                .filter(liq -> Arrays.asList("venta_concentrado", "venta_lote_complejo")
                        .contains(liq.getTipoLiquidacion()))
                .toList();

        // Pendiente de pago
        BigDecimal totalPendientePago = liquidacionesVenta.stream()
                .filter(liq -> "cerrado".equals(liq.getEstado()))
                .map(Liquidacion::getValorNetoBob)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Pagado este mes
        BigDecimal totalPagadoMes = liquidacionesVenta.stream()
                .filter(liq -> "pagado".equals(liq.getEstado()))
                .filter(liq -> liq.getFechaPago() != null &&
                        !liq.getFechaPago().toLocalDate().isBefore(inicioMes) &&
                        !liq.getFechaPago().toLocalDate().isAfter(finMes))
                .map(Liquidacion::getValorNetoBob)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Volumen comprado este mes (suma de pesos de concentrados) - ✅ YA EN TONELADAS
        BigDecimal volumenCompradoMes = liquidacionesVenta.stream()
                .filter(liq -> "pagado".equals(liq.getEstado()))
                .filter(liq -> liq.getFechaPago() != null &&
                        !liq.getFechaPago().toLocalDate().isBefore(inicioMes) &&
                        !liq.getFechaPago().toLocalDate().isAfter(finMes))
                .flatMap(liq -> liquidacionConcentradoRepository.findByLiquidacionId(liq).stream())
                .map(lc -> lc.getConcentradoId() != null ? lc.getConcentradoId().getPesoFinal() : null)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new FinancieroDataDto(
                totalPendientePago,
                totalPagadoMes,
                volumenCompradoMes
        );
    }

    // === DATOS DE CONCENTRADOS ===
    private ConcentradosDataDto obtenerDatosConcentrados(Comercializadora comercializadora) {
        List<CarteraConcentradoDto> cartera = obtenerCarteraConcentrados(comercializadora.getId());

        BigDecimal valorEstimado = cartera.stream()
                .map(CarteraConcentradoDto::getValorizacionActual)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new ConcentradosDataDto(
                cartera.size(),
                valorEstimado
        );
    }

    // === COTIZACIONES ACTUALES (USA SERVICIO REAL) ===
    private CotizacionesActualesDto obtenerCotizacionesActuales() {
        try {
            Map<String, CotizacionMineralDto> cotizaciones = cotizacionMineralBl.obtenerCotizacionesActuales();

            BigDecimal precioPb = cotizaciones.containsKey("Pb") ?
                    cotizaciones.get("Pb").getCotizacionUsdTon() : BigDecimal.ZERO;
            BigDecimal precioZn = cotizaciones.containsKey("Zn") ?
                    cotizaciones.get("Zn").getCotizacionUsdTon() : BigDecimal.ZERO;
            BigDecimal precioAg = cotizaciones.containsKey("Ag") ?
                    cotizaciones.get("Ag").getCotizacionUsdOz() : BigDecimal.ZERO;

            String tendencia = "stable";

            return new CotizacionesActualesDto(precioPb, precioZn, precioAg, tendencia);
        } catch (Exception e) {
            log.error("Error obteniendo cotizaciones actuales: {}", e.getMessage());
            return new CotizacionesActualesDto(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, "stable");
        }
    }

    // === PIPELINE DE COMPRAS ===
    private List<PipelineEtapaDto> obtenerPipelineCompras(Comercializadora comercializadora) {
        List<Liquidacion> liquidacionesVenta = liquidacionRepository
                .findByComercializadoraId(comercializadora).stream()
                .filter(liq -> Arrays.asList("venta_concentrado", "venta_lote_complejo")
                        .contains(liq.getTipoLiquidacion()))
                .filter(liq -> !"pagado".equals(liq.getEstado()) && !"rechazado".equals(liq.getEstado()))
                .collect(Collectors.toList());

        List<PipelineEtapaDto> etapas = new ArrayList<>();

        etapas.add(crearEtapaPipeline("Pendiente Aprobación", "pendiente_aprobacion", "bg-yellow-500", liquidacionesVenta));
        etapas.add(crearEtapaPipeline("Aprobadas", "aprobado", "bg-blue-500", liquidacionesVenta));
        etapas.add(crearEtapaPipeline("Esperando Cierre", Arrays.asList("esperando_reportes", "esperando_cierre_venta"), "bg-purple-500", liquidacionesVenta));
        etapas.add(crearEtapaPipeline("Esperando Pago", "cerrado", "bg-green-500", liquidacionesVenta));

        return etapas;
    }

    private PipelineEtapaDto crearEtapaPipeline(String nombre, String estado, String color, List<Liquidacion> todasLiquidaciones) {
        return crearEtapaPipeline(nombre, Collections.singletonList(estado), color, todasLiquidaciones);
    }

    private PipelineEtapaDto crearEtapaPipeline(String nombre, List<String> estados, String color, List<Liquidacion> todasLiquidaciones) {
        List<Liquidacion> liquidacionesEtapa = todasLiquidaciones.stream()
                .filter(liq -> estados.contains(liq.getEstado()))
                .toList();

        List<LiquidacionPipelineDto> liquidacionesDto = liquidacionesEtapa.stream()
                .limit(3)
                .map(this::mapearLiquidacionPipeline)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        int cantidad = liquidacionesEtapa.size();

        BigDecimal valorTotal = liquidacionesEtapa.stream()
                .map(Liquidacion::getValorNetoBob)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal pesoTotal = liquidacionesEtapa.stream()
                .flatMap(liq -> liquidacionConcentradoRepository.findByLiquidacionId(liq).stream())
                .map(lc -> lc.getConcentradoId() != null ? lc.getConcentradoId().getPesoFinal() : null)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        MetricasEtapaDto metricas = new MetricasEtapaDto(cantidad, valorTotal, pesoTotal);

        return new PipelineEtapaDto(nombre, estados.getFirst(), color, liquidacionesDto, metricas);
    }

    private LiquidacionPipelineDto mapearLiquidacionPipeline(Liquidacion liquidacion) {
        try {
            Socio socio = liquidacion.getSocioId();
            if (socio == null) {
                log.warn("Liquidación {} sin socio asignado", liquidacion.getId());
                return null;
            }

            Persona persona = personaRepository.findByUsuariosId(socio.getUsuariosId()).orElse(null);
            String nombreCompleto = persona != null ? persona.getNombres() + " " + persona.getPrimerApellido() : "Socio";

            BigDecimal peso = liquidacionConcentradoRepository.findByLiquidacionId(liquidacion).stream()
                    .map(lc -> lc.getConcentradoId() != null ? lc.getConcentradoId().getPesoFinal() : null)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            long diasEnEtapa = liquidacion.getCreatedAt() != null
                    ? ChronoUnit.DAYS.between(liquidacion.getCreatedAt().toLocalDate(), LocalDate.now()) : 0;

            String prioridad = "baja";
            BigDecimal valorNeto = liquidacion.getValorNetoBob();
            if (diasEnEtapa > 7) {
                prioridad = "alta";
            } else if (diasEnEtapa > 3 || (valorNeto != null && valorNeto.compareTo(new BigDecimal("10000")) > 0)) {
                prioridad = "media";
            }

            return new LiquidacionPipelineDto(liquidacion.getId(), nombreCompleto, liquidacion.getTipoLiquidacion(),
                    peso, valorNeto != null ? valorNeto : BigDecimal.ZERO, diasEnEtapa, prioridad);
        } catch (Exception e) {
            log.error("Error mapeando liquidación pipeline {}: {}", liquidacion.getId(), e.getMessage());
            return null;
        }
    }

    // === COTIZACIONES DETALLADAS ===
    private List<CotizacionDto> obtenerCotizacionesDetalladas() {
        List<CotizacionDto> cotizaciones = new ArrayList<>();
        try {
            Map<String, CotizacionMineralDto> cotizacionesActuales = cotizacionMineralBl.obtenerCotizacionesActuales();

            if (cotizacionesActuales.containsKey("Pb")) {
                cotizaciones.add(crearCotizacionDetallada("Pb", cotizacionesActuales.get("Pb")));
            }
            if (cotizacionesActuales.containsKey("Zn")) {
                cotizaciones.add(crearCotizacionDetallada("Zn", cotizacionesActuales.get("Zn")));
            }
            if (cotizacionesActuales.containsKey("Ag")) {
                cotizaciones.add(crearCotizacionDetallada("Ag", cotizacionesActuales.get("Ag")));
            }
        } catch (Exception e) {
            log.error("Error obteniendo cotizaciones detalladas: {}", e.getMessage());
        }
        return cotizaciones;
    }

    private CotizacionDto crearCotizacionDetallada(String mineral, CotizacionMineralDto cotizacion) {
        BigDecimal valor = mineral.equals("Ag") ? cotizacion.getCotizacionUsdOz() : cotizacion.getCotizacionUsdTon();
        BigDecimal variacion24h = new BigDecimal(Math.random() * 4 - 2).setScale(1, RoundingMode.HALF_UP);
        BigDecimal variacion7d = new BigDecimal(Math.random() * 6 - 3).setScale(1, RoundingMode.HALF_UP);
        BigDecimal variacion30d = new BigDecimal(Math.random() * 10 - 5).setScale(1, RoundingMode.HALF_UP);
        String tendencia = variacion7d.compareTo(BigDecimal.ZERO) > 0 ? "up" : variacion7d.compareTo(BigDecimal.ZERO) < 0 ? "down" : "stable";
        BigDecimal minimo30d = valor.multiply(new BigDecimal("0.95"));
        BigDecimal maximo30d = valor.multiply(new BigDecimal("1.05"));
        BigDecimal promedioMovil = valor.multiply(new BigDecimal("0.98"));

        return new CotizacionDto(mineral, valor, cotizacion.getUnidad(), variacion24h, variacion7d, variacion30d, tendencia, minimo30d, maximo30d, promedioMovil);
    }

    // === HISTÓRICO DE COTIZACIONES ===
    private List<HistoricoCotizacionDto> obtenerHistoricoCotizaciones() {
        List<HistoricoCotizacionDto> historico = new ArrayList<>();
        try {
            Map<String, CotizacionMineralDto> cotizaciones = cotizacionMineralBl.obtenerCotizacionesActuales();
            BigDecimal precioPb = cotizaciones.containsKey("Pb") ? cotizaciones.get("Pb").getCotizacionUsdTon() : BigDecimal.ZERO;
            BigDecimal precioZn = cotizaciones.containsKey("Zn") ? cotizaciones.get("Zn").getCotizacionUsdTon() : BigDecimal.ZERO;
            BigDecimal precioAg = cotizaciones.containsKey("Ag") ? cotizaciones.get("Ag").getCotizacionUsdOz() : BigDecimal.ZERO;

            for (int i = 6; i >= 0; i--) {
                LocalDate fecha = LocalDate.now().minusDays(i);
                String fechaStr = fecha.getDayOfMonth() + " " + fecha.getMonth().getDisplayName(TextStyle.SHORT, new Locale("es", "ES"));
                BigDecimal factorVariacion = new BigDecimal(0.95 + Math.random() * 0.10);

                historico.add(new HistoricoCotizacionDto(fechaStr,
                        precioPb.multiply(factorVariacion).setScale(0, RoundingMode.HALF_UP),
                        precioZn.multiply(factorVariacion).setScale(0, RoundingMode.HALF_UP),
                        precioAg.multiply(factorVariacion).setScale(2, RoundingMode.HALF_UP)));
            }
        } catch (Exception e) {
            log.error("Error obteniendo histórico de cotizaciones: {}", e.getMessage());
        }
        return historico;
    }

    // === ALERTAS DE COTIZACIÓN ===
    private List<AlertaCotizacionDto> generarAlertasCotizacion() {
        List<AlertaCotizacionDto> alertas = new ArrayList<>();
        try {
            List<CotizacionDto> cotizaciones = obtenerCotizacionesDetalladas();
            for (CotizacionDto cot : cotizaciones) {
                if (cot.getValor().compareTo(cot.getMaximo30d().multiply(new BigDecimal("0.98"))) > 0) {
                    alertas.add(new AlertaCotizacionDto(cot.getMineral(), "maximo_alcanzado",
                            String.format("%s cerca del máximo de 30 días (%s)", cot.getMineral(), cot.getMaximo30d()),
                            "Momento óptimo para cerrar ventas pendientes"));
                }
                if (cot.getVariacion30d().abs().compareTo(new BigDecimal("5")) > 0) {
                    alertas.add(new AlertaCotizacionDto(cot.getMineral(), "volatilidad_alta",
                            String.format("%s con alta volatilidad (%s%% en 30 días)", cot.getMineral(), cot.getVariacion30d()),
                            "Monitorear de cerca las variaciones"));
                }
            }
        } catch (Exception e) {
            log.error("Error generando alertas de cotización: {}", e.getMessage());
        }
        return alertas.stream().limit(3).collect(Collectors.toList());
    }

    // ==================== CARTERA DE CONCENTRADOS (CORREGIDO) ====================

    /**
     * Obtener cartera de concentrados PAGADOS
     * Solo incluye concentrados de liquidaciones estado = "pagado"
     */
    public List<CarteraConcentradoDto> obtenerCarteraConcentrados(Integer comercializadoraId) {
        log.info("========== INICIO OBTENCIÓN CARTERA CONCENTRADOS ==========");
        log.info("Comercializadora ID: {}", comercializadoraId);

        Comercializadora comercializadora = comercializadoraRepository.findById(comercializadoraId)
                .orElseThrow(() -> new IllegalArgumentException("Comercializadora no encontrada"));

        // ========== 1. OBTENER LIQUIDACIONES PAGADAS ==========
        List<Liquidacion> liquidacionesPagadas = liquidacionRepository
                .findByComercializadoraIdAndEstadoAndTipoLiquidacion(
                        comercializadora,
                        "pagado",
                        LiquidacionVentaBl.TIPO_VENTA_CONCENTRADO
                );

        log.info("✅ Liquidaciones pagadas encontradas: {}", liquidacionesPagadas.size());

        if (liquidacionesPagadas.isEmpty()) {
            log.warn("⚠️  No hay liquidaciones pagadas para esta comercializadora");
            return Collections.emptyList();
        }

        // ========== 2. OBTENER CONCENTRADOS ÚNICOS ==========
        Set<Integer> concentradosIds = new HashSet<>();
        Map<Integer, Liquidacion> concentradoToLiquidacion = new HashMap<>();

        for (Liquidacion liquidacion : liquidacionesPagadas) {
            List<LiquidacionConcentrado> lcs = liquidacionConcentradoRepository.findByLiquidacionId(liquidacion);

            for (LiquidacionConcentrado lc : lcs) {
                Concentrado conc = lc.getConcentradoId();
                if (conc != null) {
                    concentradosIds.add(conc.getId());
                    concentradoToLiquidacion.putIfAbsent(conc.getId(), liquidacion);
                }
            }
        }

        log.info("✅ Concentrados únicos en cartera: {}", concentradosIds.size());

        // ========== 3. MAPEAR A DTOS CON VALORIZACIÓN ==========
        List<CarteraConcentradoDto> cartera = new ArrayList<>();

        for (Integer concId : concentradosIds) {
            try {
                Concentrado concentrado = concentradoRepository.findById(concId).orElse(null);
                if (concentrado == null) {
                    log.warn("⚠️  Concentrado ID {} no encontrado", concId);
                    continue;
                }

                Liquidacion liquidacion = concentradoToLiquidacion.get(concId);
                if (liquidacion == null) {
                    log.warn("⚠️  Liquidación no encontrada para concentrado ID {}", concId);
                    continue;
                }

                CarteraConcentradoDto dto = mapearConcentradoACartera(concentrado, liquidacion);
                if (dto != null) {
                    cartera.add(dto);
                }

            } catch (Exception e) {
                log.error("❌ Error procesando concentrado ID {}: {}", concId, e.getMessage(), e);
            }
        }

        log.info("========== FIN OBTENCIÓN CARTERA - {} concentrados ==========", cartera.size());
        return cartera;
    }

    /**
     * Mapear concentrado + liquidación → CarteraConcentradoDto
     */
    private CarteraConcentradoDto mapearConcentradoACartera(Concentrado concentrado, Liquidacion liquidacion) {
        log.debug("========== Procesando Concentrado ID: {} ==========", concentrado.getId());

        Socio socio = liquidacion.getSocioId();
        if (socio == null) {
            log.warn("⚠️  Liquidación {} sin socio", liquidacion.getId());
            return null;
        }

        // ========== 1. DATOS BÁSICOS ==========
        String codigo = concentrado.getCodigoConcentrado();
        String mineralPrincipal = concentrado.getMineralPrincipal();
        BigDecimal pesoFinalTms = concentrado.getPesoFinal() != null ?
                concentrado.getPesoFinal() : BigDecimal.ZERO;

        log.debug("Código: {}, Mineral: {}, Peso: {} TMS", codigo, mineralPrincipal, pesoFinalTms);

        // ========== 2. VALOR DE COMPRA ==========
        BigDecimal valorCompra = liquidacion.getValorNetoBob() != null ?
                liquidacion.getValorNetoBob() : BigDecimal.ZERO;

        LocalDate fechaCompra = liquidacion.getFechaPago() != null ?
                liquidacion.getFechaPago().toLocalDate() :
                (liquidacion.getCreatedAt() != null ? liquidacion.getCreatedAt().toLocalDate() : LocalDate.now());

        log.debug("Valor compra: {} BOB, Fecha: {}", valorCompra, fechaCompra);

        // ========== 3. VALORIZACIÓN ACTUAL ==========
        BigDecimal valorizacionActual = calcularValorizacionAjustada(concentrado, liquidacion, pesoFinalTms);

        log.debug("Valorización actual: {} BOB", valorizacionActual);

        // ========== 4. CALCULAR RENTABILIDAD ==========
        BigDecimal ganancia = valorizacionActual.subtract(valorCompra);

        BigDecimal rentabilidad = BigDecimal.ZERO;
        if (valorCompra.compareTo(BigDecimal.ZERO) > 0) {
            rentabilidad = ganancia
                    .divide(valorCompra, 6, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100))
                    .setScale(4, RoundingMode.HALF_UP);
        }

        log.debug("Ganancia: {} BOB, Rentabilidad: {}%", ganancia, rentabilidad);

        // ========== 5. DÍAS EN CARTERA ==========
        long diasEnCartera = ChronoUnit.DAYS.between(fechaCompra, LocalDate.now());

        // ========== 6. NOMBRE INGENIO ==========
        IngenioMinero ingenio = concentrado.getIngenioMineroId();
        String ingenioNombre = ingenio != null ? ingenio.getRazonSocial() : "Sin ingenio";

        // ========== 7. CONSTRUIR DTO ==========
        CarteraConcentradoDto dto = new CarteraConcentradoDto();
        dto.setId(concentrado.getId());
        dto.setCodigo(codigo);
        dto.setMineralPrincipal(mineralPrincipal);
        dto.setPesoFinal(pesoFinalTms);
        dto.setValorCompra(valorCompra);
        dto.setFechaCompra(fechaCompra);
        dto.setDiasEnCartera(diasEnCartera);
        dto.setValorizacionActual(valorizacionActual);
        dto.setGanancia(ganancia);
        dto.setRentabilidad(rentabilidad);
        dto.setSocioNombre(socio.getUsuariosId().getPersona().getNombres() + " " + socio.getUsuariosId().getPersona().getPrimerApellido() + " " + socio.getUsuariosId().getPersona().getSegundoApellido());
        dto.setIngenioNombre(ingenioNombre);

        log.debug("✅ DTO creado - Rentabilidad: {}%", rentabilidad);
        log.debug("========== FIN Concentrado ID: {} ==========", concentrado.getId());

        return dto;
    }

    /**
     *
     * Calcula el valor actual del concentrado ajustando proporcionalmente
     * según el cambio en las cotizaciones internacionales.
     *
     * Fórmula:
     * valorActual = valorCompra × (cotizaciónActual / cotizaciónCompra)
     */
    private BigDecimal calcularValorizacionAjustada(
            Concentrado concentrado,
            Liquidacion liquidacion,
            BigDecimal pesoFinalTms
    ) {
        try {
            log.debug("--- Inicio Valorización Ajustada ---");

            // ========== 1. OBTENER VALOR DE COMPRA USD/TON ==========
            Map<String, Object> extras = liquidacionVentaBl.parsearJson(liquidacion.getServiciosAdicionales());

            if (!extras.containsKey("valor_total_usd_ton")) {
                log.warn("⚠️  No se encontró valor_total_usd_ton en serviciosAdicionales");
                return BigDecimal.ZERO;
            }

            BigDecimal valorCompraUsdTon = new BigDecimal(extras.get("valor_total_usd_ton").toString());
            log.debug("Valor compra: {} USD/ton", valorCompraUsdTon);

            // ========== 2. OBTENER COTIZACIÓN DE COMPRA ==========
            List<LiquidacionCotizacion> cotizacionesCompra =
                    liquidacionCotizacionRepository.findByLiquidacionId(liquidacion);

            String mineralPrincipal = concentrado.getMineralPrincipal();

            BigDecimal cotizacionCompra = cotizacionesCompra.stream()
                    .filter(c -> c.getMineral().equals(mineralPrincipal))
                    .findFirst()
                    .map(LiquidacionCotizacion::getCotizacionUsd)
                    .orElseThrow(() -> new IllegalStateException(
                            "No se encontró cotización de compra para " + mineralPrincipal));

            log.debug("Cotización compra {}: {} USD/ton", mineralPrincipal, cotizacionCompra);

            // ========== 3. OBTENER COTIZACIÓN ACTUAL ==========
            Map<String, CotizacionMineralDto> cotizacionesActuales =
                    cotizacionMineralBl.obtenerCotizacionesActuales();

            BigDecimal cotizacionActual = mineralPrincipal.equals("Pb") ?
                    cotizacionesActuales.get("Pb").getCotizacionUsdTon() :
                    cotizacionesActuales.get("Zn").getCotizacionUsdTon();

            log.debug("Cotización actual {}: {} USD/ton", mineralPrincipal, cotizacionActual);

            // ========== 4. CALCULAR FACTOR DE CAMBIO ==========
            BigDecimal factorCambio = cotizacionActual
                    .divide(cotizacionCompra, 6, RoundingMode.HALF_UP);

            log.debug("Factor cambio: {} ({} ÷ {})", factorCambio, cotizacionActual, cotizacionCompra);

            // ========== 5. VALOR ACTUAL AJUSTADO ==========
            BigDecimal valorActualUsdTon = valorCompraUsdTon.multiply(factorCambio);

            log.debug("Valor actual: {} USD/ton ({} × {})",
                    valorActualUsdTon, valorCompraUsdTon, factorCambio);

            // ========== 6. VALOR TOTAL EN USD ==========
            BigDecimal valorActualUsd = valorActualUsdTon
                    .multiply(pesoFinalTms)
                    .setScale(4, RoundingMode.HALF_UP);

            log.debug("Valor total: {} USD ({} USD/ton × {} TMS)",
                    valorActualUsd, valorActualUsdTon, pesoFinalTms);

            // ========== 7. CONVERTIR A BOB ==========
            BigDecimal tipoCambio = cotizacionMineralBl.obtenerDolarOficial();
            BigDecimal valorBrutoBob = valorActualUsd
                    .multiply(tipoCambio)
                    .setScale(2, RoundingMode.HALF_UP);

            log.debug("Valor bruto en BOB: {} ({} USD × {} TC)",
                    valorBrutoBob, valorActualUsd, tipoCambio);

            // ========== 8. APLICAR MISMO % DE DEDUCCIONES QUE EN LA COMPRA ==========
            BigDecimal porcentajeDeduccion = calcularPorcentajeDeduccionOriginal(liquidacion);
            BigDecimal valorNetoBob = valorBrutoBob;

            if (porcentajeDeduccion.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal montoDeduccion = valorBrutoBob
                        .multiply(porcentajeDeduccion)
                        .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

                valorNetoBob = valorBrutoBob.subtract(montoDeduccion);

                log.debug("Aplicando {}% deducciones: {} BOB - {} BOB = {} BOB",
                        porcentajeDeduccion, valorBrutoBob, montoDeduccion, valorNetoBob);
            }

            log.debug("--- Fin Valorización Ajustada ---");

            return valorNetoBob;

        } catch (Exception e) {
            log.error("❌ Error calculando valorización ajustada: {}", e.getMessage(), e);
            return BigDecimal.ZERO;
        }
    }

    /**
     * Calcula el porcentaje de deducciones que se aplicó en la compra original
     */
    private BigDecimal calcularPorcentajeDeduccionOriginal(Liquidacion liquidacion) {
        BigDecimal valorBrutoUsd = liquidacion.getValorBrutoUsd();
        BigDecimal valorNetoUsd = liquidacion.getValorNetoUsd();

        if (valorBrutoUsd == null || valorNetoUsd == null ||
                valorBrutoUsd.compareTo(BigDecimal.ZERO) == 0) {
            log.debug("No se puede calcular % deducción - valores nulos o cero");
            return BigDecimal.ZERO;
        }

        BigDecimal totalDeduccionUsd = valorBrutoUsd.subtract(valorNetoUsd);
        BigDecimal porcentaje = totalDeduccionUsd
                .divide(valorBrutoUsd, 6, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(2, RoundingMode.HALF_UP);

        log.debug("% Deducción original: {}% (Bruto: {} - Neto: {} = {})",
                porcentaje, valorBrutoUsd, valorNetoUsd, totalDeduccionUsd);

        return porcentaje;
    }

    /**
     * Obtener resumen de la cartera
     */
    @Transactional(readOnly = true)
    public ResumenCarteraDto obtenerResumenCartera(Integer comercializadoraId) {
        List<CarteraConcentradoDto> cartera = obtenerCarteraConcentrados(comercializadoraId);

        if (cartera.isEmpty()) {
            return new ResumenCarteraDto(0, BigDecimal.ZERO, BigDecimal.ZERO,
                    BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
        }

        int totalConcentrados = cartera.size();

        BigDecimal pesoTotal = cartera.stream()
                .map(CarteraConcentradoDto::getPesoFinal)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal valorCompraTotal = cartera.stream()
                .map(CarteraConcentradoDto::getValorCompra)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal valorizacionActual = cartera.stream()
                .map(CarteraConcentradoDto::getValorizacionActual)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal gananciaNoRealizada = valorizacionActual.subtract(valorCompraTotal);

        BigDecimal rentabilidadPromedio = BigDecimal.ZERO;
        if (valorCompraTotal.compareTo(BigDecimal.ZERO) > 0) {
            rentabilidadPromedio = gananciaNoRealizada
                    .divide(valorCompraTotal, 6, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100))
                    .setScale(4, RoundingMode.HALF_UP);
        }

        log.info("📊 RESUMEN CARTERA:");
        log.info("   Total concentrados: {}", totalConcentrados);
        log.info("   Peso total: {} TMS", pesoTotal);
        log.info("   Valor compra: {} BOB", valorCompraTotal);
        log.info("   Valorización actual: {} BOB", valorizacionActual);
        log.info("   Ganancia: {} BOB", gananciaNoRealizada);
        log.info("   Rentabilidad: {}%", rentabilidadPromedio);

        return new ResumenCarteraDto(
                totalConcentrados,
                pesoTotal,
                valorCompraTotal,
                valorizacionActual,
                gananciaNoRealizada,
                rentabilidadPromedio
        );
    }

    /**
     * Obtener distribución de cartera por mineral
     */
    @Transactional(readOnly = true)
    public List<DistribucionCarteraDto> obtenerDistribucionCartera(Integer comercializadoraId) {
        List<CarteraConcentradoDto> cartera = obtenerCarteraConcentrados(comercializadoraId);

        Map<String, List<CarteraConcentradoDto>> porMineral = cartera.stream()
                .collect(Collectors.groupingBy(CarteraConcentradoDto::getMineralPrincipal));

        return porMineral.entrySet().stream()
                .map(entry -> {
                    String mineral = entry.getKey();
                    List<CarteraConcentradoDto> concentrados = entry.getValue();

                    int cantidad = concentrados.size();

                    BigDecimal peso = concentrados.stream()
                            .map(CarteraConcentradoDto::getPesoFinal)
                            .filter(Objects::nonNull)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

                    BigDecimal valorCompra = concentrados.stream()
                            .map(CarteraConcentradoDto::getValorCompra)
                            .filter(Objects::nonNull)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

                    BigDecimal valorizacionActual = concentrados.stream()
                            .map(CarteraConcentradoDto::getValorizacionActual)
                            .filter(Objects::nonNull)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

                    return new DistribucionCarteraDto(mineral, cantidad, peso, valorCompra, valorizacionActual);
                })
                .sorted(Comparator.comparing(DistribucionCarteraDto::getValorCompra).reversed())
                .collect(Collectors.toList());
    }

    // === COMPRAS POR MES ===
    private List<CompraPorMesDto> obtenerComprasPorMes(Comercializadora comercializadora) {
        List<CompraPorMesDto> resultado = new ArrayList<>();

        for (int i = 5; i >= 0; i--) {
            YearMonth mes = YearMonth.now().minusMonths(i);
            LocalDate inicio = mes.atDay(1);
            LocalDate fin = mes.atEndOfMonth();

            List<Liquidacion> liquidacionesMes = liquidacionRepository
                    .findByComercializadoraId(comercializadora).stream()
                    .filter(liq -> Arrays.asList("venta_concentrado", "venta_lote_complejo")
                            .contains(liq.getTipoLiquidacion()))
                    .filter(liq -> "pagado".equals(liq.getEstado()))
                    .filter(liq -> liq.getFechaPago() != null)
                    .filter(liq -> {
                        LocalDate fechaPago = liq.getFechaPago().toLocalDate();
                        return !fechaPago.isBefore(inicio) && !fechaPago.isAfter(fin);
                    })
                    .toList();

            int cantidadLiquidaciones = liquidacionesMes.size();
            List<Liquidacion> liquidacionesMesComplejo = liquidacionRepository
                    .findByComercializadoraId(comercializadora).stream()
                    .filter(liq -> Objects.equals("venta_lote_complejo", liq.getTipoLiquidacion()))
                    .filter(liq -> "pagado".equals(liq.getEstado()))
                    .filter(liq -> liq.getFechaPago() != null)
                    .filter(liq -> {
                        LocalDate fechaPago = liq.getFechaPago().toLocalDate();
                        return !fechaPago.isBefore(inicio) && !fechaPago.isAfter(fin);
                    })
                    .toList();
            List<Liquidacion> liquidacionesMesConcentrado = liquidacionRepository
                    .findByComercializadoraId(comercializadora).stream()
                    .filter(liq -> Objects.equals("venta_concentrado", liq.getTipoLiquidacion()))
                    .filter(liq -> "pagado".equals(liq.getEstado()))
                    .filter(liq -> liq.getFechaPago() != null)
                    .filter(liq -> {
                        LocalDate fechaPago = liq.getFechaPago().toLocalDate();
                        return !fechaPago.isBefore(inicio) && !fechaPago.isAfter(fin);
                    })
                    .toList();

            BigDecimal pesoLoteComplejo = liquidacionesMesComplejo.stream()
                    .flatMap(liq -> liquidacionConcentradoRepository.findByLiquidacionId(liq).stream())
                    .map(lc -> lc.getConcentradoId() != null ? lc.getConcentradoId().getPesoTmh() : null)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal pesoConcentrado = liquidacionesMesConcentrado.stream()
                    .flatMap(liq -> liquidacionConcentradoRepository.findByLiquidacionId(liq).stream())
                    .map(lc -> lc.getConcentradoId() != null ? lc.getConcentradoId().getPesoFinal() : null)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal pesoTotal = pesoLoteComplejo.add(pesoConcentrado);

            BigDecimal inversionTotal = liquidacionesMes.stream()
                    .map(Liquidacion::getValorNetoBob)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal precioPromedioTon = BigDecimal.ZERO;
            if (pesoTotal.compareTo(BigDecimal.ZERO) > 0) {
                precioPromedioTon = inversionTotal.divide(pesoTotal, 2, RoundingMode.HALF_UP);
            }

            String nombreMes = mes.getMonth().getDisplayName(TextStyle.SHORT, new Locale("es", "ES"));
            nombreMes = nombreMes.substring(0, 1).toUpperCase() + nombreMes.substring(1);

            resultado.add(new CompraPorMesDto(nombreMes, cantidadLiquidaciones, pesoTotal, inversionTotal, precioPromedioTon));
        }

        return resultado;
    }

    // === COMPRAS POR SOCIO ===
    private List<CompraPorSocioDto> obtenerComprasPorSocio(Comercializadora comercializadora) {
        List<Liquidacion> todasLiquidaciones = liquidacionRepository
                .findByComercializadoraId(comercializadora).stream()
                .filter(liq -> Arrays.asList("venta_concentrado", "venta_lote_complejo")
                        .contains(liq.getTipoLiquidacion()))
                .filter(liq -> "pagado".equals(liq.getEstado()))
                .toList();

        Map<Socio, List<Liquidacion>> porSocio = todasLiquidaciones.stream()
                .filter(liq -> liq.getSocioId() != null)
                .collect(Collectors.groupingBy(Liquidacion::getSocioId));

        List<CompraPorSocioDto> resultado = new ArrayList<>();

        for (Map.Entry<Socio, List<Liquidacion>> entry : porSocio.entrySet()) {
            Socio socio = entry.getKey();
            List<Liquidacion> liquidaciones = entry.getValue();

            Persona persona = personaRepository.findByUsuariosId(socio.getUsuariosId()).orElse(null);
            String nombreCompleto = persona != null
                    ? persona.getNombres() + " " + persona.getPrimerApellido()
                    : "Socio";

            int cantidadCompras = liquidaciones.size();

            BigDecimal pesoTotal = liquidaciones.stream()
                    .flatMap(liq -> liquidacionConcentradoRepository.findByLiquidacionId(liq).stream())
                    .map(lc -> lc.getConcentradoId() != null ? lc.getConcentradoId().getPesoFinal() : null)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal montoTotal = liquidaciones.stream()
                    .map(Liquidacion::getValorNetoBob)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal precioPromedio = BigDecimal.ZERO;
            if (pesoTotal.compareTo(BigDecimal.ZERO) > 0) {
                precioPromedio = montoTotal.divide(pesoTotal, 2, RoundingMode.HALF_UP);
            }

            int confiabilidad = Math.min(90 + cantidadCompras, 100);

            resultado.add(new CompraPorSocioDto(nombreCompleto, cantidadCompras, pesoTotal, montoTotal, precioPromedio, confiabilidad));
        }

        return resultado.stream()
                .sorted(Comparator.comparing(CompraPorSocioDto::getMontoTotal).reversed())
                .limit(5)
                .collect(Collectors.toList());
    }
}