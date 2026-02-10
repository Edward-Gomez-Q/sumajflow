package ucb.edu.bo.sumajflow.bl.socio;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ucb.edu.bo.sumajflow.dto.socio.*;
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
public class DashboardSocioBl {

    private final SocioRepository socioRepository;
    private final LotesRepository lotesRepository;
    private final AsignacionCamionRepository asignacionCamionRepository;
    private final ConcentradoRepository concentradoRepository;
    private final LiquidacionRepository liquidacionRepository;
    private final LiquidacionConcentradoRepository liquidacionConcentradoRepository;
    private final UsuariosRepository usuariosRepository;
    private final PersonaRepository personaRepository;
    private final TransportistaRepository transportistaRepository;
    private final ObjectMapper objectMapper;

    // Mapeo de estados a porcentajes
    private static final Map<String, Integer> PROGRESO_ESTADOS_VIAJE = Map.of(
            "Esperando iniciar", 5,
            "En camino a la mina", 15,
            "Esperando carguío", 30,
            "En camino balanza cooperativa", 45,
            "En camino balanza destino", 65,
            "En camino almacén destino", 80,
            "Descargando", 95,
            "Completado", 100
    );

    private static final Map<String, Integer> PROGRESO_ESTADOS_CONCENTRADO = Map.of(
            "creado", 10,
            "en_camino_a_planta", 20,
            "en_proceso", 50,
            "esperando_pago", 75,
            "listo_para_venta", 80,
            "en_venta", 90,
            "vendido_a_comercializadora", 100
    );

    @Transactional(readOnly = true)
    public DashboardSocioDto obtenerDashboard(Integer usuarioId) {
        log.debug("Obteniendo dashboard para usuario: {}", usuarioId);

        // Obtener socio
        Socio socio = obtenerSocio(usuarioId);

        DashboardSocioDto dashboard = new DashboardSocioDto();

        // Cargar todos los datos
        dashboard.setFinancialData(obtenerDatosFinancieros(socio));
        dashboard.setOperationsData(obtenerDatosOperacionales(socio));
        dashboard.setCamionesEnRuta(obtenerCamionesEnRuta(socio));
        dashboard.setAlertas(generarAlertas(socio));
        dashboard.setConcentrados(obtenerConcentradosResumen(socio));
        dashboard.setLiquidacionesPendientes(obtenerLiquidacionesPendientes(socio));
        dashboard.setIngresosMensuales(obtenerIngresosMensuales(socio));
        dashboard.setIngresosPorMineral(obtenerIngresosPorMineral(socio));

        log.info("Dashboard generado exitosamente para socio {}", socio.getId());
        return dashboard;
    }

    // ==================== MÉTODOS PRIVADOS ====================

