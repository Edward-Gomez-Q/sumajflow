package ucb.edu.bo.sumajflow.bl.ingenio;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ucb.edu.bo.sumajflow.dto.ingenio.*;
import ucb.edu.bo.sumajflow.entity.*;
import ucb.edu.bo.sumajflow.repository.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.TextStyle;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardIngenioBl {

    private final IngenioMineroRepository ingenioMineroRepository;
    private final ConcentradoRepository concentradoRepository;
    private final LotesRepository lotesRepository;
    private final LoteIngenioRepository loteIngenioRepository;
    private final LiquidacionRepository liquidacionRepository;
    private final LiquidacionConcentradoRepository liquidacionConcentradoRepository;
    private final PlantaRepository plantaRepository;
    private final ProcesosRepository procesosRepository;
    private final LoteProcesoPlantaRepository loteProcesoPlantaRepository;
    private final SocioRepository socioRepository;
    private final PersonaRepository personaRepository;
    private final UsuariosRepository usuariosRepository;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public DashboardIngenioDto obtenerDashboard(Integer usuarioId) {
        log.debug("Obteniendo dashboard para ingenio - Usuario: {}", usuarioId);

        IngenioMinero ingenio = obtenerIngenio(usuarioId);

        DashboardIngenioDto dashboard = new DashboardIngenioDto();

        // Cargar todos los datos
        dashboard.setOperacionesData(obtenerDatosOperaciones(ingenio));
        dashboard.setKanbanData(obtenerDatosKanban(ingenio));
        dashboard.setFinancieroData(obtenerDatosFinancieros(ingenio));
        dashboard.setLotesPendientesData(obtenerDatosLotesPendientes(ingenio));
        dashboard.setKanbanColumnas(obtenerKanbanColumnas(ingenio));
        dashboard.setProcesosPlanta(obtenerProcesosPlanta(ingenio));
        dashboard.setCapacidadPlanta(obtenerCapacidadPlanta(ingenio));
        dashboard.setTurnoActual(obtenerTurnoActual());
        dashboard.setAlertasOperacionales(generarAlertasOperacionales(ingenio));
        dashboard.setLiquidacionesToll(obtenerLiquidacionesToll(ingenio));
        dashboard.setProduccionDiaria(obtenerProduccionDiaria(ingenio));
        dashboard.setProduccionPorMineral(obtenerProduccionPorMineral(ingenio));
        dashboard.setLotesDisponibles(obtenerLotesDisponibles(ingenio));

        log.info("Dashboard generado exitosamente para ingenio {}", ingenio.getId());
        return dashboard;
    }

    // ==================== MÉTODOS PRIVADOS ====================

    private IngenioMinero obtenerIngenio(Integer usuarioId) {
        Usuarios usuario = usuariosRepository.findById(usuarioId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        return ingenioMineroRepository.findByUsuariosId(usuario)
                .orElseThrow(() -> new IllegalArgumentException("Ingenio no encontrado"));
    }

    // === DATOS DE OPERACIONES ===
    private OperacionesDataDto obtenerDatosOperaciones(IngenioMinero ingenio) {
        // Concentrados en proceso
        long concentradosEnProceso = concentradoRepository
                .findByIngenioMineroIdAndEstado(ingenio, "en_proceso")
                .size();

        // Concentrados completados hoy
        LocalDate hoy = LocalDate.now();
        LocalDateTime inicioHoy = hoy.atStartOfDay();
        LocalDateTime finHoy = hoy.plusDays(1).atStartOfDay();

        long concentradosCompletadosHoy = concentradoRepository
                .findByIngenioMineroId(ingenio).stream()
                .filter(c -> Arrays.asList("listo_para_liquidacion", "esperando_pago", "listo_para_venta")
                        .contains(c.getEstado()))
                .filter(c -> c.getUpdatedAt() != null &&
                        !c.getUpdatedAt().isBefore(inicioHoy) &&
                        c.getUpdatedAt().isBefore(finHoy))
                .count();

        // Peso total procesamiento mes
        LocalDate inicioMes = hoy.withDayOfMonth(1);

        BigDecimal pesoTotalMes = concentradoRepository
                .findByIngenioMineroId(ingenio).stream()
                .filter(c -> c.getCreatedAt() != null &&
                        !c.getCreatedAt().toLocalDate().isBefore(inicioMes))
                .map(Concentrado::getPesoFinal)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(new BigDecimal("1000"), 2, RoundingMode.HALF_UP); // Convertir a toneladas

        // Capacidad utilizada
        BigDecimal capacidadUtilizada = calcularCapacidadUtilizada(ingenio);

        return new OperacionesDataDto(
                (int) concentradosEnProceso,
                (int) concentradosCompletadosHoy,
                pesoTotalMes,
                capacidadUtilizada
        );
    }

    private BigDecimal calcularCapacidadUtilizada(IngenioMinero ingenio) {
        // Obtener planta del ingenio
        Planta planta = plantaRepository.findByIngenioMineroId(ingenio).orElse(null);
        if (planta == null) {
            return BigDecimal.ZERO;
        }

        // Capacidad máxima (ton/día)
        BigDecimal capacidadMaxima = planta.getCapacidadProcesamiento();
        if (capacidadMaxima == null || capacidadMaxima.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }

        // Concentrados en proceso actualmente
        long concentradosActivos = concentradoRepository
                .findByIngenioMineroIdAndEstado(ingenio, "en_proceso")
                .size();

        // Estimación simple: cada concentrado en proceso usa un porcentaje de la capacidad
        BigDecimal utilizacion = new BigDecimal(concentradosActivos * 15); // Estimación: 15% por concentrado

        return utilizacion.min(new BigDecimal("100")).setScale(1, RoundingMode.HALF_UP);
    }

    // === DATOS DE KANBAN ===
    private KanbanDataDto obtenerDatosKanban(IngenioMinero ingenio) {
        List<Concentrado> concentrados = concentradoRepository.findByIngenioMineroId(ingenio);

        long porIniciar = concentrados.stream()
                .filter(c -> "en_camino_a_planta".equals(c.getEstado()))
                .count();

        long enProceso = concentrados.stream()
                .filter(c -> "en_proceso".equals(c.getEstado()))
                .count();

        long esperandoReporte = concentrados.stream()
                .filter(c -> "esperando_pago".equals(c.getEstado()))
                .count();

        long listoLiquidacion = concentrados.stream()
                .filter(c -> "procesado".equals(c.getEstado()))
                .count();

        return new KanbanDataDto(
                (int) porIniciar,
                (int) enProceso,
                (int) esperandoReporte,
                (int) listoLiquidacion
        );
    }

    // === DATOS FINANCIEROS ===
    private FinancieroDataDto obtenerDatosFinancieros(IngenioMinero ingenio) {
        List<Liquidacion> liquidacionesToll = liquidacionRepository
                .findByIngenioMineroId(ingenio).stream()
                .filter(liq -> "toll".equals(liq.getTipoLiquidacion()))
                .toList();

        // Toll pendiente de cobro
        BigDecimal tollPendiente = liquidacionesToll.stream()
                .filter(liq -> "esperando_pago".equals(liq.getEstado()))
                .map(Liquidacion::getValorNetoBob)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Toll cobrado este mes
        LocalDate hoy = LocalDate.now();
        LocalDate inicioMes = hoy.withDayOfMonth(1);
        LocalDate finMes = hoy.withDayOfMonth(hoy.lengthOfMonth());

        BigDecimal tollCobradoMes = liquidacionesToll.stream()
                .filter(liq -> "pagado".equals(liq.getEstado()))
                .filter(liq -> liq.getFechaPago() != null &&
                        !liq.getFechaPago().toLocalDate().isBefore(inicioMes) &&
                        !liq.getFechaPago().toLocalDate().isAfter(finMes))
                .map(Liquidacion::getValorNetoBob)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Ingreso proyectado mes (simplificado: cobrado + pendiente)
        BigDecimal ingresoProyectado = tollCobradoMes.add(tollPendiente);

        return new FinancieroDataDto(
                tollPendiente,
                tollCobradoMes,
                ingresoProyectado
        );
    }

    // === DATOS DE LOTES PENDIENTES ===
    private LotesPendientesDataDto obtenerDatosLotesPendientes(IngenioMinero ingenio) {
        List<LoteIngenio> lotes = loteIngenioRepository.findByIngenioMineroId(ingenio);

        long pendienteAprobacion = lotes.stream()
                .map(LoteIngenio::getLotesId)
                .filter(lote -> "Pendiente de aprobación por Ingenio/Comercializadora".equals(lote.getEstado()))
                .count();

        long transporteCompleto = lotes.stream()
                .map(LoteIngenio::getLotesId)
                .filter(lote -> "Transporte completo".equals(lote.getEstado()))
                .count();

        return new LotesPendientesDataDto(
                (int) pendienteAprobacion,
                (int) transporteCompleto
        );
    }

    // === KANBAN COLUMNAS ===
    private List<KanbanColumnaDto> obtenerKanbanColumnas(IngenioMinero ingenio) {
        List<Concentrado> concentrados = concentradoRepository.findByIngenioMineroId(ingenio);

        List<KanbanColumnaDto> columnas = new ArrayList<>();

        // Columna: Por Iniciar
        columnas.add(crearColumnaKanban(
                "por_iniciar",
                "Por Iniciar",
                "en_camino_a_planta",
                "bg-slate-500",
                concentrados
        ));

        // Columna: En Proceso
        columnas.add(crearColumnaKanban(
                "en_proceso",
                "En Proceso",
                "en_proceso",
                "bg-blue-500",
                concentrados
        ));

        // Columna: Esperando Reporte
        columnas.add(crearColumnaKanban(
                "esperando_pago",
                "Esperando Pago",
                "esperando_pago",
                "bg-yellow-500",
                concentrados
        ));

        // Columna: Listo Liquidación
        columnas.add(crearColumnaKanban(
                "procesado",
                "Procesado",
                "listo_para_venta",
                "bg-green-500",
                concentrados
        ));

        return columnas;
    }

    private KanbanColumnaDto crearColumnaKanban(
            String id,
            String titulo,
            String estado,
            String color,
            List<Concentrado> todosConcentrados
    ) {
        List<ConcentradoKanbanDto> concentrados = todosConcentrados.stream()
                .filter(c -> estado.equals(c.getEstado()))
                .limit(2) // Máximo 2 concentrados por columna
                .map(this::mapearConcentradoKanban)
                .collect(Collectors.toList());

        return new KanbanColumnaDto(id, titulo, estado, color, concentrados);
    }

    private ConcentradoKanbanDto mapearConcentradoKanban(Concentrado concentrado) {
        Socio socio = concentrado.getSocioPropietarioId();
        Persona persona = personaRepository.findByUsuariosId(socio.getUsuariosId()).orElse(null);

        String nombreCompleto = persona != null
                ? persona.getNombres() + " " + persona.getPrimerApellido()
                : "Socio";

        String codigo = concentrado.getCodigoConcentrado() != null
                ? concentrado.getCodigoConcentrado()
                : "CON-" + concentrado.getId();

        // Progreso
        ProgresoConcentradoDto progreso = calcularProgresoConcentrado(concentrado);

        // Alertas
        AlertaConcentradoDto alertas = detectarAlertas(concentrado);

        return new ConcentradoKanbanDto(
                concentrado.getId(),
                codigo,
                concentrado.getMineralPrincipal(),
                concentrado.getPesoInicial(),
                nombreCompleto,
                concentrado.getCreatedAt(),
                progreso,
                alertas
        );
    }

    private ProgresoConcentradoDto calcularProgresoConcentrado(Concentrado concentrado) {
        String estado = concentrado.getEstado();

        // Etapa actual y porcentaje basado en el estado
        String etapaActual;
        Integer porcentaje = switch (estado) {
            case "en_camino_a_planta" -> {
                etapaActual = "En camino";
                yield 0;
            }
            case "en_proceso" -> {
                etapaActual = obtenerProcesoActual(concentrado);
                yield calcularPorcentajeProceso(concentrado);
            }
            case "esperando_reporte_quimico" -> {
                etapaActual = "Esperando análisis";
                yield 90;
            }
            case "listo_para_liquidacion" -> {
                etapaActual = "Completado";
                yield 100;
            }
            default -> {
                etapaActual = estado;
                yield 50;
            }
        };

        // Contar etapas completadas
        int etapasCompletadas = contarEtapasCompletadas(concentrado);
        int etapasTotal = obtenerTotalEtapas(concentrado);

        return new ProgresoConcentradoDto(
                etapaActual,
                porcentaje,
                etapasCompletadas,
                etapasTotal
        );
    }

    private String obtenerProcesoActual(Concentrado concentrado) {
        // Obtener el último proceso activo
        List<LoteProcesoPlanta> procesos = loteProcesoPlantaRepository
                .findByConcentradoId(concentrado);

        Optional<LoteProcesoPlanta> procesoActivo = procesos.stream()
                .filter(p -> "en_proceso".equals(p.getEstado()))
                .findFirst();

        if (procesoActivo.isPresent()) {
            Procesos proceso = procesoActivo.get().getProcesoId();
            return proceso.getNombre();
        }

        return "En proceso";
    }

    private Integer calcularPorcentajeProceso(Concentrado concentrado) {
        int completadas = contarEtapasCompletadas(concentrado);
        int total = obtenerTotalEtapas(concentrado);

        if (total == 0) return 50;

        return (int) ((completadas / (double) total) * 100);
    }

    private int contarEtapasCompletadas(Concentrado concentrado) {
        List<LoteProcesoPlanta> procesos = loteProcesoPlantaRepository
                .findByConcentradoId(concentrado);

        return (int) procesos.stream()
                .filter(p -> "completado".equals(p.getEstado()))
                .count();
    }

    private int obtenerTotalEtapas(Concentrado concentrado) {
        return loteProcesoPlantaRepository.findByConcentradoId(concentrado).size();
    }

    private AlertaConcentradoDto detectarAlertas(Concentrado concentrado) {
        // Detectar si hay retrasos (simplificado: > 7 días en proceso)
        if ("en_proceso".equals(concentrado.getEstado())) {
            long diasEnProceso = concentrado.getCreatedAt() != null
                    ? ChronoUnit.DAYS.between(concentrado.getCreatedAt().toLocalDate(), LocalDate.now())
                    : 0;

            if (diasEnProceso > 7) {
                return new AlertaConcentradoDto(
                        "retraso",
                        "Procesamiento retrasado (+" + diasEnProceso + " días)"
                );
            }
        }

        return new AlertaConcentradoDto("normal", null);
    }

    // === PROCESOS DE PLANTA ===
    private List<ProcesoPlantaDashboardDto> obtenerProcesosPlanta(IngenioMinero ingenio) {
        Planta planta = plantaRepository.findByIngenioMineroId(ingenio).orElse(null);
        if (planta == null) {
            return Collections.emptyList();
        }

        List<Procesos> procesosPlanta = procesosRepository.findAll();

        return procesosPlanta.stream()
                .map(proceso -> mapearProcesoPlanta(proceso, planta))
                .collect(Collectors.toList());
    }

    private ProcesoPlantaDashboardDto mapearProcesoPlanta(Procesos proceso, Planta planta) {
        // Datos simplificados (en producción obtendrían de sensores/mediciones)
        BigDecimal capacidadMaxima = new BigDecimal("50"); // ton/h
        BigDecimal utilizado = new BigDecimal(30 + Math.random() * 15).setScale(0, RoundingMode.HALF_UP);
        BigDecimal eficiencia = new BigDecimal(85 + Math.random() * 10).setScale(0, RoundingMode.HALF_UP);

        // Concentrados en esta etapa
        int concentradosEnEtapa = (int) loteProcesoPlantaRepository
                .findByProcesoIdAndEstado(proceso, "en_proceso")
                .size();

        BigDecimal tiempoPromedio = new BigDecimal(3 + Math.random() * 5).setScale(1, RoundingMode.HALF_UP);

        LocalDate ultimoMantenimiento = LocalDate.now().minusDays(15);
        LocalDate proximoMantenimiento = LocalDate.now().plusDays(45);

        return new ProcesoPlantaDashboardDto(
                proceso.getNombre(),
                capacidadMaxima,
                utilizado,
                eficiencia,
                concentradosEnEtapa,
                tiempoPromedio,
                ultimoMantenimiento,
                proximoMantenimiento
        );
    }

    // === CAPACIDAD PLANTA ===
    private CapacidadPlantaDto obtenerCapacidadPlanta(IngenioMinero ingenio) {
        Planta planta = plantaRepository.findByIngenioMineroId(ingenio).orElse(null);
        if (planta == null) {
            return new CapacidadPlantaDto(
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    BigDecimal.ZERO
            );
        }

        BigDecimal capacidadMaxima = planta.getCapacidadProcesamiento();
        if (capacidadMaxima == null) {
            capacidadMaxima = new BigDecimal("180");
        }

        // Procesamiento actual (estimado)
        long concentradosEnProceso = concentradoRepository
                .findByIngenioMineroIdAndEstado(ingenio, "en_proceso")
                .size();

        BigDecimal procesamientoActual = new BigDecimal(concentradosEnProceso * 25); // 25 ton por concentrado

        BigDecimal utilizacion = BigDecimal.ZERO;
        if (capacidadMaxima.compareTo(BigDecimal.ZERO) > 0) {
            utilizacion = procesamientoActual
                    .divide(capacidadMaxima, 4, RoundingMode.HALF_UP)
                    .multiply(new BigDecimal("100"))
                    .setScale(1, RoundingMode.HALF_UP);
        }

        BigDecimal proyeccionDia = procesamientoActual.multiply(new BigDecimal("1.2"));

        return new CapacidadPlantaDto(
                capacidadMaxima,
                procesamientoActual,
                utilizacion,
                proyeccionDia
        );
    }

    // === TURNO ACTUAL ===
    private TurnoActualDto obtenerTurnoActual() {
        LocalTime ahora = LocalTime.now();

        String turno;
        LocalDateTime horaInicio;
        LocalDateTime horaFin;

        if (ahora.isBefore(LocalTime.of(14, 0))) {
            turno = "mañana";
            horaInicio = LocalDate.now().atTime(6, 0);
            horaFin = LocalDate.now().atTime(14, 0);
        } else if (ahora.isBefore(LocalTime.of(22, 0))) {
            turno = "tarde";
            horaInicio = LocalDate.now().atTime(14, 0);
            horaFin = LocalDate.now().atTime(22, 0);
        } else {
            turno = "noche";
            horaInicio = LocalDate.now().atTime(22, 0);
            horaFin = LocalDate.now().plusDays(1).atTime(6, 0);
        }

        int operadores = 12; // Fijo por ahora
        int concentradosProcesados = 4; // Simplificado

        return new TurnoActualDto(
                turno,
                horaInicio,
                horaFin,
                operadores,
                concentradosProcesados
        );
    }

    // === ALERTAS OPERACIONALES ===
    private List<AlertaOperacionalDto> generarAlertasOperacionales(IngenioMinero ingenio) {
        List<AlertaOperacionalDto> alertas = new ArrayList<>();

        // Alerta de mantenimiento próximo
        alertas.add(new AlertaOperacionalDto(
                "mantenimiento",
                "media",
                "Flotación requiere mantenimiento en 30 días",
                "Programar mantenimiento"
        ));

        // Alerta de capacidad
        BigDecimal capacidadUtilizada = calcularCapacidadUtilizada(ingenio);
        if (capacidadUtilizada.compareTo(new BigDecimal("80")) > 0) {
            alertas.add(new AlertaOperacionalDto(
                    "capacidad",
                    "alta",
                    "Utilización al " + capacidadUtilizada + "% - cerca del límite",
                    "Considerar planificación"
            ));
        } else {
            alertas.add(new AlertaOperacionalDto(
                    "capacidad",
                    "baja",
                    "Utilización al " + capacidadUtilizada + "% - dentro de rango óptimo",
                    "Mantener monitoreo"
            ));
        }

        return alertas;
    }

    // === LIQUIDACIONES TOLL ===
    private LiquidacionesTollDto obtenerLiquidacionesToll(IngenioMinero ingenio) {
        List<Liquidacion> liquidacionesToll = liquidacionRepository
                .findByIngenioMineroId(ingenio).stream()
                .filter(liq -> "toll".equals(liq.getTipoLiquidacion()))
                .collect(Collectors.toList());

        // Pendientes
        List<LiquidacionTollPendienteDto> pendientes = liquidacionesToll.stream()
                .filter(liq -> "esperando_pago".equals(liq.getEstado()))
                .map(this::mapearLiquidacionTollPendiente)
                .limit(5)
                .collect(Collectors.toList());

        // Pagadas recientes
        List<LiquidacionTollPagadaDto> pagadasRecientes = liquidacionesToll.stream()
                .filter(liq -> "pagado".equals(liq.getEstado()))
                .sorted(Comparator.comparing(Liquidacion::getFechaPago).reversed())
                .limit(3)
                .map(this::mapearLiquidacionTollPagada)
                .collect(Collectors.toList());

        // Estadísticas
        EstadisticasTollDto estadisticas = calcularEstadisticasToll(liquidacionesToll);

        return new LiquidacionesTollDto(pendientes, pagadasRecientes, estadisticas);
    }

    private LiquidacionTollPendienteDto mapearLiquidacionTollPendiente(Liquidacion liquidacion) {
        Socio socio = liquidacion.getSocioId();
        Persona persona = personaRepository.findByUsuariosId(socio.getUsuariosId()).orElse(null);

        String nombreCompleto = persona != null
                ? persona.getNombres() + " " + persona.getPrimerApellido() + " " +
                (persona.getSegundoApellido() != null ? persona.getSegundoApellido() : "")
                : "Socio";

        // Lotes asociados (simplificado)
        List<Integer> lotes = List.of(liquidacion.getId());

        // Peso total (de concentrados)
        BigDecimal pesoTotal = liquidacionConcentradoRepository
                .findByLiquidacionId(liquidacion).stream()
                .map(lc -> lc.getConcentradoId().getPesoFinal())
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Costos (simplificado)
        BigDecimal valorNeto = liquidacion.getValorNetoBob();
        BigDecimal costoProcesamiento = valorNeto.multiply(new BigDecimal("0.8"));
        BigDecimal serviciosAdicionales = valorNeto.multiply(new BigDecimal("0.2"));

        long diasPendiente = liquidacion.getCreatedAt() != null
                ? ChronoUnit.DAYS.between(liquidacion.getCreatedAt().toLocalDate(), LocalDate.now())
                : 0;

        return new LiquidacionTollPendienteDto(
                liquidacion.getId(),
                nombreCompleto,
                lotes,
                pesoTotal,
                costoProcesamiento,
                serviciosAdicionales,
                valorNeto,
                liquidacion.getEstado(),
                diasPendiente
        );
    }

    private LiquidacionTollPagadaDto mapearLiquidacionTollPagada(Liquidacion liquidacion) {
        Socio socio = liquidacion.getSocioId();
        Persona persona = personaRepository.findByUsuariosId(socio.getUsuariosId()).orElse(null);

        String nombreCompleto = persona != null
                ? persona.getNombres() + " " + persona.getPrimerApellido()
                : "Socio";

        return new LiquidacionTollPagadaDto(
                liquidacion.getId(),
                nombreCompleto,
                liquidacion.getValorNetoBob(),
                liquidacion.getFechaPago(),
                "Transferencia" // Simplificado
        );
    }

    private EstadisticasTollDto calcularEstadisticasToll(List<Liquidacion> liquidaciones) {
        BigDecimal totalPendiente = liquidaciones.stream()
                .filter(liq -> "esperando_pago".equals(liq.getEstado()))
                .map(Liquidacion::getValorNetoBob)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Promedio tiempo cobranza
        List<Liquidacion> pagadas = liquidaciones.stream()
                .filter(liq -> "pagado".equals(liq.getEstado()))
                .filter(liq -> liq.getFechaPago() != null && liq.getCreatedAt() != null)
                .toList();

        BigDecimal promedioTiempo = BigDecimal.ZERO;
        if (!pagadas.isEmpty()) {
            long totalDias = pagadas.stream()
                    .mapToLong(liq -> ChronoUnit.DAYS.between(
                            liq.getCreatedAt().toLocalDate(),
                            liq.getFechaPago().toLocalDate()
                    ))
                    .sum();
            promedioTiempo = new BigDecimal(totalDias)
                    .divide(new BigDecimal(pagadas.size()), 2, RoundingMode.HALF_UP);
        }

        // Tasa de cobranza
        long totalLiquidaciones = liquidaciones.size();
        long liquidacionesPagadas = pagadas.size();
        BigDecimal tasaCobranza = BigDecimal.ZERO;
        if (totalLiquidaciones > 0) {
            tasaCobranza = new BigDecimal(liquidacionesPagadas)
                    .divide(new BigDecimal(totalLiquidaciones), 4, RoundingMode.HALF_UP)
                    .multiply(new BigDecimal("100"))
                    .setScale(1, RoundingMode.HALF_UP);
        }

        // Ingresos últimos 30 días
        LocalDateTime hace30Dias = LocalDateTime.now().minusDays(30);
        BigDecimal ingresosRecientes = liquidaciones.stream()
                .filter(liq -> "pagado".equals(liq.getEstado()))
                .filter(liq -> liq.getFechaPago() != null && liq.getFechaPago().isAfter(hace30Dias))
                .map(Liquidacion::getValorNetoBob)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new EstadisticasTollDto(
                totalPendiente,
                promedioTiempo,
                tasaCobranza,
                ingresosRecientes
        );
    }

    // === PRODUCCIÓN DIARIA ===
    private List<ProduccionDiariaDto> obtenerProduccionDiaria(IngenioMinero ingenio) {
        List<ProduccionDiariaDto> resultado = new ArrayList<>();

        // Últimos 6 días
        for (int i = 5; i >= 0; i--) {
            LocalDate dia = LocalDate.now().minusDays(i);
            LocalDateTime inicioDia = dia.atStartOfDay();
            LocalDateTime finDia = dia.plusDays(1).atStartOfDay();

            List<Concentrado> concentradosDia = concentradoRepository
                    .findByIngenioMineroId(ingenio).stream()
                    .filter(c -> c.getCreatedAt() != null &&
                            !c.getCreatedAt().isBefore(inicioDia) &&
                            c.getCreatedAt().isBefore(finDia))
                    .toList();

            int concentradosCreados = concentradosDia.size();

            int concentradosFinalizados = (int) concentradoRepository
                    .findByIngenioMineroId(ingenio).stream()
                    .filter(c -> c.getUpdatedAt() != null &&
                            !c.getUpdatedAt().isBefore(inicioDia) &&
                            c.getUpdatedAt().isBefore(finDia))
                    .filter(c -> Arrays.asList("listo_para_liquidacion", "esperando_pago", "listo_para_venta")
                            .contains(c.getEstado()))
                    .count();

            BigDecimal pesoTotal = concentradosDia.stream()
                    .map(Concentrado::getPesoFinal)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                    .divide(new BigDecimal("1000"), 2, RoundingMode.HALF_UP);

            String fechaStr = dia.getDayOfMonth() + " " +
                    dia.getMonth().getDisplayName(TextStyle.SHORT, new Locale("es", "ES"));

            resultado.add(new ProduccionDiariaDto(
                    fechaStr,
                    concentradosCreados,
                    concentradosFinalizados,
                    pesoTotal
            ));
        }

        return resultado;
    }

    // === PRODUCCIÓN POR MINERAL ===
    private List<ProduccionPorMineralDto> obtenerProduccionPorMineral(IngenioMinero ingenio) {
        List<Concentrado> concentrados = concentradoRepository.findByIngenioMineroId(ingenio);

        Map<String, List<Concentrado>> porMineral = concentrados.stream()
                .collect(Collectors.groupingBy(Concentrado::getMineralPrincipal));

        int totalConcentrados = concentrados.size();

        List<ProduccionPorMineralDto> resultado = new ArrayList<>();

        for (Map.Entry<String, List<Concentrado>> entry : porMineral.entrySet()) {
            String mineral = entry.getKey();
            List<Concentrado> concentradosMineral = entry.getValue();

            int cantidad = concentradosMineral.size();

            BigDecimal pesoTotal = concentradosMineral.stream()
                    .map(Concentrado::getPesoFinal)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            int porcentaje = totalConcentrados > 0
                    ? (int) ((cantidad / (double) totalConcentrados) * 100)
                    : 0;

            resultado.add(new ProduccionPorMineralDto(
                    mineral,
                    cantidad,
                    pesoTotal,
                    porcentaje
            ));
        }

        return resultado.stream()
                .sorted(Comparator.comparing(ProduccionPorMineralDto::getCantidad).reversed())
                .collect(Collectors.toList());
    }

    // === LOTES DISPONIBLES ===
    private List<LoteDisponibleDto> obtenerLotesDisponibles(IngenioMinero ingenio) {
        // Obtener lotes en "Transporte completo"
        List<LoteIngenio> lotesIngenio = loteIngenioRepository.findByIngenioMineroId(ingenio);

        return lotesIngenio.stream()
                .map(LoteIngenio::getLotesId)
                .filter(lote -> "Transporte completo".equals(lote.getEstado()))
                .map(this::mapearLoteDisponible)
                .sorted(Comparator.comparing(LoteDisponibleDto::getDiasEspera).reversed())
                .limit(5)
                .collect(Collectors.toList());
    }

    private LoteDisponibleDto mapearLoteDisponible(Lotes lote) {
        Socio socio = lote.getMinasId().getSocioId();
        Persona persona = personaRepository.findByUsuariosId(socio.getUsuariosId()).orElse(null);

        String nombreCompleto = persona != null
                ? persona.getNombres() + " " + persona.getPrimerApellido()
                : "Socio";

        String codigo = String.format("LT-%d-%03d",
                lote.getFechaCreacion().getYear(),
                lote.getId());

        // Minerales (simplificado)
        List<String> minerales = List.of("Zn", "Ag"); // En producción obtener de lote_minerales

        LocalDateTime fechaLlegada = lote.getUpdatedAt();
        long diasEspera = fechaLlegada != null
                ? ChronoUnit.DAYS.between(fechaLlegada.toLocalDate(), LocalDate.now())
                : 0;

        // Prioridad
        String prioridad = "baja";
        String motivoPrioridad = "Lote reciente";

        if (diasEspera > 2) {
            prioridad = "alta";
            motivoPrioridad = "Más de 48 horas esperando";
        } else if (lote.getPesoTotalReal() != null && lote.getPesoTotalReal().compareTo(new BigDecimal("5000")) > 0) {
            prioridad = "media";
            motivoPrioridad = "Peso estándar";
        }

        // Estimaciones
        int concentradosEstimados = minerales.size();
        BigDecimal tiempoEstimado = new BigDecimal(concentradosEstimados * 8); // 8h por concentrado

        return new LoteDisponibleDto(
                lote.getId(),
                codigo,
                nombreCompleto,
                minerales.getFirst(), // Tipo mineral principal
                lote.getPesoTotalReal(),
                minerales,
                fechaLlegada,
                diasEspera,
                prioridad,
                motivoPrioridad,
                concentradosEstimados,
                tiempoEstimado
        );
    }
}