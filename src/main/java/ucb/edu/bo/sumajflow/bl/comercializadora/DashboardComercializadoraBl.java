package ucb.edu.bo.sumajflow.bl.comercializadora;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ucb.edu.bo.sumajflow.dto.comercializadora.*;
import ucb.edu.bo.sumajflow.entity.*;
import ucb.edu.bo.sumajflow.repository.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
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
    private final ConcentradoRepository concentradoRepository;
    private final TablaPreciosMineralRepository tablaPreciosMineralRepository;
    private final SocioRepository socioRepository;
    private final PersonaRepository personaRepository;
    private final IngenioMineroRepository ingenioMineroRepository;
    private final UsuariosRepository usuariosRepository;

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
        dashboard.setCarteraConcentrados(obtenerCarteraConcentrados(comercializadora));
        dashboard.setResumenCartera(obtenerResumenCartera(comercializadora));
        dashboard.setDistribucionCartera(obtenerDistribucionCartera(comercializadora));
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

        // Filtrar solo liquidaciones de venta
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

        // Volumen comprado este mes (suma de pesos de concentrados)
        BigDecimal volumenCompradoMes = liquidacionesVenta.stream()
                .filter(liq -> "pagado".equals(liq.getEstado()))
                .filter(liq -> liq.getFechaPago() != null &&
                        !liq.getFechaPago().toLocalDate().isBefore(inicioMes) &&
                        !liq.getFechaPago().toLocalDate().isAfter(finMes))
                .flatMap(liq -> liquidacionConcentradoRepository.findByLiquidacionId(liq).stream())
                .map(lc -> lc.getConcentradoId().getPesoFinal())
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(new BigDecimal("1000"), 2, RoundingMode.HALF_UP); // Convertir a toneladas

        return new FinancieroDataDto(
                totalPendientePago,
                totalPagadoMes,
                volumenCompradoMes
        );
    }

    // === DATOS DE CONCENTRADOS ===
    private ConcentradosDataDto obtenerDatosConcentrados(Comercializadora comercializadora) {
        // Concentrados comprados y en cartera
        List<Liquidacion> liquidacionesPagadas = liquidacionRepository
                .findByComercializadoraIdAndEstado(comercializadora, "pagado");

        Set<Concentrado> concentradosEnCartera = new HashSet<>();
        for (Liquidacion liq : liquidacionesPagadas) {
            List<LiquidacionConcentrado> lcs = liquidacionConcentradoRepository
                    .findByLiquidacionId(liq);
            lcs.forEach(lc -> concentradosEnCartera.add(lc.getConcentradoId()));
        }

        // Valorización estimada actual
        BigDecimal valorEstimado = concentradosEnCartera.stream()
                .map(this::calcularValorizacionActual)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new ConcentradosDataDto(
                concentradosEnCartera.size(),
                valorEstimado
        );
    }

    // === COTIZACIONES ACTUALES ===
    private CotizacionesActualesDto obtenerCotizacionesActuales() {
        // Obtener precios más recientes de cada mineral
        BigDecimal precioPb = obtenerPrecioActual("Pb");
        BigDecimal precioZn = obtenerPrecioActual("Zn");
        BigDecimal precioAg = obtenerPrecioActual("Ag");

        // Determinar tendencia (simplificado)
        String tendencia = "stable";

        return new CotizacionesActualesDto(precioPb, precioZn, precioAg, tendencia);
    }

    private BigDecimal obtenerPrecioActual(String mineral) {
        // Obtener el precio promedio más alto de la tabla
        List<TablaPreciosMineral> precios = tablaPreciosMineralRepository
                .findByMineral(mineral);

        if (precios.isEmpty()) {
            return BigDecimal.ZERO;
        }

        // Retornar el precio promedio del rango más alto
        return precios.stream()
                .map(TablaPreciosMineral::getPrecioUsd)
                .max(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);
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

        // Etapa 1: Pendiente Aprobación
        etapas.add(crearEtapaPipeline(
                "Pendiente Aprobación",
                "pendiente_aprobacion",
                "bg-yellow-500",
                liquidacionesVenta
        ));

        // Etapa 2: Aprobadas
        etapas.add(crearEtapaPipeline(
                "Aprobadas",
                "aprobado",
                "bg-blue-500",
                liquidacionesVenta
        ));

        // Etapa 3: Esperando Cierre
        etapas.add(crearEtapaPipeline(
                "Esperando Cierre",
                Arrays.asList("esperando_reportes", "esperando_cierre_venta"),
                "bg-purple-500",
                liquidacionesVenta
        ));

        // Etapa 4: Esperando Pago
        etapas.add(crearEtapaPipeline(
                "Esperando Pago",
                "cerrado",
                "bg-green-500",
                liquidacionesVenta
        ));

        return etapas;
    }

    private PipelineEtapaDto crearEtapaPipeline(
            String nombre,
            String estado,
            String color,
            List<Liquidacion> todasLiquidaciones
    ) {
        return crearEtapaPipeline(nombre, Collections.singletonList(estado), color, todasLiquidaciones);
    }

    private PipelineEtapaDto crearEtapaPipeline(
            String nombre,
            List<String> estados,
            String color,
            List<Liquidacion> todasLiquidaciones
    ) {
        List<Liquidacion> liquidacionesEtapa = todasLiquidaciones.stream()
                .filter(liq -> estados.contains(liq.getEstado()))
                .toList();

        List<LiquidacionPipelineDto> liquidacionesDto = liquidacionesEtapa.stream()
                .limit(3) // Solo mostrar 3 en el frontend
                .map(this::mapearLiquidacionPipeline)
                .collect(Collectors.toList());

        // Métricas
        int cantidad = liquidacionesEtapa.size();

        BigDecimal valorTotal = liquidacionesEtapa.stream()
                .map(Liquidacion::getValorNetoBob)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal pesoTotal = liquidacionesEtapa.stream()
                .flatMap(liq -> liquidacionConcentradoRepository.findByLiquidacionId(liq).stream())
                .map(lc -> lc.getConcentradoId().getPesoFinal())
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        MetricasEtapaDto metricas = new MetricasEtapaDto(cantidad, valorTotal, pesoTotal);

        return new PipelineEtapaDto(
                nombre,
                estados.getFirst(),
                color,
                liquidacionesDto,
                metricas
        );
    }

    private LiquidacionPipelineDto mapearLiquidacionPipeline(Liquidacion liquidacion) {
        Socio socio = liquidacion.getSocioId();
        Persona persona = personaRepository.findByUsuariosId(socio.getUsuariosId()).orElse(null);

        String nombreCompleto = persona != null
                ? persona.getNombres() + " " + persona.getPrimerApellido()
                : "Socio";

        BigDecimal peso = liquidacionConcentradoRepository.findByLiquidacionId(liquidacion).stream()
                .map(lc -> lc.getConcentradoId().getPesoFinal())
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long diasEnEtapa = liquidacion.getCreatedAt() != null
                ? ChronoUnit.DAYS.between(liquidacion.getCreatedAt().toLocalDate(), LocalDate.now())
                : 0;

        // Determinar prioridad
        String prioridad = "baja";
        if (diasEnEtapa > 7) {
            prioridad = "alta";
        } else if (diasEnEtapa > 3 || liquidacion.getValorNetoBob().compareTo(new BigDecimal("10000")) > 0) {
            prioridad = "media";
        }

        return new LiquidacionPipelineDto(
                liquidacion.getId(),
                nombreCompleto,
                liquidacion.getTipoLiquidacion(),
                peso,
                liquidacion.getValorNetoBob(),
                diasEnEtapa,
                prioridad
        );
    }

    // === COTIZACIONES DETALLADAS ===
    private List<CotizacionDto> obtenerCotizacionesDetalladas() {
        List<CotizacionDto> cotizaciones = new ArrayList<>();

        // Pb
        cotizaciones.add(crearCotizacionDetallada("Pb", "USD/ton"));

        // Zn
        cotizaciones.add(crearCotizacionDetallada("Zn", "USD/ton"));

        // Ag
        cotizaciones.add(crearCotizacionDetallada("Ag", "USD/oz"));

        return cotizaciones;
    }

    private CotizacionDto crearCotizacionDetallada(String mineral, String unidad) {
        BigDecimal valor = obtenerPrecioActual(mineral);

        // Simulación de variaciones (en producción obtener de fuente real)
        BigDecimal variacion24h = new BigDecimal(Math.random() * 4 - 2).setScale(1, RoundingMode.HALF_UP);
        BigDecimal variacion7d = new BigDecimal(Math.random() * 6 - 3).setScale(1, RoundingMode.HALF_UP);
        BigDecimal variacion30d = new BigDecimal(Math.random() * 10 - 5).setScale(1, RoundingMode.HALF_UP);

        String tendencia = variacion7d.compareTo(BigDecimal.ZERO) > 0 ? "up" :
                variacion7d.compareTo(BigDecimal.ZERO) < 0 ? "down" : "stable";

        BigDecimal minimo30d = valor.multiply(new BigDecimal("0.95"));
        BigDecimal maximo30d = valor.multiply(new BigDecimal("1.05"));
        BigDecimal promedioMovil = valor.multiply(new BigDecimal("0.98"));

        return new CotizacionDto(
                mineral,
                valor,
                unidad,
                variacion24h,
                variacion7d,
                variacion30d,
                tendencia,
                minimo30d,
                maximo30d,
                promedioMovil
        );
    }

    // === HISTÓRICO DE COTIZACIONES ===
    private List<HistoricoCotizacionDto> obtenerHistoricoCotizaciones() {
        List<HistoricoCotizacionDto> historico = new ArrayList<>();

        BigDecimal precioPb = obtenerPrecioActual("Pb");
        BigDecimal precioZn = obtenerPrecioActual("Zn");
        BigDecimal precioAg = obtenerPrecioActual("Ag");

        // Últimos 7 días (simulación)
        for (int i = 6; i >= 0; i--) {
            LocalDate fecha = LocalDate.now().minusDays(i);
            String fechaStr = fecha.getDayOfMonth() + " " +
                    fecha.getMonth().getDisplayName(TextStyle.SHORT, new Locale("es", "ES"));

            BigDecimal factorVariacion = new BigDecimal(0.95 + Math.random() * 0.10);

            historico.add(new HistoricoCotizacionDto(
                    fechaStr,
                    precioPb.multiply(factorVariacion).setScale(0, RoundingMode.HALF_UP),
                    precioZn.multiply(factorVariacion).setScale(0, RoundingMode.HALF_UP),
                    precioAg.multiply(factorVariacion).setScale(2, RoundingMode.HALF_UP)
            ));
        }

        return historico;
    }

    // === ALERTAS DE COTIZACIÓN ===
    private List<AlertaCotizacionDto> generarAlertasCotizacion() {
        List<AlertaCotizacionDto> alertas = new ArrayList<>();

        // Simulación de alertas (en producción, analizar datos reales)
        List<CotizacionDto> cotizaciones = obtenerCotizacionesDetalladas();

        for (CotizacionDto cot : cotizaciones) {
            // Alerta si cerca del máximo
            if (cot.getValor().compareTo(cot.getMaximo30d().multiply(new BigDecimal("0.98"))) > 0) {
                alertas.add(new AlertaCotizacionDto(
                        cot.getMineral(),
                        "maximo_alcanzado",
                        String.format("%s cerca del máximo de 30 días (%s)",
                                cot.getMineral(),
                                cot.getMaximo30d()),
                        "Momento óptimo para cerrar ventas pendientes"
                ));
            }

            // Alerta si volatilidad alta
            if (cot.getVariacion30d().abs().compareTo(new BigDecimal("5")) > 0) {
                alertas.add(new AlertaCotizacionDto(
                        cot.getMineral(),
                        "volatilidad_alta",
                        String.format("%s con alta volatilidad (%s%% en 30 días)",
                                cot.getMineral(),
                                cot.getVariacion30d()),
                        "Monitorear de cerca las variaciones"
                ));
            }
        }

        return alertas.stream().limit(3).collect(Collectors.toList());
    }

    // === CARTERA DE CONCENTRADOS ===
    private List<CarteraConcentradoDto> obtenerCarteraConcentrados(Comercializadora comercializadora) {
        List<Liquidacion> liquidacionesPagadas = liquidacionRepository
                .findByComercializadoraIdAndEstado(comercializadora, "pagado");

        List<CarteraConcentradoDto> cartera = new ArrayList<>();

        for (Liquidacion liq : liquidacionesPagadas) {
            List<LiquidacionConcentrado> lcs = liquidacionConcentradoRepository
                    .findByLiquidacionId(liq);

            for (LiquidacionConcentrado lc : lcs) {
                Concentrado conc = lc.getConcentradoId();
                cartera.add(mapearCarteraConcentrado(conc, liq));
            }
        }

        return cartera.stream()
                .sorted(Comparator.comparing(CarteraConcentradoDto::getFechaCompra).reversed())
                .limit(10) // Top 10 más recientes
                .collect(Collectors.toList());
    }

    private CarteraConcentradoDto mapearCarteraConcentrado(Concentrado concentrado, Liquidacion liquidacion) {
        Socio socio = concentrado.getSocioPropietarioId();
        Persona persona = personaRepository.findByUsuariosId(socio.getUsuariosId()).orElse(null);

        String nombreSocio = persona != null
                ? persona.getNombres() + " " + persona.getPrimerApellido()
                : "Socio";

        String nombreIngenio = "Ingenio";
        // Obtener nombre del ingenio desde el concentrado o lote relacionado
        // (simplificado por ahora)

        String codigo = concentrado.getCodigoConcentrado() != null
                ? concentrado.getCodigoConcentrado()
                : "CON-" + concentrado.getId();

        LocalDate fechaCompra = liquidacion.getFechaPago() != null
                ? liquidacion.getFechaPago().toLocalDate()
                : LocalDate.now();

        long diasEnCartera = ChronoUnit.DAYS.between(fechaCompra, LocalDate.now());

        BigDecimal valorCompra = liquidacion.getValorNetoBob();
        BigDecimal valorizacionActual = calcularValorizacionActual(concentrado);
        BigDecimal ganancia = valorizacionActual.subtract(valorCompra);
        BigDecimal rentabilidad = BigDecimal.ZERO;

        if (valorCompra.compareTo(BigDecimal.ZERO) > 0) {
            rentabilidad = ganancia.divide(valorCompra, 4, RoundingMode.HALF_UP)
                    .multiply(new BigDecimal("100"));
        }

        return new CarteraConcentradoDto(
                concentrado.getId(),
                codigo,
                concentrado.getMineralPrincipal(),
                concentrado.getPesoFinal(),
                valorCompra,
                fechaCompra,
                diasEnCartera,
                valorizacionActual,
                ganancia,
                rentabilidad,
                nombreSocio,
                nombreIngenio
        );
    }

    private BigDecimal calcularValorizacionActual(Concentrado concentrado) {
        String mineral = concentrado.getMineralPrincipal();
        BigDecimal precioActual = obtenerPrecioActual(mineral);
        BigDecimal peso = concentrado.getPesoFinal();

        if (peso == null || precioActual.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }

        // Valorización simple: peso (ton) * precio
        BigDecimal pesoTon = peso.divide(new BigDecimal("1000"), 4, RoundingMode.HALF_UP);
        return pesoTon.multiply(precioActual).setScale(2, RoundingMode.HALF_UP);
    }

    // === RESUMEN DE CARTERA ===
    private ResumenCarteraDto obtenerResumenCartera(Comercializadora comercializadora) {
        List<CarteraConcentradoDto> cartera = obtenerCarteraConcentrados(comercializadora);

        int totalConcentrados = cartera.size();

        BigDecimal pesoTotal = cartera.stream()
                .map(CarteraConcentradoDto::getPesoFinal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal valorCompraTotal = cartera.stream()
                .map(CarteraConcentradoDto::getValorCompra)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal valorizacionActual = cartera.stream()
                .map(CarteraConcentradoDto::getValorizacionActual)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal gananciaNoRealizada = valorizacionActual.subtract(valorCompraTotal);

        BigDecimal rentabilidadPromedio = BigDecimal.ZERO;
        if (valorCompraTotal.compareTo(BigDecimal.ZERO) > 0) {
            rentabilidadPromedio = gananciaNoRealizada
                    .divide(valorCompraTotal, 4, RoundingMode.HALF_UP)
                    .multiply(new BigDecimal("100"));
        }

        return new ResumenCarteraDto(
                totalConcentrados,
                pesoTotal,
                valorCompraTotal,
                valorizacionActual,
                gananciaNoRealizada,
                rentabilidadPromedio
        );
    }

    // === DISTRIBUCIÓN DE CARTERA ===
    private List<DistribucionCarteraDto> obtenerDistribucionCartera(Comercializadora comercializadora) {
        List<CarteraConcentradoDto> cartera = obtenerCarteraConcentrados(comercializadora);

        Map<String, List<CarteraConcentradoDto>> porMineral = cartera.stream()
                .collect(Collectors.groupingBy(CarteraConcentradoDto::getMineralPrincipal));

        List<DistribucionCarteraDto> distribucion = new ArrayList<>();

        for (Map.Entry<String, List<CarteraConcentradoDto>> entry : porMineral.entrySet()) {
            String mineral = entry.getKey();
            List<CarteraConcentradoDto> concentrados = entry.getValue();

            int cantidad = concentrados.size();
            BigDecimal peso = concentrados.stream()
                    .map(CarteraConcentradoDto::getPesoFinal)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal valorCompra = concentrados.stream()
                    .map(CarteraConcentradoDto::getValorCompra)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal valorizacionActual = concentrados.stream()
                    .map(CarteraConcentradoDto::getValorizacionActual)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            distribucion.add(new DistribucionCarteraDto(
                    mineral,
                    cantidad,
                    peso,
                    valorCompra,
                    valorizacionActual
            ));
        }

        return distribucion.stream()
                .sorted(Comparator.comparing(DistribucionCarteraDto::getValorCompra).reversed())
                .collect(Collectors.toList());
    }

    // === COMPRAS POR MES ===
    private List<CompraPorMesDto> obtenerComprasPorMes(Comercializadora comercializadora) {
        List<CompraPorMesDto> resultado = new ArrayList<>();

        // Últimos 6 meses
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

            BigDecimal pesoTotal = liquidacionesMes.stream()
                    .flatMap(liq -> liquidacionConcentradoRepository.findByLiquidacionId(liq).stream())
                    .map(lc -> lc.getConcentradoId().getPesoFinal())
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal inversionTotal = liquidacionesMes.stream()
                    .map(Liquidacion::getValorNetoBob)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal precioPromedioTon = BigDecimal.ZERO;
            if (pesoTotal.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal pesoTon = pesoTotal.divide(new BigDecimal("1000"), 4, RoundingMode.HALF_UP);
                precioPromedioTon = inversionTotal.divide(pesoTon, 2, RoundingMode.HALF_UP);
            }

            String nombreMes = mes.getMonth().getDisplayName(TextStyle.SHORT, new Locale("es", "ES"));
            nombreMes = nombreMes.substring(0, 1).toUpperCase() + nombreMes.substring(1);

            resultado.add(new CompraPorMesDto(
                    nombreMes,
                    cantidadLiquidaciones,
                    pesoTotal,
                    inversionTotal,
                    precioPromedioTon
            ));
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
                    .map(lc -> lc.getConcentradoId().getPesoFinal())
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal montoTotal = liquidaciones.stream()
                    .map(Liquidacion::getValorNetoBob)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal precioPromedio = BigDecimal.ZERO;
            if (pesoTotal.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal pesoTon = pesoTotal.divide(new BigDecimal("1000"), 4, RoundingMode.HALF_UP);
                precioPromedio = montoTotal.divide(pesoTon, 2, RoundingMode.HALF_UP);
            }

            // Confiabilidad (simplificado: basado en cantidad de compras)
            int confiabilidad = Math.min(90 + cantidadCompras, 100);

            resultado.add(new CompraPorSocioDto(
                    nombreCompleto,
                    cantidadCompras,
                    pesoTotal,
                    montoTotal,
                    precioPromedio,
                    confiabilidad
            ));
        }

        return resultado.stream()
                .sorted(Comparator.comparing(CompraPorSocioDto::getMontoTotal).reversed())
                .limit(5) // Top 5
                .collect(Collectors.toList());
    }
}