    private Socio obtenerSocio(Integer usuarioId) {
        Usuarios usuario = usuariosRepository.findById(usuarioId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        return socioRepository.findByUsuariosId(usuario)
                .orElseThrow(() -> new IllegalArgumentException("Socio no encontrado"));
    }

    // === DATOS FINANCIEROS ===
    private FinancialDataDto obtenerDatosFinancieros(Socio socio) {
        LocalDate hoy = LocalDate.now();
        LocalDate inicioMes = hoy.withDayOfMonth(1);
        LocalDate finMes = hoy.withDayOfMonth(hoy.lengthOfMonth());

        LocalDate inicioMesAnterior = inicioMes.minusMonths(1);
        LocalDate finMesAnterior = inicioMesAnterior.withDayOfMonth(inicioMesAnterior.lengthOfMonth());

        // Total pendiente de cobro
        BigDecimal pendienteCobro = liquidacionRepository.findBySocioId(socio).stream()
                .filter(liq -> Arrays.asList("esperando_pago", "pendiente_aprobacion", "aprobado",
                                "esperando_reportes", "esperando_cierre_venta", "cerrado")
                        .contains(liq.getEstado()))
                .map(Liquidacion::getValorNetoBob)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Total cobrado mes actual
        BigDecimal cobradoMesActual = liquidacionRepository.findBySocioId(socio).stream()
                .filter(liq -> "pagado".equals(liq.getEstado()))
                .filter(liq -> liq.getFechaPago() != null &&
                        !liq.getFechaPago().toLocalDate().isBefore(inicioMes) &&
                        !liq.getFechaPago().toLocalDate().isAfter(finMes))
                .map(Liquidacion::getValorNetoBob)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Total cobrado mes anterior
        BigDecimal cobradoMesAnterior = liquidacionRepository.findBySocioId(socio).stream()
                .filter(liq -> "pagado".equals(liq.getEstado()))
                .filter(liq -> liq.getFechaPago() != null &&
                        !liq.getFechaPago().toLocalDate().isBefore(inicioMesAnterior) &&
                        !liq.getFechaPago().toLocalDate().isAfter(finMesAnterior))
                .map(Liquidacion::getValorNetoBob)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Comparativo con mes anterior
        BigDecimal comparativo = BigDecimal.ZERO;
        if (cobradoMesAnterior.compareTo(BigDecimal.ZERO) > 0) {
            comparativo = cobradoMesActual.subtract(cobradoMesAnterior)
                    .divide(cobradoMesAnterior, 4, RoundingMode.HALF_UP)
                    .multiply(new BigDecimal("100"));
        }

        // Proyección mensual (simple: cobrado actual + pendiente)
        BigDecimal proyeccion = cobradoMesActual.add(pendienteCobro);

        return new FinancialDataDto(
                pendienteCobro,
                cobradoMesActual,
                proyeccion,
                comparativo
        );
    }

    // === DATOS OPERACIONALES ===
    private OperationsDataDto obtenerDatosOperacionales(Socio socio) {
        // Obtener todos los lotes del socio
        List<Lotes> todosLotes = lotesRepository.findByMinasSocioId(socio);

        // Lotes activos (no finalizados)
        long lotesActivos = todosLotes.stream()
                .filter(lote -> !Arrays.asList("Rechazado", "Vendido a comercializadora",
                        "Procesado").contains(lote.getEstado()))
                .count();

        // Lotes en transporte
        long lotesEnTransporte = todosLotes.stream()
                .filter(lote -> "En transporte".equals(lote.getEstado()) ||
                        "Transporte completo".equals(lote.getEstado()))
                .count();

        // Lotes en proceso
        long lotesEnProceso = todosLotes.stream()
                .filter(lote -> "En planta".equals(lote.getEstado()))
                .count();

        // Concentrados del socio
        List<Concentrado> concentrados = concentradoRepository.findBySocioPropietarioId(socio);

        long concentradosListos = concentrados.stream()
                .filter(c -> "listo_para_venta".equals(c.getEstado()))
                .count();

        long concentradosEnVenta = concentrados.stream()
                .filter(c -> "en_venta".equals(c.getEstado()))
                .count();

        return new OperationsDataDto(
                (int) lotesActivos,
                (int) lotesEnTransporte,
                (int) lotesEnProceso,
                (int) concentradosListos,
                (int) concentradosEnVenta
        );
    }

    // === CAMIONES EN RUTA ===
    private List<CamionEnRutaDto> obtenerCamionesEnRuta(Socio socio) {
        // Obtener todas las asignaciones activas del socio
        List<AsignacionCamion> asignaciones = asignacionCamionRepository
                .findByLotesIdMinasSocioIdAndEstadoIn(
                        socio,
                        Arrays.asList("En camino a la mina", "Esperando carguío",
                                "En camino balanza cooperativa", "En camino balanza destino",
                                "En camino almacén destino", "Descargando")
                );

        return asignaciones.stream()
                .map(this::mapearCamionEnRuta)
                .collect(Collectors.toList());
    }

    private CamionEnRutaDto mapearCamionEnRuta(AsignacionCamion asignacion) {
        Lotes lote = asignacion.getLotesId();
        Transportista transportista = asignacion.getTransportistaId();
        Persona persona = personaRepository.findByUsuariosId(transportista.getUsuariosId())
                .orElse(null);

        String nombreCompleto = persona != null
                ? persona.getNombres() + " " + persona.getPrimerApellido()
                : "Transportista";

        // Código del lote (formato: LT-2026-001)
        String codigoLote = String.format("LT-%d-%03d",
                lote.getFechaCreacion().getYear(),
                lote.getId());

        // Progreso basado en estado
        Integer progreso = PROGRESO_ESTADOS_VIAJE.getOrDefault(asignacion.getEstado(), 50);

        // Extraer última ubicación del JSONB
        String ubicacionTexto = extraerUltimaUbicacion(asignacion);

        // Calcular minutos transcurridos
        Integer minutosTranscurridos = calcularMinutosTranscurridos(asignacion);

        return new CamionEnRutaDto(
                lote.getId(),
                codigoLote,
                asignacion.getId(),
                asignacion.getNumeroCamion(),
                transportista.getPlacaVehiculo(),
                nombreCompleto,
                asignacion.getEstado(),
                progreso,
                ubicacionTexto,
                minutosTranscurridos
        );
    }

    private String extraerUltimaUbicacion(AsignacionCamion asignacion) {
        try {
            if (asignacion.getObservaciones() == null) {
                return "Ubicación desconocida";
            }

            JsonNode observaciones = objectMapper.readTree(asignacion.getObservaciones());
            String estado = asignacion.getEstado();

            // Mapear estado a evento en el JSONB
            Map<String, String> estadoAEvento = Map.of(
                    "En camino a la mina", "inicio_viaje",
                    "Esperando carguío", "llegada_mina",
                    "En camino balanza cooperativa", "carguio_completo",
                    "En camino balanza destino", "pesaje_origen",
                    "En camino almacén destino", "pesaje_destino",
                    "Descargando", "llegada_almacen"
            );

            String evento = estadoAEvento.getOrDefault(estado, "inicio_viaje");

            if (observaciones.has(evento)) {
                JsonNode eventoNode = observaciones.get(evento);
                if (eventoNode.has("lat") && eventoNode.has("lng")) {
                    double lat = eventoNode.get("lat").asDouble();
                    double lng = eventoNode.get("lng").asDouble();
                    return String.format("%.5f, %.5f", lat, lng);
                }
            }

            return "Ubicación no disponible";

        } catch (Exception e) {
            log.warn("Error extrayendo ubicación de asignación {}: {}",
                    asignacion.getId(), e.getMessage());
            return "Error en ubicación";
        }
    }

    private Integer calcularMinutosTranscurridos(AsignacionCamion asignacion) {
        LocalDateTime inicio = asignacion.getFechaInicio();
        if (inicio == null) {
            return 0;
        }
        return (int) ChronoUnit.MINUTES.between(inicio, LocalDateTime.now());
    }

    // === ALERTAS ===
    private List<AlertaDto> generarAlertas(Socio socio) {
        List<AlertaDto> alertas = new ArrayList<>();
        int alertaId = 1;

        // 1. Liquidaciones Toll vencidas
        List<Liquidacion> tollVencidas = liquidacionRepository.findBySocioId(socio).stream()
                .filter(liq -> "toll".equals(liq.getTipoLiquidacion()))
                .filter(liq -> "esperando_pago".equals(liq.getEstado()))
                .filter(liq -> {
                    if (liq.getCreatedAt() == null) return false;
                    long diasPendientes = ChronoUnit.DAYS.between(
                            liq.getCreatedAt().toLocalDate(),
                            LocalDate.now()
                    );
                    return diasPendientes > 7; // Vencido si > 7 días
                })
                .toList();

        for (Liquidacion liq : tollVencidas) {
            long diasVencido = ChronoUnit.DAYS.between(
                    liq.getCreatedAt().toLocalDate(),
                    LocalDate.now()
            );
            alertas.add(new AlertaDto(
                    alertaId++,
                    "critico",
                    "financiero",
                    "Liquidación Toll vencida",
                    String.format("Liquidación #%d lleva %d días sin pagar (%.2f BOB)",
                            liq.getId(), diasVencido, liq.getValorNetoBob()),
                    "Pagar ahora",
                    liq.getCreatedAt()
            ));
        }

        // 2. Concentrados listos para venta
        long concentradosListos = concentradoRepository.findBySocioPropietarioId(socio).stream()
                .filter(c -> "listo_para_venta".equals(c.getEstado()))
                .count();

        if (concentradosListos > 0) {
            alertas.add(new AlertaDto(
                    alertaId++,
                    "info",
                    "cotizacion",
                    "Concentrados listos para venta",
                    String.format("Tienes %d concentrado(s) listo(s). Revisa las cotizaciones actuales.",
                            concentradosListos),
                    "Ver concentrados",
                    LocalDateTime.now()
            ));
        }

        // 3. Camiones próximos a llegar
        List<AsignacionCamion> proximosLlegar = asignacionCamionRepository
                .findByLotesIdMinasSocioIdAndEstadoIn(
                        socio,
                        List.of("En camino balanza destino", "En camino almacén destino")
                );

        for (AsignacionCamion asig : proximosLlegar) {
            alertas.add(new AlertaDto(
                    alertaId++,
                    "info",
                    "transporte",
                    "Camión próximo a llegar",
                    String.format("Camión #%d del Lote #%d está %s",
                            asig.getNumeroCamion(),
                            asig.getLotesId().getId(),
                            asig.getEstado().toLowerCase()),
                    "Ver detalle",
                    LocalDateTime.now()
            ));
        }

        // Limitar a 5 alertas más recientes
        return alertas.stream()
                .sorted(Comparator.comparing(AlertaDto::getTimestamp).reversed())
                .limit(5)
                .collect(Collectors.toList());
    }

    // === CONCENTRADOS RESUMEN ===
    private List<ConcentradoResumenDto> obtenerConcentradosResumen(Socio socio) {
        return concentradoRepository.findBySocioPropietarioId(socio).stream()
                .filter(c -> !c.getEstado().equals("vendido_a_comercializadora"))
                .sorted(Comparator.comparing(Concentrado::getCreatedAt).reversed())
                .limit(3) // Solo los 3 más recientes
                .map(this::mapearConcentradoResumen)
                .collect(Collectors.toList());
    }

    private ConcentradoResumenDto mapearConcentradoResumen(Concentrado concentrado) {
        Integer porcentaje = PROGRESO_ESTADOS_CONCENTRADO.getOrDefault(concentrado.getEstado(), 50);

        String etapaActual = obtenerEtapaConcentrado(concentrado.getEstado());

        LiquidacionTollDto tollDto = null;

        // Si tiene liquidación toll pendiente
        if ("esperando_pago".equals(concentrado.getEstado())) {
            List<Liquidacion> liquidaciones = liquidacionRepository
                    .findBySocioIdAndTipoLiquidacion(concentrado.getSocioPropietarioId(), "toll");

            for (Liquidacion liq : liquidaciones) {
                List<LiquidacionConcentrado> liquidacionConcentrados =
                        liquidacionConcentradoRepository.findByLiquidacionId(liq);

                boolean tieneEsteConcentrado = liquidacionConcentrados.stream()
                        .anyMatch(lc -> lc.getConcentradoId().getId().equals(concentrado.getId()));

                if (tieneEsteConcentrado && "esperando_pago".equals(liq.getEstado())) {
                    long diasPendiente = liq.getCreatedAt() != null
                            ? ChronoUnit.DAYS.between(liq.getCreatedAt().toLocalDate(), LocalDate.now())
                            : 0;

                    tollDto = new LiquidacionTollDto(
                            liq.getValorNetoBob(),
                            (int) diasPendiente
                    );
                    break;
                }
            }
        }

        return new ConcentradoResumenDto(
                concentrado.getId(),
                concentrado.getCodigoConcentrado(),
                concentrado.getMineralPrincipal(),
                concentrado.getPesoFinal(),
                concentrado.getEstado(),
                new ProgresoConcentradoDto(etapaActual, porcentaje),
                tollDto
        );
    }

    private String obtenerEtapaConcentrado(String estado) {
        Map<String, String> etapas = Map.of(
                "creado", "Creado",
                "en_camino_a_planta", "En camino a planta",
                "en_proceso", "En procesamiento",
                "esperando_pago", "Esperando pago Toll",
                "listo_para_venta", "Listo para venta",
                "en_venta", "En proceso de venta",
                "vendido_a_comercializadora", "Vendido"
        );
        return etapas.getOrDefault(estado, estado);
    }

    // === LIQUIDACIONES PENDIENTES ===
    private LiquidacionesPendientesDto obtenerLiquidacionesPendientes(Socio socio) {
        List<Liquidacion> liquidaciones = liquidacionRepository.findBySocioId(socio);

        // Toll pendiente de pago
        long tollPendiente = liquidaciones.stream()
                .filter(liq -> "toll".equals(liq.getTipoLiquidacion()))
                .filter(liq -> "esperando_pago".equals(liq.getEstado()))
                .count();

        BigDecimal tollMonto = liquidaciones.stream()
                .filter(liq -> "toll".equals(liq.getTipoLiquidacion()))
                .filter(liq -> "esperando_pago".equals(liq.getEstado()))
                .map(Liquidacion::getValorNetoBob)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Ventas pendientes de cierre
        long ventasPendientesCierre = liquidaciones.stream()
                .filter(liq -> Arrays.asList("venta_concentrado", "venta_lote_complejo")
                        .contains(liq.getTipoLiquidacion()))
                .filter(liq -> Arrays.asList("esperando_reportes", "esperando_cierre_venta")
                        .contains(liq.getEstado()))
                .count();

        // Ventas esperando pago
        long ventasEsperandoPago = liquidaciones.stream()
                .filter(liq -> Arrays.asList("venta_concentrado", "venta_lote_complejo")
                        .contains(liq.getTipoLiquidacion()))
                .filter(liq -> "cerrado".equals(liq.getEstado()))
                .count();

        return new LiquidacionesPendientesDto(
                (int) tollPendiente,
                tollMonto,
                (int) ventasPendientesCierre,
                (int) ventasEsperandoPago
        );
    }

    // === INGRESOS MENSUALES ===
    private List<IngresoMensualDto> obtenerIngresosMensuales(Socio socio) {
        List<IngresoMensualDto> resultado = new ArrayList<>();

        // Últimos 6 meses
        for (int i = 5; i >= 0; i--) {
            YearMonth mes = YearMonth.now().minusMonths(i);
            LocalDate inicio = mes.atDay(1);
            LocalDate fin = mes.atEndOfMonth();

            List<Liquidacion> liquidacionesMes = liquidacionRepository.findBySocioId(socio).stream()
                    .filter(liq -> "pagado".equals(liq.getEstado()))
                    .filter(liq -> liq.getFechaPago() != null)
                    .filter(liq -> {
                        LocalDate fechaPago = liq.getFechaPago().toLocalDate();
                        return !fechaPago.isBefore(inicio) && !fechaPago.isAfter(fin);
                    })
                    .toList();

            BigDecimal ingresoToll = liquidacionesMes.stream()
                    .filter(liq -> "toll".equals(liq.getTipoLiquidacion()))
                    .map(Liquidacion::getValorNetoBob)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal ingresoVenta = liquidacionesMes.stream()
                    .filter(liq -> Arrays.asList("venta_concentrado", "venta_lote_complejo")
                            .contains(liq.getTipoLiquidacion()))
                    .map(Liquidacion::getValorNetoBob)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            String nombreMes = mes.getMonth().getDisplayName(TextStyle.SHORT, new Locale("es", "ES"));
            nombreMes = nombreMes.substring(0, 1).toUpperCase() + nombreMes.substring(1).toLowerCase();

            resultado.add(new IngresoMensualDto(
                    nombreMes,
                    ingresoToll,
                    ingresoVenta,
                    ingresoToll.add(ingresoVenta)
            ));
        }

        return resultado;
    }

    // === INGRESOS POR MINERAL ===
    private List<IngresoPorMineralDto> obtenerIngresosPorMineral(Socio socio) {
        // Obtener liquidaciones pagadas
        List<Liquidacion> liquidacionesPagadas = liquidacionRepository.findBySocioId(socio).stream()
                .filter(liq -> "pagado".equals(liq.getEstado()))
                .toList();

        Map<String, BigDecimal> ingresosPorMineral = new HashMap<>();
        ingresosPorMineral.put("Pb", BigDecimal.ZERO);
        ingresosPorMineral.put("Zn", BigDecimal.ZERO);
        ingresosPorMineral.put("Ag", BigDecimal.ZERO);

        for (Liquidacion liq : liquidacionesPagadas) {
            // Obtener concentrados de la liquidación
            List<LiquidacionConcentrado> liquidacionConcentrados =
                    liquidacionConcentradoRepository.findByLiquidacionId(liq);

            for (LiquidacionConcentrado lc : liquidacionConcentrados) {
                Concentrado conc = lc.getConcentradoId();
                String mineralPrincipal = conc.getMineralPrincipal();

                if (mineralPrincipal != null && ingresosPorMineral.containsKey(mineralPrincipal)) {
                    // Distribuir proporcionalmente el valor neto
                    BigDecimal valorPorConcentrado = liq.getValorNetoBob()
                            .divide(new BigDecimal(liquidacionConcentrados.size()), 2, RoundingMode.HALF_UP);

                    ingresosPorMineral.put(
                            mineralPrincipal,
                            ingresosPorMineral.get(mineralPrincipal).add(valorPorConcentrado)
                    );
                }
            }
        }

        BigDecimal total = ingresosPorMineral.values().stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<IngresoPorMineralDto> resultado = new ArrayList<>();

        for (Map.Entry<String, BigDecimal> entry : ingresosPorMineral.entrySet()) {
            BigDecimal porcentaje = BigDecimal.ZERO;
            if (total.compareTo(BigDecimal.ZERO) > 0) {
                porcentaje = entry.getValue()
                        .divide(total, 4, RoundingMode.HALF_UP)
                        .multiply(new BigDecimal("100"));
            }

            resultado.add(new IngresoPorMineralDto(
                    entry.getKey(),
                    entry.getValue(),
                    porcentaje
            ));
        }

        return resultado.stream()
                .sorted(Comparator.comparing(IngresoPorMineralDto::getIngreso).reversed())
                .collect(Collectors.toList());
    }
}