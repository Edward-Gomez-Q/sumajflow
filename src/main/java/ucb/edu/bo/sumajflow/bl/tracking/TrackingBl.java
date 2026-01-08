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

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Servicio principal de tracking GPS
 * Maneja la lógica de ubicaciones, geofencing y sincronización
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TrackingBl {

    private final TrackingUbicacionRepository trackingRepository;
    private final AsignacionCamionRepository asignacionCamionRepository;
    private final LotesRepository lotesRepository;
    private final PersonaRepository personaRepository;
    private final GeoUtilsService geoUtils;

    // Umbral de tiempo para considerar offline (5 minutos)
    private static final long OFFLINE_THRESHOLD_MINUTES = 5;

    // ==================== INICIAR TRACKING ====================

    /**
     * Inicia el tracking para una asignación de camión
     */
    @Transactional
    public TrackingResponseDto iniciarTracking(Integer asignacionCamionId, Double latInicial, Double lngInicial) {
        log.info("Iniciando tracking para asignación ID: {}", asignacionCamionId);

        // Validar que la asignación existe
        AsignacionCamion asignacion = asignacionCamionRepository.findById(asignacionCamionId)
                .orElseThrow(() -> new IllegalArgumentException("Asignación de camión no encontrada"));

        // Verificar que no exista ya un tracking
        if (trackingRepository.existsByAsignacionCamionId(asignacionCamionId)) {
            log.warn("Ya existe tracking para la asignación ID: {}", asignacionCamionId);
            return getTrackingByAsignacion(asignacionCamionId);
        }

        // Obtener información relacionada
        Lotes lote = asignacion.getLotesId();
        Transportista transportista = asignacion.getTransportistaId();
        Persona persona = personaRepository.findByUsuariosId(transportista.getUsuariosId()).orElse(null);

        // Construir puntos de control según la ruta
        List<TrackingUbicacion.PuntoControl> puntosControl = construirPuntosControl(lote, asignacion);

        // Crear ubicación inicial si se proporcionó
        TrackingUbicacion.UbicacionActual ubicacionActual = null;
        if (latInicial != null && lngInicial != null && geoUtils.esUbicacionValida(latInicial, lngInicial)) {
            ubicacionActual = TrackingUbicacion.UbicacionActual.builder()
                    .lat(latInicial)
                    .lng(lngInicial)
                    .location(TrackingUbicacion.GeoJsonPoint.of(latInicial, lngInicial))
                    .timestamp(LocalDateTime.now())
                    .velocidad(0.0)
                    .build();
        }

        // Crear documento de tracking
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
                        .build())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        tracking = trackingRepository.save(tracking);
        log.info("Tracking iniciado exitosamente - ID: {}", tracking.getId());

        return convertToResponseDto(tracking);
    }

    // ==================== ACTUALIZAR UBICACIÓN ====================

    /**
     * Actualiza la ubicación actual de un camión
     */
    @Transactional
    public ActualizacionUbicacionResponseDto actualizarUbicacion(ActualizarUbicacionDto dto) {
        log.debug("Actualizando ubicación - Asignación ID: {}, Lat: {}, Lng: {}",
                dto.getAsignacionCamionId(), dto.getLat(), dto.getLng());

        // Validar ubicación
        if (!geoUtils.esUbicacionValida(dto.getLat(), dto.getLng())) {
            throw new IllegalArgumentException("Ubicación inválida");
        }

        // Obtener tracking existente o crear uno nuevo
        TrackingUbicacion tracking = trackingRepository.findByAsignacionCamionId(dto.getAsignacionCamionId())
                .orElseGet(() -> {
                    log.info("Tracking no encontrado, creando nuevo...");
                    TrackingResponseDto nuevoTracking = iniciarTracking(
                            dto.getAsignacionCamionId(), dto.getLat(), dto.getLng());
                    return trackingRepository.findByAsignacionCamionId(dto.getAsignacionCamionId())
                            .orElseThrow(() -> new IllegalStateException("Error al crear tracking"));
                });

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
                    .build();

            tracking.getHistorialUbicaciones().add(puntoHistorial);

            // Actualizar métricas
            actualizarMetricas(tracking, dto);
        }

        // Determinar timestamp
        LocalDateTime timestamp = dto.getTimestampCaptura() != null
                ? dto.getTimestampCaptura()
                : LocalDateTime.now();

        // Actualizar ubicación actual
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

        // Verificar geofencing
        GeofencingStatusDto geofencingStatus = verificarGeofencing(tracking, dto.getLat(), dto.getLng());

        // Guardar
        trackingRepository.save(tracking);

        // Construir respuesta
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

    // ==================== SINCRONIZACIÓN OFFLINE ====================

    /**
     * Sincroniza ubicaciones capturadas offline
     */
    @Transactional
    public SincronizacionResponseDto sincronizarUbicacionesOffline(
            Integer asignacionCamionId,
            List<UbicacionOfflineDto> ubicaciones) {

        log.info("Sincronizando {} ubicaciones offline para asignación ID: {}",
                ubicaciones.size(), asignacionCamionId);

        TrackingUbicacion tracking = trackingRepository.findByAsignacionCamionId(asignacionCamionId)
                .orElseThrow(() -> new IllegalArgumentException("Tracking no encontrado"));

        int sincronizadas = 0;
        int fallidas = 0;
        List<String> errores = new ArrayList<>();

        // Ordenar por timestamp
        ubicaciones.sort(Comparator.comparing(UbicacionOfflineDto::getTimestamp));

        for (UbicacionOfflineDto ubicacion : ubicaciones) {
            try {
                if (!geoUtils.esUbicacionValida(ubicacion.getLat(), ubicacion.getLng())) {
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
                        .build();

                tracking.getHistorialUbicaciones().add(punto);
                sincronizadas++;

            } catch (Exception e) {
                errores.add("Error en ubicación " + ubicacion.getTimestamp() + ": " + e.getMessage());
                fallidas++;
            }
        }

        // Actualizar última ubicación si hay datos
        if (!ubicaciones.isEmpty()) {
            UbicacionOfflineDto ultima = ubicaciones.get(ubicaciones.size() - 1);
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

            // Recalcular métricas
            recalcularMetricas(tracking);
        }

        tracking.setEstadoConexion("online");
        tracking.setUltimaSincronizacion(LocalDateTime.now());
        tracking.setUpdatedAt(LocalDateTime.now());
        tracking.getUbicacionesPendientesSincronizar().clear();

        trackingRepository.save(tracking);

        log.info("Sincronización completada - Éxito: {}, Fallidas: {}", sincronizadas, fallidas);

        return SincronizacionResponseDto.builder()
                .success(fallidas == 0)
                .ubicacionesSincronizadas(sincronizadas)
                .ubicacionesFallidas(fallidas)
                .errores(errores)
                .ultimaSincronizacion(LocalDateTime.now())
                .build();
    }

    // ==================== GEOFENCING ====================

    /**
     * Verifica el estado de geofencing para una ubicación
     */
    public GeofencingStatusDto verificarGeofencing(TrackingUbicacion tracking, double lat, double lng) {
        GeofencingStatusDto.GeofencingStatusDtoBuilder builder = GeofencingStatusDto.builder()
                .dentroDeZona(false)
                .puedeRegistrarLlegada(false)
                .puedeRegistrarSalida(false);

        if (tracking.getPuntosControl() == null || tracking.getPuntosControl().isEmpty()) {
            return builder.build();
        }

        // Buscar si está dentro de alguna zona
        for (TrackingUbicacion.PuntoControl punto : tracking.getPuntosControl()) {
            double distancia = geoUtils.calcularDistanciaMetros(lat, lng, punto.getLat(), punto.getLng());

            if (distancia <= punto.getRadio()) {
                builder.dentroDeZona(true)
                        .zonaNombre(punto.getNombre())
                        .zonaTipo(punto.getTipo())
                        .distanciaAZona(distancia);

                // Determinar acciones posibles
                if ("pendiente".equals(punto.getEstado()) || punto.getLlegada() == null) {
                    builder.puedeRegistrarLlegada(true);
                } else if (punto.getLlegada() != null && punto.getSalida() == null) {
                    builder.puedeRegistrarSalida(true);
                }
                break;
            }
        }

        // Encontrar próximo punto pendiente
        TrackingUbicacion.PuntoControl proximoPunto = geoUtils.encontrarProximoPuntoControlPendiente(tracking.getPuntosControl());
        if (proximoPunto != null) {
            double distanciaProximo = geoUtils.calcularDistanciaMetros(lat, lng, proximoPunto.getLat(), proximoPunto.getLng());
            builder.proximoPuntoControl(proximoPunto.getNombre())
                    .distanciaProximoPunto(distanciaProximo);
        }

        return builder.build();
    }

    /**
     * Registra llegada a un punto de control
     */
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

        // Buscar el punto de control
        TrackingUbicacion.PuntoControl punto = tracking.getPuntosControl().stream()
                .filter(p -> p.getTipo().equals(tipoPunto))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Punto de control no encontrado: " + tipoPunto));

        // Validar que está cerca del punto (si se proporciona ubicación)
        if (lat != null && lng != null) {
            double distancia = geoUtils.calcularDistanciaMetros(lat, lng, punto.getLat(), punto.getLng());
            if (distancia > punto.getRadio() * 1.5) { // 50% de tolerancia extra
                throw new IllegalArgumentException(
                        "Estás demasiado lejos del punto de control (" + (int) distancia + "m). " +
                                "Debes estar a menos de " + punto.getRadio() + "m");
            }
        }

        // Registrar llegada
        punto.setLlegada(LocalDateTime.now());
        punto.setEstado("en_punto");

        tracking.setUpdatedAt(LocalDateTime.now());
        trackingRepository.save(tracking);

        log.info("Llegada registrada exitosamente a: {}", tipoPunto);

        return convertToResponseDto(tracking);
    }

    /**
     * Registra salida de un punto de control
     */
    @Transactional
    public TrackingResponseDto registrarSalidaPuntoControl(
            Integer asignacionCamionId,
            String tipoPunto,
            String observaciones) {

        log.info("Registrando salida de {} para asignación ID: {}", tipoPunto, asignacionCamionId);

        TrackingUbicacion tracking = trackingRepository.findByAsignacionCamionId(asignacionCamionId)
                .orElseThrow(() -> new IllegalArgumentException("Tracking no encontrado"));

        // Buscar el punto de control
        TrackingUbicacion.PuntoControl punto = tracking.getPuntosControl().stream()
                .filter(p -> p.getTipo().equals(tipoPunto))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Punto de control no encontrado: " + tipoPunto));

        // Validar que ya registró llegada
        if (punto.getLlegada() == null) {
            throw new IllegalArgumentException("Debe registrar llegada antes de la salida");
        }

        // Registrar salida
        punto.setSalida(LocalDateTime.now());
        punto.setEstado("completado");

        tracking.setUpdatedAt(LocalDateTime.now());
        trackingRepository.save(tracking);

        log.info("Salida registrada exitosamente de: {}", tipoPunto);

        return convertToResponseDto(tracking);
    }

    // ==================== CONSULTAS ====================

    /**
     * Obtiene el tracking de una asignación
     */
    @Transactional(readOnly = true)
    public TrackingResponseDto getTrackingByAsignacion(Integer asignacionCamionId) {
        TrackingUbicacion tracking = trackingRepository.findByAsignacionCamionId(asignacionCamionId)
                .orElseThrow(() -> new IllegalArgumentException("Tracking no encontrado para esta asignación"));

        // Verificar estado de conexión
        actualizarEstadoConexion(tracking);

        return convertToResponseDto(tracking);
    }

    /**
     * Obtiene todos los trackings activos de un lote
     */
    @Transactional(readOnly = true)
    public MonitoreoLoteDto getMonitoreoLote(Integer loteId) {
        log.debug("Obteniendo monitoreo para lote ID: {}", loteId);

        Lotes lote = lotesRepository.findById(loteId)
                .orElseThrow(() -> new IllegalArgumentException("Lote no encontrado"));

        List<TrackingUbicacion> trackings = trackingRepository.findByLoteId(loteId);

        // Convertir a DTOs de camiones
        List<CamionEnRutaDto> camiones = trackings.stream()
                .map(this::convertToCamionEnRutaDto)
                .collect(Collectors.toList());

        // Estadísticas
        long enRuta = camiones.stream()
                .filter(c -> !"completado".equals(c.getEstadoViaje()) && !"cancelado".equals(c.getEstadoViaje()))
                .count();
        long completados = camiones.stream()
                .filter(c -> "Viaje terminado".equals(c.getEstadoViaje()))
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

    /**
     * Obtiene el historial de ubicaciones de una asignación
     */
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

        // Agregar ubicación actual al final
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

    // ==================== MÉTODOS AUXILIARES PRIVADOS ====================

    /**
     * Construye los puntos de control según el tipo de operación del lote
     */
    private List<TrackingUbicacion.PuntoControl> construirPuntosControl(Lotes lote, AsignacionCamion asignacion) {
        List<TrackingUbicacion.PuntoControl> puntos = new ArrayList<>();
        int orden = 1;

        // 1. Mina (origen)
        Minas mina = lote.getMinasId();
        puntos.add(TrackingUbicacion.PuntoControl.builder()
                .tipo("mina")
                .nombre(mina.getNombre())
                .lat(mina.getLatitud().doubleValue())
                .lng(mina.getLongitud().doubleValue())
                .radio(100) // 100 metros de radio
                .orden(orden++)
                .requerido(true)
                .estado("pendiente")
                .build());

        // 2. Balanza cooperativa
        // TODO: Obtener la balanza de la cooperativa del socio
        Sectores sector = mina.getSectoresId();
        Cooperativa cooperativa = sector.getCooperativaId();
        // Por ahora usamos coordenadas de la cooperativa
        if (cooperativa.getLatitud() != null && cooperativa.getLongitud() != null) {
            puntos.add(TrackingUbicacion.PuntoControl.builder()
                    .tipo("balanza_cooperativa")
                    .nombre("Balanza " + cooperativa.getRazonSocial())
                    .lat(cooperativa.getLatitud().doubleValue())
                    .lng(cooperativa.getLongitud().doubleValue())
                    .radio(50)
                    .orden(orden++)
                    .requerido(true)
                    .estado("pendiente")
                    .build());
        }

        // 3. Balanza y almacén destino (según tipo de operación)
        String tipoOperacion = lote.getTipoOperacion();

        if ("procesamiento_planta".equals(tipoOperacion)) {
            // Destino: Ingenio
            // TODO: Obtener ingenio del lote_ingenio
            puntos.add(TrackingUbicacion.PuntoControl.builder()
                    .tipo("balanza_ingenio")
                    .nombre("Balanza Ingenio")
                    .lat(-19.5750) // Placeholder - obtener de la BD
                    .lng(-65.7400)
                    .radio(50)
                    .orden(orden++)
                    .requerido(true)
                    .estado("pendiente")
                    .build());

            puntos.add(TrackingUbicacion.PuntoControl.builder()
                    .tipo("almacen_ingenio")
                    .nombre("Almacén Ingenio")
                    .lat(-19.5750)
                    .lng(-65.7400)
                    .radio(100)
                    .orden(orden++)
                    .requerido(true)
                    .estado("pendiente")
                    .build());

        } else if ("venta_directa".equals(tipoOperacion)) {
            // Destino: Comercializadora
            puntos.add(TrackingUbicacion.PuntoControl.builder()
                    .tipo("balanza_comercializadora")
                    .nombre("Balanza Comercializadora")
                    .lat(-19.5700)
                    .lng(-65.7350)
                    .radio(50)
                    .orden(orden++)
                    .requerido(true)
                    .estado("pendiente")
                    .build());

            puntos.add(TrackingUbicacion.PuntoControl.builder()
                    .tipo("almacen_comercializadora")
                    .nombre("Almacén Comercializadora")
                    .lat(-19.5700)
                    .lng(-65.7350)
                    .radio(100)
                    .orden(orden++)
                    .requerido(true)
                    .estado("pendiente")
                    .build());
        }

        return puntos;
    }

    /**
     * Actualiza las métricas del viaje con una nueva ubicación
     */
    private void actualizarMetricas(TrackingUbicacion tracking, ActualizarUbicacionDto dto) {
        TrackingUbicacion.MetricasViaje metricas = tracking.getMetricas();
        TrackingUbicacion.UbicacionActual ubicacionAnterior = tracking.getUbicacionActual();

        if (ubicacionAnterior == null || metricas == null) {
            return;
        }

        // Calcular distancia desde última ubicación
        double distancia = geoUtils.calcularDistancia(
                ubicacionAnterior.getLat(), ubicacionAnterior.getLng(),
                dto.getLat(), dto.getLng());

        metricas.setDistanciaRecorrida(metricas.getDistanciaRecorrida() + distancia);

        // Calcular tiempo transcurrido
        if (ubicacionAnterior.getTimestamp() != null) {
            long segundosTranscurridos = ChronoUnit.SECONDS.between(
                    ubicacionAnterior.getTimestamp(),
                    dto.getTimestampCaptura() != null ? dto.getTimestampCaptura() : LocalDateTime.now());

            // Determinar si estaba en movimiento o detenido
            if (dto.getVelocidad() != null && dto.getVelocidad() > 1.0) { // > 1 km/h = en movimiento
                metricas.setTiempoEnMovimiento(metricas.getTiempoEnMovimiento() + segundosTranscurridos);
            } else {
                metricas.setTiempoDetenido(metricas.getTiempoDetenido() + segundosTranscurridos);
            }
        }

        // Actualizar velocidad máxima
        if (dto.getVelocidad() != null && dto.getVelocidad() > metricas.getVelocidadMaxima()) {
            metricas.setVelocidadMaxima(dto.getVelocidad());
        }

        // Calcular velocidad promedio
        long tiempoTotal = metricas.getTiempoEnMovimiento();
        if (tiempoTotal > 0) {
            double velocidadPromedio = (metricas.getDistanciaRecorrida() / tiempoTotal) * 3600;
            metricas.setVelocidadPromedio(velocidadPromedio);
        }
    }

    /**
     * Recalcula las métricas del viaje completo (después de sincronización offline)
     */
    private void recalcularMetricas(TrackingUbicacion tracking) {
        List<TrackingUbicacion.PuntoUbicacion> historial = tracking.getHistorialUbicaciones();
        if (historial.isEmpty()) {
            return;
        }

        // Ordenar por timestamp
        historial.sort(Comparator.comparing(TrackingUbicacion.PuntoUbicacion::getTimestamp));

        double distanciaTotal = 0;
        long tiempoMovimiento = 0;
        long tiempoDetenido = 0;
        double velocidadMaxima = 0;

        for (int i = 1; i < historial.size(); i++) {
            TrackingUbicacion.PuntoUbicacion anterior = historial.get(i - 1);
            TrackingUbicacion.PuntoUbicacion actual = historial.get(i);

            // Distancia
            double distancia = geoUtils.calcularDistancia(
                    anterior.getLat(), anterior.getLng(),
                    actual.getLat(), actual.getLng());
            distanciaTotal += distancia;

            // Tiempo
            long segundos = ChronoUnit.SECONDS.between(anterior.getTimestamp(), actual.getTimestamp());
            if (actual.getVelocidad() != null && actual.getVelocidad() > 1.0) {
                tiempoMovimiento += segundos;
            } else {
                tiempoDetenido += segundos;
            }

            // Velocidad máxima
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

    /**
     * Actualiza el estado de conexión basado en última sincronización
     */
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

    /**
     * Determina qué acción se requiere según el estado de geofencing
     */
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

    // ==================== CONVERSIONES ====================

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
                .tiempoTranscurrido(geoUtils.formatearDuracion(tiempoTotal))
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

        // Próximo punto
        TrackingUbicacion.PuntoControl proximoPunto =
                geoUtils.encontrarProximoPuntoControlPendiente(tracking.getPuntosControl());
        if (proximoPunto != null && tracking.getUbicacionActual() != null) {
            double distancia = geoUtils.calcularDistanciaMetros(
                    tracking.getUbicacionActual().getLat(),
                    tracking.getUbicacionActual().getLng(),
                    proximoPunto.getLat(),
                    proximoPunto.getLng());

            builder.proximoPuntoControl(proximoPunto.getNombre())
                    .distanciaProximoPunto(distancia);

            double velocidad = tracking.getUbicacionActual().getVelocidad() != null
                    ? tracking.getUbicacionActual().getVelocidad() : 30.0;
            long tiempoEstimado = geoUtils.calcularTiempoEstimadoLlegada(distancia / 1000, velocidad);
            builder.tiempoEstimadoLlegada(geoUtils.formatearDuracion(tiempoEstimado));
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
                .puntoInicio(ubicaciones.get(0))
                .puntoFin(ubicaciones.get(ubicaciones.size() - 1))
                .distanciaTotal(tracking.getMetricas().getDistanciaRecorrida())
                .duracionTotal(tracking.getMetricas().getTiempoEnMovimiento() + tracking.getMetricas().getTiempoDetenido())
                .puntosVisitados(puntosVisitados)
                .build();
    }
}