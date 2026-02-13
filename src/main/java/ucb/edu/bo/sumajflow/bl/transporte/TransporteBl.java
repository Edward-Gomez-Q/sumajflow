package ucb.edu.bo.sumajflow.bl.transporte;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ucb.edu.bo.sumajflow.bl.LotesWebSocketBl;
import ucb.edu.bo.sumajflow.bl.NotificacionBl;
import ucb.edu.bo.sumajflow.bl.cooperativa.AuditoriaLotesBl;
import ucb.edu.bo.sumajflow.bl.routing.RoutingService;
import ucb.edu.bo.sumajflow.bl.tracking.TrackingBl;
import ucb.edu.bo.sumajflow.dto.routing.RutaCalculadaDto;
import ucb.edu.bo.sumajflow.dto.tracking.LoteDetalleViajeDto;
import ucb.edu.bo.sumajflow.dto.transporte.*;
import ucb.edu.bo.sumajflow.entity.*;
import ucb.edu.bo.sumajflow.repository.*;
import com.fasterxml.jackson.databind.JsonNode;

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
    private final PersonaRepository personaRepository;
    private final LotesWebSocketBl lotesWebSocketBl;

    // Flujo de estados del viaje
    private static final Map<String, EstadoTransicion> FLUJO_ESTADOS = Map.ofEntries(
            Map.entry("Esperando iniciar", new EstadoTransicion("En camino a la mina", "INICIO_VIAJE")),
            Map.entry("En camino a la mina", new EstadoTransicion("Esperando carguío", "LLEGADA_MINA")),
            Map.entry("Esperando carguío", new EstadoTransicion("En camino balanza cooperativa", "FIN_CARGUIO")),
            Map.entry("En camino balanza cooperativa", new EstadoTransicion("En camino balanza destino", "PESAJE_COOPERATIVA")),
            Map.entry("En camino balanza destino", new EstadoTransicion("En camino almacén destino", "PESAJE_DESTINO")),
            Map.entry("En camino almacén destino", new EstadoTransicion("Descargando", "LLEGADA_ALMACEN")),
            Map.entry("Descargando", new EstadoTransicion("Completado", "FIN_DESCARGA"))
    );
    private final LoteIngenioRepository loteIngenioRepository;
    private final LoteComercializadoraRepository loteComercializadoraRepository;

    // Clase interna para transiciones
    private static class EstadoTransicion {
        final String nuevoEstado;
        final String tipoEvento;

        EstadoTransicion(String nuevoEstado, String tipoEvento) {
            this.nuevoEstado = nuevoEstado;
            this.tipoEvento = tipoEvento;
        }
    }

    // ==================== CONSULTAS DE LOTES ====================

    /**
     * Obtener lotes asignados al transportista según filtro
     */
    public List<LoteAsignadoResumenDto> obtenerLotesTransportista(Integer usuarioId, String filtro) {
        var transportista = transportistaRepository.findByUsuariosId_Id(usuarioId)
                .orElseThrow(() -> new IllegalArgumentException("Transportista no encontrado"));

        List<AsignacionCamion> asignaciones = switch (filtro) {
            case "activos" -> asignacionCamionRepository.findByTransportistaIdAndEstadoNotIn(
                    transportista,
                    List.of("Completado", "Cancelado por rechazo")
            );
            case "completados" -> asignacionCamionRepository.findByTransportistaIdAndEstado(
                    transportista,
                    "Completado"
            );
            default -> asignacionCamionRepository.findByTransportistaId(transportista);
        };

        return asignaciones.stream()
                .map(this::convertToLoteResumen)
                .collect(Collectors.toList());
    }

    /**
     * Obtener detalle completo de un lote para iniciar viaje
     */
    public LoteDetalleViajeDto obtenerDetalleLoteParaViaje(Integer asignacionId, Integer usuarioId) {
        AsignacionCamion asignacion = obtenerYValidarAsignacion(asignacionId, usuarioId);
        return construirDetalleViaje(asignacion);
    }

    // ==================== FLUJO PRINCIPAL DEL VIAJE ====================

    /**
     * 1. Iniciar viaje: Esperando iniciar → En camino a la mina
     */
    public TransicionEstadoResponseDto iniciarViaje(
            Integer asignacionId,
            IniciarViajeDto dto,
            Integer usuarioId
    ) {
        log.info("=== INICIO: iniciarViaje - Asignación: {} ===", asignacionId);

        AsignacionCamion asignacion = obtenerYValidarAsignacion(asignacionId, usuarioId);
        validarEstadoEsperado(asignacion, "Esperando iniciar");

        Transportista transportista = asignacion.getTransportistaId();
        if (!"en_ruta".equals(transportista.getEstado())) {
            throw new IllegalStateException(
                    "No puedes iniciar el viaje. Estado del transportista: " + transportista.getEstado()
            );
        }

        LocalDateTime ahora = LocalDateTime.now();
        String estadoAnterior = asignacion.getEstado();

        // Registrar evento en observaciones
        Map<String, Object> eventoData = new HashMap<>();
        eventoData.put("timestamp", ahora.toString());
        eventoData.put("lat", dto.getLat());
        eventoData.put("lng", dto.getLng());
        eventoData.put("usuario_id", usuarioId);
        eventoData.put("dispositivo", "app_movil");
        if (dto.getObservaciones() != null && !dto.getObservaciones().trim().isEmpty()) {
            eventoData.put("observaciones", dto.getObservaciones().trim());
        }

        registrarEvento(asignacion, "inicio_viaje", eventoData);

        // Actualizar estado
        asignacion.setEstado("En camino a la mina");
        asignacion.setFechaInicio(ahora);
        asignacionCamionRepository.save(asignacion);

        Lotes lote = asignacion.getLotesId();
        String estadoLoteAnterior = lote.getEstado();

        // Operaciones asíncronas
        ejecutarOperacionesAsync(() -> {
            trackingBl.iniciarTracking(asignacionId, dto.getLat(), dto.getLng());
            trackingBl.actualizarEstadoYRegistrarEvento(
                    asignacionId, estadoAnterior, "En camino a la mina",
                    "INICIO_VIAJE", dto.getLat(), dto.getLng()
            );

            actualizarEstadoLote(lote);

            Lotes loteActualizado = lotesRepository.findById(lote.getId())
                    .orElseThrow(() -> new RuntimeException("Lote no encontrado"));

            if (!estadoLoteAnterior.equals(loteActualizado.getEstado())) {
                log.info("🔔 Estado del lote cambió: {} -> {}", estadoLoteAnterior, loteActualizado.getEstado());
                lotesWebSocketBl.publicarInicioTransporte(loteActualizado, asignacion.getNumeroCamion());
            }

            registrarAuditoria(lote, estadoAnterior, "En camino a la mina",
                    "INICIAR_VIAJE", "Transportista inició el viaje", asignacion);
            notificarInicioViaje(asignacion);
        });

        log.info("=== FIN: iniciarViaje exitoso ===");

        return construirRespuestaTransicion(
                asignacion,
                estadoAnterior,
                "En camino a la mina",
                "Dirígete a la mina para iniciar la carga del mineral",
                ahora
        );
    }

    /**
     * 2. Confirmar llegada a mina: En camino a la mina → Esperando carguío
     */
    @Transactional
    public TransicionEstadoResponseDto confirmarLlegadaMina(
            Integer asignacionId,
            ConfirmarLlegadaMinaDto dto,
            Integer usuarioId
    ) {
        log.info("=== Confirmando llegada a mina - Asignación: {} ===", asignacionId);

        AsignacionCamion asignacion = obtenerYValidarAsignacion(asignacionId, usuarioId);
        validarEstadoEsperado(asignacion, "En camino a la mina");

        LocalDateTime ahora = LocalDateTime.now();
        String estadoAnterior = asignacion.getEstado();

        // Registrar evento
        Map<String, Object> eventoData = new HashMap<>();
        eventoData.put("timestamp", ahora.toString());
        eventoData.put("lat", dto.getLat());
        eventoData.put("lng", dto.getLng());
        eventoData.put("pala_operativa", dto.getPalaOperativa());
        eventoData.put("mineral_visible", dto.getMineralVisible());
        if (dto.getFotoReferenciaUrl() != null) {
            eventoData.put("foto_referencia_url", dto.getFotoReferenciaUrl());
        }
        if (dto.getObservaciones() != null && !dto.getObservaciones().trim().isEmpty()) {
            eventoData.put("observaciones", dto.getObservaciones().trim());
        }

        registrarEvento(asignacion, "llegada_mina", eventoData);

        // Actualizar estado
        asignacion.setEstado("Esperando carguío");
        asignacionCamionRepository.save(asignacion);

        // Operaciones asíncronas
        ejecutarOperacionesAsync(() -> {
            trackingBl.registrarLlegadaPuntoControl(asignacionId, "mina", dto.getLat(), dto.getLng(), dto.getObservaciones());
            trackingBl.actualizarEstadoYRegistrarEvento(
                    asignacionId, estadoAnterior, "Esperando carguío",
                    "LLEGADA_MINA", dto.getLat(), dto.getLng()
            );
            registrarAuditoria(asignacion.getLotesId(), estadoAnterior, "Esperando carguío",
                    "LLEGADA_MINA", "Transportista llegó a la mina", asignacion);
        });

        return construirRespuestaTransicion(
                asignacion,
                estadoAnterior,
                "Esperando carguío",
                "Espera tu turno y realiza la carga del mineral",
                ahora
        );
    }

    /**
     * 3. Confirmar carguío: Esperando carguío → En camino balanza cooperativa
     */
    @Transactional
    public TransicionEstadoResponseDto confirmarCarguio(
            Integer asignacionId,
            ConfirmarCarguioDto dto,
            Integer usuarioId
    ) {
        log.info("=== Confirmando carguío - Asignación: {} ===", asignacionId);

        AsignacionCamion asignacion = obtenerYValidarAsignacion(asignacionId, usuarioId);
        validarEstadoEsperado(asignacion, "Esperando carguío");

        LocalDateTime ahora = LocalDateTime.now();
        String estadoAnterior = asignacion.getEstado();

        // Registrar evento
        Map<String, Object> eventoData = new HashMap<>();
        eventoData.put("timestamp", ahora.toString());
        eventoData.put("lat", dto.getLat());
        eventoData.put("lng", dto.getLng());
        eventoData.put("mineral_cargado_completamente", dto.getMineralCargadoCompletamente());
        if (dto.getFotoCamionCargadoUrl() != null) {
            eventoData.put("foto_camion_cargado_url", dto.getFotoCamionCargadoUrl());
        }
        if (dto.getObservaciones() != null && !dto.getObservaciones().trim().isEmpty()) {
            eventoData.put("observaciones", dto.getObservaciones().trim());
        }

        registrarEvento(asignacion, "carguio_completo", eventoData);

        // Actualizar estado
        asignacion.setEstado("En camino balanza cooperativa");
        asignacionCamionRepository.save(asignacion);

        // Operaciones asíncronas
        ejecutarOperacionesAsync(() -> {
            trackingBl.registrarSalidaPuntoControl(asignacionId, "mina", dto.getObservaciones());
            trackingBl.actualizarEstadoYRegistrarEvento(
                    asignacionId, estadoAnterior, "En camino balanza cooperativa",
                    "FIN_CARGUIO", dto.getLat(), dto.getLng()
            );
            actualizarEstadoLote(asignacion.getLotesId());
            registrarAuditoria(asignacion.getLotesId(), estadoAnterior, "En camino balanza cooperativa",
                    "FIN_CARGUIO", "Carguío completado", asignacion);
        });

        return construirRespuestaTransicion(
                asignacion,
                estadoAnterior,
                "En camino balanza cooperativa",
                "Dirígete a la balanza de la cooperativa para el primer pesaje",
                ahora
        );
    }

    /**
     * 4. Registrar pesaje cooperativa: En camino balanza cooperativa → En camino balanza destino
     */
    @Transactional
    public TransicionEstadoResponseDto registrarPesajeCooperativa(
            Integer asignacionId,
            RegistrarPesajeDto dto,
            Integer usuarioId
    ) {
        log.info("=== Registrando pesaje cooperativa - Asignación: {} ===", asignacionId);

        AsignacionCamion asignacion = obtenerYValidarAsignacion(asignacionId, usuarioId);
        validarEstadoEsperado(asignacion, "En camino balanza cooperativa");
        validarDatosPesaje(dto);

        return registrarPesajeInterno(asignacion, dto, usuarioId, "cooperativa",
                "pesaje_origen", "En camino balanza destino",
                "Dirígete a la balanza del destino para el segundo pesaje");
    }

    /**
     * 5. Registrar pesaje destino: En camino balanza destino → En camino almacén destino
     */
    @Transactional
    public TransicionEstadoResponseDto registrarPesajeDestino(
            Integer asignacionId,
            RegistrarPesajeDto dto,
            Integer usuarioId
    ) {
        log.info("=== Registrando pesaje destino - Asignación: {} ===", asignacionId);

        AsignacionCamion asignacion = obtenerYValidarAsignacion(asignacionId, usuarioId);
        validarEstadoEsperado(asignacion, "En camino balanza destino");
        validarDatosPesaje(dto);

        return registrarPesajeInterno(asignacion, dto, usuarioId, "destino",
                "pesaje_destino", "En camino almacén destino",
                "Dirígete al almacén para descargar el mineral");
    }

    /**
     * 6. Confirmar llegada a almacén: En camino almacén destino → Descargando
     */
    @Transactional
    public TransicionEstadoResponseDto confirmarLlegadaAlmacen(
            Integer asignacionId,
            ConfirmarLlegadaAlmacenDto dto,
            Integer usuarioId
    ) {
        log.info("=== Confirmando llegada a almacén - Asignación: {} ===", asignacionId);

        AsignacionCamion asignacion = obtenerYValidarAsignacion(asignacionId, usuarioId);
        validarEstadoEsperado(asignacion, "En camino almacén destino");

        LocalDateTime ahora = LocalDateTime.now();
        String estadoAnterior = asignacion.getEstado();

        // Registrar evento
        Map<String, Object> eventoData = new HashMap<>();
        eventoData.put("timestamp", ahora.toString());
        eventoData.put("lat", dto.getLat());
        eventoData.put("lng", dto.getLng());
        eventoData.put("confirmacion_llegada", dto.getConfirmacionLlegada());
        if (dto.getObservaciones() != null && !dto.getObservaciones().trim().isEmpty()) {
            eventoData.put("observaciones", dto.getObservaciones().trim());
        }

        registrarEvento(asignacion, "llegada_almacen", eventoData);

        // Actualizar estado
        asignacion.setEstado("Descargando");
        asignacionCamionRepository.save(asignacion);

        // Operaciones asíncronas
        ejecutarOperacionesAsync(() -> {
            trackingBl.actualizarEstadoYRegistrarEvento(
                    asignacionId, estadoAnterior, "Descargando",
                    "LLEGADA_ALMACEN", dto.getLat(), dto.getLng()
            );
            registrarAuditoria(asignacion.getLotesId(), estadoAnterior, "Descargando",
                    "LLEGADA_ALMACEN", "Llegada a almacén confirmada", asignacion);
        });

        return construirRespuestaTransicion(
                asignacion,
                estadoAnterior,
                "Descargando",
                "Realiza la descarga del mineral",
                ahora
        );
    }

    /**
     * 7. Confirmar descarga: Descargando → Descargando (preparado para finalizar)
     */
    @Transactional
    public TransicionEstadoResponseDto confirmarDescarga(
            Integer asignacionId,
            ConfirmarDescargaDto dto,
            Integer usuarioId
    ) {
        log.info("=== Confirmando descarga - Asignación: {} ===", asignacionId);

        AsignacionCamion asignacion = obtenerYValidarAsignacion(asignacionId, usuarioId);
        validarEstadoEsperado(asignacion, "Descargando");

        LocalDateTime ahora = LocalDateTime.now();
        String estadoAnterior = asignacion.getEstado();

        // Registrar evento
        Map<String, Object> eventoData = new HashMap<>();
        eventoData.put("timestamp", ahora.toString());
        eventoData.put("lat", dto.getLat());
        eventoData.put("lng", dto.getLng());
        if (dto.getObservaciones() != null && !dto.getObservaciones().trim().isEmpty()) {
            eventoData.put("observaciones", dto.getObservaciones().trim());
        }

        registrarEvento(asignacion, "descarga_iniciada", eventoData);

        // Mantener estado "Descargando" hasta finalizar ruta
        asignacionCamionRepository.save(asignacion);

        // Operaciones asíncronas
        ejecutarOperacionesAsync(() -> {
            trackingBl.actualizarEstadoYRegistrarEvento(
                    asignacionId, estadoAnterior, "Descargando",
                    "INICIO_DESCARGA", dto.getLat(), dto.getLng()
            );
        });

        return construirRespuestaTransicion(
                asignacion,
                estadoAnterior,
                "Descargando",
                "Completa la descarga y luego finaliza la ruta",
                ahora
        );
    }

    /**
     * 8. Finalizar ruta: Descargando → Completado
     */
    public TransicionEstadoResponseDto finalizarRuta(
            Integer asignacionId,
            FinalizarRutaDto dto,
            Integer usuarioId
    ) {
        log.info("=== Finalizando ruta - Asignación: {} ===", asignacionId);

        AsignacionCamion asignacion = obtenerYValidarAsignacion(asignacionId, usuarioId);
        validarEstadoEsperado(asignacion, "Descargando");

        LocalDateTime ahora = LocalDateTime.now();
        String estadoAnterior = asignacion.getEstado();

        // Registrar evento
        Map<String, Object> eventoData = new HashMap<>();
        eventoData.put("timestamp", ahora.toString());
        eventoData.put("lat", dto.getLat());
        eventoData.put("lng", dto.getLng());
        if (dto.getObservacionesFinales() != null && !dto.getObservacionesFinales().trim().isEmpty()) {
            eventoData.put("observaciones_finales", dto.getObservacionesFinales().trim());
        }

        registrarEvento(asignacion, "ruta_finalizada", eventoData);

        // Actualizar estado
        asignacion.setEstado("Completado");
        asignacion.setFechaFin(ahora);
        asignacionCamionRepository.save(asignacion);

        // Cambiar estado del transportista a disponible
        Transportista transportista = asignacion.getTransportistaId();
        transportista.setEstado("aprobado");
        transportista.setViajesCompletados(transportista.getViajesCompletados() + 1);
        transportistaRepository.save(transportista);

        Lotes lote = asignacion.getLotesId();
        String estadoLoteAnterior = lote.getEstado();

        // Operaciones asíncronas
        ejecutarOperacionesAsync(() -> {
            trackingBl.actualizarEstadoYRegistrarEvento(
                    asignacionId, estadoAnterior, "Completado",
                    "FIN_DESCARGA", dto.getLat(), dto.getLng()
            );

            actualizarEstadoLote(lote);

            Lotes loteActualizado = lotesRepository.findById(lote.getId())
                    .orElseThrow(() -> new RuntimeException("Lote no encontrado"));

            if (!estadoLoteAnterior.equals(loteActualizado.getEstado()) &&
                    "Transporte completo".equals(loteActualizado.getEstado())) {
                log.info("🔔 Lote completado: {} -> {}", estadoLoteAnterior, loteActualizado.getEstado());
                lotesWebSocketBl.publicarFinTransporte(loteActualizado, asignacion.getNumeroCamion());
            }

            registrarAuditoria(lote, estadoAnterior, "Completado",
                    "FIN_RUTA", "Viaje completado exitosamente", asignacion);
            notificarFinalizacionViaje(asignacion);
        });

        return construirRespuestaTransicion(
                asignacion,
                estadoAnterior,
                "Completado",
                "Viaje finalizado exitosamente",
                ahora
        );
    }

    // ==================== MÉTODOS AUXILIARES ====================

    /**
     * Registrar pesaje interno (cooperativa o destino)
     */
    private TransicionEstadoResponseDto registrarPesajeInterno(
            AsignacionCamion asignacion,
            RegistrarPesajeDto dto,
            Integer usuarioId,
            String tipoPesaje,
            String nombreEvento,
            String nuevoEstado,
            String mensajeExito
    ) {
        LocalDateTime ahora = LocalDateTime.now();
        String estadoAnterior = asignacion.getEstado();

        // Calcular peso neto
        BigDecimal pesoNeto = dto.getPesoBrutoKg().subtract(dto.getPesoTaraKg());

        // Guardar en tabla pesajes
        Pesajes pesaje = new Pesajes();
        pesaje.setAsignacionCamionId(asignacion);
        pesaje.setTipoPesaje(nombreEvento);
        pesaje.setPesoBruto(dto.getPesoBrutoKg());
        pesaje.setPesoTara(dto.getPesoTaraKg());
        pesaje.setPesoNeto(pesoNeto);
        pesaje.setFechaPesaje(ahora);
        pesaje.setObservaciones(dto.getObservaciones());
        pesajesRepository.save(pesaje);

        // Registrar evento en observaciones
        Map<String, Object> eventoData = new HashMap<>();
        eventoData.put("timestamp", ahora.toString());
        eventoData.put("lat", dto.getLat());
        eventoData.put("lng", dto.getLng());
        eventoData.put("peso_bruto_kg", dto.getPesoBrutoKg());
        eventoData.put("peso_tara_kg", dto.getPesoTaraKg());
        eventoData.put("peso_neto_kg", pesoNeto);
        if (dto.getTicketPesajeUrl() != null) {
            eventoData.put("ticket_pesaje_url", dto.getTicketPesajeUrl());
        }
        if (dto.getObservaciones() != null && !dto.getObservaciones().trim().isEmpty()) {
            eventoData.put("observaciones", dto.getObservaciones().trim());
        }

        registrarEvento(asignacion, nombreEvento, eventoData);

        // Actualizar estado
        asignacion.setEstado(nuevoEstado);
        asignacionCamionRepository.save(asignacion);

        // Operaciones asíncronas
        String tipoEventoPesaje = "cooperativa".equals(tipoPesaje) ? "PESAJE_COOPERATIVA" : "PESAJE_DESTINO";
        ejecutarOperacionesAsync(() -> {
            trackingBl.actualizarEstadoYRegistrarEvento(
                    asignacion.getId(), estadoAnterior, nuevoEstado,
                    tipoEventoPesaje, dto.getLat(), dto.getLng()
            );
            registrarAuditoria(asignacion.getLotesId(), estadoAnterior, nuevoEstado,
                    tipoEventoPesaje, "Pesaje " + tipoPesaje + " registrado: " + pesoNeto + " kg netos", asignacion);
        });

        log.info("Pesaje {} registrado - Peso neto: {} kg", tipoPesaje, pesoNeto);

        return construirRespuestaTransicion(
                asignacion,
                estadoAnterior,
                nuevoEstado,
                mensajeExito,
                ahora
        );
    }

    /**
     * Validar datos de pesaje
     */
    private void validarDatosPesaje(RegistrarPesajeDto dto) {
        if (dto.getPesoBrutoKg().compareTo(dto.getPesoTaraKg()) <= 0) {
            throw new IllegalArgumentException("El peso bruto debe ser mayor que el peso tara");
        }
    }

    /**
     * Obtener y validar asignación
     */
    private AsignacionCamion obtenerYValidarAsignacion(Integer asignacionId, Integer usuarioId) {
        AsignacionCamion asignacion = asignacionCamionRepository.findById(asignacionId)
                .orElseThrow(() -> new IllegalArgumentException("Asignación no encontrada"));

        if (!asignacion.getTransportistaId().getUsuariosId().getId().equals(usuarioId)) {
            throw new SecurityException("No autorizado");
        }

        return asignacion;
    }

    /**
     * Validar estado esperado
     */
    private void validarEstadoEsperado(AsignacionCamion asignacion, String estadoEsperado) {
        if (!asignacion.getEstado().equals(estadoEsperado)) {
            throw new IllegalStateException(
                    "No se puede realizar esta acción desde el estado: " + asignacion.getEstado()
            );
        }
    }

    /**
     * Registrar evento en observaciones (JSONB)
     */
    private void registrarEvento(AsignacionCamion asignacion, String nombreEvento, Map<String, Object> eventoData) {
        try {
            Map<String, Object> observaciones = obtenerObservaciones(asignacion);
            observaciones.put(nombreEvento, eventoData);
            asignacion.setObservaciones(objectMapper.writeValueAsString(observaciones));
        } catch (JsonProcessingException e) {
            log.error("Error al registrar evento: {}", e.getMessage());
        }
    }

    /**
     * Obtener observaciones desde JSONB
     */
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

    /**
     * Ejecutar operaciones asíncronas sin bloquear respuesta
     */
    private void ejecutarOperacionesAsync(Runnable operaciones) {
        try {
            operaciones.run();
        } catch (Exception e) {
            log.error("Error en operaciones asíncronas (no crítico): {}", e.getMessage());
        }
    }

    /**
     * Actualizar estado del lote basado en camiones
     */
    private void actualizarEstadoLote(Lotes lote) {
        List<AsignacionCamion> asignaciones = asignacionCamionRepository.findByLotesId(lote);
        List<AsignacionCamion> asignacionesActivas = asignaciones.stream()
                .filter(a -> !a.getEstado().equals("Cancelado por rechazo"))
                .toList();

        if (asignacionesActivas.isEmpty()) return;

        // Definir jerarquía de estados
        List<String> ordenEstados = List.of(
                "Esperando iniciar", "En camino a la mina", "Esperando carguío",
                "En camino balanza cooperativa", "En camino balanza destino",
                "En camino almacén destino", "Descargando", "Completado"
        );

        // Encontrar estado más atrasado
        String estadoMasAtrasado = asignacionesActivas.stream()
                .map(AsignacionCamion::getEstado)
                .min(Comparator.comparingInt(estado -> {
                    int index = ordenEstados.indexOf(estado);
                    return index == -1 ? Integer.MAX_VALUE : index;
                }))
                .orElse("Esperando iniciar");

        // Mapear a estado del lote
        String nuevoEstadoLote;
        if (estadoMasAtrasado.equals("Esperando iniciar")) {
            nuevoEstadoLote = "Aprobado - Pendiente de iniciar";
        } else if (estadoMasAtrasado.equals("Completado")) {
            nuevoEstadoLote = "Transporte completo";
        } else {
            nuevoEstadoLote = "En Transporte";
        }

        // Actualizar solo si cambió
        if (!nuevoEstadoLote.equals(lote.getEstado())) {
            String estadoAnterior = lote.getEstado();
            lote.setEstado(nuevoEstadoLote);

            if ("En Transporte".equals(nuevoEstadoLote) && lote.getFechaInicioTransporte() == null) {
                lote.setFechaInicioTransporte(LocalDateTime.now());
            } else if ("Transporte completo".equals(nuevoEstadoLote)) {
                lote.setFechaFinTransporte(LocalDateTime.now());

                // Calcular y actualizar peso real total
                BigDecimal pesoRealTotal = calcularPesoRealTotal(asignacionesActivas);
                lote.setPesoTotalReal(pesoRealTotal);

                log.info("Peso real total del lote {}: {} kg", lote.getId(), pesoRealTotal);
            }

            lotesRepository.save(lote);
            log.info("Estado del lote {} actualizado: {} -> {}", lote.getId(), estadoAnterior, nuevoEstadoLote);
        }
    }

    /**
     * Calcular peso real total sumando los pesos netos de destino de todas las asignaciones
     */
    private BigDecimal calcularPesoRealTotal(List<AsignacionCamion> asignaciones) {
        return asignaciones.stream()
                .map(this::extraerPesoNetoDestino)
                .filter(peso -> peso != null && peso.compareTo(BigDecimal.ZERO) > 0)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Extraer peso_neto_kg del pesaje_destino desde el JSON de observaciones
     */
    private BigDecimal extraerPesoNetoDestino(AsignacionCamion asignacion) {
        try {
            if (asignacion.getObservaciones() == null || asignacion.getObservaciones().isEmpty()) {
                return BigDecimal.ZERO;
            }

            // Parsear el JSON
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode rootNode = objectMapper.readTree(asignacion.getObservaciones());

            // Navegar a pesaje_destino.peso_neto_kg
            JsonNode pesajeDestino = rootNode.path("pesaje_destino");
            if (pesajeDestino.isMissingNode()) {
                log.warn("No se encontró pesaje_destino para asignación {}", asignacion.getId());
                return BigDecimal.ZERO;
            }

            JsonNode pesoNetoNode = pesajeDestino.path("peso_neto_kg");
            if (pesoNetoNode.isMissingNode() || pesoNetoNode.isNull()) {
                log.warn("No se encontró peso_neto_kg en pesaje_destino para asignación {}", asignacion.getId());
                return BigDecimal.ZERO;
            }

            return BigDecimal.valueOf(pesoNetoNode.asDouble());

        } catch (Exception e) {
            log.error("Error al extraer peso neto de destino para asignación {}: {}",
                    asignacion.getId(), e.getMessage());
            return BigDecimal.ZERO;
        }
    }
    /**
     * Registrar auditoría
     */
    private void registrarAuditoria(
            Lotes lote,
            String estadoAnterior,
            String estadoNuevo,
            String accion,
            String descripcion,
            AsignacionCamion asignacion
    ) {
        try {
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("asignacion_camion_id", asignacion.getId());
            metadata.put("transportista_id", asignacion.getTransportistaId().getId());
            metadata.put("numero_camion", asignacion.getNumeroCamion());
            metadata.put("placa_vehiculo", asignacion.getTransportistaId().getPlacaVehiculo());

            auditoriaLotesBl.registrarAuditoria(
                    lote.getId(),
                    "transportista",
                    estadoAnterior,
                    estadoNuevo,
                    accion,
                    descripcion,
                    null,
                    metadata,
                    null
            );
        } catch (Exception e) {
            log.error("Error al registrar auditoría: {}", e.getMessage());
        }
    }

    /**
     * Notificar inicio de viaje al socio
     */
    private void notificarInicioViaje(AsignacionCamion asignacion) {
        try {
            Lotes lote = asignacion.getLotesId();
            Socio socio = lote.getMinasId().getSocioId();
            Integer socioUsuarioId = socio.getUsuariosId().getId();

            Persona personaTransportista = personaRepository.findByUsuariosId(asignacion.getTransportistaId().getUsuariosId()).orElse(null);
            String nombreTransportista = personaTransportista != null
                    ? personaTransportista.getNombres() + " " + personaTransportista.getPrimerApellido()
                    : "Transportista";

            Map<String, Object> metadata = new HashMap<>();
            metadata.put("lote_id", lote.getId());
            metadata.put("asignacion_camion_id", asignacion.getId());
            metadata.put("numero_camion", asignacion.getNumeroCamion());
            metadata.put("transportista_nombre", nombreTransportista);

            String titulo = String.format("Viaje iniciado - Camión #%d", asignacion.getNumeroCamion());
            String mensaje = String.format("%s ha iniciado el transporte del lote %s", nombreTransportista, lote.getId().toString());

            notificacionBl.crearNotificacion(socioUsuarioId, "info", titulo, mensaje, metadata);

            // Notificar a la cooperativa que el camión está en camino a la mina
            Integer cooperativaUsuarioId = lote.getMinasId().getSectoresId().getCooperativaId().getUsuariosId().getId();
            String tituloCoop = String.format("Camión #%d en camino a la mina", asignacion.getNumeroCamion());
            String mensajeCoop = String.format("El camión #%d ha iniciado su viaje hacia la mina para el lote %s", asignacion.getNumeroCamion(), lote.getId().toString());
            notificacionBl.crearNotificacion(cooperativaUsuarioId, "info", tituloCoop, mensajeCoop, metadata);

            //Notificar al destino (ingenio o comercializadora) que el camión está en camino
            Integer destinoUsuarioId = null;

            if("procesamiento_planta".equals(lote.getTipoOperacion())) {
                LoteIngenio loteIngenio = loteIngenioRepository.findByLotesId(lote)
                        .orElseThrow(() -> new IllegalArgumentException("Relación lote-ingenio no encontrada"));
                destinoUsuarioId = loteIngenio.getIngenioMineroId().getUsuariosId().getId();

            } else {
                LoteComercializadora loteComercializadora = loteComercializadoraRepository.findByLotesId(lote)
                        .orElseThrow(() -> new IllegalArgumentException("Relación lote-comercializadora no encontrada"));
                destinoUsuarioId = loteComercializadora.getComercializadoraId().getUsuariosId().getId();
            }
            String tituloDestino = String.format("Camión #%d en camino al destino", asignacion.getNumeroCamion());
            String mensajeDestino = String.format("El camión #%d ha iniciado su viaje hacia el destino para el lote %s", asignacion.getNumeroCamion(), lote.getId().toString());
            notificacionBl.crearNotificacion(destinoUsuarioId, "info", tituloDestino, mensajeDestino, metadata);

        } catch (Exception e) {
            log.error("Error al enviar notificación: {}", e.getMessage());
        }
    }

    /**
     * Notificar finalización de viaje
     */
    private void notificarFinalizacionViaje(AsignacionCamion asignacion) {
        try {
            Lotes lote = asignacion.getLotesId();
            Socio socio = lote.getMinasId().getSocioId();
            Integer socioUsuarioId = socio.getUsuariosId().getId();

            Map<String, Object> metadata = new HashMap<>();
            metadata.put("lote_id", lote.getId());
            metadata.put("asignacion_camion_id", asignacion.getId());
            metadata.put("numero_camion", asignacion.getNumeroCamion());

            String titulo = "Viaje completado";
            String mensaje = String.format("El transporte del lote %s ha sido completado exitosamente", lote.getId().toString());

            notificacionBl.crearNotificacion(socioUsuarioId, "success", titulo, mensaje, metadata);

            // Notificar a la cooperativa que el camión ha completado el viaje
            Integer cooperativaUsuarioId = lote.getMinasId().getSectoresId().getCooperativaId().getUsuariosId().getId();
            String tituloCoop = String.format("Camión #%d ha completado el viaje", asignacion.getNumeroCamion());
            String mensajeCoop = String.format("El camión #%d ha completado su viaje para el lote %s", asignacion.getNumeroCamion(), lote.getId().toString());
            notificacionBl.crearNotificacion(cooperativaUsuarioId, "success", tituloCoop, mensajeCoop, metadata);
            //Notificar al destino (ingenio o comercializadora) que el camión ha completado el viaje
            Integer destinoUsuarioId = null;
            if("procesamiento_planta".equals(lote.getTipoOperacion())) {
                LoteIngenio loteIngenio = loteIngenioRepository.findByLotesId(lote)
                        .orElseThrow(() -> new IllegalArgumentException("Relación lote-ingenio no encontrada"));
                destinoUsuarioId = loteIngenio.getIngenioMineroId().getUsuariosId().getId();

            } else {
                LoteComercializadora loteComercializadora = loteComercializadoraRepository.findByLotesId(lote)
                        .orElseThrow(() -> new IllegalArgumentException("Relación lote-comercializadora no encontrada"));
                destinoUsuarioId = loteComercializadora.getComercializadoraId().getUsuariosId().getId();
            }
            String tituloDestino = String.format("Camión #%d ha completado el viaje", asignacion.getNumeroCamion());
            String mensajeDestino = String.format("El camión #%d ha completado su viaje para el lote %s", asignacion.getNumeroCamion(), lote.getId().toString());
            notificacionBl.crearNotificacion(destinoUsuarioId, "success", tituloDestino, mensajeDestino, metadata);

        } catch (Exception e) {
            log.error("Error al enviar notificación: {}", e.getMessage());
        }
    }

    /**
     * Construir respuesta de transición
     */
    private TransicionEstadoResponseDto construirRespuestaTransicion(
            AsignacionCamion asignacion,
            String estadoAnterior,
            String estadoNuevo,
            String proximoPaso,
            LocalDateTime timestamp
    ) {
        try {
            ProximoPuntoControlDto proximoPunto = calcularProximoPunto(asignacion);

            Map<String, Object> metadata = new HashMap<>();
            metadata.put("asignacion_id", asignacion.getId());
            metadata.put("numero_camion", asignacion.getNumeroCamion());
            metadata.put("lote_id", asignacion.getLotesId().getId());

            return TransicionEstadoResponseDto.builder()
                    .success(true)
                    .message("Estado actualizado exitosamente")
                    .estadoAnterior(estadoAnterior)
                    .estadoNuevo(estadoNuevo)
                    .proximoPaso(proximoPaso)
                    .proximoPuntoControl(proximoPunto)
                    .metadata(metadata)
                    .timestamp(timestamp)
                    .build();
        } catch (Exception e) {
            log.error("Error al construir respuesta: {}", e.getMessage());
            return TransicionEstadoResponseDto.builder()
                    .success(true)
                    .message("Estado actualizado exitosamente")
                    .estadoAnterior(estadoAnterior)
                    .estadoNuevo(estadoNuevo)
                    .proximoPaso(proximoPaso)
                    .timestamp(timestamp)
                    .build();
        }
    }

    /**
     * Calcular próximo punto de control
     */
    private ProximoPuntoControlDto calcularProximoPunto(AsignacionCamion asignacion) {
        try {
            Lotes lote = asignacion.getLotesId();
            String estadoActual = asignacion.getEstado();

            switch (estadoActual) {
                case "En camino a la mina":
                    Minas mina = lote.getMinasId();
                    return ProximoPuntoControlDto.builder()
                            .tipo("mina")
                            .nombre(mina.getNombre())
                            .latitud(mina.getLatitud().doubleValue())
                            .longitud(mina.getLongitud().doubleValue())
                            .descripcion("Mina de origen")
                            .build();

                case "En camino balanza cooperativa":
                    Cooperativa coop = lote.getMinasId().getSectoresId().getCooperativaId();
                    if (!coop.getBalanzaCooperativaList().isEmpty()) {
                        var balanza = coop.getBalanzaCooperativaList().getFirst();
                        return ProximoPuntoControlDto.builder()
                                .tipo("balanza_cooperativa")
                                .nombre("Balanza " + coop.getRazonSocial())
                                .latitud(balanza.getLatitud().doubleValue())
                                .longitud(balanza.getLongitud().doubleValue())
                                .descripcion("Primer pesaje")
                                .build();
                    }
                    break;

                case "En camino balanza destino":
                case "En camino almacén destino":
                    if (!lote.getLoteIngenioList().isEmpty()) {
                        var ingenio = lote.getLoteIngenioList().getFirst().getIngenioMineroId();
                        if (estadoActual.contains("balanza") && !ingenio.getBalanzasIngenioList().isEmpty()) {
                            var balanza = ingenio.getBalanzasIngenioList().getFirst();
                            return ProximoPuntoControlDto.builder()
                                    .tipo("balanza_destino")
                                    .nombre("Balanza " + ingenio.getRazonSocial())
                                    .latitud(balanza.getLatitud().doubleValue())
                                    .longitud(balanza.getLongitud().doubleValue())
                                    .descripcion("Segundo pesaje")
                                    .build();
                        } else if (estadoActual.contains("almacén") && !ingenio.getAlmacenesIngenioList().isEmpty()) {
                            var almacen = ingenio.getAlmacenesIngenioList().getFirst();
                            return ProximoPuntoControlDto.builder()
                                    .tipo("almacen")
                                    .nombre("Almacén " + ingenio.getRazonSocial())
                                    .latitud(almacen.getLatitud().doubleValue())
                                    .longitud(almacen.getLongitud().doubleValue())
                                    .descripcion("Punto de descarga")
                                    .build();
                        }
                    } else if (!lote.getLoteComercializadoraList().isEmpty()) {
                        var comercializadora = lote.getLoteComercializadoraList().getFirst().getComercializadoraId();
                        if (estadoActual.contains("balanza") && !comercializadora.getBalanzasList().isEmpty()) {
                            var balanza = comercializadora.getBalanzasList().getFirst();
                            return ProximoPuntoControlDto.builder()
                                    .tipo("balanza_destino")
                                    .nombre("Balanza " + comercializadora.getRazonSocial())
                                    .latitud(balanza.getLatitud().doubleValue())
                                    .longitud(balanza.getLongitud().doubleValue())
                                    .descripcion("Segundo pesaje")
                                    .build();
                        } else if (estadoActual.contains("almacén") && !comercializadora.getAlmacenesList().isEmpty()) {
                            var almacen = comercializadora.getAlmacenesList().getFirst();
                            return ProximoPuntoControlDto.builder()
                                    .tipo("almacen")
                                    .nombre("Almacén " + comercializadora.getRazonSocial())
                                    .latitud(almacen.getLatitud().doubleValue())
                                    .longitud(almacen.getLongitud().doubleValue())
                                    .descripcion("Punto de descarga")
                                    .build();
                        }
                    }
                    break;
            }

            return null;
        } catch (Exception e) {
            log.error("Error al calcular próximo punto: {}", e.getMessage());
            return null;
        }
    }

    // Métodos de conversión a DTOs (mantener los existentes)
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
                .fechaAsignacion(asignacion.getCreatedAt())
                .mineralTags(lote.getLoteMineralesList().stream()
                        .map(lm -> lm.getMineralesId().getNomenclatura())
                        .collect(Collectors.toList()))
                .build();
    }

    private LoteDetalleViajeDto construirDetalleViaje(AsignacionCamion asignacion) {
        Lotes lote = asignacion.getLotesId();
        var mina = lote.getMinasId();
        var socio = mina.getSocioId();
        var persona = personaRepository.findByUsuariosId(socio.getUsuariosId()).orElse(null);
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
                balanzaDestinoLat = ingenio.getBalanzasIngenioList().getFirst().getLatitud();
                balanzaDestinoLng = ingenio.getBalanzasIngenioList().getFirst().getLongitud();
            }
            if (!ingenio.getAlmacenesIngenioList().isEmpty()) {
                almacenLat = ingenio.getAlmacenesIngenioList().getFirst().getLatitud();
                almacenLng = ingenio.getAlmacenesIngenioList().getFirst().getLongitud();
            }
        } else if (!lote.getLoteComercializadoraList().isEmpty()) {
            var comercializadora = lote.getLoteComercializadoraList().getFirst().getComercializadoraId();
            destinoNombre = comercializadora.getRazonSocial();
            destinoTipo = "Comercializadora";
            destinoColor = "#DC2626";

            if (!comercializadora.getBalanzasList().isEmpty()) {
                balanzaDestinoLat = comercializadora.getBalanzasList().getFirst().getLatitud();
                balanzaDestinoLng = comercializadora.getBalanzasList().getFirst().getLongitud();
            }
            if (!comercializadora.getAlmacenesList().isEmpty()) {
                almacenLat = comercializadora.getAlmacenesList().getFirst().getLatitud();
                almacenLng = comercializadora.getAlmacenesList().getFirst().getLongitud();
            }
        }

        // Calcular ruta
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
            } catch (Exception e) {
                log.error("Error al calcular ruta: {}", e.getMessage());
            }
        }

        // Construir waypoints
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
                .socioNombre(persona != null ? persona.getNombres() + " " + persona.getPrimerApellido() : "")
                .socioTelefono(persona != null ? persona.getNumeroCelular() : "")
                .minaNombre(mina.getNombre())
                .minaLat(minaLat != null ? minaLat.doubleValue() : null)
                .minaLng(minaLng != null ? minaLng.doubleValue() : null)
                .tipoOperacion(lote.getTipoOperacion())
                .tipoMineral(lote.getTipoMineral())
                .mineralTags(lote.getLoteMineralesList().stream()
                        .map(lm -> lm.getMineralesId().getNomenclatura())
                        .collect(Collectors.toList()))
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
                .estado("Pendiente de aprobación por Ingenio/Comercializadora".equals(lote.getEstado()) ? "Esperando aprobación del destino" : asignacion.getEstado())
                .numeroCamion(asignacion.getNumeroCamion())
                .totalCamiones(lote.getCamionesSolicitados())
                .build();
    }
}