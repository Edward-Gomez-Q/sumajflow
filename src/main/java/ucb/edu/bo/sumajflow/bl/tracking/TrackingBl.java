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
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;

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
    private final TrackingWebSocketService trackingWebSocketService;
    private final ObjectMapper objectMapper;

    private static final long OFFLINE_THRESHOLD_SECONDS = 40;
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

        boolean esActualizacionOffline = dto.getEsOffline() != null ? dto.getEsOffline() : false;

        boolean huboCorteComunicacion = false;
        if (!esActualizacionOffline && tracking.getUbicacionActual() != null
                && tracking.getUbicacionActual().getTimestamp() != null) {

            LocalDateTime ahora = dto.getTimestampCaptura() != null
                    ? dto.getTimestampCaptura()
                    : LocalDateTime.now();

            long segundosDesdeUltima = ChronoUnit.SECONDS.between(
                    tracking.getUbicacionActual().getTimestamp(),
                    ahora
            );

            if (segundosDesdeUltima > OFFLINE_THRESHOLD_SECONDS) {
                huboCorteComunicacion = true;
                log.warn("⚠️ Detectado corte de comunicación de {} segundos (>{} seg), " +
                                "las ubicaciones intermedias se perdieron",
                        segundosDesdeUltima, OFFLINE_THRESHOLD_SECONDS);
            }
        }

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
                    .esOffline(huboCorteComunicacion || esActualizacionOffline)
                    .estadoViaje(tracking.getEstadoViaje())
                    .build();

            tracking.getHistorialUbicaciones().add(puntoHistorial);

            if (!huboCorteComunicacion) {
                actualizarMetricas(tracking, dto);
            }
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

        TrackingResponseDto responseDto = convertToResponseDto(tracking);
        trackingWebSocketService.enviarActualizacionCompleta(
                tracking.getLoteId(),
                tracking.getAsignacionCamionId(),
                responseDto
        );

        return ActualizacionUbicacionResponseDto.builder()
                .success(true)
                .mensaje(huboCorteComunicacion
                        ? "Ubicación actualizada - Se detectó pérdida de señal previa"
                        : "Ubicación actualizada correctamente")
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

        ubicaciones.sort(Comparator.comparing(UbicacionOfflineDto::getTimestamp));

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
                        .sincronizado(true)
                        .esOffline(true)
                        .estadoViaje(tracking.getEstadoViaje())
                        .build();

                tracking.getHistorialUbicaciones().add(punto);
                sincronizadas++;

            } catch (Exception e) {
                errores.add("Error en ubicación " + ubicacion.getTimestamp() + ": " + e.getMessage());
                fallidas++;
            }
        }

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

        TrackingResponseDto responseDto = convertToResponseDto(tracking);
        trackingWebSocketService.enviarActualizacionCompleta(
                tracking.getLoteId(),
                tracking.getAsignacionCamionId(),
                responseDto
        );

        log.info("✅ Sincronización completada - Éxito: {}, Fallidas: {}, VOLVIÓ A ONLINE",
                sincronizadas, fallidas);

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

        TrackingResponseDto responseDto = convertToResponseDto(tracking);
        trackingWebSocketService.enviarActualizacionCompleta(
                tracking.getLoteId(),
                tracking.getAsignacionCamionId(),
                responseDto
        );

        trackingWebSocketService.enviarEventoTracking(
                tracking.getLoteId(),
                tracking.getAsignacionCamionId(),
                "LLEGADA_PUNTO_CONTROL",
                "Llegada confirmada a: " + punto.getNombre()
        );

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

        TrackingResponseDto responseDto = convertToResponseDto(tracking);
        trackingWebSocketService.enviarActualizacionCompleta(
                tracking.getLoteId(),
                tracking.getAsignacionCamionId(),
                responseDto
        );

        trackingWebSocketService.enviarEventoTracking(
                tracking.getLoteId(),
                tracking.getAsignacionCamionId(),
                "SALIDA_PUNTO_CONTROL",
                "Salida confirmada de: " + punto.getNombre()
        );

        log.info("Salida registrada exitosamente de: {}", tipoPunto);

        return convertToResponseDto(tracking);
    }

    @Transactional(readOnly = true)
    public TrackingResponseDto getTrackingByAsignacion(Integer asignacionCamionId) {
        TrackingUbicacion tracking = trackingRepository.findByAsignacionCamionId(asignacionCamionId)
                .orElseThrow(() -> new IllegalArgumentException("Tracking no encontrado para esta asignación"));
        verificarYActualizarEstadoConexion(tracking);

        return convertToResponseDto(tracking);
    }

    private void verificarYActualizarEstadoConexion(TrackingUbicacion tracking) {
        if (tracking.getUltimaSincronizacion() == null) {
            return;
        }

        long segundosDesdeUltimaSync = ChronoUnit.SECONDS.between(
                tracking.getUltimaSincronizacion(),
                LocalDateTime.now());

        String estadoActual = segundosDesdeUltimaSync > OFFLINE_THRESHOLD_SECONDS
                ? "offline"
                : "online";

        if (!estadoActual.equals(tracking.getEstadoConexion())) {
            tracking.setEstadoConexion(estadoActual);
            log.debug("Estado conexión calculado: {} ({} seg desde última sync)",
                    estadoActual, segundosDesdeUltimaSync);
        }
    }

    @Transactional(readOnly = true)
    public MonitoreoLoteDto getMonitoreoLote(Integer loteId) {
        log.debug("Obteniendo monitoreo para lote ID: {}", loteId);

        Lotes lote = lotesRepository.findById(loteId)
                .orElseThrow(() -> new IllegalArgumentException("Lote no encontrado"));

        List<TrackingUbicacion> trackings = trackingRepository.findByLoteId(loteId);

        trackings.forEach(this::verificarYActualizarEstadoConexion);

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

            tracking.setEstadoViaje(estadoNuevo);

            TrackingUbicacion.EventoEstado evento = TrackingUbicacion.EventoEstado.builder()
                    .timestamp(LocalDateTime.now())
                    .estadoAnterior(estadoAnterior)
                    .estadoNuevo(estadoNuevo)
                    .lat(lat)
                    .lng(lng)
                    .tipoEvento(tipoEvento)
                    .build();

            tracking.getEventosEstado().add(evento);
            actualizarEstadoPuntosDeControl(tracking, tipoEvento);
            tracking.setUpdatedAt(LocalDateTime.now());

            trackingRepository.save(tracking);

            TrackingResponseDto responseDto = convertToResponseDto(tracking);
            trackingWebSocketService.enviarActualizacionCompleta(
                    tracking.getLoteId(),
                    tracking.getAsignacionCamionId(),
                    responseDto
            );

            trackingWebSocketService.enviarEventoTracking(
                    tracking.getLoteId(),
                    tracking.getAsignacionCamionId(),
                    tipoEvento,
                    String.format("Cambio de estado: %s → %s", estadoAnterior, estadoNuevo)
            );

            log.info("✅ MongoDB actualizado - Estado: {}, Evento: {} registrado, Puntos actualizados",
                    estadoNuevo, tipoEvento);

        } catch (Exception e) {
            log.error("❌ Error al actualizar estado en MongoDB: {}", e.getMessage(), e);
        }
    }

    /**
     * Actualizar estado de puntos de control basado en eventos de estado
     */
    private void actualizarEstadoPuntosDeControl(TrackingUbicacion tracking, String tipoEvento) {
        if (tracking.getPuntosControl() == null || tracking.getPuntosControl().isEmpty()) {
            return;
        }

        log.info("🔄 Actualizando estados de puntos de control para evento: {}", tipoEvento);

        switch (tipoEvento) {
            case "LLEGADA_MINA":
                tracking.getPuntosControl().stream()
                        .filter(p -> "mina".equals(p.getTipo()))
                        .findFirst()
                        .ifPresent(p -> {
                            p.setEstado("en_punto");
                            if (p.getLlegada() == null) {
                                p.setLlegada(LocalDateTime.now());
                            }
                            log.info("✅ Mina marcada como 'en_punto'");
                        });
                break;

            case "FIN_CARGUIO":
                tracking.getPuntosControl().stream()
                        .filter(p -> "mina".equals(p.getTipo()))
                        .findFirst()
                        .ifPresent(p -> {
                            p.setEstado("completado");
                            if (p.getSalida() == null) {
                                p.setSalida(LocalDateTime.now());
                            }
                            log.info("✅ Mina marcada como 'completado'");
                        });
                break;

            case "PESAJE_COOPERATIVA":
                tracking.getPuntosControl().stream()
                        .filter(p -> "balanza_cooperativa".equals(p.getTipo()))
                        .findFirst()
                        .ifPresent(p -> {
                            if (p.getLlegada() == null) {
                                p.setLlegada(LocalDateTime.now());
                            }
                            p.setSalida(LocalDateTime.now());
                            p.setEstado("completado");
                            log.info("✅ Balanza cooperativa marcada como 'completado'");
                        });
                break;

            case "PESAJE_DESTINO":
                tracking.getPuntosControl().stream()
                        .filter(p -> "balanza_ingenio".equals(p.getTipo()) || "balanza_comercializadora".equals(p.getTipo()))
                        .findFirst()
                        .ifPresent(p -> {
                            if (p.getLlegada() == null) {
                                p.setLlegada(LocalDateTime.now());
                            }
                            p.setSalida(LocalDateTime.now());
                            p.setEstado("completado");
                            log.info("✅ Balanza destino marcada como 'completado'");
                        });
                break;

            case "LLEGADA_ALMACEN":
                tracking.getPuntosControl().stream()
                        .filter(p -> "almacen_ingenio".equals(p.getTipo()) || "almacen_comercializadora".equals(p.getTipo()))
                        .findFirst()
                        .ifPresent(p -> {
                            p.setEstado("en_punto");
                            if (p.getLlegada() == null) {
                                p.setLlegada(LocalDateTime.now());
                            }
                            log.info("✅ Almacén marcado como 'en_punto'");
                        });
                break;

            case "FIN_DESCARGA":
                tracking.getPuntosControl().stream()
                        .filter(p -> "almacen_ingenio".equals(p.getTipo()) || "almacen_comercializadora".equals(p.getTipo()))
                        .findFirst()
                        .ifPresent(p -> {
                            p.setEstado("completado");
                            if (p.getSalida() == null) {
                                p.setSalida(LocalDateTime.now());
                            }
                            log.info("✅ Almacén marcado como 'completado'");
                        });
                break;

            default:
                log.debug("Tipo de evento sin actualización de puntos: {}", tipoEvento);
        }
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
                .eventosEstado(tracking.getEventosEstado().stream()
                        .map(this::convertToEventoEstadoDto)
                        .collect(Collectors.toList()))
                .createdAt(tracking.getCreatedAt())
                .updatedAt(tracking.getUpdatedAt())
                .build();
    }

    private EventoEstadoDto convertToEventoEstadoDto(TrackingUbicacion.EventoEstado evento) {
        return EventoEstadoDto.builder()
                .timestamp(evento.getTimestamp())
                .estadoAnterior(evento.getEstadoAnterior())
                .estadoNuevo(evento.getEstadoNuevo())
                .lat(evento.getLat())
                .lng(evento.getLng())
                .tipoEvento(evento.getTipoEvento())
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

    @Transactional(readOnly = true)
    public HistorialPorEstadoDto getHistorialPorEstado(Integer asignacionCamionId) {
        log.info("📊 Obteniendo historial agrupado por estado - Asignación: {}", asignacionCamionId);

        TrackingUbicacion tracking = trackingRepository.findByAsignacionCamionId(asignacionCamionId)
                .orElseThrow(() -> new IllegalArgumentException("Tracking no encontrado"));

        List<TrackingUbicacion.PuntoUbicacion> historial = tracking.getHistorialUbicaciones();

        if (historial.isEmpty()) {
            return construirHistorialVacio(tracking);
        }

        Map<String, List<TrackingUbicacion.PuntoUbicacion>> historialPorEstado = historial.stream()
                .collect(Collectors.groupingBy(
                        punto -> punto.getEstadoViaje() != null ? punto.getEstadoViaje() : "Sin estado",
                        LinkedHashMap::new,
                        Collectors.toList()
                ));

        List<EstadoHistorialDto> estadosHistorial = historialPorEstado.entrySet().stream()
                .map(entry -> construirEstadoHistorial(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());

        return HistorialPorEstadoDto.builder()
                .asignacionCamionId(asignacionCamionId)
                .loteId(tracking.getLoteId())
                .codigoLote(tracking.getCodigoLote())
                .placaVehiculo(tracking.getPlacaVehiculo())
                .nombreTransportista(tracking.getNombreTransportista())
                .totalUbicaciones(historial.size())
                .estadosHistorial(estadosHistorial)
                .build();
    }

    private EstadoHistorialDto construirEstadoHistorial(String estadoViaje, List<TrackingUbicacion.PuntoUbicacion> ubicaciones) {
        if (ubicaciones.isEmpty()) {
            return EstadoHistorialDto.builder()
                    .estadoViaje(estadoViaje)
                    .totalUbicaciones(0)
                    .ubicaciones(List.of())
                    .build();
        }

        ubicaciones.sort(Comparator.comparing(TrackingUbicacion.PuntoUbicacion::getTimestamp));

        LocalDateTime inicioEstado = ubicaciones.getFirst().getTimestamp();
        LocalDateTime finEstado = ubicaciones.getLast().getTimestamp();
        long duracionSegundos = ChronoUnit.SECONDS.between(inicioEstado, finEstado);

        double distanciaTotal = 0.0;
        double velocidadMaxima = 0.0;
        long tiempoMovimiento = 0;
        int ubicacionesOffline = 0;

        for (int i = 1; i < ubicaciones.size(); i++) {
            TrackingUbicacion.PuntoUbicacion anterior = ubicaciones.get(i - 1);
            TrackingUbicacion.PuntoUbicacion actual = ubicaciones.get(i);

            double distancia = GeometryUtils.calcularDistancia(
                    anterior.getLat(), anterior.getLng(),
                    actual.getLat(), actual.getLng()
            );
            distanciaTotal += distancia;

            if (actual.getVelocidad() != null) {
                if (actual.getVelocidad() > velocidadMaxima) {
                    velocidadMaxima = actual.getVelocidad();
                }
                if (actual.getVelocidad() > 1.0) {
                    tiempoMovimiento += ChronoUnit.SECONDS.between(anterior.getTimestamp(), actual.getTimestamp());
                }
            }

            if (Boolean.TRUE.equals(actual.getEsOffline())) {
                ubicacionesOffline++;
            }
        }

        double velocidadPromedio = tiempoMovimiento > 0
                ? (distanciaTotal / tiempoMovimiento) * 3600
                : 0.0;

        List<UbicacionDto> ubicacionesDto = ubicaciones.stream()
                .map(p -> UbicacionDto.builder()
                        .lat(p.getLat())
                        .lng(p.getLng())
                        .timestamp(p.getTimestamp())
                        .precision(p.getPrecision())
                        .velocidad(p.getVelocidad())
                        .rumbo(p.getRumbo())
                        .altitud(p.getAltitud())
                        .esOffline(p.getEsOffline())
                        .build())
                .collect(Collectors.toList());

        return EstadoHistorialDto.builder()
                .estadoViaje(estadoViaje)
                .inicioEstado(inicioEstado)
                .finEstado(finEstado)
                .duracionSegundos(duracionSegundos)
                .totalUbicaciones(ubicaciones.size())
                .distanciaRecorridaKm(distanciaTotal)
                .velocidadPromedioKmH(velocidadPromedio)
                .velocidadMaximaKmH(velocidadMaxima)
                .ubicacionesOffline(ubicacionesOffline)
                .ubicaciones(ubicacionesDto)
                .build();
    }

    private HistorialPorEstadoDto construirHistorialVacio(TrackingUbicacion tracking) {
        return HistorialPorEstadoDto.builder()
                .asignacionCamionId(tracking.getAsignacionCamionId())
                .loteId(tracking.getLoteId())
                .codigoLote(tracking.getCodigoLote())
                .placaVehiculo(tracking.getPlacaVehiculo())
                .nombreTransportista(tracking.getNombreTransportista())
                .totalUbicaciones(0)
                .estadosHistorial(List.of())
                .build();
    }

    @Transactional(readOnly = true)
    public ResumenFinalTrackingDto getResumenFinalTracking(Integer asignacionCamionId) {
        log.info("📊 Obteniendo resumen final de tracking - Asignación: {}", asignacionCamionId);

        TrackingUbicacion tracking = trackingRepository.findByAsignacionCamionId(asignacionCamionId)
                .orElseThrow(() -> new IllegalArgumentException("Tracking no encontrado"));

        // Obtener última ubicación registrada
        UbicacionDto ubicacionFinal = null;
        if (!tracking.getHistorialUbicaciones().isEmpty()) {
            TrackingUbicacion.PuntoUbicacion ultimoPunto = tracking.getHistorialUbicaciones()
                    .stream()
                    .max(Comparator.comparing(TrackingUbicacion.PuntoUbicacion::getTimestamp))
                    .orElse(null);

            ubicacionFinal = UbicacionDto.builder()
                    .lat(ultimoPunto.getLat())
                    .lng(ultimoPunto.getLng())
                    .timestamp(ultimoPunto.getTimestamp())
                    .velocidad(ultimoPunto.getVelocidad())
                    .rumbo(ultimoPunto.getRumbo())
                    .altitud(ultimoPunto.getAltitud())
                    .build();
        } else if (tracking.getUbicacionActual() != null) {
            ubicacionFinal = UbicacionDto.builder()
                    .lat(tracking.getUbicacionActual().getLat())
                    .lng(tracking.getUbicacionActual().getLng())
                    .timestamp(tracking.getUbicacionActual().getTimestamp())
                    .velocidad(tracking.getUbicacionActual().getVelocidad())
                    .rumbo(tracking.getUbicacionActual().getRumbo())
                    .altitud(tracking.getUbicacionActual().getAltitud())
                    .build();
        }

        return ResumenFinalTrackingDto.builder()
                .asignacionCamionId(tracking.getAsignacionCamionId())
                .loteId(tracking.getLoteId())
                .codigoLote(tracking.getCodigoLote())
                .placaVehiculo(tracking.getPlacaVehiculo())
                .nombreTransportista(tracking.getNombreTransportista())
                .estadoViaje(tracking.getEstadoViaje())
                .ubicacionFinal(ubicacionFinal)
                .metricas(convertToMetricasDto(tracking.getMetricas()))
                .puntosControl(tracking.getPuntosControl().stream()
                        .map(this::convertToPuntoControlDto)
                        .collect(Collectors.toList()))
                .eventosEstado(tracking.getEventosEstado().stream()
                        .map(this::convertToEventoEstadoDto)
                        .collect(Collectors.toList()))
                .inicioViaje(tracking.getMetricas() != null ? tracking.getMetricas().getInicioViaje() : null)
                .finViaje(tracking.getMetricas() != null ? tracking.getMetricas().getFinViaje() : null)
                .createdAt(tracking.getCreatedAt())
                .updatedAt(tracking.getUpdatedAt())
                .build();
    }

    @Transactional(readOnly = true)
    public EvidenciasViajeDto getEvidenciasViaje(Integer asignacionCamionId) {
        log.info("📸 Obteniendo evidencias de viaje - Asignación: {}", asignacionCamionId);

        AsignacionCamion asignacion = asignacionCamionRepository.findById(asignacionCamionId)
                .orElseThrow(() -> new IllegalArgumentException("Asignación de camión no encontrada"));

        Lotes lote = asignacion.getLotesId();
        Transportista transportista = asignacion.getTransportistaId();
        Persona persona = personaRepository.findByUsuariosId(transportista.getUsuariosId()).orElse(null);

        String codigoLote = "LT-" + lote.getFechaCreacion().getYear() + "-" + String.format("%04d", lote.getId());

        EvidenciasViajeDto.EvidenciasViajeDtoBuilder builder = EvidenciasViajeDto.builder()
                .asignacionCamionId(asignacionCamionId)
                .loteId(lote.getId())
                .codigoLote(codigoLote)
                .estadoViaje(asignacion.getEstado())
                .numeroCamion(asignacion.getNumeroCamion())
                .placaVehiculo(transportista.getPlacaVehiculo())
                .nombreTransportista(persona != null ?
                        persona.getNombres() + " " + persona.getPrimerApellido() : "N/A");

        // Parsear las observaciones JSONB
        String observacionesJson = asignacion.getObservaciones();

        if (observacionesJson != null && !observacionesJson.isEmpty()) {
            try {
                JsonNode root = objectMapper.readTree(observacionesJson);

                // Inicio de viaje
                if (root.has("inicio_viaje")) {
                    builder.inicioViaje(parseInicioViaje(root.get("inicio_viaje")));
                }

                // Llegada a mina
                if (root.has("llegada_mina")) {
                    builder.llegadaMina(parseLlegadaMina(root.get("llegada_mina")));
                }

                // Carguío completo
                if (root.has("carguio_completo")) {
                    builder.carguioCompleto(parseCarguioCompleto(root.get("carguio_completo")));
                }

                // Pesaje origen
                if (root.has("pesaje_origen")) {
                    builder.pesajeOrigen(parsePesaje(root.get("pesaje_origen")));
                }

                // Pesaje destino
                if (root.has("pesaje_destino")) {
                    builder.pesajeDestino(parsePesaje(root.get("pesaje_destino")));
                }

                // Llegada almacén
                if (root.has("llegada_almacen")) {
                    builder.llegadaAlmacen(parseLlegadaAlmacen(root.get("llegada_almacen")));
                }

                // Descarga iniciada
                if (root.has("descarga_iniciada")) {
                    builder.descargaIniciada(parseDescargaIniciada(root.get("descarga_iniciada")));
                }

                // Ruta finalizada
                if (root.has("ruta_finalizada")) {
                    builder.rutaFinalizada(parseRutaFinalizada(root.get("ruta_finalizada")));
                }

            } catch (Exception e) {
                log.error("❌ Error al parsear observaciones JSON: {}", e.getMessage(), e);
            }
        }

        return builder.build();
    }

    private EvidenciasViajeDto.EvidenciaInicioViaje parseInicioViaje(JsonNode node) {
        return EvidenciasViajeDto.EvidenciaInicioViaje.builder()
                .lat(node.has("lat") ? node.get("lat").asDouble() : null)
                .lng(node.has("lng") ? node.get("lng").asDouble() : null)
                .timestamp(node.has("timestamp") ? LocalDateTime.parse(node.get("timestamp").asText()) : null)
                .usuarioId(node.has("usuario_id") ? node.get("usuario_id").asInt() : null)
                .dispositivo(node.has("dispositivo") ? node.get("dispositivo").asText() : null)
                .build();
    }

    private EvidenciasViajeDto.EvidenciaLlegadaMina parseLlegadaMina(JsonNode node) {
        return EvidenciasViajeDto.EvidenciaLlegadaMina.builder()
                .lat(node.has("lat") ? node.get("lat").asDouble() : null)
                .lng(node.has("lng") ? node.get("lng").asDouble() : null)
                .timestamp(node.has("timestamp") ? LocalDateTime.parse(node.get("timestamp").asText()) : null)
                .observaciones(node.has("observaciones") ? node.get("observaciones").asText() : null)
                .palaOperativa(node.has("pala_operativa") ? node.get("pala_operativa").asBoolean() : null)
                .mineralVisible(node.has("mineral_visible") ? node.get("mineral_visible").asBoolean() : null)
                .fotoReferenciaUrl(node.has("foto_referencia_url") ? node.get("foto_referencia_url").asText() : null) // ⭐ AGREGAR ESTA LÍNEA
                .build();
    }

    private EvidenciasViajeDto.EvidenciaCarguioCompleto parseCarguioCompleto(JsonNode node) {
        return EvidenciasViajeDto.EvidenciaCarguioCompleto.builder()
                .lat(node.has("lat") ? node.get("lat").asDouble() : null)
                .lng(node.has("lng") ? node.get("lng").asDouble() : null)
                .timestamp(node.has("timestamp") ? LocalDateTime.parse(node.get("timestamp").asText()) : null)
                .fotoCamionCargadoUrl(node.has("foto_camion_cargado_url") ?
                        node.get("foto_camion_cargado_url").asText() : null)
                .mineralCargadoCompletamente(node.has("mineral_cargado_completamente") ?
                        node.get("mineral_cargado_completamente").asBoolean() : null)
                .build();
    }

    private EvidenciasViajeDto.EvidenciaPesaje parsePesaje(JsonNode node) {
        return EvidenciasViajeDto.EvidenciaPesaje.builder()
                .lat(node.has("lat") ? node.get("lat").asDouble() : null)
                .lng(node.has("lng") ? node.get("lng").asDouble() : null)
                .timestamp(node.has("timestamp") ? LocalDateTime.parse(node.get("timestamp").asText()) : null)
                .pesoBrutoKg(node.has("peso_bruto_kg") ? node.get("peso_bruto_kg").asDouble() : null)
                .pesoTaraKg(node.has("peso_tara_kg") ? node.get("peso_tara_kg").asDouble() : null)
                .pesoNetoKg(node.has("peso_neto_kg") ? node.get("peso_neto_kg").asDouble() : null)
                .ticketPesajeUrl(node.has("ticket_pesaje_url") ? node.get("ticket_pesaje_url").asText() : null)
                .observaciones(node.has("observaciones") ? node.get("observaciones").asText() : null)
                .build();
    }

    private EvidenciasViajeDto.EvidenciaLlegadaAlmacen parseLlegadaAlmacen(JsonNode node) {
        return EvidenciasViajeDto.EvidenciaLlegadaAlmacen.builder()
                .lat(node.has("lat") ? node.get("lat").asDouble() : null)
                .lng(node.has("lng") ? node.get("lng").asDouble() : null)
                .timestamp(node.has("timestamp") ? LocalDateTime.parse(node.get("timestamp").asText()) : null)
                .observaciones(node.has("observaciones") ? node.get("observaciones").asText() : null)
                .confirmacionLlegada(node.has("confirmacion_llegada") ?
                        node.get("confirmacion_llegada").asBoolean() : null)
                .build();
    }

    private EvidenciasViajeDto.EvidenciaDescargaIniciada parseDescargaIniciada(JsonNode node) {
        return EvidenciasViajeDto.EvidenciaDescargaIniciada.builder()
                .lat(node.has("lat") ? node.get("lat").asDouble() : null)
                .lng(node.has("lng") ? node.get("lng").asDouble() : null)
                .timestamp(node.has("timestamp") ? LocalDateTime.parse(node.get("timestamp").asText()) : null)
                .observaciones(node.has("observaciones") ? node.get("observaciones").asText() : null)
                .build();
    }

    private EvidenciasViajeDto.EvidenciaRutaFinalizada parseRutaFinalizada(JsonNode node) {
        return EvidenciasViajeDto.EvidenciaRutaFinalizada.builder()
                .lat(node.has("lat") ? node.get("lat").asDouble() : null)
                .lng(node.has("lng") ? node.get("lng").asDouble() : null)
                .timestamp(node.has("timestamp") ? LocalDateTime.parse(node.get("timestamp").asText()) : null)
                .observacionesFinales(node.has("observaciones_finales") ?
                        node.get("observaciones_finales").asText() : null)
                .build();
    }
}