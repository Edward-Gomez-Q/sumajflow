package ucb.edu.bo.sumajflow.controller.transportista;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ucb.edu.bo.sumajflow.bl.transporte.TransporteBl;
import ucb.edu.bo.sumajflow.dto.tracking.LoteDetalleViajeDto;
import ucb.edu.bo.sumajflow.dto.transporte.*;
import ucb.edu.bo.sumajflow.utils.JwtUtil;

import java.util.List;
import java.util.Map;

/**
 * Controlador REST para gestión de transporte por transportistas
 * Endpoints limpios y unificados siguiendo mejores prácticas REST
 */
@Slf4j
@RestController
@RequestMapping("/transportista")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class LotesTransportistaController {

    private final TransporteBl transporteBl;
    private final JwtUtil jwtUtil;

    // ==================== CONSULTAS DE LOTES ====================

    /**
     * GET /transportista/lotes?filtro=activos
     * Obtiene los lotes asignados al transportista
     *
     * @param filtro: "activos" (default), "completados", "todos"
     */
    @GetMapping("/lotes")
    public ResponseEntity<Map<String, Object>> obtenerLotes(
            @RequestHeader("Authorization") String token,
            @RequestParam(required = false, defaultValue = "activos") String filtro
    ) {
        try {
            Integer usuarioId = extractUsuarioId(token);
            List<LoteAsignadoResumenDto> lotes = transporteBl.obtenerLotesTransportista(usuarioId, filtro);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", lotes,
                    "total", lotes.size()
            ));

        } catch (IllegalArgumentException e) {
            return badRequest(e.getMessage());
        } catch (Exception e) {
            log.error("Error al obtener lotes", e);
            return internalError();
        }
    }

    /**
     * GET /api/transportista/lotes/{asignacionId}
     * Obtiene el detalle completo de un lote asignado
     */
    @GetMapping("/lotes/{asignacionId}")
    public ResponseEntity<Map<String, Object>> obtenerDetalleLote(
            @PathVariable Integer asignacionId,
            @RequestHeader("Authorization") String token
    ) {
        try {
            Integer usuarioId = extractUsuarioId(token);
            LoteDetalleViajeDto detalle = transporteBl.obtenerDetalleLoteParaViaje(asignacionId, usuarioId);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", detalle
            ));

        } catch (IllegalArgumentException | SecurityException e) {
            return badRequest(e.getMessage());
        } catch (Exception e) {
            log.error("Error al obtener detalle del lote", e);
            return internalError();
        }
    }

    // ==================== FLUJO DEL VIAJE ====================

    /**
     * POST /transportista/viaje/{asignacionId}/iniciar
     * Paso 1: Iniciar viaje (Esperando iniciar → En camino a la mina)
     */
    @PostMapping("/viaje/{asignacionId}/iniciar")
    public ResponseEntity<TransicionEstadoResponseDto> iniciarViaje(
            @PathVariable Integer asignacionId,
            @Valid @RequestBody IniciarViajeDto dto,
            @RequestHeader("Authorization") String token
    ) {
        log.info("📍 POST /viaje/{}/iniciar - Lat: {}, Lng: {}", asignacionId, dto.getLat(), dto.getLng());

        try {
            Integer usuarioId = extractUsuarioId(token);
            TransicionEstadoResponseDto response = transporteBl.iniciarViaje(asignacionId, dto, usuarioId);
            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest()
                    .body(errorResponse(e.getMessage()));
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(errorResponse("No autorizado"));
        } catch (Exception e) {
            log.error("Error al iniciar viaje", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(errorResponse("Error al iniciar el viaje"));
        }
    }

    /**
     * POST /transportista/viaje/{asignacionId}/llegada-mina
     * Paso 2: Confirmar llegada a mina (En camino a la mina → Esperando carguío)
     */
    @PostMapping("/viaje/{asignacionId}/llegada-mina")
    public ResponseEntity<TransicionEstadoResponseDto> confirmarLlegadaMina(
            @PathVariable Integer asignacionId,
            @Valid @RequestBody ConfirmarLlegadaMinaDto dto,
            @RequestHeader("Authorization") String token
    ) {
        log.info("🏔️ POST /viaje/{}/llegada-mina", asignacionId);

        try {
            Integer usuarioId = extractUsuarioId(token);
            TransicionEstadoResponseDto response = transporteBl.confirmarLlegadaMina(asignacionId, dto, usuarioId);
            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest()
                    .body(errorResponse(e.getMessage()));
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(errorResponse("No autorizado"));
        } catch (Exception e) {
            log.error("Error al confirmar llegada a mina", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(errorResponse("Error al confirmar llegada"));
        }
    }

    /**
     * POST /api/transportista/viaje/{asignacionId}/carguio
     * Paso 3: Confirmar carguío completado (Esperando carguío → En camino balanza cooperativa)
     */
    @PostMapping("/viaje/{asignacionId}/carguio")
    public ResponseEntity<TransicionEstadoResponseDto> confirmarCarguio(
            @PathVariable Integer asignacionId,
            @Valid @RequestBody ConfirmarCarguioDto dto,
            @RequestHeader("Authorization") String token
    ) {
        log.info("🚛 POST /viaje/{}/carguio", asignacionId);

        try {
            Integer usuarioId = extractUsuarioId(token);
            TransicionEstadoResponseDto response = transporteBl.confirmarCarguio(asignacionId, dto, usuarioId);
            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest()
                    .body(errorResponse(e.getMessage()));
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(errorResponse("No autorizado"));
        } catch (Exception e) {
            log.error("Error al confirmar carguío", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(errorResponse("Error al confirmar carguío"));
        }
    }

    /**
     * POST /api/transportista/viaje/{asignacionId}/pesaje-cooperativa
     * Paso 4: Registrar pesaje cooperativa (En camino balanza cooperativa → En camino balanza destino)
     */
    @PostMapping("/viaje/{asignacionId}/pesaje-cooperativa")
    public ResponseEntity<TransicionEstadoResponseDto> registrarPesajeCooperativa(
            @PathVariable Integer asignacionId,
            @Valid @RequestBody RegistrarPesajeDto dto,
            @RequestHeader("Authorization") String token
    ) {
        log.info("⚖️ POST /viaje/{}/pesaje-cooperativa", asignacionId);

        try {
            Integer usuarioId = extractUsuarioId(token);
            dto.setTipoPesaje("cooperativa");
            TransicionEstadoResponseDto response = transporteBl.registrarPesajeCooperativa(asignacionId, dto, usuarioId);
            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest()
                    .body(errorResponse(e.getMessage()));
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(errorResponse("No autorizado"));
        } catch (Exception e) {
            log.error("Error al registrar pesaje cooperativa", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(errorResponse("Error al registrar pesaje"));
        }
    }

    /**
     * POST /api/transportista/viaje/{asignacionId}/pesaje-destino
     * Paso 5: Registrar pesaje destino (En camino balanza destino → En camino almacén destino)
     */
    @PostMapping("/viaje/{asignacionId}/pesaje-destino")
    public ResponseEntity<TransicionEstadoResponseDto> registrarPesajeDestino(
            @PathVariable Integer asignacionId,
            @Valid @RequestBody RegistrarPesajeDto dto,
            @RequestHeader("Authorization") String token
    ) {
        log.info("⚖️ POST /viaje/{}/pesaje-destino", asignacionId);

        try {
            Integer usuarioId = extractUsuarioId(token);
            dto.setTipoPesaje("destino");
            TransicionEstadoResponseDto response = transporteBl.registrarPesajeDestino(asignacionId, dto, usuarioId);
            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest()
                    .body(errorResponse(e.getMessage()));
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(errorResponse("No autorizado"));
        } catch (Exception e) {
            log.error("Error al registrar pesaje destino", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(errorResponse("Error al registrar pesaje"));
        }
    }

    /**
     * POST /api/transportista/viaje/{asignacionId}/llegada-almacen
     * Paso 6: Confirmar llegada a almacén (En camino almacén destino → Descargando)
     */
    @PostMapping("/viaje/{asignacionId}/llegada-almacen")
    public ResponseEntity<TransicionEstadoResponseDto> confirmarLlegadaAlmacen(
            @PathVariable Integer asignacionId,
            @Valid @RequestBody ConfirmarLlegadaAlmacenDto dto,
            @RequestHeader("Authorization") String token
    ) {
        log.info("🏭 POST /viaje/{}/llegada-almacen", asignacionId);

        try {
            Integer usuarioId = extractUsuarioId(token);
            TransicionEstadoResponseDto response = transporteBl.confirmarLlegadaAlmacen(asignacionId, dto, usuarioId);
            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest()
                    .body(errorResponse(e.getMessage()));
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(errorResponse("No autorizado"));
        } catch (Exception e) {
            log.error("Error al confirmar llegada a almacén", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(errorResponse("Error al confirmar llegada"));
        }
    }

    /**
     * POST /api/transportista/viaje/{asignacionId}/descarga
     * Paso 7: Confirmar descarga (Descargando → Descargando - preparado para finalizar)
     */
    @PostMapping("/viaje/{asignacionId}/descarga")
    public ResponseEntity<TransicionEstadoResponseDto> confirmarDescarga(
            @PathVariable Integer asignacionId,
            @Valid @RequestBody ConfirmarDescargaDto dto,
            @RequestHeader("Authorization") String token
    ) {
        log.info("📦 POST /viaje/{}/descarga", asignacionId);

        try {
            Integer usuarioId = extractUsuarioId(token);
            TransicionEstadoResponseDto response = transporteBl.confirmarDescarga(asignacionId, dto, usuarioId);
            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest()
                    .body(errorResponse(e.getMessage()));
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(errorResponse("No autorizado"));
        } catch (Exception e) {
            log.error("Error al confirmar descarga", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(errorResponse("Error al confirmar descarga"));
        }
    }

    /**
     * POST /api/transportista/viaje/{asignacionId}/finalizar
     * Paso 8: Finalizar ruta (Descargando → Completado)
     */
    @PostMapping("/viaje/{asignacionId}/finalizar")
    public ResponseEntity<TransicionEstadoResponseDto> finalizarRuta(
            @PathVariable Integer asignacionId,
            @Valid @RequestBody FinalizarRutaDto dto,
            @RequestHeader("Authorization") String token
    ) {
        log.info("✅ POST /viaje/{}/finalizar", asignacionId);

        try {
            Integer usuarioId = extractUsuarioId(token);
            TransicionEstadoResponseDto response = transporteBl.finalizarRuta(asignacionId, dto, usuarioId);
            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest()
                    .body(errorResponse(e.getMessage()));
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(errorResponse("No autorizado"));
        } catch (Exception e) {
            log.error("Error al finalizar ruta", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(errorResponse("Error al finalizar ruta"));
        }
    }

    // ==================== MÉTODOS AUXILIARES ====================

    private Integer extractUsuarioId(String token) {
        String cleanToken = token.replace("Bearer ", "");
        return jwtUtil.extractUsuarioId(cleanToken);
    }

    private ResponseEntity<Map<String, Object>> badRequest(String message) {
        return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", message
        ));
    }

    private ResponseEntity<Map<String, Object>> internalError() {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "success", false,
                "message", "Error interno del servidor"
        ));
    }

    private TransicionEstadoResponseDto errorResponse(String message) {
        return TransicionEstadoResponseDto.builder()
                .success(false)
                .message(message)
                .build();
    }
}