package ucb.edu.bo.sumajflow.bl.tracking;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ucb.edu.bo.sumajflow.document.TrackingUbicacion;
import ucb.edu.bo.sumajflow.dto.tracking.*;
import ucb.edu.bo.sumajflow.entity.*;
import ucb.edu.bo.sumajflow.repository.*;
import ucb.edu.bo.sumajflow.repository.mongodb.TrackingUbicacionRepository;
import ucb.edu.bo.sumajflow.utils.GeometryUtils;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TrackingBl {

    private final TrackingUbicacionRepository trackingRepository;
    private final AsignacionCamionRepository asignacionCamionRepository;
    private final LotesRepository lotesRepository;
    private final PersonaRepository personaRepository;

    private static final long OFFLINE_THRESHOLD_MINUTES = 5;

    // Radios de geofencing en metros
    private static final int RADIO_MINA = 1000;
    private static final int RADIO_BALANZA_COOPERATIVA = 1000;
    private static final int RADIO_BALANZA_DESTINO = 1000;
    private static final int RADIO_ALMACEN = 1000;

    @Transactional
    public TrackingResponseDto iniciarTracking(Integer asignacionCamionId, Double latInicial, Double lngInicial) {
        log.info("Iniciando tracking para asignación ID: {}", asignacionCamionId);

        AsignacionCamion asignacion = asignacionCamionRepository.findById(asignacionCamionId)
                .orElseThrow(() -> new IllegalArgumentException("Asignación de camión no encontrada"));

        if (trackingRepository.existsByAsignacionCamionId(asignacionCamionId)) {
            log.warn("Ya existe tracking para la asignación ID: {}", asignacionCamionId);
            return getTrackingByAsignacion(asignacionCamionId);
        }

        Lotes lote = asignacion.getLotesId();
        Transportista transportista = asignacion.getTransportistaId();
        Persona persona = personaRepository.findByUsuariosId(transportista.getUsuariosId()).orElse(null);

        List<TrackingUbicacion.PuntoControl> puntosControl = construirPuntosControl(lote, asignacion);

        TrackingUbicacion.UbicacionActual ubicacionActual = null;
        if (lngInicial != null && GeometryUtils.esUbicacionValida(latInicial, lngInicial)) {
            ubicacionActual = TrackingUbicacion.UbicacionActual.builder()
                    .lat(latInicial)
                    .lng(lngInicial)
                    .location(TrackingUbicacion.GeoJsonPoint.of(latInicial, lngInicial))
                    .timestamp(LocalDateTime.now())
                    .velocidad(0.0)
                    .build();
        }

        TrackingUbicacion tracking = TrackingUbicacion.builder()
                .asignacionCamionId(asignacionCamionId)
                .loteId(lote.getId())
                .transportistaId(transportista.getId())
                .codigoLote("LT-" + lote.getFechaCreacion().getYear() + "-" + String.format("%04d", lote.getId()))
                .placaVehiculo(transportista.getPlacaVehiculo())
                .nombreTransportista(persona != null ? persona.getNombres() + " " + persona.getPrimerApellido() : "N/A")
                .ubicacionActual(ubicacionActual)
                .estadoViaje(asignacion.getEstado())
                .estadoConexion("online")
                .ultimaSincronizacion(LocalDateTime.now())
                .puntosControl(puntosControl)
                .metricas(TrackingUbicacion.MetricasViaje.builder()
                        .inicioViaje(LocalDateTime.now())
                        .distanciaRecorrida(0.0)
                        .tiempoEnMovimiento(0L)
                        .tiempoDetenido(0L)
                        .velocidadPromedio(0.0)
                        .velocidadMaxima(0.0)
                        .build())
                .historialUbicaciones(new ArrayList<>())
                .ubicacionesPendientesSincronizar(new ArrayList<>())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        tracking = trackingRepository.save(tracking);
        log.info("Tracking iniciado exitosamente - ID: {}", tracking.getId());

        return convertToResponseDto(tracking);
    }

    @Transactional
    public ActualizacionUbicacionResponseDto actualizarUbicacion(ActualizarUbicacionDto dto) {
        log.debug("Actualizando ubicación - Asignación ID: {}, Lat: {}, Lng: {}",
                dto.getAsignacionCamionId(), dto.getLat(), dto.getLng());

        if (!GeometryUtils.esUbicacionValida(dto.getLat(), dto.getLng())) {
            throw new IllegalArgumentException("Ubicación inválida");
        }

        TrackingUbicacion tracking = trackingRepository.findByAsignacionCamionId(dto.getAsignacionCamionId())
                .orElseGet(() -> {
                    log.info("Tracking no encontrado, creando nuevo...");
                    TrackingResponseDto nuevoTracking = iniciarTracking(
                            dto.getAsignacionCamionId(), dto.getLat(), dto.getLng());
                    return trackingRepository.findByAsignacionCamionId(dto.getAsignacionCamionId())
                            .orElseThrow(() -> new IllegalStateException("Error al crear tracking"));
                });

        // Detectar si es offline basado en el DTO o en el gap temporal
        boolean esOffline = dto.getEsOffline() != null ? dto.getEsOffline() : false;

        // Si no viene marcado como offline, detectar por gap temporal
        if (!esOffline && tracking.getUbicacionActual() != null
                && tracking.getUbicacionActual().getTimestamp() != null) {

            LocalDateTime ahora = dto.getTimestampCaptura() != null
                    ? dto.getTimestampCaptura()
                    : LocalDateTime.now();

            long minutosDesdeUltima = ChronoUnit.MINUTES.between(
                    tracking.getUbicacionActual().getTimestamp(),
                    ahora
            );

            // Si pasaron más de 5 minutos, considerar como offline
            if (minutosDesdeUltima > OFFLINE_THRESHOLD_MINUTES) {
                esOffline = true;
                log.warn("⚠️ Detectado gap temporal de {} minutos, marcando como offline",
                        minutosDesdeUltima);
            }
        }
        // =======================================================================

        // Guardar ubicación anterior en historial
        if (tracking.getUbicacionActual() != null) {
            TrackingUbicacion.PuntoUbicacion puntoHistorial = TrackingUbicacion.PuntoUbicacion.builder()
                    .lat(tracking.getUbicacionActual().getLat())
                    .lng(tracking.getUbicacionActual().getLng())
                    .timestamp(tracking.getUbicacionActual().getTimestamp())
                    .precision(tracking.getUbicacionActual().getPrecision())
                    .velocidad(tracking.getUbicacionActual().getVelocidad())
                    .rumbo(tracking.getUbicacionActual().getRumbo())
                    .altitud(tracking.getUbicacionActual().getAltitud())
                    .sincronizado(true)
                    .esOffline(esOffline)
                    .build();

            tracking.getHistorialUbicaciones().add(puntoHistorial);

            actualizarMetricas(tracking, dto);
        }

        LocalDateTime timestamp = dto.getTimestampCaptura() != null
                ? dto.getTimestampCaptura()
                : LocalDateTime.now();

        TrackingUbicacion.UbicacionActual nuevaUbicacion = TrackingUbicacion.UbicacionActual.builder()
                .lat(dto.getLat())
                .lng(dto.getLng())
                .location(TrackingUbicacion.GeoJsonPoint.of(dto.getLat(), dto.getLng()))
                .timestamp(timestamp)
                .precision(dto.getPrecision())
                .velocidad(dto.getVelocidad())
                .rumbo(dto.getRumbo())
                .altitud(dto.getAltitud())
                .build();

        tracking.setUbicacionActual(nuevaUbicacion);
        tracking.setEstadoConexion("online");
        tracking.setUltimaSincronizacion(LocalDateTime.now());
        tracking.setUpdatedAt(LocalDateTime.now());

        GeofencingStatusDto geofencingStatus = verificarGeofencing(tracking, dto.getLat(), dto.getLng());

        trackingRepository.save(tracking);

        return ActualizacionUbicacionResponseDto.builder()
                .success(true)
                .mensaje("Ubicación actualizada correctamente")
                .ubicacionRegistrada(UbicacionDto.builder()
                        .lat(dto.getLat())
                        .lng(dto.getLng())
                        .timestamp(timestamp)
                        .velocidad(dto.getVelocidad())
                        .build())
                .geofencingStatus(geofencingStatus)
                .nuevoEstadoViaje(tracking.getEstadoViaje())
                .requiereAccion(geofencingStatus.getPuedeRegistrarLlegada() || geofencingStatus.getPuedeRegistrarSalida())
                .accionRequerida(determinarAccionRequerida(geofencingStatus))
                .build();
    }
    @Transactional
    public SincronizacionResponseDto sincronizarUbicacionesOffline(
            Integer asignacionCamionId,
            List<UbicacionOfflineDto> ubicaciones) {

        log.info("🔄 Sincronizando {} ubicaciones offline para asignación ID: {}",
                ubicaciones.size(), asignacionCamionId);

        TrackingUbicacion tracking = trackingRepository.findByAsignacionCamionId(asignacionCamionId)
                .orElseThrow(() -> new IllegalArgumentException("Tracking no encontrado"));

        int sincronizadas = 0;
        int fallidas = 0;
        List<String> errores = new ArrayList<>();

        // Ordenar por timestamp
        ubicaciones.sort(Comparator.comparing(UbicacionOfflineDto::getTimestamp));

        log.debug("📝 Agregando {} ubicaciones a pendientes de sincronizar", ubicaciones.size());
        for (UbicacionOfflineDto ubicacion : ubicaciones) {
            try {
                if (!GeometryUtils.esUbicacionValida(ubicacion.getLat(), ubicacion.getLng())) {
                    errores.add("Ubicación inválida: " + ubicacion.getTimestamp());
                    fallidas++;
                    continue;
                }

                TrackingUbicacion.PuntoUbicacion punto = TrackingUbicacion.PuntoUbicacion.builder()
                        .lat(ubicacion.getLat())
                        .lng(ubicacion.getLng())
                        .timestamp(ubicacion.getTimestamp())
                        .precision(ubicacion.getPrecision())
                        .velocidad(ubicacion.getVelocidad())
                        .rumbo(ubicacion.getRumbo())
                        .altitud(ubicacion.getAltitud())
                        .sincronizado(false)
                        .esOffline(true)
                        .build();

                // Agregar a pendientes primero
                tracking.getUbicacionesPendientesSincronizar().add(punto);

            } catch (Exception e) {
                errores.add("Error en ubicación " + ubicacion.getTimestamp() + ": " + e.getMessage());
                fallidas++;
            }
        }

        log.debug("📦 Moviendo {} puntos de pendientes a historial",
                tracking.getUbicacionesPendientesSincronizar().size());

        for (TrackingUbicacion.PuntoUbicacion punto : tracking.getUbicacionesPendientesSincronizar()) {
            punto.setSincronizado(true);
            tracking.getHistorialUbicaciones().add(punto);
            sincronizadas++;
        }

        // Limpiar pendientes
        tracking.getUbicacionesPendientesSincronizar().clear();
        log.debug("✅ Pendientes limpiados, {} ubicaciones ahora en historial", sincronizadas);
        // ================================================================================

        // Actualizar ubicación actual con la última
        if (!ubicaciones.isEmpty()) {
            UbicacionOfflineDto ultima = ubicaciones.getLast();
            tracking.setUbicacionActual(TrackingUbicacion.UbicacionActual.builder()
                    .lat(ultima.getLat())
                    .lng(ultima.getLng())
                    .location(TrackingUbicacion.GeoJsonPoint.of(ultima.getLat(), ultima.getLng()))
                    .timestamp(ultima.getTimestamp())
                    .precision(ultima.getPrecision())
                    .velocidad(ultima.getVelocidad())
                    .rumbo(ultima.getRumbo())
                    .altitud(ultima.getAltitud())
                    .build());

            recalcularMetricas(tracking);
        }

        tracking.setEstadoConexion("online");
        tracking.setUltimaSincronizacion(LocalDateTime.now());
        tracking.setUpdatedAt(LocalDateTime.now());

        trackingRepository.save(tracking);

        log.info("✅ Sincronización completada - Éxito: {}, Fallidas: {}", sincronizadas, fallidas);

        return SincronizacionResponseDto.builder()
                .success(fallidas == 0)
                .ubicacionesSincronizadas(sincronizadas)
                .ubicacionesFallidas(fallidas)
                .errores(errores)
                .ultimaSincronizacion(LocalDateTime.now())
                .build();
    }
    public GeofencingStatusDto verificarGeofencing(TrackingUbicacion tracking, double lat, double lng) {
        GeofencingStatusDto.GeofencingStatusDtoBuilder builder = GeofencingStatusDto.builder()
                .dentroDeZona(false)
                .puedeRegistrarLlegada(false)
                .puedeRegistrarSalida(false);

        if (tracking.getPuntosControl() == null || tracking.getPuntosControl().isEmpty()) {
            return builder.build();
        }

        for (TrackingUbicacion.PuntoControl punto : tracking.getPuntosControl()) {
            double distancia = GeometryUtils.calcularDistanciaMetros(lat, lng, punto.getLat(), punto.getLng());

            if (distancia <= punto.getRadio()) {
                builder.dentroDeZona(true)
                        .zonaNombre(punto.getNombre())
                        .zonaTipo(punto.getTipo())
                        .distanciaAZona(distancia);

                if ("pendiente".equals(punto.getEstado()) || punto.getLlegada() == null) {
                    builder.puedeRegistrarLlegada(true);
                } else if (punto.getSalida() == null) {
                    builder.puedeRegistrarSalida(true);
                }
                break;
            }
        }

        TrackingUbicacion.PuntoControl proximoPunto = GeometryUtils.encontrarProximoPuntoControlPendiente(tracking.getPuntosControl());
        if (proximoPunto != null) {
            double distanciaProximo = GeometryUtils.calcularDistanciaMetros(lat, lng, proximoPunto.getLat(), proximoPunto.getLng());
            builder.proximoPuntoControl(proximoPunto.getNombre())
                    .distanciaProximoPunto(distanciaProximo);
        }

        return builder.build();
    }

    @Transactional
    public TrackingResponseDto registrarLlegadaPuntoControl(
            Integer asignacionCamionId,
            String tipoPunto,
            Double lat,
            Double lng,
            String observaciones) {

        log.info("Registrando llegada a {} para asignación ID: {}", tipoPunto, asignacionCamionId);

        TrackingUbicacion tracking = trackingRepository.findByAsignacionCamionId(asignacionCamionId)
                .orElseThrow(() -> new IllegalArgumentException("Tracking no encontrado"));

        TrackingUbicacion.PuntoControl punto = tracking.getPuntosControl().stream()
                .filter(p -> p.getTipo().equals(tipoPunto))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Punto de control no encontrado: " + tipoPunto));

        if (lat != null && lng != null) {
            double distancia = GeometryUtils.calcularDistanciaMetros(lat, lng, punto.getLat(), punto.getLng());
            if (distancia > punto.getRadio() * 1.5) {
                throw new IllegalArgumentException(
                        "Estás demasiado lejos del punto de control (" + (int) distancia + "m). " +
                                "Debes estar a menos de " + punto.getRadio() + "m");
            }
        }

        punto.setLlegada(LocalDateTime.now());
        punto.setEstado("en_punto");

        tracking.setUpdatedAt(LocalDateTime.now());
        trackingRepository.save(tracking);

        log.info("Llegada registrada exitosamente a: {}", tipoPunto);

        return convertToResponseDto(tracking);
    }

    @Transactional
    public TrackingResponseDto registrarSalidaPuntoControl(
            Integer asignacionCamionId,
            String tipoPunto,
            String observaciones) {

        log.info("Registrando salida de {} para asignación ID: {}", tipoPunto, asignacionCamionId);

        TrackingUbicacion tracking = trackingRepository.findByAsignacionCamionId(asignacionCamionId)
                .orElseThrow(() -> new IllegalArgumentException("Tracking no encontrado"));

        TrackingUbicacion.PuntoControl punto = tracking.getPuntosControl().stream()
                .filter(p -> p.getTipo().equals(tipoPunto))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Punto de control no encontrado: " + tipoPunto));

        if (punto.getLlegada() == null) {
            throw new IllegalArgumentException("Debe registrar llegada antes de la salida");
        }

        punto.setSalida(LocalDateTime.now());
        punto.setEstado("completado");

        tracking.setUpdatedAt(LocalDateTime.now());
        trackingRepository.save(tracking);

        log.info("Salida registrada exitosamente de: {}", tipoPunto);

        return convertToResponseDto(tracking);
    }

    @Transactional(readOnly = true)
    public TrackingResponseDto getTrackingByAsignacion(Integer asignacionCamionId) {
        TrackingUbicacion tracking = trackingRepository.findByAsignacionCamionId(asignacionCamionId)
                .orElseThrow(() -> new IllegalArgumentException("Tracking no encontrado para esta asignación"));

        actualizarEstadoConexion(tracking);

        return convertToResponseDto(tracking);
    }

    @Transactional(readOnly = true)
    public MonitoreoLoteDto getMonitoreoLote(Integer loteId) {
        log.debug("Obteniendo monitoreo para lote ID: {}", loteId);

        Lotes lote = lotesRepository.findById(loteId)
                .orElseThrow(() -> new IllegalArgumentException("Lote no encontrado"));

        List<TrackingUbicacion> trackings = trackingRepository.findByLoteId(loteId);

        List<CamionEnRutaDto> camiones = trackings.stream()
                .map(this::convertToCamionEnRutaDto)
                .collect(Collectors.toList());

        long enRuta = camiones.stream()
                .filter(c -> !"Completado".equals(c.getEstadoViaje()) && !"Cancelado por rechazo".equals(c.getEstadoViaje()))
                .count();
        long completados = camiones.stream()
                .filter(c -> "Completado".equals(c.getEstadoViaje()))
                .count();

        return MonitoreoLoteDto.builder()
                .loteId(loteId)
                .codigoLote("LT-" + lote.getFechaCreacion().getYear() + "-" + String.format("%04d", loteId))
                .estadoLote(lote.getEstado())
                .totalCamiones(lote.getCamionesSolicitados())
                .camionesEnRuta((int) enRuta)
                .camionesCompletados((int) completados)
                .camiones(camiones)
                .build();
    }

    @Transactional(readOnly = true)
    public HistorialUbicacionesDto getHistorialUbicaciones(Integer asignacionCamionId) {
        TrackingUbicacion tracking = trackingRepository.findByAsignacionCamionId(asignacionCamionId)
                .orElseThrow(() -> new IllegalArgumentException("Tracking no encontrado"));

        List<UbicacionDto> ubicaciones = tracking.getHistorialUbicaciones().stream()
                .map(p -> UbicacionDto.builder()
                        .lat(p.getLat())
                        .lng(p.getLng())
                        .timestamp(p.getTimestamp())
                        .precision(p.getPrecision())
                        .velocidad(p.getVelocidad())
                        .rumbo(p.getRumbo())
                        .altitud(p.getAltitud())
                        .build())
                .collect(Collectors.toList());

        if (tracking.getUbicacionActual() != null) {
            ubicaciones.add(UbicacionDto.builder()
                    .lat(tracking.getUbicacionActual().getLat())
                    .lng(tracking.getUbicacionActual().getLng())
                    .timestamp(tracking.getUbicacionActual().getTimestamp())
                    .velocidad(tracking.getUbicacionActual().getVelocidad())
                    .build());
        }

        return HistorialUbicacionesDto.builder()
                .asignacionCamionId(asignacionCamionId)
                .totalPuntos(ubicaciones.size())
                .ubicaciones(ubicaciones)
                .resumen(construirResumenRuta(tracking, ubicaciones))
                .build();
    }

    private List<TrackingUbicacion.PuntoControl> construirPuntosControl(Lotes lote, AsignacionCamion asignacion) {
        List<TrackingUbicacion.PuntoControl> puntos = new ArrayList<>();
        int orden = 1;

        Minas mina = lote.getMinasId();
        puntos.add(TrackingUbicacion.PuntoControl.builder()
                .tipo("mina")
                .nombre(mina.getNombre())
                .lat(mina.getLatitud().doubleValue())
                .lng(mina.getLongitud().doubleValue())
                .radio(RADIO_MINA)
                .orden(orden++)
                .requerido(true)
                .estado("pendiente")
                .build());

        Sectores sector = mina.getSectoresId();
        Cooperativa cooperativa = sector.getCooperativaId();
        if (!cooperativa.getBalanzaCooperativaList().isEmpty()) {
            var balanza = cooperativa.getBalanzaCooperativaList().getFirst();
            puntos.add(TrackingUbicacion.PuntoControl.builder()
                    .tipo("balanza_cooperativa")
                    .nombre("Balanza " + cooperativa.getRazonSocial())
                    .lat(balanza.getLatitud().doubleValue())
                    .lng(balanza.getLongitud().doubleValue())
                    .radio(RADIO_BALANZA_COOPERATIVA)
                    .orden(orden++)
                    .requerido(true)
                    .estado("pendiente")
                    .build());
        }

        if (!lote.getLoteIngenioList().isEmpty()) {
            IngenioMinero ingenio = lote.getLoteIngenioList().getFirst().getIngenioMineroId();

            if (!ingenio.getBalanzasIngenioList().isEmpty()) {
                var balanza = ingenio.getBalanzasIngenioList().getFirst();
                puntos.add(TrackingUbicacion.PuntoControl.builder()
                        .tipo("balanza_ingenio")
                        .nombre("Balanza " + ingenio.getRazonSocial())
                        .lat(balanza.getLatitud().doubleValue())
                        .lng(balanza.getLongitud().doubleValue())
                        .radio(RADIO_BALANZA_DESTINO)
                        .orden(orden++)
                        .requerido(true)
                        .estado("pendiente")
                        .build());
            }

            if (!ingenio.getAlmacenesIngenioList().isEmpty()) {
                var almacen = ingenio.getAlmacenesIngenioList().getFirst();
                puntos.add(TrackingUbicacion.PuntoControl.builder()
                        .tipo("almacen_ingenio")
                        .nombre("Almacén " + ingenio.getRazonSocial())
                        .lat(almacen.getLatitud().doubleValue())
                        .lng(almacen.getLongitud().doubleValue())
                        .radio(RADIO_ALMACEN)
                        .orden(orden++)
                        .requerido(true)
                        .estado("pendiente")
                        .build());
            }

        } else if (!lote.getLoteComercializadoraList().isEmpty()) {
            Comercializadora comercializadora = lote.getLoteComercializadoraList().getFirst().getComercializadoraId();

            if (!comercializadora.getBalanzasList().isEmpty()) {
                var balanza = comercializadora.getBalanzasList().getFirst();
                puntos.add(TrackingUbicacion.PuntoControl.builder()
                        .tipo("balanza_comercializadora")
                        .nombre("Balanza " + comercializadora.getRazonSocial())
                        .lat(balanza.getLatitud().doubleValue())
                        .lng(balanza.getLongitud().doubleValue())
                        .radio(RADIO_BALANZA_DESTINO)
                        .orden(orden++)
                        .requerido(true)
                        .estado("pendiente")
                        .build());
            }

            if (!comercializadora.getAlmacenesList().isEmpty()) {
                var almacen = comercializadora.getAlmacenesList().getFirst();
                puntos.add(TrackingUbicacion.PuntoControl.builder()
                        .tipo("almacen_comercializadora")
                        .nombre("Almacén " + comercializadora.getRazonSocial())
                        .lat(almacen.getLatitud().doubleValue())
                        .lng(almacen.getLongitud().doubleValue())
                        .radio(RADIO_ALMACEN)
                        .orden(orden++)
                        .requerido(true)
                        .estado("pendiente")
                        .build());
            }
        }

        return puntos;
    }

    private void actualizarMetricas(TrackingUbicacion tracking, ActualizarUbicacionDto dto) {
        TrackingUbicacion.MetricasViaje metricas = tracking.getMetricas();
        TrackingUbicacion.UbicacionActual ubicacionAnterior = tracking.getUbicacionActual();

        if (ubicacionAnterior == null || metricas == null) {
            return;
        }

        double distancia = GeometryUtils.calcularDistancia(
                ubicacionAnterior.getLat(), ubicacionAnterior.getLng(),
                dto.getLat(), dto.getLng());

        metricas.setDistanciaRecorrida(metricas.getDistanciaRecorrida() + distancia);

        if (ubicacionAnterior.getTimestamp() != null) {
            long segundosTranscurridos = ChronoUnit.SECONDS.between(
                    ubicacionAnterior.getTimestamp(),
                    dto.getTimestampCaptura() != null ? dto.getTimestampCaptura() : LocalDateTime.now());

            if (dto.getVelocidad() != null && dto.getVelocidad() > 1.0) {
                metricas.setTiempoEnMovimiento(metricas.getTiempoEnMovimiento() + segundosTranscurridos);
            } else {
                metricas.setTiempoDetenido(metricas.getTiempoDetenido() + segundosTranscurridos);
            }
        }

        if (dto.getVelocidad() != null && dto.getVelocidad() > metricas.getVelocidadMaxima()) {
            metricas.setVelocidadMaxima(dto.getVelocidad());
        }

        long tiempoTotal = metricas.getTiempoEnMovimiento();
        if (tiempoTotal > 0) {
            double velocidadPromedio = (metricas.getDistanciaRecorrida() / tiempoTotal) * 3600;
            metricas.setVelocidadPromedio(velocidadPromedio);
        }
    }

    private void recalcularMetricas(TrackingUbicacion tracking) {
        List<TrackingUbicacion.PuntoUbicacion> historial = tracking.getHistorialUbicaciones();
        if (historial.isEmpty()) {
            return;
        }

        historial.sort(Comparator.comparing(TrackingUbicacion.PuntoUbicacion::getTimestamp));

        double distanciaTotal = 0;
        long tiempoMovimiento = 0;
        long tiempoDetenido = 0;
        double velocidadMaxima = 0;

        for (int i = 1; i < historial.size(); i++) {
            TrackingUbicacion.PuntoUbicacion anterior = historial.get(i - 1);
            TrackingUbicacion.PuntoUbicacion actual = historial.get(i);

            double distancia = GeometryUtils.calcularDistancia(
                    anterior.getLat(), anterior.getLng(),
                    actual.getLat(), actual.getLng());
            distanciaTotal += distancia;

            long segundos = ChronoUnit.SECONDS.between(anterior.getTimestamp(), actual.getTimestamp());
            if (actual.getVelocidad() != null && actual.getVelocidad() > 1.0) {
                tiempoMovimiento += segundos;
            } else {
                tiempoDetenido += segundos;
            }

            if (actual.getVelocidad() != null && actual.getVelocidad() > velocidadMaxima) {
                velocidadMaxima = actual.getVelocidad();
            }
        }

        TrackingUbicacion.MetricasViaje metricas = tracking.getMetricas();
        metricas.setDistanciaRecorrida(distanciaTotal);
        metricas.setTiempoEnMovimiento(tiempoMovimiento);
        metricas.setTiempoDetenido(tiempoDetenido);
        metricas.setVelocidadMaxima(velocidadMaxima);

        if (tiempoMovimiento > 0) {
            metricas.setVelocidadPromedio((distanciaTotal / tiempoMovimiento) * 3600);
        }
    }

    private void actualizarEstadoConexion(TrackingUbicacion tracking) {
        if (tracking.getUltimaSincronizacion() == null) {
            return;
        }

        long minutosDesdeUltimaSync = ChronoUnit.MINUTES.between(
                tracking.getUltimaSincronizacion(), LocalDateTime.now());

        if (minutosDesdeUltimaSync > OFFLINE_THRESHOLD_MINUTES) {
            tracking.setEstadoConexion("offline");
        } else {
            tracking.setEstadoConexion("online");
        }
    }

    private String determinarAccionRequerida(GeofencingStatusDto geofencing) {
        if (geofencing.getPuedeRegistrarLlegada()) {
            return "registrar_llegada";
        } else if (geofencing.getPuedeRegistrarSalida()) {
            String tipo = geofencing.getZonaTipo();
            if (tipo != null && tipo.contains("balanza")) {
                return "registrar_pesaje";
            }
            return "registrar_salida";
        }
        return null;
    }

    private TrackingResponseDto convertToResponseDto(TrackingUbicacion tracking) {
        GeofencingStatusDto geofencing = null;
        if (tracking.getUbicacionActual() != null) {
            geofencing = verificarGeofencing(tracking,
                    tracking.getUbicacionActual().getLat(),
                    tracking.getUbicacionActual().getLng());
        }

        return TrackingResponseDto.builder()
                .id(tracking.getId())
                .asignacionCamionId(tracking.getAsignacionCamionId())
                .loteId(tracking.getLoteId())
                .transportistaId(tracking.getTransportistaId())
                .codigoLote(tracking.getCodigoLote())
                .placaVehiculo(tracking.getPlacaVehiculo())
                .nombreTransportista(tracking.getNombreTransportista())
                .ubicacionActual(tracking.getUbicacionActual() != null ?
                        UbicacionDto.builder()
                                .lat(tracking.getUbicacionActual().getLat())
                                .lng(tracking.getUbicacionActual().getLng())
                                .timestamp(tracking.getUbicacionActual().getTimestamp())
                                .precision(tracking.getUbicacionActual().getPrecision())
                                .velocidad(tracking.getUbicacionActual().getVelocidad())
                                .rumbo(tracking.getUbicacionActual().getRumbo())
                                .altitud(tracking.getUbicacionActual().getAltitud())
                                .build() : null)
                .estadoViaje(tracking.getEstadoViaje())
                .estadoConexion(tracking.getEstadoConexion())
                .ultimaSincronizacion(tracking.getUltimaSincronizacion())
                .puntosControl(tracking.getPuntosControl().stream()
                        .map(this::convertToPuntoControlDto)
                        .collect(Collectors.toList()))
                .metricas(convertToMetricasDto(tracking.getMetricas()))
                .geofencingStatus(geofencing)
                .createdAt(tracking.getCreatedAt())
                .updatedAt(tracking.getUpdatedAt())
                .build();
    }

    private PuntoControlDto convertToPuntoControlDto(TrackingUbicacion.PuntoControl punto) {
        return PuntoControlDto.builder()
                .tipo(punto.getTipo())
                .nombre(punto.getNombre())
                .lat(punto.getLat())
                .lng(punto.getLng())
                .radio(punto.getRadio())
                .orden(punto.getOrden())
                .requerido(punto.getRequerido())
                .llegada(punto.getLlegada())
                .salida(punto.getSalida())
                .estado(punto.getEstado())
                .build();
    }

    private MetricasViajeDto convertToMetricasDto(TrackingUbicacion.MetricasViaje metricas) {
        if (metricas == null) {
            return null;
        }

        long tiempoTotal = (metricas.getTiempoEnMovimiento() != null ? metricas.getTiempoEnMovimiento() : 0)
                + (metricas.getTiempoDetenido() != null ? metricas.getTiempoDetenido() : 0);

        return MetricasViajeDto.builder()
                .distanciaRecorrida(metricas.getDistanciaRecorrida())
                .tiempoEnMovimiento(metricas.getTiempoEnMovimiento())
                .tiempoDetenido(metricas.getTiempoDetenido())
                .velocidadPromedio(metricas.getVelocidadPromedio())
                .velocidadMaxima(metricas.getVelocidadMaxima())
                .inicioViaje(metricas.getInicioViaje())
                .finViaje(metricas.getFinViaje())
                .tiempoTranscurrido(GeometryUtils.formatearDuracion(tiempoTotal))
                .build();
    }

    private CamionEnRutaDto convertToCamionEnRutaDto(TrackingUbicacion tracking) {
        CamionEnRutaDto.CamionEnRutaDtoBuilder builder = CamionEnRutaDto.builder()
                .asignacionCamionId(tracking.getAsignacionCamionId())
                .placaVehiculo(tracking.getPlacaVehiculo())
                .nombreTransportista(tracking.getNombreTransportista())
                .estadoViaje(tracking.getEstadoViaje())
                .estadoConexion(tracking.getEstadoConexion())
                .ultimaActualizacion(tracking.getUltimaSincronizacion());

        if (tracking.getUbicacionActual() != null) {
            builder.ubicacionActual(UbicacionDto.builder()
                    .lat(tracking.getUbicacionActual().getLat())
                    .lng(tracking.getUbicacionActual().getLng())
                    .timestamp(tracking.getUbicacionActual().getTimestamp())
                    .velocidad(tracking.getUbicacionActual().getVelocidad())
                    .build());
        }

        TrackingUbicacion.PuntoControl proximoPunto =
                GeometryUtils.encontrarProximoPuntoControlPendiente(tracking.getPuntosControl());
        if (proximoPunto != null && tracking.getUbicacionActual() != null) {
            double distancia = GeometryUtils.calcularDistanciaMetros(
                    tracking.getUbicacionActual().getLat(),
                    tracking.getUbicacionActual().getLng(),
                    proximoPunto.getLat(),
                    proximoPunto.getLng());

            builder.proximoPuntoControl(proximoPunto.getNombre())
                    .distanciaProximoPunto(distancia);

            double velocidad = tracking.getUbicacionActual().getVelocidad() != null
                    ? tracking.getUbicacionActual().getVelocidad() : 30.0;
            long tiempoEstimado = GeometryUtils.calcularTiempoEstimadoLlegada(distancia / 1000, velocidad);
            builder.tiempoEstimadoLlegada(GeometryUtils.formatearDuracion(tiempoEstimado));
        }

        return builder.build();
    }

    private RutaResumenDto construirResumenRuta(TrackingUbicacion tracking, List<UbicacionDto> ubicaciones) {
        if (ubicaciones.isEmpty()) {
            return null;
        }

        List<PuntoControlDto> puntosVisitados = tracking.getPuntosControl().stream()
                .filter(p -> "completado".equals(p.getEstado()))
                .map(this::convertToPuntoControlDto)
                .collect(Collectors.toList());

        return RutaResumenDto.builder()
                .puntoInicio(ubicaciones.getFirst())
                .puntoFin(ubicaciones.getLast())
                .distanciaTotal(tracking.getMetricas().getDistanciaRecorrida())
                .duracionTotal(tracking.getMetricas().getTiempoEnMovimiento() + tracking.getMetricas().getTiempoDetenido())
                .puntosVisitados(puntosVisitados)
                .build();
    }
    /**
     * Actualiza el estado del viaje en MongoDB y registra el evento de cambio de estado.
     * @param asignacionCamionId ID de la asignación
     * @param estadoAnterior Estado previo del viaje
     * @param estadoNuevo Nuevo estado del viaje
     * @param tipoEvento Tipo de evento (INICIO_VIAJE, LLEGADA_MINA, etc.)
     * @param lat Latitud donde ocurrió el evento (opcional)
     * @param lng Longitud donde ocurrió el evento (opcional)
     */
    @Transactional
    public void actualizarEstadoYRegistrarEvento(
            Integer asignacionCamionId,
            String estadoAnterior,
            String estadoNuevo,
            String tipoEvento,
            Double lat,
            Double lng) {

        log.info("📊 Actualizando estado MongoDB - Asignación: {}, {} -> {}, Evento: {}",
                asignacionCamionId, estadoAnterior, estadoNuevo, tipoEvento);

        try {
            TrackingUbicacion tracking = trackingRepository
                    .findByAsignacionCamionId(asignacionCamionId)
                    .orElseThrow(() -> new IllegalStateException(
                            "Tracking no encontrado para asignación: " + asignacionCamionId));

            // 1. Actualizar estado del viaje
            tracking.setEstadoViaje(estadoNuevo);

            // 2. Crear y agregar evento
            TrackingUbicacion.EventoEstado evento = TrackingUbicacion.EventoEstado.builder()
                    .timestamp(LocalDateTime.now())
                    .estadoAnterior(estadoAnterior)
                    .estadoNuevo(estadoNuevo)
                    .lat(lat)
                    .lng(lng)
                    .tipoEvento(tipoEvento)
                    .build();

            tracking.getEventosEstado().add(evento);

            // 3. Actualizar timestamp
            tracking.setUpdatedAt(LocalDateTime.now());

            // 4. Guardar en MongoDB
            trackingRepository.save(tracking);

            log.info("✅ MongoDB actualizado - Estado: {}, Evento: {} registrado",
                    estadoNuevo, tipoEvento);

        } catch (Exception e) {
            log.error("❌ Error al actualizar estado en MongoDB: {}", e.getMessage(), e);
        }
    }
}