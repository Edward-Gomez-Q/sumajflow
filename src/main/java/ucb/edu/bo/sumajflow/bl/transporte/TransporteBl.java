package ucb.edu.bo.sumajflow.bl.transporte;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ucb.edu.bo.sumajflow.bl.NotificacionBl;
import ucb.edu.bo.sumajflow.bl.cooperativa.AuditoriaLotesBl;
import ucb.edu.bo.sumajflow.bl.routing.RoutingService;
import ucb.edu.bo.sumajflow.bl.tracking.TrackingBl;
import ucb.edu.bo.sumajflow.dto.routing.RutaCalculadaDto;
import ucb.edu.bo.sumajflow.dto.tracking.LoteAsignadoResumenDto;
import ucb.edu.bo.sumajflow.dto.tracking.LoteDetalleViajeDto;
import ucb.edu.bo.sumajflow.dto.transporte.*;
import ucb.edu.bo.sumajflow.entity.*;
import ucb.edu.bo.sumajflow.repository.*;
import ucb.edu.bo.sumajflow.utils.GeometryUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransporteBl {

    private final AsignacionCamionRepository asignacionCamionRepository;
    private final LotesRepository lotesRepository;
    private final PesajesRepository pesajesRepository;
    private final TransportistaRepository transportistaRepository;
    private final AuditoriaLotesBl auditoriaLotesBl;
    private final NotificacionBl notificacionBl;
    private final RoutingService routingService;
    private final TrackingBl trackingBl;
    private final ObjectMapper objectMapper;

    // Radio de tolerancia para geofencing (metros)
    private static final int RADIO_MINA = 200;
    private static final int RADIO_BALANZA = 100;
    private static final int RADIO_ALMACEN = 150;

    // Flujo de estados unificado para asignacion_camion
    private static final Map<String, String> FLUJO_ESTADOS = Map.of(
            "Esperando iniciar", "En camino a la mina",
            "En camino a la mina", "Esperando carguío",
            "Esperando carguío", "En camino balanza cooperativa",
            "En camino balanza cooperativa", "En camino balanza destino",
            "En camino balanza destino", "En camino almacén destino",
            "En camino almacén destino", "Descargando",
            "Descargando", "Completado"
    );

    // Obtiene los lotes asignados al transportista según filtro
    public List<LoteAsignadoResumenDto> obtenerLotesTransportista(Integer usuarioId, String filtro) {
        var transportista = transportistaRepository.findByUsuariosId_Id(usuarioId)
                .orElseThrow(() -> new IllegalArgumentException("Transportista no encontrado"));

        List<AsignacionCamion> asignaciones;

        if ("activos".equals(filtro)) {
            asignaciones = asignacionCamionRepository.findByTransportistaIdAndEstadoNotIn(
                    transportista,
                    List.of("Completado", "Cancelado por rechazo")
            );
        } else if ("completados".equals(filtro)) {
            asignaciones = asignacionCamionRepository.findByTransportistaIdAndEstado(
                    transportista,
                    "Completado"
            );
        } else {
            asignaciones = asignacionCamionRepository.findByTransportistaId(transportista);
        }

        return asignaciones.stream()
                .map(this::convertToLoteResumen)
                .collect(Collectors.toList());
    }

    // Obtiene el detalle completo de un lote asignado para iniciar el viaje
    public LoteDetalleViajeDto obtenerDetalleLoteParaViaje(Integer asignacionId, Integer usuarioId) {
        AsignacionCamion asignacion = asignacionCamionRepository.findById(asignacionId)
                .orElseThrow(() -> new IllegalArgumentException("Asignación no encontrada"));

        if (!asignacion.getTransportistaId().getUsuariosId().getId().equals(usuarioId)) {
            throw new SecurityException("No tienes permiso para ver esta asignación");
        }

        return construirDetalleViaje(asignacion);
    }

    /**
     * Inicia el viaje: "Esperando iniciar" -> "En camino a la mina"
     * Versión simplificada y robusta
     */
    @Transactional
    public TransicionEstadoResponseDto iniciarViaje(
            Integer asignacionId,
            Double latInicial,
            Double lngInicial,
            String observaciones,
            Integer usuarioId
    ) {
        log.info("=== INICIO: iniciarViaje - Asignacion: {}, Usuario: {} ===", asignacionId, usuarioId);

        try {
            // 1. Validaciones básicas
            validarCoordenadas(latInicial, lngInicial);

            // 2. Obtener y validar asignación
            AsignacionCamion asignacion = obtenerYValidarAsignacion(asignacionId, usuarioId);
            validarEstado(asignacion, "Esperando iniciar");

            // 3. Validar transportista
            Transportista transportista = asignacion.getTransportistaId();
            if (!"aprobado".equals(transportista.getEstado())) {
                throw new IllegalStateException(
                        "No puedes iniciar el viaje. Estado del transportista: " + transportista.getEstado()
                );
            }

            String estadoAnterior = asignacion.getEstado();
            LocalDateTime ahora = LocalDateTime.now();

            // 4. Actualizar estado de la asignación
            asignacion.setEstado("En camino a la mina");
            asignacion.setFechaInicio(ahora);

            // 5. Registrar información del inicio en observaciones
            registrarInicioEnObservaciones(asignacion, latInicial, lngInicial, observaciones, usuarioId, ahora);

            // 6. Cambiar estado del transportista
            transportista.setEstado("en_viaje");
            transportistaRepository.save(transportista);

            // 7. Guardar asignación
            asignacionCamionRepository.save(asignacion);

            log.info("=== Estado actualizado: {} -> {} ===", estadoAnterior, "En camino a la mina");

            try {
                trackingBl.actualizarEstadoYRegistrarEvento(
                        asignacionId,
                        estadoAnterior,
                        "En camino a la mina",
                        "INICIO_VIAJE",
                        latInicial,
                        lngInicial
                );
                log.info("✅ Estado actualizado en MongoDB");
            } catch (Exception e) {
                log.error("⚠️ Error al actualizar MongoDB (no crítico): {}", e.getMessage());
                // No bloqueamos el flujo si falla MongoDB
            }

            // 8. Operaciones async (no bloquean la respuesta)
            ejecutarOperacionesAsyncInicioViaje(asignacionId, latInicial, lngInicial, asignacion, estadoAnterior, usuarioId);

            // 9. Construir y retornar respuesta
            TransicionEstadoResponseDto response = construirRespuestaTransicion(
                    asignacion,
                    estadoAnterior,
                    "En camino a la mina",
                    "Dirigete a la mina para iniciar la carga del mineral"
            );

            log.info("=== FIN: iniciarViaje exitoso ===");
            return response;

        } catch (IllegalArgumentException | IllegalStateException | SecurityException e) {
            log.error("Error de validacion al iniciar viaje: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Error inesperado al iniciar viaje", e);
            throw new RuntimeException("Error al iniciar el viaje: " + e.getMessage(), e);
        }
    }

    /**
     * Valida que las coordenadas sean correctas
     */
    private void validarCoordenadas(Double lat, Double lng) {
        if (lat == null || lng == null) {
            throw new IllegalArgumentException("Las coordenadas GPS son obligatorias");
        }
        if (Math.abs(lat) > 90 || Math.abs(lng) > 180) {
            throw new IllegalArgumentException("Coordenadas GPS invalidas");
        }
    }

    /**
     * Registra la información del inicio en las observaciones
     */
    private void registrarInicioEnObservaciones(
            AsignacionCamion asignacion,
            Double latInicial,
            Double lngInicial,
            String observaciones,
            Integer usuarioId,
            LocalDateTime ahora
    ) {
        try {
            Map<String, Object> obs = obtenerObservaciones(asignacion);
            Map<String, Object> inicioViaje = new HashMap<>();

            inicioViaje.put("timestamp", ahora.toString());
            inicioViaje.put("lat", latInicial);
            inicioViaje.put("lng", lngInicial);
            inicioViaje.put("usuario_id", usuarioId);
            inicioViaje.put("dispositivo", "app_movil");

            if (observaciones != null && !observaciones.trim().isEmpty()) {
                inicioViaje.put("observaciones", observaciones.trim());
            }

            obs.put("inicio_viaje", inicioViaje);
            actualizarObservaciones(asignacion, obs);

        } catch (Exception e) {
            log.error("Error al registrar observaciones de inicio: {}", e.getMessage());
            // No lanzamos excepción, solo registramos el error
        }
    }

    /**
     * Ejecuta operaciones asíncronas que no deben bloquear la respuesta
     */
    private void ejecutarOperacionesAsyncInicioViaje(
            Integer asignacionId,
            Double latInicial,
            Double lngInicial,
            AsignacionCamion asignacion,
            String estadoAnterior,
            Integer usuarioId
    ) {
        // Estas operaciones se ejecutan en segundo plano
        // Si fallan, no afectan la respuesta principal

        // 1. Iniciar tracking GPS
        try {
            trackingBl.iniciarTracking(asignacionId, latInicial, lngInicial);
            log.info("Tracking GPS iniciado para asignacion: {}", asignacionId);
        } catch (Exception e) {
            log.error("Error al iniciar tracking GPS (no critico): {}", e.getMessage());
        }

        // 2. Actualizar estado del lote
        try {
            actualizarEstadoLote(asignacion.getLotesId());
        } catch (Exception e) {
            log.error("Error al actualizar estado del lote (no critico): {}", e.getMessage());
        }

        // 3. Registrar auditoría
        try {
            registrarAuditoriaInicioViaje(
                    asignacion.getLotesId(),
                    asignacion,
                    estadoAnterior,
                    usuarioId
            );
        } catch (Exception e) {
            log.error("Error al registrar auditoria (no critico): {}", e.getMessage());
        }

        // 4. Enviar notificación
        try {
            enviarNotificacionInicioViaje(
                    asignacion.getLotesId(),
                    asignacion,
                    asignacion.getTransportistaId()
            );
        } catch (Exception e) {
            log.error("Error al enviar notificacion (no critico): {}", e.getMessage());
        }
    }

    /**
     * Registra en auditoría el inicio del viaje
     */
    private void registrarAuditoriaInicioViaje(
            Lotes lote,
            AsignacionCamion asignacion,
            String estadoAnterior,
            Integer usuarioId
    ) {
        try {
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("asignacion_camion_id", asignacion.getId());
            metadata.put("transportista_id", asignacion.getTransportistaId().getId());
            metadata.put("numero_camion", asignacion.getNumeroCamion());
            metadata.put("placa_vehiculo", asignacion.getTransportistaId().getPlacaVehiculo());
            metadata.put("tipo_accion", "inicio_viaje_operativo");
            metadata.put("fecha_inicio", asignacion.getFechaInicio().toString());

            // Obtener coordenadas del inicio
            Map<String, Object> obs = obtenerObservaciones(asignacion);
            if (obs.containsKey("inicio_viaje")) {
                @SuppressWarnings("unchecked")
                Map<String, Object> inicioViaje = (Map<String, Object>) obs.get("inicio_viaje");
                Map<String, Object> coords = new HashMap<>();
                coords.put("lat", inicioViaje.get("lat"));
                coords.put("lng", inicioViaje.get("lng"));
                metadata.put("coordenadas_inicio", coords);
            }

            String descripcion = String.format(
                    "Transportista inicio el viaje - Camion #%d - Placa: %s",
                    asignacion.getNumeroCamion(),
                    asignacion.getTransportistaId().getPlacaVehiculo()
            );

            auditoriaLotesBl.registrarAuditoria(
                    lote.getId(),
                    "transportista",
                    estadoAnterior,
                    asignacion.getEstado(),
                    "INICIAR_VIAJE",
                    descripcion,
                    null,
                    metadata,
                    null
            );

            log.info("Auditoria registrada - Lote ID: {}, Accion: INICIAR_VIAJE", lote.getId());

        } catch (Exception e) {
            log.error("Error al registrar auditoria: {}", e.getMessage(), e);
            // No relanzamos la excepción
        }
    }

    /**
     * Envía notificación al socio dueño del lote
     */
    private void enviarNotificacionInicioViaje(
            Lotes lote,
            AsignacionCamion asignacion,
            Transportista transportista
    ) {
        try {
            // Obtener el socio dueño de la mina del lote
            Socio socio = lote.getMinasId().getSocioId();
            Integer socioUsuarioId = socio.getUsuariosId().getId();

            // Obtener datos del transportista
            Persona personaTransportista = transportista.getUsuariosId().getPersona();
            String nombreTransportista = personaTransportista.getNombres() + " " +
                    personaTransportista.getPrimerApellido();

            // Construir metadata de la notificación
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("lote_id", lote.getId());
            metadata.put("asignacion_camion_id", asignacion.getId());
            metadata.put("numero_camion", asignacion.getNumeroCamion());
            metadata.put("transportista_id", transportista.getId());
            metadata.put("transportista_nombre", nombreTransportista);
            metadata.put("placa_vehiculo", transportista.getPlacaVehiculo());
            metadata.put("mina_nombre", lote.getMinasId().getNombre());
            metadata.put("tipo_notificacion", "inicio_viaje");
            metadata.put("fecha_inicio", asignacion.getFechaInicio().toString());

            String codigoLote = String.format("LT-%d-%04d",
                    lote.getFechaCreacion().getYear(),
                    lote.getId());

            String titulo = String.format("Viaje iniciado - Camion #%d", asignacion.getNumeroCamion());
            String mensaje = String.format(
                    "%s ha iniciado el transporte del lote %s desde %s. El camion esta en camino a la mina.",
                    nombreTransportista,
                    codigoLote,
                    lote.getMinasId().getNombre()
            );

            // Crear y enviar notificación
            notificacionBl.crearNotificacion(
                    socioUsuarioId,
                    "info",
                    titulo,
                    mensaje,
                    metadata
            );

            log.info("Notificacion enviada - Usuario ID: {}, Lote: {}, Camion: #{}",
                    socioUsuarioId, codigoLote, asignacion.getNumeroCamion());

        } catch (Exception e) {
            log.error("Error al enviar notificacion: {}", e.getMessage(), e);
            // No relanzamos la excepción
        }
    }



    /**
     * Registra un evento del viaje de forma unificada.
     * Este método actúa como dispatcher hacia los métodos específicos existentes.
     *
     * @param asignacionId ID de la asignación del camión
     * @param dto          Datos del evento
     * @param usuarioId    ID del usuario autenticado
     * @return Respuesta con el nuevo estado
     */
    @Transactional
    public TransicionEstadoResponseDto registrarEvento(
            Integer asignacionId,
            RegistrarEventoDto dto,
            Integer usuarioId
    ) {
        log.info("📝 Registrando evento {} - Asignación: {}", dto.getTipoEvento(), asignacionId);

        // Validar coordenadas para eventos que las requieren
        List<String> eventosConUbicacion = List.of(
                "INICIO_VIAJE", "LLEGADA_MINA", "FIN_CARGUIO",
                "INICIO_DESCARGA", "FIN_DESCARGA"
        );

        if (eventosConUbicacion.contains(dto.getTipoEvento().toUpperCase())) {
            if (dto.getLat() == null || dto.getLng() == null) {
                throw new IllegalArgumentException("Las coordenadas GPS son obligatorias para este evento");
            }
        }

        String tipoEvento = dto.getTipoEvento().toUpperCase();

        switch (tipoEvento) {
            case "INICIO_VIAJE":
                return iniciarViaje(
                        asignacionId,
                        dto.getLat(),
                        dto.getLng(),
                        dto.getComentario(),
                        usuarioId
                );

            case "LLEGADA_MINA":
                ConfirmarLlegadaMinaDto llegadaDto = new ConfirmarLlegadaMinaDto();
                llegadaDto.setAsignacionCamionId(asignacionId);
                llegadaDto.setLat(dto.getLat());
                llegadaDto.setLng(dto.getLng());
                llegadaDto.setObservaciones(dto.getComentario());
                llegadaDto.setFotosUrls(dto.getEvidencias());
                // Checklist desde metadatos
                if (dto.getMetadatosExtra() != null) {
                    llegadaDto.setPalaOperativa((Boolean) dto.getMetadatosExtra().get("palaOperativa"));
                    llegadaDto.setMineralVisible((Boolean) dto.getMetadatosExtra().get("mineralVisible"));
                    llegadaDto.setEspacioParaCarga((Boolean) dto.getMetadatosExtra().get("espacioParaCarga"));
                }
                return confirmarLlegadaMina(llegadaDto, usuarioId);

            case "INICIO_CARGUIO":
                // Transición interna: Esperando carguío se mantiene hasta FIN_CARGUIO
                // Este evento es solo para registro, no cambia estado
                return registrarEventoInterno(asignacionId, "inicio_carguio", dto, usuarioId);

            case "FIN_CARGUIO":
            case "CONFIRMAR_CARGUIO":
                ConfirmarCarguioDto carguioDto = new ConfirmarCarguioDto();
                carguioDto.setAsignacionCamionId(asignacionId);
                carguioDto.setLat(dto.getLat());
                carguioDto.setLng(dto.getLng());
                carguioDto.setObservaciones(dto.getComentario());
                carguioDto.setFotosUrls(dto.getEvidencias());
                if (dto.getMetadatosExtra() != null && dto.getMetadatosExtra().containsKey("pesoEstimadoKg")) {
                    carguioDto.setPesoEstimadoKg(((Number) dto.getMetadatosExtra().get("pesoEstimadoKg")).doubleValue());
                }
                return confirmarCarguio(carguioDto, usuarioId);

            case "PESAJE_BALANZA_COOP":
                validarDatosPesaje(dto);
                RegistrarPesajeDto pesajeCoopDto = new RegistrarPesajeDto();
                pesajeCoopDto.setAsignacionCamionId(asignacionId);
                pesajeCoopDto.setTipoPesaje("cooperativa");
                pesajeCoopDto.setPesoBrutoKg(dto.getDatosPesaje().getPesoBruto());
                pesajeCoopDto.setPesoTaraKg(dto.getDatosPesaje().getPesoTara());
                pesajeCoopDto.setObservaciones(dto.getComentario());
                if (dto.getEvidencias() != null && !dto.getEvidencias().isEmpty()) {
                    pesajeCoopDto.setTicketPesajeUrl(dto.getEvidencias().get(0));
                }
                return registrarPesaje(pesajeCoopDto, usuarioId);

            case "PESAJE_BALANZA_DESTINO":
                validarDatosPesaje(dto);
                RegistrarPesajeDto pesajeDestinoDto = new RegistrarPesajeDto();
                pesajeDestinoDto.setAsignacionCamionId(asignacionId);
                pesajeDestinoDto.setTipoPesaje("destino");
                pesajeDestinoDto.setPesoBrutoKg(dto.getDatosPesaje().getPesoBruto());
                pesajeDestinoDto.setPesoTaraKg(dto.getDatosPesaje().getPesoTara());
                pesajeDestinoDto.setObservaciones(dto.getComentario());
                if (dto.getEvidencias() != null && !dto.getEvidencias().isEmpty()) {
                    pesajeDestinoDto.setTicketPesajeUrl(dto.getEvidencias().get(0));
                }
                return registrarPesaje(pesajeDestinoDto, usuarioId);

            case "INICIO_DESCARGA":
                return iniciarDescarga(asignacionId, dto.getLat(), dto.getLng(), usuarioId);

            case "FIN_DESCARGA":
            case "CONFIRMAR_DESCARGA":
                ConfirmarDescargaDto descargaDto = new ConfirmarDescargaDto();
                descargaDto.setAsignacionCamionId(asignacionId);
                descargaDto.setLat(dto.getLat());
                descargaDto.setLng(dto.getLng());
                descargaDto.setObservaciones(dto.getComentario());
                descargaDto.setFotosUrls(dto.getEvidencias());
                if (dto.getMetadatosExtra() != null && dto.getMetadatosExtra().containsKey("firmaReceptor")) {
                    descargaDto.setFirmaReceptor((String) dto.getMetadatosExtra().get("firmaReceptor"));
                }
                return confirmarDescarga(descargaDto, usuarioId);

            default:
                throw new IllegalArgumentException("Tipo de evento no reconocido: " + dto.getTipoEvento());
        }
    }

    /**
     * Valida que los datos de pesaje estén completos
     */
    private void validarDatosPesaje(RegistrarEventoDto dto) {
        if (dto.getDatosPesaje() == null) {
            throw new IllegalArgumentException("Los datos de pesaje son obligatorios");
        }
        if (dto.getDatosPesaje().getPesoBruto() == null || dto.getDatosPesaje().getPesoBruto() <= 0) {
            throw new IllegalArgumentException("El peso bruto es obligatorio y debe ser mayor a 0");
        }
        if (dto.getDatosPesaje().getPesoTara() == null || dto.getDatosPesaje().getPesoTara() <= 0) {
            throw new IllegalArgumentException("El peso tara es obligatorio y debe ser mayor a 0");
        }
        if (dto.getDatosPesaje().getPesoTara() >= dto.getDatosPesaje().getPesoBruto()) {
            throw new IllegalArgumentException("El peso tara debe ser menor al peso bruto");
        }
    }

    /**
     * Registra un evento interno sin cambio de estado
     * Útil para eventos intermedios como inicio_carguio
     */
    private TransicionEstadoResponseDto registrarEventoInterno(
            Integer asignacionId,
            String tipoEvento,
            RegistrarEventoDto dto,
            Integer usuarioId
    ) {
        AsignacionCamion asignacion = obtenerYValidarAsignacion(asignacionId, usuarioId);

        // Registrar en observaciones
        Map<String, Object> obs = obtenerObservaciones(asignacion);
        Map<String, Object> eventoData = new HashMap<>();
        eventoData.put("timestamp", LocalDateTime.now());
        if (dto.getLat() != null) eventoData.put("lat", dto.getLat());
        if (dto.getLng() != null) eventoData.put("lng", dto.getLng());
        if (dto.getComentario() != null) eventoData.put("comentario", dto.getComentario());
        if (dto.getEvidencias() != null) eventoData.put("evidencias", dto.getEvidencias());

        obs.put(tipoEvento, eventoData);
        actualizarObservaciones(asignacion, obs);
        asignacionCamionRepository.save(asignacion);

        log.info("✅ Evento interno registrado: {} - Asignación: {}", tipoEvento, asignacionId);

        return TransicionEstadoResponseDto.builder()
                .success(true)
                .message("Evento registrado correctamente")
                .estadoAnterior(asignacion.getEstado())
                .estadoNuevo(asignacion.getEstado()) // No cambia
                .proximoPaso("Continúa con el proceso actual")
                .build();
    }

    // Confirma llegada a mina: "En camino a la mina" -> "Esperando carguío"
    @Transactional
    public TransicionEstadoResponseDto confirmarLlegadaMina(
            ConfirmarLlegadaMinaDto dto,
            Integer usuarioId
    ) {
        log.info("Confirmando llegada a mina - Asignación: {}", dto.getAsignacionCamionId());

        AsignacionCamion asignacion = obtenerYValidarAsignacion(dto.getAsignacionCamionId(), usuarioId);
        validarEstado(asignacion, "En camino a la mina");

        Minas mina = asignacion.getLotesId().getMinasId();
        double distancia = GeometryUtils.calcularDistancia(
                BigDecimal.valueOf(dto.getLat()),
                BigDecimal.valueOf(dto.getLng()),
                mina.getLatitud(),
                mina.getLongitud()
        ) * 1000;

        if (distancia > RADIO_MINA) {
            throw new IllegalStateException(
                    String.format("Estás demasiado lejos de la mina (%.0fm). " +
                                    "Debes estar a menos de %dm para confirmar llegada.",
                            distancia, RADIO_MINA)
            );
        }

        trackingBl.registrarLlegadaPuntoControl(
                dto.getAsignacionCamionId(),
                "mina",
                dto.getLat(),
                dto.getLng(),
                dto.getObservaciones()
        );

        Map<String, Object> obs = obtenerObservaciones(asignacion);
        Map<String, Object> llegada = new HashMap<>();
        llegada.put("timestamp", LocalDateTime.now());
        llegada.put("lat", dto.getLat());
        llegada.put("lng", dto.getLng());
        llegada.put("distancia_metros", (int) distancia);
        llegada.put("checklist", Map.of(
                "pala_operativa", dto.getPalaOperativa() != null ? dto.getPalaOperativa() : false,
                "mineral_visible", dto.getMineralVisible() != null ? dto.getMineralVisible() : false,
                "espacio_carga", dto.getEspacioParaCarga() != null ? dto.getEspacioParaCarga() : false
        ));
        if (dto.getFotosUrls() != null && !dto.getFotosUrls().isEmpty()) {
            llegada.put("fotos", dto.getFotosUrls());
        }
        if (dto.getObservaciones() != null) {
            llegada.put("observaciones", dto.getObservaciones());
        }
        obs.put("llegada_mina", llegada);
        actualizarObservaciones(asignacion, obs);

        String estadoAnterior = asignacion.getEstado();
        asignacion.setEstado("Esperando carguío");
        asignacionCamionRepository.save(asignacion);

        log.info("Llegada a mina confirmada - Distancia: {}m", (int) distancia);

        try {
            trackingBl.actualizarEstadoYRegistrarEvento(
                    dto.getAsignacionCamionId(),
                    estadoAnterior,
                    "Esperando carguío",
                    "LLEGADA_MINA",
                    dto.getLat(),
                    dto.getLng()
            );
        } catch (Exception e) {
            log.error("⚠️ Error al actualizar MongoDB: {}", e.getMessage());
        }

        return construirRespuestaTransicion(
                asignacion,
                estadoAnterior,
                "Esperando carguío",
                "Espera tu turno y realiza la carga del mineral"
        );
    }

    // Confirma carga: "Esperando carguío" -> "En camino balanza cooperativa"
    @Transactional
    public TransicionEstadoResponseDto confirmarCarguio(
            ConfirmarCarguioDto dto,
            Integer usuarioId
    ) {
        log.info("Confirmando carguío - Asignación: {}", dto.getAsignacionCamionId());

        AsignacionCamion asignacion = obtenerYValidarAsignacion(dto.getAsignacionCamionId(), usuarioId);
        validarEstado(asignacion, "Esperando carguío");

        trackingBl.registrarSalidaPuntoControl(
                dto.getAsignacionCamionId(),
                "mina",
                dto.getObservaciones()
        );

        Map<String, Object> obs = obtenerObservaciones(asignacion);
        Map<String, Object> carguio = new HashMap<>();
        carguio.put("timestamp", LocalDateTime.now());
        carguio.put("lat", dto.getLat());
        carguio.put("lng", dto.getLng());
        if (dto.getPesoEstimadoKg() != null) {
            carguio.put("peso_estimado_kg", dto.getPesoEstimadoKg());
        }
        if (dto.getFotosUrls() != null && !dto.getFotosUrls().isEmpty()) {
            carguio.put("fotos", dto.getFotosUrls());
        }
        if (dto.getObservaciones() != null) {
            carguio.put("observaciones", dto.getObservaciones());
        }
        obs.put("carguio_completo", carguio);
        actualizarObservaciones(asignacion, obs);

        String estadoAnterior = asignacion.getEstado();
        asignacion.setEstado("En camino balanza cooperativa");
        asignacionCamionRepository.save(asignacion);

        actualizarEstadoLote(asignacion.getLotesId());

        log.info("Carguío confirmado");

        try {
            trackingBl.actualizarEstadoYRegistrarEvento(
                    dto.getAsignacionCamionId(),
                    estadoAnterior,
                    "En camino balanza cooperativa",
                    "FIN_CARGUIO",
                    dto.getLat(),
                    dto.getLng()
            );
        } catch (Exception e) {
            log.error("⚠️ Error al actualizar MongoDB: {}", e.getMessage());
        }

        return construirRespuestaTransicion(
                asignacion,
                estadoAnterior,
                "En camino balanza cooperativa",
                "Dirígete a la balanza de la cooperativa para el primer pesaje"
        );
    }

    // Registra pesaje en balanza (cooperativa o destino)
    @Transactional
    public TransicionEstadoResponseDto registrarPesaje(
            RegistrarPesajeDto dto,
            Integer usuarioId
    ) {
        log.info("Registrando pesaje {} - Asignación: {}",
                dto.getTipoPesaje(), dto.getAsignacionCamionId());

        AsignacionCamion asignacion = obtenerYValidarAsignacion(dto.getAsignacionCamionId(), usuarioId);

        String estadoEsperado, nuevoEstado, tipoPesaje, mensajeExito;

        if ("cooperativa".equals(dto.getTipoPesaje())) {
            estadoEsperado = "En camino balanza cooperativa";
            nuevoEstado = "En camino balanza destino";
            tipoPesaje = "pesaje_origen";
            mensajeExito = "Dirígete a la balanza del destino para el segundo pesaje";
        } else {
            estadoEsperado = "En camino balanza destino";
            nuevoEstado = "En camino almacén destino";
            tipoPesaje = "pesaje_destino";
            mensajeExito = "Dirígete al almacén para descargar el mineral";
        }

        validarEstado(asignacion, estadoEsperado);

        if (dto.getPesoBrutoKg() <= dto.getPesoTaraKg()) {
            throw new IllegalArgumentException("El peso bruto debe ser mayor que el peso tara");
        }

        double pesoNeto = dto.getPesoBrutoKg() - dto.getPesoTaraKg();

        Pesajes pesaje = new Pesajes();
        pesaje.setAsignacionCamionId(asignacion);
        pesaje.setTipoPesaje(tipoPesaje);
        pesaje.setPesoBruto(BigDecimal.valueOf(dto.getPesoBrutoKg()));
        pesaje.setPesoTara(BigDecimal.valueOf(dto.getPesoTaraKg()));
        pesaje.setPesoNeto(BigDecimal.valueOf(pesoNeto));
        pesaje.setFechaPesaje(LocalDateTime.now());
        pesaje.setObservaciones(dto.getObservaciones());
        pesajesRepository.save(pesaje);

        Map<String, Object> obs = obtenerObservaciones(asignacion);
        Map<String, Object> pesajeInfo = new HashMap<>();
        pesajeInfo.put("timestamp", LocalDateTime.now());
        pesajeInfo.put("peso_bruto_kg", dto.getPesoBrutoKg());
        pesajeInfo.put("peso_tara_kg", dto.getPesoTaraKg());
        pesajeInfo.put("peso_neto_kg", pesoNeto);
        if (dto.getTicketPesajeUrl() != null) {
            pesajeInfo.put("ticket_pesaje", dto.getTicketPesajeUrl());
        }
        if (dto.getObservaciones() != null) {
            pesajeInfo.put("observaciones", dto.getObservaciones());
        }
        obs.put(tipoPesaje, pesajeInfo);
        actualizarObservaciones(asignacion, obs);

        String estadoAnterior = asignacion.getEstado();
        asignacion.setEstado(nuevoEstado);
        asignacionCamionRepository.save(asignacion);

        log.info("Pesaje {} registrado - Peso neto: {}kg", tipoPesaje, pesoNeto);

        try {
            String tipoEventoPesaje = "cooperativa".equals(dto.getTipoPesaje())
                    ? "PESAJE_COOPERATIVA"
                    : "PESAJE_DESTINO";

            // Para pesaje no tenemos coordenadas, usamos null
            trackingBl.actualizarEstadoYRegistrarEvento(
                    dto.getAsignacionCamionId(),
                    estadoAnterior,
                    nuevoEstado,
                    tipoEventoPesaje,
                    null,  // No tenemos coordenadas en pesaje
                    null
            );
        } catch (Exception e) {
            log.error("⚠️ Error al actualizar MongoDB: {}", e.getMessage());
        }

        return construirRespuestaTransicion(
                asignacion,
                estadoAnterior,
                nuevoEstado,
                mensajeExito
        );
    }

    // Inicia descarga: "En camino almacén destino" -> "Descargando"
    @Transactional
    public TransicionEstadoResponseDto iniciarDescarga(
            Integer asignacionId,
            Double lat,
            Double lng,
            Integer usuarioId
    ) {
        log.info("Iniciando descarga - Asignación: {}", asignacionId);

        AsignacionCamion asignacion = obtenerYValidarAsignacion(asignacionId, usuarioId);
        validarEstado(asignacion, "En camino almacén destino");

        Lotes lote = asignacion.getLotesId();
        BigDecimal almacenLat = null;
        BigDecimal almacenLng = null;

        if (!lote.getLoteIngenioList().isEmpty()) {
            var ingenio = lote.getLoteIngenioList().getFirst().getIngenioMineroId();
            if (!ingenio.getAlmacenesIngenioList().isEmpty()) {
                var almacen = ingenio.getAlmacenesIngenioList().getFirst();
                almacenLat = almacen.getLatitud();
                almacenLng = almacen.getLongitud();
            }
        } else if (!lote.getLoteComercializadoraList().isEmpty()) {
            var comercializadora = lote.getLoteComercializadoraList().getFirst().getComercializadoraId();
            if (!comercializadora.getAlmacenesList().isEmpty()) {
                var almacen = comercializadora.getAlmacenesList().getFirst();
                almacenLat = almacen.getLatitud();
                almacenLng = almacen.getLongitud();
            }
        }

        if (almacenLat != null && almacenLng != null) {
            double distancia = GeometryUtils.calcularDistancia(
                    BigDecimal.valueOf(lat),
                    BigDecimal.valueOf(lng),
                    almacenLat,
                    almacenLng
            ) * 1000;

            if (distancia > RADIO_ALMACEN) {
                throw new IllegalStateException(
                        String.format("Estás demasiado lejos del almacén (%.0fm). " +
                                        "Debes estar a menos de %dm para iniciar descarga.",
                                distancia, RADIO_ALMACEN)
                );
            }
        }

        String estadoAnterior = asignacion.getEstado();
        asignacion.setEstado("Descargando");
        asignacionCamionRepository.save(asignacion);

        try {
            trackingBl.actualizarEstadoYRegistrarEvento(
                    asignacionId,
                    estadoAnterior,
                    "Descargando",
                    "INICIO_DESCARGA",
                    lat,
                    lng
            );
        } catch (Exception e) {
            log.error("⚠️ Error al actualizar MongoDB: {}", e.getMessage());
        }

        return construirRespuestaTransicion(
                asignacion,
                estadoAnterior,
                "Descargando",
                "Realiza la descarga del mineral"
        );
    }

    // Confirma descarga: "Descargando" -> "Completado"
    @Transactional
    public TransicionEstadoResponseDto confirmarDescarga(
            ConfirmarDescargaDto dto,
            Integer usuarioId
    ) {
        log.info("Confirmando descarga - Asignación: {}", dto.getAsignacionCamionId());

        AsignacionCamion asignacion = obtenerYValidarAsignacion(dto.getAsignacionCamionId(), usuarioId);
        validarEstado(asignacion, "Descargando");

        Map<String, Object> obs = obtenerObservaciones(asignacion);
        Map<String, Object> descarga = new HashMap<>();
        descarga.put("timestamp", LocalDateTime.now());
        descarga.put("lat", dto.getLat());
        descarga.put("lng", dto.getLng());
        if (dto.getFirmaReceptor() != null) {
            descarga.put("firma_receptor", dto.getFirmaReceptor());
        }
        if (dto.getFotosUrls() != null && !dto.getFotosUrls().isEmpty()) {
            descarga.put("fotos", dto.getFotosUrls());
        }
        if (dto.getObservaciones() != null) {
            descarga.put("observaciones", dto.getObservaciones());
        }
        obs.put("descarga_completa", descarga);
        actualizarObservaciones(asignacion, obs);

        String estadoAnterior = asignacion.getEstado();
        asignacion.setEstado("Completado");
        asignacion.setFechaFin(LocalDateTime.now());
        asignacionCamionRepository.save(asignacion);

        //Cambiar estado de transportista a disponible
        Transportista transportista = asignacion.getTransportistaId();
        transportista.setEstado("aprobado");
        transportistaRepository.save(transportista);

        actualizarEstadoLote(asignacion.getLotesId());

        log.info("Viaje completado - Asignación: {}", dto.getAsignacionCamionId());

        try {
            trackingBl.actualizarEstadoYRegistrarEvento(
                    dto.getAsignacionCamionId(),
                    estadoAnterior,
                    "Completado",
                    "FIN_DESCARGA",
                    dto.getLat(),
                    dto.getLng()
            );
        } catch (Exception e) {
            log.error("⚠️ Error al actualizar MongoDB: {}", e.getMessage());
        }

        return TransicionEstadoResponseDto.builder()
                .success(true)
                .message("Viaje completado exitosamente")
                .estadoAnterior(estadoAnterior)
                .estadoNuevo("Completado")
                .proximoPaso("Viaje finalizado")
                .build();
    }
    /**
     * Actualiza el estado del lote basándose en el progreso de TODOS los camiones asignados.
     * El lote solo avanza de estado cuando TODOS los camiones han alcanzado ese estado.
     */
    private void sincronizarEstadoLoteConCamiones(Lotes lote) {
        List<AsignacionCamion> asignaciones = asignacionCamionRepository.findByLotesId(lote);

        // Filtrar solo camiones activos (excluir cancelados)
        List<AsignacionCamion> asignacionesActivas = asignaciones.stream()
                .filter(a -> !a.getEstado().equals("Cancelado por rechazo"))
                .toList();

        if (asignacionesActivas.isEmpty()) {
            return;
        }

        // Definir la jerarquía de estados de camiones y su correspondencia con estados de lote
        Map<String, String> estadoCamionAEstadoLote = Map.of(
                "Esperando iniciar", "Aprobado - Pendiente de iniciar",
                "En camino a la mina", "En Transporte",
                "Esperando carguío", "En Transporte",
                "En camino balanza cooperativa", "En Transporte",
                "En camino balanza destino", "En Transporte",
                "En camino almacén destino", "En Transporte",
                "Descargando", "En Transporte",
                "Completado", "En Transporte Completo"
        );

        // Orden jerárquico de estados (menor = más atrasado)
        List<String> ordenEstados = List.of(
                "Esperando iniciar",
                "En camino a la mina",
                "Esperando carguío",
                "En camino balanza cooperativa",
                "En camino balanza destino",
                "En camino almacén destino",
                "Descargando",
                "Completado"
        );

        // Encontrar el estado más atrasado entre todos los camiones
        String estadoMasAtrasado = asignacionesActivas.stream()
                .map(AsignacionCamion::getEstado)
                .min(Comparator.comparingInt(estado -> {
                    int index = ordenEstados.indexOf(estado);
                    return index == -1 ? Integer.MAX_VALUE : index;
                }))
                .orElse("Esperando iniciar");

        // Determinar el nuevo estado del lote
        String nuevoEstadoLote = estadoCamionAEstadoLote.getOrDefault(
                estadoMasAtrasado,
                lote.getEstado()
        );

        // Solo actualizar si el estado cambió
        if (!nuevoEstadoLote.equals(lote.getEstado())) {
            String estadoAnterior = lote.getEstado();
            lote.setEstado(nuevoEstadoLote);

            // Actualizar fechas según el nuevo estado
            if ("En Transporte".equals(nuevoEstadoLote) && lote.getFechaInicioTransporte() == null) {
                lote.setFechaInicioTransporte(LocalDateTime.now());
            } else if ("En Transporte Completo".equals(nuevoEstadoLote)) {
                lote.setFechaFinTransporte(LocalDateTime.now());
            }

            lotesRepository.save(lote);

            log.info("Estado del lote {} actualizado: {} -> {} (basado en estado más atrasado: {})",
                    lote.getId(), estadoAnterior, nuevoEstadoLote, estadoMasAtrasado);
        }
    }

    private AsignacionCamion obtenerYValidarAsignacion(Integer asignacionId, Integer usuarioId) {
        AsignacionCamion asignacion = asignacionCamionRepository.findById(asignacionId)
                .orElseThrow(() -> new IllegalArgumentException("Asignación no encontrada"));

        if (!asignacion.getTransportistaId().getUsuariosId().getId().equals(usuarioId)) {
            throw new SecurityException("No autorizado");
        }

        return asignacion;
    }

    private void validarEstado(AsignacionCamion asignacion, String... estadosPermitidos) {
        boolean esValido = Arrays.asList(estadosPermitidos).contains(asignacion.getEstado());
        if (!esValido) {
            throw new IllegalStateException(
                    "No se puede realizar esta acción desde el estado: " + asignacion.getEstado()
            );
        }
    }

    private Map<String, Object> obtenerObservaciones(AsignacionCamion asignacion) {
        if (asignacion.getObservaciones() == null || asignacion.getObservaciones().isEmpty()) {
            return new HashMap<>();
        }
        try {
            return objectMapper.readValue(asignacion.getObservaciones(), Map.class);
        } catch (JsonProcessingException e) {
            return new HashMap<>();
        }
    }

    private void actualizarObservaciones(AsignacionCamion asignacion, Map<String, Object> obs) {
        try {
            asignacion.setObservaciones(objectMapper.writeValueAsString(obs));
        } catch (JsonProcessingException e) {
            log.error("Error al serializar observaciones", e);
        }
    }

    private void actualizarEstadoLote(Lotes lote) {
        sincronizarEstadoLoteConCamiones(lote);
    }
    private TransicionEstadoResponseDto construirRespuestaTransicion(
            AsignacionCamion asignacion,
            String estadoAnterior,
            String estadoNuevo,
            String proximoPaso
    ) {
        try {
            // Calcular próximo punto de forma segura
            ProximoPuntoControlDto proximoPunto = calcularProximoPuntoSeguro(asignacion);

            // Construir metadata básico y seguro
            Map<String, Object> metadata = construirMetadataSeguro(asignacion);

            return TransicionEstadoResponseDto.builder()
                    .success(true)
                    .message("Estado actualizado exitosamente")
                    .estadoAnterior(estadoAnterior)
                    .estadoNuevo(estadoNuevo)
                    .proximoPaso(proximoPaso)
                    .proximoPuntoControl(proximoPunto)
                    .metadata(metadata)
                    .build();

        } catch (Exception e) {
            log.error("Error al construir respuesta de transicion: {}", e.getMessage(), e);

            // Respuesta mínima en caso de error
            return TransicionEstadoResponseDto.builder()
                    .success(true)
                    .message("Estado actualizado exitosamente")
                    .estadoAnterior(estadoAnterior)
                    .estadoNuevo(estadoNuevo)
                    .proximoPaso(proximoPaso)
                    .build();
        }
    }

    private ProximoPuntoControlDto calcularProximoPunto(AsignacionCamion asignacion) {
        Lotes lote = asignacion.getLotesId();
        String estadoActual = asignacion.getEstado();

        String tipo = null;
        String nombre = null;
        BigDecimal lat = null;
        BigDecimal lng = null;

        switch (estadoActual) {
            case "En camino a la mina":
                Minas mina = lote.getMinasId();
                tipo = "mina";
                nombre = mina.getNombre();
                lat = mina.getLatitud();
                lng = mina.getLongitud();
                break;

            case "En camino balanza cooperativa":
                Cooperativa cooperativa = lote.getMinasId().getSectoresId().getCooperativaId();
                if (!cooperativa.getBalanzaCooperativaList().isEmpty()) {
                    var balanza = cooperativa.getBalanzaCooperativaList().getFirst();
                    tipo = "balanza_cooperativa";
                    nombre = "Balanza " + cooperativa.getRazonSocial();
                    lat = balanza.getLatitud();
                    lng = balanza.getLongitud();
                }
                break;

            case "En camino balanza destino":
                if (!lote.getLoteIngenioList().isEmpty()) {
                    var ingenio = lote.getLoteIngenioList().getFirst().getIngenioMineroId();
                    if (!ingenio.getBalanzasIngenioList().isEmpty()) {
                        var balanza = ingenio.getBalanzasIngenioList().getFirst();
                        tipo = "balanza_ingenio";
                        nombre = "Balanza " + ingenio.getRazonSocial();
                        lat = balanza.getLatitud();
                        lng = balanza.getLongitud();
                    }
                } else if (!lote.getLoteComercializadoraList().isEmpty()) {
                    var comercializadora = lote.getLoteComercializadoraList().getFirst().getComercializadoraId();
                    if (!comercializadora.getBalanzasList().isEmpty()) {
                        var balanza = comercializadora.getBalanzasList().getFirst();
                        tipo = "balanza_comercializadora";
                        nombre = "Balanza " + comercializadora.getRazonSocial();
                        lat = balanza.getLatitud();
                        lng = balanza.getLongitud();
                    }
                }
                break;

            case "En camino almacén destino":
                if (!lote.getLoteIngenioList().isEmpty()) {
                    var ingenio = lote.getLoteIngenioList().getFirst().getIngenioMineroId();
                    if (!ingenio.getAlmacenesIngenioList().isEmpty()) {
                        var almacen = ingenio.getAlmacenesIngenioList().getFirst();
                        tipo = "almacen_ingenio";
                        nombre = "Almacén " + ingenio.getRazonSocial();
                        lat = almacen.getLatitud();
                        lng = almacen.getLongitud();
                    }
                } else if (!lote.getLoteComercializadoraList().isEmpty()) {
                    var comercializadora = lote.getLoteComercializadoraList().getFirst().getComercializadoraId();
                    if (!comercializadora.getAlmacenesList().isEmpty()) {
                        var almacen = comercializadora.getAlmacenesList().getFirst();
                        tipo = "almacen_comercializadora";
                        nombre = "Almacén " + comercializadora.getRazonSocial();
                        lat = almacen.getLatitud();
                        lng = almacen.getLongitud();
                    }
                }
                break;
        }

        if (tipo != null && lat != null && lng != null) {
            return ProximoPuntoControlDto.builder()
                    .tipo(tipo)
                    .nombre(nombre)
                    .latitud(lat.doubleValue())
                    .longitud(lng.doubleValue())
                    .build();
        }

        return null;
    }
    private ProximoPuntoControlDto calcularProximoPuntoSeguro(AsignacionCamion asignacion) {
        try {
            Lotes lote = asignacion.getLotesId();
            String estadoActual = asignacion.getEstado();

            String tipo = null;
            String nombre = null;
            Double lat = null;
            Double lng = null;

            switch (estadoActual) {
                case "En camino a la mina":
                    Minas mina = lote.getMinasId();
                    if (mina != null) {
                        tipo = "mina";
                        nombre = mina.getNombre();
                        lat = mina.getLatitud() != null ? mina.getLatitud().doubleValue() : null;
                        lng = mina.getLongitud() != null ? mina.getLongitud().doubleValue() : null;
                    }
                    break;

                case "En camino balanza cooperativa":
                    Cooperativa cooperativa = lote.getMinasId().getSectoresId().getCooperativaId();
                    if (cooperativa != null && !cooperativa.getBalanzaCooperativaList().isEmpty()) {
                        var balanza = cooperativa.getBalanzaCooperativaList().get(0);
                        tipo = "balanza_cooperativa";
                        nombre = "Balanza " + cooperativa.getRazonSocial();
                        lat = balanza.getLatitud() != null ? balanza.getLatitud().doubleValue() : null;
                        lng = balanza.getLongitud() != null ? balanza.getLongitud().doubleValue() : null;
                    }
                    break;

                case "En camino balanza destino":
                    if (!lote.getLoteIngenioList().isEmpty()) {
                        var ingenio = lote.getLoteIngenioList().get(0).getIngenioMineroId();
                        if (ingenio != null && !ingenio.getBalanzasIngenioList().isEmpty()) {
                            var balanza = ingenio.getBalanzasIngenioList().get(0);
                            tipo = "balanza_ingenio";
                            nombre = "Balanza " + ingenio.getRazonSocial();
                            lat = balanza.getLatitud() != null ? balanza.getLatitud().doubleValue() : null;
                            lng = balanza.getLongitud() != null ? balanza.getLongitud().doubleValue() : null;
                        }
                    } else if (!lote.getLoteComercializadoraList().isEmpty()) {
                        var comercializadora = lote.getLoteComercializadoraList().get(0).getComercializadoraId();
                        if (comercializadora != null && !comercializadora.getBalanzasList().isEmpty()) {
                            var balanza = comercializadora.getBalanzasList().get(0);
                            tipo = "balanza_comercializadora";
                            nombre = "Balanza " + comercializadora.getRazonSocial();
                            lat = balanza.getLatitud() != null ? balanza.getLatitud().doubleValue() : null;
                            lng = balanza.getLongitud() != null ? balanza.getLongitud().doubleValue() : null;
                        }
                    }
                    break;

                case "En camino almacen destino":
                    if (!lote.getLoteIngenioList().isEmpty()) {
                        var ingenio = lote.getLoteIngenioList().get(0).getIngenioMineroId();
                        if (ingenio != null && !ingenio.getAlmacenesIngenioList().isEmpty()) {
                            var almacen = ingenio.getAlmacenesIngenioList().get(0);
                            tipo = "almacen_ingenio";
                            nombre = "Almacen " + ingenio.getRazonSocial();
                            lat = almacen.getLatitud() != null ? almacen.getLatitud().doubleValue() : null;
                            lng = almacen.getLongitud() != null ? almacen.getLongitud().doubleValue() : null;
                        }
                    } else if (!lote.getLoteComercializadoraList().isEmpty()) {
                        var comercializadora = lote.getLoteComercializadoraList().get(0).getComercializadoraId();
                        if (comercializadora != null && !comercializadora.getAlmacenesList().isEmpty()) {
                            var almacen = comercializadora.getAlmacenesList().get(0);
                            tipo = "almacen_comercializadora";
                            nombre = "Almacen " + comercializadora.getRazonSocial();
                            lat = almacen.getLatitud() != null ? almacen.getLatitud().doubleValue() : null;
                            lng = almacen.getLongitud() != null ? almacen.getLongitud().doubleValue() : null;
                        }
                    }
                    break;
            }

            // Solo retornar si tenemos datos válidos
            if (tipo != null && nombre != null && lat != null && lng != null) {
                return ProximoPuntoControlDto.builder()
                        .tipo(tipo)
                        .nombre(nombre)
                        .latitud(lat)
                        .longitud(lng)
                        .build();
            }

            return null;

        } catch (Exception e) {
            log.error("Error al calcular proximo punto: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Construye metadata seguro sin objetos complejos
     */
    private Map<String, Object> construirMetadataSeguro(AsignacionCamion asignacion) {
        Map<String, Object> metadata = new HashMap<>();

        try {
            metadata.put("asignacion_id", asignacion.getId());
            metadata.put("numero_camion", asignacion.getNumeroCamion());
            metadata.put("estado_actual", asignacion.getEstado());

            if (asignacion.getFechaInicio() != null) {
                metadata.put("fecha_inicio", asignacion.getFechaInicio().toString());
            }

            // Información del lote (simple)
            Lotes lote = asignacion.getLotesId();
            if (lote != null) {
                metadata.put("lote_id", lote.getId());
                metadata.put("codigo_lote", String.format("LT-%d-%04d",
                        lote.getFechaCreacion().getYear(),
                        lote.getId()));
                metadata.put("tipo_operacion", lote.getTipoOperacion());
            }

            // Información del transportista (simple)
            Transportista transportista = asignacion.getTransportistaId();
            if (transportista != null) {
                metadata.put("transportista_id", transportista.getId());
                metadata.put("placa_vehiculo", transportista.getPlacaVehiculo());
            }

        } catch (Exception e) {
            log.error("Error al construir metadata: {}", e.getMessage());
            // Retornar metadata vacío en caso de error
            return new HashMap<>();
        }

        return metadata;
    }

    private LoteAsignadoResumenDto convertToLoteResumen(AsignacionCamion asignacion) {
        Lotes lote = asignacion.getLotesId();

        return LoteAsignadoResumenDto.builder()
                .asignacionId(asignacion.getId())
                .loteId(lote.getId())
                .codigoLote("LT-" + lote.getFechaCreacion().getYear() + "-" + String.format("%04d", lote.getId()))
                .minaNombre(lote.getMinasId().getNombre())
                .tipoOperacion(lote.getTipoOperacion())
                .tipoMineral(lote.getTipoMineral())
                .estado(asignacion.getEstado())
                .numeroCamion(asignacion.getNumeroCamion())
                .destinoNombre("procesamiento_planta".equals(lote.getTipoOperacion())
                        ? lote.getLoteIngenioList().getFirst().getIngenioMineroId().getRazonSocial()
                        : lote.getLoteComercializadoraList().getFirst().getComercializadoraId().getRazonSocial()
                )
                .fechaAsignacion(asignacion.getFechaAsignacion())
                .mineralTags(obtenerMineralesTags(lote))
                .build();
    }

    private List<String> obtenerMineralesTags(Lotes lote) {
        return lote.getLoteMineralesList() != null
                ? lote.getLoteMineralesList().stream()
                .map(lm -> lm.getMineralesId().getNomenclatura())
                .collect(Collectors.toList())
                : List.of();
    }

    private LoteDetalleViajeDto construirDetalleViaje(AsignacionCamion asignacion) {
        Lotes lote = asignacion.getLotesId();
        var mina = lote.getMinasId();
        var socio = mina.getSocioId();
        var persona = socio.getUsuariosId().getPersona();
        var cooperativa = mina.getSectoresId().getCooperativaId();
        var sector = mina.getSectoresId();

        BigDecimal minaLat = mina.getLatitud();
        BigDecimal minaLng = mina.getLongitud();
        String minaColor = sector.getColor() != null ? sector.getColor() : "#1E3A8A";

        BigDecimal balanzaCoopLat = null;
        BigDecimal balanzaCoopLng = null;
        if (!cooperativa.getBalanzaCooperativaList().isEmpty()) {
            var balanzaCoop = cooperativa.getBalanzaCooperativaList().getFirst();
            balanzaCoopLat = balanzaCoop.getLatitud();
            balanzaCoopLng = balanzaCoop.getLongitud();
        }

        BigDecimal balanzaDestinoLat = null;
        BigDecimal balanzaDestinoLng = null;
        BigDecimal almacenLat = null;
        BigDecimal almacenLng = null;
        String destinoNombre = null;
        String destinoTipo = null;
        String destinoColor = null;

        if (!lote.getLoteIngenioList().isEmpty()) {
            var ingenio = lote.getLoteIngenioList().getFirst().getIngenioMineroId();
            destinoNombre = ingenio.getRazonSocial();
            destinoTipo = "Ingenio";
            destinoColor = "#059669";

            if (!ingenio.getBalanzasIngenioList().isEmpty()) {
                var balanza = ingenio.getBalanzasIngenioList().getFirst();
                balanzaDestinoLat = balanza.getLatitud();
                balanzaDestinoLng = balanza.getLongitud();
            }
            if (!ingenio.getAlmacenesIngenioList().isEmpty()) {
                var almacen = ingenio.getAlmacenesIngenioList().getFirst();
                almacenLat = almacen.getLatitud();
                almacenLng = almacen.getLongitud();
            }
        } else if (!lote.getLoteComercializadoraList().isEmpty()) {
            var comercializadora = lote.getLoteComercializadoraList().getFirst().getComercializadoraId();
            destinoNombre = comercializadora.getRazonSocial();
            destinoTipo = "Comercializadora";
            destinoColor = "#DC2626";

            if (!comercializadora.getBalanzasList().isEmpty()) {
                var balanza = comercializadora.getBalanzasList().getFirst();
                balanzaDestinoLat = balanza.getLatitud();
                balanzaDestinoLng = balanza.getLongitud();
            }
            if (!comercializadora.getAlmacenesList().isEmpty()) {
                var almacen = comercializadora.getAlmacenesList().getFirst();
                almacenLat = almacen.getLatitud();
                almacenLng = almacen.getLongitud();
            }
        }

        Double distanciaKm = 0.0;
        Double tiempoHoras = 0.0;
        Boolean rutaExitosa = false;
        String metodoCalculo = "linea_recta";

        if (minaLat != null && minaLng != null &&
                balanzaCoopLat != null && balanzaCoopLng != null &&
                balanzaDestinoLat != null && balanzaDestinoLng != null &&
                almacenLat != null && almacenLng != null) {

            try {
                RutaCalculadaDto ruta = routingService.calcularRutaCompleta(
                        minaLat, minaLng,
                        balanzaCoopLat, balanzaCoopLng,
                        balanzaDestinoLat, balanzaDestinoLng,
                        almacenLat, almacenLng
                );

                distanciaKm = ruta.getDistanciaKm();
                tiempoHoras = ruta.getTiempoHoras();
                rutaExitosa = ruta.getExitosa();
                metodoCalculo = ruta.getMetodoCalculo();

                log.info("Ruta calculada para lote {}: {} km, {} horas",
                        lote.getId(),
                        String.format("%.2f", distanciaKm),
                        String.format("%.2f", tiempoHoras));

            } catch (Exception e) {
                log.error("Error al calcular ruta para lote {}", lote.getId(), e);
            }
        }

        LoteDetalleViajeDto.WaypointDto puntoOrigen = LoteDetalleViajeDto.WaypointDto.builder()
                .nombre(mina.getNombre())
                .tipo("mina")
                .latitud(minaLat != null ? minaLat.doubleValue() : null)
                .longitud(minaLng != null ? minaLng.doubleValue() : null)
                .color(minaColor)
                .orden(1)
                .build();

        LoteDetalleViajeDto.WaypointDto puntoBalanzaCoop = LoteDetalleViajeDto.WaypointDto.builder()
                .nombre(cooperativa.getRazonSocial())
                .tipo("balanza_coop")
                .latitud(balanzaCoopLat != null ? balanzaCoopLat.doubleValue() : null)
                .longitud(balanzaCoopLng != null ? balanzaCoopLng.doubleValue() : null)
                .color("#F59E0B")
                .orden(2)
                .build();

        LoteDetalleViajeDto.WaypointDto puntoBalanzaDestino = LoteDetalleViajeDto.WaypointDto.builder()
                .nombre(destinoNombre)
                .tipo("balanza_destino")
                .latitud(balanzaDestinoLat != null ? balanzaDestinoLat.doubleValue() : null)
                .longitud(balanzaDestinoLng != null ? balanzaDestinoLng.doubleValue() : null)
                .color("#F59E0B")
                .orden(3)
                .build();

        LoteDetalleViajeDto.WaypointDto puntoAlmacenDestino = LoteDetalleViajeDto.WaypointDto.builder()
                .nombre(destinoNombre)
                .tipo("almacen")
                .latitud(almacenLat != null ? almacenLat.doubleValue() : null)
                .longitud(almacenLng != null ? almacenLng.doubleValue() : null)
                .color(destinoColor)
                .orden(4)
                .build();

        return LoteDetalleViajeDto.builder()
                .asignacionId(asignacion.getId())
                .loteId(lote.getId())
                .codigoLote("LT-" + lote.getFechaCreacion().getYear() + "-" + String.format("%04d", lote.getId()))
                .socioNombre(persona.getNombres() + " " + persona.getPrimerApellido() +
                        (persona.getSegundoApellido() != null ? " " + persona.getSegundoApellido() : ""))
                .socioTelefono(persona.getNumeroCelular())
                .minaNombre(mina.getNombre())
                .minaLat(minaLat != null ? minaLat.doubleValue() : null)
                .minaLng(minaLng != null ? minaLng.doubleValue() : null)
                .tipoOperacion(lote.getTipoOperacion())
                .tipoMineral(lote.getTipoMineral())
                .mineralTags(obtenerMineralesTags(lote))
                .destinoNombre(destinoNombre)
                .destinoTipo(destinoTipo)
                .distanciaEstimadaKm(distanciaKm)
                .tiempoEstimadoHoras(tiempoHoras)
                .rutaCalculadaConExito(rutaExitosa)
                .metodoCalculo(metodoCalculo)
                .puntoOrigen(puntoOrigen)
                .puntoBalanzaCoop(puntoBalanzaCoop)
                .puntoBalanzaDestino(puntoBalanzaDestino)
                .puntoAlmacenDestino(puntoAlmacenDestino)
                .estado(asignacion.getEstado())
                .numeroCamion(asignacion.getNumeroCamion())
                .totalCamiones(lote.getCamionesSolicitados())
                .build();
    }
}