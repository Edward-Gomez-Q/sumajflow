package ucb.edu.bo.sumajflow.bl.cooperativa;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ucb.edu.bo.sumajflow.dto.cooperativa.*;
import ucb.edu.bo.sumajflow.entity.*;
import ucb.edu.bo.sumajflow.repository.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.TextStyle;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardCooperativaBl {

    private final CooperativaRepository cooperativaRepository;
    private final CooperativaSocioRepository cooperativaSocioRepository;
    private final SocioRepository socioRepository;
    private final MinasRepository minasRepository;
    private final SectoresRepository sectoresRepository;
    private final LotesRepository lotesRepository;
    private final AsignacionCamionRepository asignacionCamionRepository;
    private final TransportistaRepository transportistaRepository;
    private final InvitacionCooperativaRepository invitacionCooperativaRepository;
    private final BalanzaCooperativaRepository balanzaCooperativaRepository;
    private final AuditoriaLotesRepository auditoriaLotesRepository;
    private final PersonaRepository personaRepository;
    private final UsuariosRepository usuariosRepository;
    private final ObjectMapper objectMapper;

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

    @Transactional(readOnly = true)
    public DashboardCooperativaDto obtenerDashboard(Integer usuarioId) {
        log.debug("Obteniendo dashboard para cooperativa - Usuario: {}", usuarioId);

        // Obtener cooperativa
        Cooperativa cooperativa = obtenerCooperativa(usuarioId);

        DashboardCooperativaDto dashboard = new DashboardCooperativaDto();

        // Cargar todos los datos
        dashboard.setSociosData(obtenerDatosSocios(cooperativa));
        dashboard.setLotesData(obtenerDatosLotes(cooperativa));
        dashboard.setTransportistasData(obtenerDatosTransportistas(cooperativa));
        dashboard.setVolumetriaData(obtenerDatosVolumetria(cooperativa));
        dashboard.setLotesPendientes(obtenerLotesPendientes(cooperativa));
        dashboard.setTransportistasEnRuta(obtenerTransportistasEnRuta(cooperativa));
        dashboard.setBalanzasMonitor(obtenerMonitorBalanzas(cooperativa));
        dashboard.setAprobacionesPorDia(obtenerAprobacionesPorDia(cooperativa));
        dashboard.setMinasPorSector(obtenerMinasPorSector(cooperativa));

        log.info("Dashboard generado exitosamente para cooperativa {}", cooperativa.getId());
        return dashboard;
    }

    // ==================== MÉTODOS PRIVADOS ====================

    private Cooperativa obtenerCooperativa(Integer usuarioId) {
        Usuarios usuario = usuariosRepository.findById(usuarioId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        return cooperativaRepository.findByUsuariosId(usuario)
                .orElseThrow(() -> new IllegalArgumentException("Cooperativa no encontrada"));
    }

    // === DATOS DE SOCIOS ===
    private SociosDataDto obtenerDatosSocios(Cooperativa cooperativa) {
        // Total de socios
        List<CooperativaSocio> todasRelaciones = cooperativaSocioRepository
                .findByCooperativaId(cooperativa);

        int totalSocios = todasRelaciones.size();

        // Socios activos (aprobados)
        long sociosActivos = todasRelaciones.stream()
                .filter(cs -> "aprobado".equalsIgnoreCase(cs.getEstado()))
                .count();

        // Nuevos este mes
        LocalDate inicioMes = LocalDate.now().withDayOfMonth(1);
        long nuevosEsteMes = todasRelaciones.stream()
                .filter(cs -> "aprobado".equalsIgnoreCase(cs.getEstado()))
                .filter(cs -> cs.getFechaAfiliacion() != null &&
                        !cs.getFechaAfiliacion().isBefore(inicioMes))
                .count();

        // Minas registradas
        List<Sectores> sectores = sectoresRepository.findByCooperativaId(cooperativa);
        List<Minas> todasMinas = new ArrayList<>();
        for (Sectores sector : sectores) {
            todasMinas.addAll(minasRepository.findBySectoresId(sector));
        }

        // Minas por sector
        List<MinasPorSectorDto> minasPorSector = obtenerMinasPorSector(cooperativa);

        return new SociosDataDto(
                totalSocios,
                (int) sociosActivos,
                (int) nuevosEsteMes,
                todasMinas.size(),
                minasPorSector
        );
    }

    // === DATOS DE LOTES ===
    private LotesDataDto obtenerDatosLotes(Cooperativa cooperativa) {
        // Obtener todos los lotes de los socios de esta cooperativa
        List<CooperativaSocio> sociosAprobados = cooperativaSocioRepository
                .findByCooperativaIdAndEstado(cooperativa, "aprobado");

        List<Lotes> todosLotes = new ArrayList<>();
        for (CooperativaSocio cs : sociosAprobados) {
            todosLotes.addAll(lotesRepository.findByMinasSocioId(cs.getSocioId()));
        }

        // Pendientes de aprobación
        long pendientes = todosLotes.stream()
                .filter(lote -> "Pendiente de aprobación cooperativa".equals(lote.getEstado()))
                .count();

        // Hoy
        LocalDate hoy = LocalDate.now();
        LocalDateTime inicioHoy = hoy.atStartOfDay();
        LocalDateTime finHoy = hoy.plusDays(1).atStartOfDay();

        // Aprobados hoy (auditoria)
        long aprobadosHoy = auditoriaLotesRepository.findByFechaRegistroBetween(inicioHoy, finHoy).stream()
                .filter(a -> "lote_aprobado_cooperativa".equals(a.getAccion()))
                .count();

        // Rechazados hoy
        long rechazadosHoy = auditoriaLotesRepository.findByFechaRegistroBetween(inicioHoy, finHoy).stream()
                .filter(a -> "lote_rechazado_cooperativa".equals(a.getAccion()))
                .count();

        // Tasa de aprobación del mes
        LocalDate inicioMes = hoy.withDayOfMonth(1);
        LocalDateTime inicioMesDateTime = inicioMes.atStartOfDay();

        List<AuditoriaLotes> auditoriasMes = auditoriaLotesRepository
                .findByFechaRegistroAfter(inicioMesDateTime);

        long aprobadosMes = auditoriasMes.stream()
                .filter(a -> "lote_aprobado_cooperativa".equals(a.getAccion()))
                .count();

        long rechazadosMes = auditoriasMes.stream()
                .filter(a -> "lote_rechazado_cooperativa".equals(a.getAccion()))
                .count();

        BigDecimal tasaAprobacion = BigDecimal.ZERO;
        long totalRevisados = aprobadosMes + rechazadosMes;
        if (totalRevisados > 0) {
            tasaAprobacion = BigDecimal.valueOf(aprobadosMes)
                    .divide(BigDecimal.valueOf(totalRevisados), 4, RoundingMode.HALF_UP)
                    .multiply(new BigDecimal("100"));
        }

        // Tiempo promedio de aprobación (últimos 30 días)
        LocalDateTime hace30Dias = LocalDateTime.now().minusDays(30);
        List<AuditoriaLotes> aprobacionesRecientes = auditoriaLotesRepository
                .findByFechaRegistroAfter(hace30Dias).stream()
                .filter(a -> "lote_aprobado_cooperativa".equals(a.getAccion()))
                .toList();

        BigDecimal tiempoPromedio = BigDecimal.ZERO;
        if (!aprobacionesRecientes.isEmpty()) {
            long totalHoras = 0;
            int count = 0;

            for (AuditoriaLotes auditoria : aprobacionesRecientes) {
                Lotes lote = lotesRepository.findById(auditoria.getLoteId().getId()).orElse(null);
                if (lote != null && lote.getFechaCreacion() != null && auditoria.getFechaRegistro() != null) {
                    long horas = ChronoUnit.HOURS.between(
                            lote.getFechaCreacion(),
                            auditoria.getFechaRegistro()
                    );
                    totalHoras += horas;
                    count++;
                }
            }

            if (count > 0) {
                tiempoPromedio = BigDecimal.valueOf(totalHoras)
                        .divide(BigDecimal.valueOf(count), 2, RoundingMode.HALF_UP);
            }
        }

        return new LotesDataDto(
                (int) pendientes,
                (int) aprobadosHoy,
                (int) rechazadosHoy,
                tasaAprobacion,
                tiempoPromedio
        );
    }

    // === DATOS DE TRANSPORTISTAS ===
    private TransportistasDataDto obtenerDatosTransportistas(Cooperativa cooperativa) {
        // Obtener invitaciones de esta cooperativa
        List<InvitacionCooperativa> invitaciones = invitacionCooperativaRepository
                .findByCooperativa(cooperativa);

        List<Transportista> transportistas = new ArrayList<>();
        for (InvitacionCooperativa ic : invitaciones) {
            transportistaRepository.findByInvitacionTransportista(ic.getInvitacionTransportista())
                    .ifPresent(transportistas::add);
        }

        // Total disponibles (aprobados)
        long totalDisponibles = transportistas.stream()
                .filter(t -> "aprobado".equalsIgnoreCase(t.getEstado()))
                .count();

        // En ruta (asignaciones activas)
        List<AsignacionCamion> asignacionesActivas = asignacionCamionRepository.findByEstadoIn(
                Arrays.asList("En camino a la mina", "Esperando carguío",
                        "En camino balanza cooperativa", "En camino balanza destino",
                        "En camino almacén destino", "Descargando")
        );

        // Filtrar solo asignaciones de lotes de esta cooperativa
        long enRuta = asignacionesActivas.stream()
                .filter(asig -> {
                    Lotes lote = asig.getLotesId();
                    Socio socio = lote.getMinasId().getSocioId();
                    return cooperativaSocioRepository
                            .findByCooperativaIdAndSocioId(cooperativa, socio)
                            .isPresent();
                })
                .count();

        // Completados hoy
        LocalDate hoy = LocalDate.now();
        LocalDateTime inicioHoy = hoy.atStartOfDay();
        LocalDateTime finHoy = hoy.plusDays(1).atStartOfDay();

        long completadosHoy = asignacionCamionRepository
                .findByEstadoAndFechaFinBetween("Completado", inicioHoy, finHoy)
                .stream()
                .filter(asig -> {
                    Lotes lote = asig.getLotesId();
                    Socio socio = lote.getMinasId().getSocioId();
                    return cooperativaSocioRepository
                            .findByCooperativaIdAndSocioId(cooperativa, socio)
                            .isPresent();
                })
                .count();

        // Calificación promedio
        BigDecimal calificacionPromedio = transportistas.stream()
                .map(Transportista::getCalificacionPromedio)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(Math.max(transportistas.size(), 1)), 2, RoundingMode.HALF_UP);

        // Viajes completados este mes
        LocalDate inicioMes = hoy.withDayOfMonth(1);
        LocalDateTime inicioMesDateTime = inicioMes.atStartOfDay();

        long viajesCompletadosMes = asignacionCamionRepository
                .findByEstadoAndFechaFinAfter("Completado", inicioMesDateTime)
                .stream()
                .filter(asig -> {
                    Lotes lote = asig.getLotesId();
                    Socio socio = lote.getMinasId().getSocioId();
                    return cooperativaSocioRepository
                            .findByCooperativaIdAndSocioId(cooperativa, socio)
                            .isPresent();
                })
                .count();

        return new TransportistasDataDto(
                (int) totalDisponibles,
                (int) enRuta,
                (int) completadosHoy,
                calificacionPromedio,
                (int) viajesCompletadosMes
        );
    }

    // === DATOS DE VOLUMETRÍA ===
    private VolumetriaDataDto obtenerDatosVolumetria(Cooperativa cooperativa) {
        LocalDate hoy = LocalDate.now();
        LocalDate inicioMes = hoy.withDayOfMonth(1);
        LocalDate finMes = hoy.withDayOfMonth(hoy.lengthOfMonth());
        LocalDateTime inicioMesDateTime = inicioMes.atStartOfDay();
        LocalDateTime finMesDateTime = finMes.plusDays(1).atStartOfDay();

        // Mes anterior
        LocalDate inicioMesAnterior = inicioMes.minusMonths(1);
        LocalDate finMesAnterior = inicioMesAnterior.withDayOfMonth(inicioMesAnterior.lengthOfMonth());
        LocalDateTime inicioMesAnteriorDateTime = inicioMesAnterior.atStartOfDay();
        LocalDateTime finMesAnteriorDateTime = finMesAnterior.plusDays(1).atStartOfDay();

        // Obtener asignaciones completadas este mes
        List<AsignacionCamion> asignacionesMes = asignacionCamionRepository
                .findByEstadoAndFechaFinBetween("Completado", inicioMesDateTime, finMesDateTime)
                .stream()
                .filter(asig -> {
                    Lotes lote = asig.getLotesId();
                    Socio socio = lote.getMinasId().getSocioId();
                    return cooperativaSocioRepository
                            .findByCooperativaIdAndSocioId(cooperativa, socio)
                            .isPresent();
                })
                .toList();

        BigDecimal pesoTotalMes = asignacionesMes.stream()
                .map(this::extraerPesoNetoPesajeOrigen)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        int camionesDespachadosMes = asignacionesMes.size();

        BigDecimal promedioKgPorCamion = BigDecimal.ZERO;
        if (camionesDespachadosMes > 0) {
            promedioKgPorCamion = pesoTotalMes
                    .divide(BigDecimal.valueOf(camionesDespachadosMes), 2, RoundingMode.HALF_UP);
        }

        // Mes anterior
        List<AsignacionCamion> asignacionesMesAnterior = asignacionCamionRepository
                .findByEstadoAndFechaFinBetween("Completado", inicioMesAnteriorDateTime, finMesAnteriorDateTime)
                .stream()
                .filter(asig -> {
                    Lotes lote = asig.getLotesId();
                    Socio socio = lote.getMinasId().getSocioId();
                    return cooperativaSocioRepository
                            .findByCooperativaIdAndSocioId(cooperativa, socio)
                            .isPresent();
                })
                .toList();

        BigDecimal pesoTotalMesAnterior = asignacionesMesAnterior.stream()
                .map(this::extraerPesoNetoPesajeOrigen)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal comparativo = BigDecimal.ZERO;
        if (pesoTotalMesAnterior.compareTo(BigDecimal.ZERO) > 0) {
            comparativo = pesoTotalMes.subtract(pesoTotalMesAnterior)
                    .divide(pesoTotalMesAnterior, 4, RoundingMode.HALF_UP)
                    .multiply(new BigDecimal("100"));
        }

        return new VolumetriaDataDto(
                pesoTotalMes,
                camionesDespachadosMes,
                promedioKgPorCamion,
                comparativo
        );
    }

    // === LOTES PENDIENTES ===
    private List<LotePendienteDashboardDto> obtenerLotesPendientes(Cooperativa cooperativa) {
        // Obtener socios de la cooperativa
        List<CooperativaSocio> sociosAprobados = cooperativaSocioRepository
                .findByCooperativaIdAndEstado(cooperativa, "aprobado");

        List<Lotes> lotesPendientes = new ArrayList<>();
        for (CooperativaSocio cs : sociosAprobados) {
            lotesPendientes.addAll(lotesRepository.findByMinasSocioIdAndEstado(
                    cs.getSocioId(),
                    "Pendiente de aprobación cooperativa"
            ));
        }

        return lotesPendientes.stream()
                .map(this::mapearLotePendiente)
                .sorted(Comparator.comparing((LotePendienteDashboardDto lp) -> {
                    if ("alta".equals(lp.getPrioridad())) return 0;
                    if ("media".equals(lp.getPrioridad())) return 1;
                    return 2;
                }).thenComparing(LotePendienteDashboardDto::getHorasEspera).reversed())
                .collect(Collectors.toList());
    }

    private LotePendienteDashboardDto mapearLotePendiente(Lotes lote) {
        Socio socio = lote.getMinasId().getSocioId();
        Persona persona = personaRepository.findByUsuariosId(socio.getUsuariosId()).orElse(null);

        String nombreCompleto = persona != null
                ? persona.getNombres() + " " + persona.getPrimerApellido() +
                (persona.getSegundoApellido() != null ? " " + persona.getSegundoApellido() : "")
                : "Socio";

        String codigo = String.format("LT-%d-%03d",
                lote.getFechaCreacion().getYear(),
                lote.getId());

        long horasEspera = lote.getFechaCreacion() != null
                ? ChronoUnit.HOURS.between(lote.getFechaCreacion(), LocalDateTime.now())
                : 0;

        // Determinar prioridad
        String prioridad = "baja";
        String razonPrioridad = "Lote reciente";

        if (horasEspera > 24) {
            prioridad = "alta";
            razonPrioridad = "Más de 24 horas esperando";
        } else if (horasEspera > 12) {
            prioridad = "alta";
            razonPrioridad = "Más de 12 horas esperando";
        } else if (lote.getPesoTotalEstimado() != null && lote.getPesoTotalEstimado().compareTo(new BigDecimal("50000")) > 0) {
            prioridad = "alta";
            razonPrioridad = "Alto volumen solicitado";
        } else if (lote.getCamionesSolicitados() >= 5) {
            prioridad = "media";
            razonPrioridad = "Múltiples camiones solicitados";
        }

        // Validaciones
        List<ValidacionDto> validaciones = generarValidaciones(lote, socio);

        return new LotePendienteDashboardDto(
                lote.getId(),
                codigo,
                nombreCompleto,
                lote.getMinasId().getNombre(),
                lote.getTipoOperacion(),
                lote.getCamionesSolicitados(),
                lote.getPesoTotalEstimado(),
                lote.getFechaCreacion(),
                horasEspera,
                prioridad,
                razonPrioridad,
                validaciones
        );
    }

    private List<ValidacionDto> generarValidaciones(Lotes lote, Socio socio) {
        List<ValidacionDto> validaciones = new ArrayList<>();

        // Validación 1: Mina activa
        String estadoMina = lote.getMinasId().getEstado();
        validaciones.add(new ValidacionDto(
                "Mina activa",
                "activo".equalsIgnoreCase(estadoMina) ? "ok" : "error",
                "activo".equalsIgnoreCase(estadoMina) ? null : "Mina inactiva"
        ));

        // Validación 2: Socio al día (simplificado - solo verificar estado)
        validaciones.add(new ValidacionDto(
                "Socio al día",
                "ok",
                null
        ));

        // Validación 3: Documentación completa
        validaciones.add(new ValidacionDto(
                "Documentación completa",
                "ok",
                null
        ));

        return validaciones;
    }

    // === TRANSPORTISTAS EN RUTA ===
    private List<TransportistaEnRutaDto> obtenerTransportistasEnRuta(Cooperativa cooperativa) {
        List<AsignacionCamion> asignacionesActivas = asignacionCamionRepository.findByEstadoIn(
                Arrays.asList("En camino a la mina", "Esperando carguío",
                        "En camino balanza cooperativa", "En camino balanza destino",
                        "En camino almacén destino", "Descargando")
        );

        return asignacionesActivas.stream()
                .filter(asig -> {
                    Lotes lote = asig.getLotesId();
                    Socio socio = lote.getMinasId().getSocioId();
                    return cooperativaSocioRepository
                            .findByCooperativaIdAndSocioId(cooperativa, socio)
                            .isPresent();
                })
                .map(this::mapearTransportistaEnRuta)
                .limit(10) // Máximo 10
                .collect(Collectors.toList());
    }

    private TransportistaEnRutaDto mapearTransportistaEnRuta(AsignacionCamion asignacion) {
        Transportista transportista = asignacion.getTransportistaId();
        Persona persona = personaRepository.findByUsuariosId(transportista.getUsuariosId()).orElse(null);

        String nombreCompleto = persona != null
                ? persona.getNombres() + " " + persona.getPrimerApellido()
                : "Transportista";

        Integer progreso = PROGRESO_ESTADOS_VIAJE.getOrDefault(asignacion.getEstado(), 50);

        UbicacionDto ubicacion = extraerUltimaUbicacion(asignacion);

        return new TransportistaEnRutaDto(
                asignacion.getId(),
                nombreCompleto,
                transportista.getPlacaVehiculo(),
                asignacion.getEstado(),
                progreso,
                asignacion.getLotesId().getId(),
                ubicacion
        );
    }

    // === MONITOR DE BALANZAS ===
    private List<BalanzaMonitorDto> obtenerMonitorBalanzas(Cooperativa cooperativa) {
        List<BalanzaCooperativa> balanzas = balanzaCooperativaRepository
                .findByCooperativaIdQuery(cooperativa);

        LocalDate hoy = LocalDate.now();
        LocalDateTime inicioHoy = hoy.atStartOfDay();
        LocalDateTime finHoy = hoy.plusDays(1).atStartOfDay();

        return balanzas.stream()
                .map(balanza -> mapearBalanzaMonitor(balanza, inicioHoy, finHoy, cooperativa))
                .collect(Collectors.toList());
    }

    private BalanzaMonitorDto mapearBalanzaMonitor(
            BalanzaCooperativa balanza,
            LocalDateTime inicioHoy,
            LocalDateTime finHoy,
            Cooperativa cooperativa
    ) {
        // Contar pesajes de hoy (asignaciones con pesaje_origen hoy)
        List<AsignacionCamion> asignacionesHoy = asignacionCamionRepository
                .findByFechaInicioAfter(inicioHoy).stream()
                .filter(asig -> {
                    Lotes lote = asig.getLotesId();
                    Socio socio = lote.getMinasId().getSocioId();
                    return cooperativaSocioRepository
                            .findByCooperativaIdAndSocioId(cooperativa, socio)
                            .isPresent();
                })
                .filter(this::tienePesajeOrigen)
                .toList();

        int pesajesHoy = asignacionesHoy.size();

        // Estado de la balanza (simplificado)
        String estado = "disponible";
        PesajeActualDto pesajeActual = null;

        // Buscar asignaciones en "En camino balanza cooperativa"
        List<AsignacionCamion> enBalanzaCooperativa = asignacionCamionRepository
                .findByEstado("En camino balanza cooperativa").stream()
                .filter(asig -> {
                    Lotes lote = asig.getLotesId();
                    Socio socio = lote.getMinasId().getSocioId();
                    return cooperativaSocioRepository
                            .findByCooperativaIdAndSocioId(cooperativa, socio)
                            .isPresent();
                })
                .limit(1)
                .toList();

        if (!enBalanzaCooperativa.isEmpty()) {
            estado = "en_uso";
            AsignacionCamion asig = enBalanzaCooperativa.getFirst();
            pesajeActual = new PesajeActualDto(
                    asig.getId(),
                    asig.getLotesId().getId(),
                    asig.getTransportistaId().getPlacaVehiculo(),
                    LocalDateTime.now(),
                    "origen"
            );
        }

        // Tiempo promedio de espera (simplificado a 5 min)
        int tiempoPromedio = 5;

        // Próximos camiones (simplificado)
        List<ProximoCamionDto> proximosCamiones = new ArrayList<>();

        return new BalanzaMonitorDto(
                balanza.getId(),
                "cooperativa",
                balanza.getNombre(),
                estado,
                pesajeActual,
                pesajesHoy,
                tiempoPromedio,
                proximosCamiones
        );
    }

    // === APROBACIONES POR DÍA ===
    private List<AprobacionPorDiaDto> obtenerAprobacionesPorDia(Cooperativa cooperativa) {
        List<AprobacionPorDiaDto> resultado = new ArrayList<>();

        // Últimos 6 días
        for (int i = 5; i >= 0; i--) {
            LocalDate dia = LocalDate.now().minusDays(i);
            LocalDateTime inicioDia = dia.atStartOfDay();
            LocalDateTime finDia = dia.plusDays(1).atStartOfDay();

            List<AuditoriaLotes> auditoriasDia = auditoriaLotesRepository
                    .findByFechaRegistroBetween(inicioDia, finDia);

            long aprobados = auditoriasDia.stream()
                    .filter(a -> "APROBAR_COOPERATIVA".equals(a.getAccion()))
                    .filter(a -> perteneceLoteACooperativa(a.getLoteId(), cooperativa))
                    .count();

            long rechazados = auditoriasDia.stream()
                    .filter(a -> "RECHAZAR_COOPERATIVA".equals(a.getAccion()))
                    .filter(a -> perteneceLoteACooperativa(a.getLoteId(), cooperativa))
                    .count();

            BigDecimal tasaAprobacion = BigDecimal.ZERO;
            long total = aprobados + rechazados;
            if (total > 0) {
                tasaAprobacion = BigDecimal.valueOf(aprobados)
                        .divide(BigDecimal.valueOf(total), 4, RoundingMode.HALF_UP)
                        .multiply(new BigDecimal("100"));
            }

            String nombreDia = dia.getDayOfMonth() + " " +
                    dia.getMonth().getDisplayName(TextStyle.SHORT, new Locale("es", "ES"));

            resultado.add(new AprobacionPorDiaDto(
                    nombreDia,
                    (int) aprobados,
                    (int) rechazados,
                    tasaAprobacion
            ));
        }

        return resultado;
    }

    private boolean perteneceLoteACooperativa(Lotes lote, Cooperativa cooperativa) {
        if (lote == null) return false;
        Socio socio = lote.getMinasId().getSocioId();
        return cooperativaSocioRepository
                .findByCooperativaIdAndSocioId(cooperativa, socio)
                .isPresent();
    }

    // === MINAS POR SECTOR ===
    private List<MinasPorSectorDto> obtenerMinasPorSector(Cooperativa cooperativa) {
        List<Sectores> sectores = sectoresRepository.findByCooperativaId(cooperativa);

        return sectores.stream()
                .map(sector -> {
                    int cantidadMinas = minasRepository.findBySectoresId(sector).size();
                    return new MinasPorSectorDto(sector.getNombre(), cantidadMinas);
                })
                .filter(m -> m.getCantidad() > 0)
                .sorted(Comparator.comparing(MinasPorSectorDto::getCantidad).reversed())
                .collect(Collectors.toList());
    }

    // === MÉTODOS AUXILIARES ===

    private BigDecimal extraerPesoNetoPesajeOrigen(AsignacionCamion asignacion) {
        try {
            if (asignacion.getObservaciones() == null) return BigDecimal.ZERO;

            JsonNode observaciones = objectMapper.readTree(asignacion.getObservaciones());

            if (observaciones.has("pesaje_origen")) {
                JsonNode pesaje = observaciones.get("pesaje_origen");
                if (pesaje.has("peso_neto_kg")) {
                    return new BigDecimal(pesaje.get("peso_neto_kg").asText());
                }
            }

            return BigDecimal.ZERO;

        } catch (Exception e) {
            log.warn("Error extrayendo peso de asignación {}: {}", asignacion.getId(), e.getMessage());
            return BigDecimal.ZERO;
        }
    }

    private boolean tienePesajeOrigen(AsignacionCamion asignacion) {
        try {
            if (asignacion.getObservaciones() == null) return false;
            JsonNode observaciones = objectMapper.readTree(asignacion.getObservaciones());
            return observaciones.has("pesaje_origen");
        } catch (Exception e) {
            return false;
        }
    }

    private UbicacionDto extraerUltimaUbicacion(AsignacionCamion asignacion) {
        try {
            if (asignacion.getObservaciones() == null) {
                return new UbicacionDto(null, null);
            }

            JsonNode observaciones = objectMapper.readTree(asignacion.getObservaciones());
            String estado = asignacion.getEstado();

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
                    return new UbicacionDto(
                            eventoNode.get("lat").asDouble(),
                            eventoNode.get("lng").asDouble()
                    );
                }
            }

            return new UbicacionDto(null, null);

        } catch (Exception e) {
            log.warn("Error extrayendo ubicación de asignación {}: {}",
                    asignacion.getId(), e.getMessage());
            return new UbicacionDto(null, null);
        }
    }
}