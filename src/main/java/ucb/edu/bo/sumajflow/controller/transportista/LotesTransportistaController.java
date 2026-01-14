package ucb.edu.bo.sumajflow.controller.transportista;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ucb.edu.bo.sumajflow.bl.transporte.TransporteBl;
import ucb.edu.bo.sumajflow.dto.tracking.LoteAsignadoResumenDto;
import ucb.edu.bo.sumajflow.dto.tracking.LoteDetalleViajeDto;
import ucb.edu.bo.sumajflow.dto.transporte.*;
import ucb.edu.bo.sumajflow.entity.AsignacionCamion;
import ucb.edu.bo.sumajflow.repository.AsignacionCamionRepository;
import ucb.edu.bo.sumajflow.utils.JwtUtil;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Controlador unificado para transportistas
 * Gestiona consultas de lotes y flujo completo del viaje
 */
@Slf4j
@RestController
@RequestMapping("/transportista")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class LotesTransportistaController {

    private final TransporteBl transporteBl;
    private final JwtUtil jwtUtil;
    private final AsignacionCamionRepository asignacionCamionRepository;
    private final ObjectMapper objectMapper;

    // ==================== CONSULTAS DE LOTES ====================

    /**
     * Obtiene los lotes asignados al transportista autenticado
     * GET /transportista/mis-lotes
     */
    @GetMapping("/mis-lotes")
    public ResponseEntity<Map<String, Object>> getMisLotes(
            @RequestHeader("Authorization") String token,
            @RequestParam(required = false, defaultValue = "activos") String filtro) {

        Map<String, Object> response = new HashMap<>();

        try {
            Integer usuarioId = extractUsuarioId(token);
            List<LoteAsignadoResumenDto> lotesResumen = transporteBl.obtenerLotesTransportista(usuarioId, filtro);

            response.put("success", true);
            response.put("data", lotesResumen);
            response.put("total", lotesResumen.size());

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);

        } catch (Exception e) {
            log.error("Error al obtener lotes del transportista", e);
            response.put("success", false);
            response.put("message", "Error interno del servidor");
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * Obtiene el detalle completo de un lote asignado para iniciar el viaje
     * GET /transportista/lote/{asignacionId}/detalle
     */
    @GetMapping("/lote/{asignacionId}/detalle")
    public ResponseEntity<Map<String, Object>> getDetalleLoteParaViaje(
            @PathVariable Integer asignacionId,
            @RequestHeader("Authorization") String token) {

        Map<String, Object> response = new HashMap<>();

        try {
            Integer usuarioId = extractUsuarioId(token);
            LoteDetalleViajeDto detalle = transporteBl.obtenerDetalleLoteParaViaje(asignacionId, usuarioId);

            response.put("success", true);
            response.put("data", detalle);

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException | SecurityException e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);

        } catch (Exception e) {
            log.error("Error al obtener detalle del lote", e);
            response.put("success", false);
            response.put("message", "Error interno del servidor");
            return ResponseEntity.internalServerError().body(response);
        }
    }

    // ==================== GESTIÓN DEL VIAJE ====================

    /**
     * Iniciar viaje: Esperando iniciar → En camino a la mina
     * POST /transportista/viaje/{asignacionId}/iniciar
     */
    @PostMapping("/viaje/{asignacionId}/iniciar")
    public ResponseEntity<TransicionEstadoResponseDto> iniciarViaje(
            @PathVariable Integer asignacionId,
            @RequestBody IniciarViajeRequestDto request,
            @RequestHeader("Authorization") String token
    ) {
        log.info("=== REQUEST: Iniciar viaje - Asignacion: {} ===", asignacionId);

        TransicionEstadoResponseDto errorResponse;

        try {
            // 1. Extraer usuario ID
            Integer usuarioId = extractUsuarioId(token);
            log.info("Usuario autenticado: {}", usuarioId);

            // 2. Validar coordenadas
            if (request.getLat() == null || request.getLng() == null) {
                log.warn("Coordenadas GPS faltantes");
                errorResponse = TransicionEstadoResponseDto.builder()
                        .success(false)
                        .message("Las coordenadas GPS son obligatorias para iniciar el viaje")
                        .build();
                return ResponseEntity.badRequest().body(errorResponse);
            }

            log.info("Coordenadas recibidas - Lat: {}, Lng: {}", request.getLat(), request.getLng());

            // 3. Ejecutar lógica de negocio
            TransicionEstadoResponseDto response = transporteBl.iniciarViaje(
                    asignacionId,
                    request.getLat(),
                    request.getLng(),
                    request.getObservaciones(),
                    usuarioId
            );

            log.info("=== RESPONSE: Viaje iniciado exitosamente ===");
            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            log.error("Error de validacion: {}", e.getMessage());
            errorResponse = TransicionEstadoResponseDto.builder()
                    .success(false)
                    .message(e.getMessage())
                    .build();
            return ResponseEntity.badRequest().body(errorResponse);

        } catch (IllegalStateException e) {
            log.error("Error de estado: {}", e.getMessage());
            errorResponse = TransicionEstadoResponseDto.builder()
                    .success(false)
                    .message(e.getMessage())
                    .build();
            return ResponseEntity.badRequest().body(errorResponse);

        } catch (SecurityException e) {
            log.error("Error de seguridad: {}", e.getMessage());
            errorResponse = TransicionEstadoResponseDto.builder()
                    .success(false)
                    .message("No tienes permiso para realizar esta accion")
                    .build();
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);

        } catch (Exception e) {
            log.error("Error inesperado al iniciar viaje", e);
            errorResponse = TransicionEstadoResponseDto.builder()
                    .success(false)
                    .message("Error al iniciar el viaje. Por favor intenta nuevamente")
                    .build();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Endpoint unificado para registrar cualquier evento del viaje
     * POST /transportista/viaje/{asignacionId}/evento
     *
     * Este endpoint simplifica el frontend al tener un solo punto de entrada
     * para todos los eventos del viaje.
     */
    @PostMapping("/viaje/{asignacionId}/evento")
    public ResponseEntity<TransicionEstadoResponseDto> registrarEvento(
            @PathVariable Integer asignacionId,
            @Valid @RequestBody RegistrarEventoDto dto,
            @RequestHeader("Authorization") String token
    ) {
        try {
            Integer usuarioId = extractUsuarioId(token);

            log.info("📥 Evento recibido: {} para asignación: {}", dto.getTipoEvento(), asignacionId);

            TransicionEstadoResponseDto response = transporteBl.registrarEvento(
                    asignacionId,
                    dto,
                    usuarioId
            );

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            log.error("❌ Error de validación al registrar evento: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(TransicionEstadoResponseDto.builder()
                            .success(false)
                            .message(e.getMessage())
                            .build());

        } catch (IllegalStateException e) {
            log.error("❌ Error de estado al registrar evento: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(TransicionEstadoResponseDto.builder()
                            .success(false)
                            .message(e.getMessage())
                            .build());

        } catch (SecurityException e) {
            log.error("❌ Error de seguridad al registrar evento: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(TransicionEstadoResponseDto.builder()
                            .success(false)
                            .message("No tienes permiso para realizar esta acción")
                            .build());

        } catch (Exception e) {
            log.error("❌ Error inesperado al registrar evento", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(TransicionEstadoResponseDto.builder()
                            .success(false)
                            .message("Error al procesar el evento. Por favor intenta nuevamente")
                            .build());
        }
    }

    /**
     * Obtiene el estado actual del viaje y los eventos registrados
     * GET /transportista/viaje/{asignacionId}/estado
     */
    @GetMapping("/viaje/{asignacionId}/estado")
    public ResponseEntity<Map<String, Object>> getEstadoViaje(
            @PathVariable Integer asignacionId,
            @RequestHeader("Authorization") String token
    ) {
        Map<String, Object> response = new HashMap<>();

        try {
            Integer usuarioId = extractUsuarioId(token);

            AsignacionCamion asignacion = asignacionCamionRepository.findById(asignacionId)
                    .orElseThrow(() -> new IllegalArgumentException("Asignación no encontrada"));

            // Validar que pertenece al usuario
            if (!asignacion.getTransportistaId().getUsuariosId().getId().equals(usuarioId)) {
                throw new SecurityException("No autorizado");
            }

            response.put("success", true);
            response.put("data", Map.of(
                    "asignacionId", asignacion.getId(),
                    "estado", asignacion.getEstado(),
                    "fechaInicio", asignacion.getFechaInicio(),
                    "fechaFin", asignacion.getFechaFin(),
                    "observaciones", asignacion.getObservaciones() != null
                            ? objectMapper.readValue(asignacion.getObservaciones(), Map.class)
                            : new HashMap<>()
            ));

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException | SecurityException e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);

        } catch (Exception e) {
            log.error("Error al obtener estado del viaje", e);
            response.put("success", false);
            response.put("message", "Error interno del servidor");
            return ResponseEntity.internalServerError().body(response);
        }
    }
    /**
     * Confirmar llegada a mina: En camino a la mina → Esperando carguío
     * POST /transportista/viaje/{asignacionId}/llegada-mina
     */
    @PostMapping("/viaje/{asignacionId}/llegada-mina")
    public ResponseEntity<TransicionEstadoResponseDto> confirmarLlegadaMina(
            @PathVariable Integer asignacionId,
            @Valid @RequestBody ConfirmarLlegadaMinaDto dto,
            @RequestHeader("Authorization") String token
    ) {
        Integer usuarioId = extractUsuarioId(token);
        dto.setAsignacionCamionId(asignacionId);

        TransicionEstadoResponseDto response = transporteBl.confirmarLlegadaMina(dto, usuarioId);
        return ResponseEntity.ok(response);
    }

    /**
     * Confirmar carguío: Esperando carguío → En camino balanza cooperativa
     * POST /transportista/viaje/{asignacionId}/confirmar-carguio
     */
    @PostMapping("/viaje/{asignacionId}/confirmar-carguio")
    public ResponseEntity<TransicionEstadoResponseDto> confirmarCarguio(
            @PathVariable Integer asignacionId,
            @Valid @RequestBody ConfirmarCarguioDto dto,
            @RequestHeader("Authorization") String token
    ) {
        Integer usuarioId = extractUsuarioId(token);
        dto.setAsignacionCamionId(asignacionId);

        TransicionEstadoResponseDto response = transporteBl.confirmarCarguio(dto, usuarioId);
        return ResponseEntity.ok(response);
    }

    /**
     * Registrar pesaje (cooperativa o destino)
     * POST /transportista/viaje/{asignacionId}/registrar-pesaje
     */
    @PostMapping("/viaje/{asignacionId}/registrar-pesaje")
    public ResponseEntity<TransicionEstadoResponseDto> registrarPesaje(
            @PathVariable Integer asignacionId,
            @Valid @RequestBody RegistrarPesajeDto dto,
            @RequestHeader("Authorization") String token
    ) {
        Integer usuarioId = extractUsuarioId(token);
        dto.setAsignacionCamionId(asignacionId);

        TransicionEstadoResponseDto response = transporteBl.registrarPesaje(dto, usuarioId);
        return ResponseEntity.ok(response);
    }

    /**
     * Iniciar descarga: En camino almacén destino → Descargando
     * POST /transportista/viaje/{asignacionId}/iniciar-descarga
     */
    @PostMapping("/viaje/{asignacionId}/iniciar-descarga")
    public ResponseEntity<TransicionEstadoResponseDto> iniciarDescarga(
            @PathVariable Integer asignacionId,
            @RequestBody Map<String, Object> body,
            @RequestHeader("Authorization") String token
    ) {
        Integer usuarioId = extractUsuarioId(token);

        Double lat = ((Number) body.get("lat")).doubleValue();
        Double lng = ((Number) body.get("lng")).doubleValue();

        TransicionEstadoResponseDto response = transporteBl.iniciarDescarga(
                asignacionId, lat, lng, usuarioId
        );

        return ResponseEntity.ok(response);
    }

    /**
     * Confirmar descarga: Descargando → Completado
     * POST /transportista/viaje/{asignacionId}/confirmar-descarga
     */
    @PostMapping("/viaje/{asignacionId}/confirmar-descarga")
    public ResponseEntity<TransicionEstadoResponseDto> confirmarDescarga(
            @PathVariable Integer asignacionId,
            @Valid @RequestBody ConfirmarDescargaDto dto,
            @RequestHeader("Authorization") String token
    ) {
        Integer usuarioId = extractUsuarioId(token);
        dto.setAsignacionCamionId(asignacionId);

        TransicionEstadoResponseDto response = transporteBl.confirmarDescarga(dto, usuarioId);
        return ResponseEntity.ok(response);
    }

    // ==================== MÉTODOS AUXILIARES ====================

    private Integer extractUsuarioId(String token) {
        String cleanToken = token.replace("Bearer ", "");
        return jwtUtil.extractUsuarioId(cleanToken);
    }
}