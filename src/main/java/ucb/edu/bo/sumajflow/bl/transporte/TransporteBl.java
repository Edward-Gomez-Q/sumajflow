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
    private final PersonaRepository personaRepository;
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

    // Inicia el viaje: "Esperando iniciar" -> "En camino a la mina"
    @Transactional
    public TransicionEstadoResponseDto iniciarViaje(
            Integer asignacionId,
            Double latInicial,
            Double lngInicial,
            String observaciones,
            Integer usuarioId
    ) {
        log.info("Iniciando viaje - Asignación: {}", asignacionId);

        AsignacionCamion asignacion = obtenerYValidarAsignacion(asignacionId, usuarioId);

        validarEstado(asignacion, "Esperando iniciar");

        String estadoAnterior = asignacion.getEstado();
        asignacion.setEstado("En camino a la mina");
        asignacion.setFechaInicio(LocalDateTime.now());

        Map<String, Object> obs = obtenerObservaciones(asignacion);
        obs.put("inicio_viaje", Map.of(
                "timestamp", LocalDateTime.now(),
                "lat", latInicial,
                "lng", lngInicial,
                "observaciones", observaciones != null ? observaciones : ""
        ));
        actualizarObservaciones(asignacion, obs);

        asignacionCamionRepository.save(asignacion);

        trackingBl.iniciarTracking(asignacionId, latInicial, lngInicial);

        actualizarEstadoLote(asignacion.getLotesId());

        log.info("Viaje iniciado - Nuevo estado: En camino a la mina");

        return construirRespuestaTransicion(
                asignacion,
                estadoAnterior,
                "En camino a la mina",
                "Dirígete a la mina para iniciar la carga"
        );
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
        ProximoPuntoControlDto proximoPunto = calcularProximoPunto(asignacion);

        return TransicionEstadoResponseDto.builder()
                .success(true)
                .message("Estado actualizado exitosamente")
                .estadoAnterior(estadoAnterior)
                .estadoNuevo(estadoNuevo)
                .proximoPaso(proximoPaso)
                .proximoPuntoControl(proximoPunto)
                .build();
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