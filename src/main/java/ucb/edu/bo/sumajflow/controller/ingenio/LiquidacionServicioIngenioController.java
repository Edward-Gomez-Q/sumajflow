package ucb.edu.bo.sumajflow.controller.ingenio;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ucb.edu.bo.sumajflow.bl.ingenio.LiquidacionServicioIngenioBl;
import ucb.edu.bo.sumajflow.dto.ingenio.*;
import ucb.edu.bo.sumajflow.utils.HttpUtils;
import ucb.edu.bo.sumajflow.utils.JwtUtil;

import java.util.HashMap;
import java.util.Map;

/**
 * Controlador REST para gestión de Liquidaciones de Servicio
 * Endpoints para aprobación y registro de pagos de servicios de procesamiento
 */
@RestController
@RequestMapping("/ingenio/liquidaciones-servicio")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class LiquidacionServicioIngenioController {

    private final LiquidacionServicioIngenioBl liquidacionServicioIngenioBl;
    private final JwtUtil jwtUtil;

    /**
     * Revisar solicitud de liquidación de servicio
     * PATCH /ingenio/liquidaciones-servicio/concentrados/{concentradoId}/revisar
     */
    @PatchMapping("/concentrados/{concentradoId}/revisar")
    public ResponseEntity<Map<String, Object>> revisarLiquidacionServicio(
            @PathVariable Integer concentradoId,
            @RequestHeader("Authorization") String token,
            HttpServletRequest request
    ) {
        Map<String, Object> response = new HashMap<>();

        try {
            Integer usuarioId = extractUsuarioId(token);
            String ipOrigen = HttpUtils.obtenerIpCliente(request);

            ConcentradoResponseDto concentrado = liquidacionServicioIngenioBl.revisarLiquidacionServicio(
                    concentradoId, usuarioId, ipOrigen
            );

            response.put("success", true);
            response.put("message", "Liquidación en revisión");
            response.put("data", concentrado);
            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error interno del servidor: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * Aprobar liquidación de servicio (definir costo)
     * POST /ingenio/liquidaciones-servicio/concentrados/{concentradoId}/aprobar
     */
    @PostMapping("/concentrados/{concentradoId}/aprobar")
    public ResponseEntity<Map<String, Object>> aprobarLiquidacionServicio(
            @PathVariable Integer concentradoId,
            @Valid @RequestBody AprobarLiquidacionServicioDto aprobarDto,
            @RequestHeader("Authorization") String token,
            HttpServletRequest request
    ) {
        Map<String, Object> response = new HashMap<>();

        try {
            Integer usuarioId = extractUsuarioId(token);
            String ipOrigen = HttpUtils.obtenerIpCliente(request);

            LiquidacionServicioResponseDto liquidacion = liquidacionServicioIngenioBl.aprobarLiquidacionServicio(
                    concentradoId, aprobarDto, usuarioId, ipOrigen
            );

            response.put("success", true);
            response.put("message", "Liquidación de servicio aprobada exitosamente");
            response.put("data", liquidacion);
            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error interno del servidor: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * Registrar pago del servicio recibido del socio
     * POST /ingenio/liquidaciones-servicio/concentrados/{concentradoId}/registrar-pago
     */
    @PostMapping("/concentrados/{concentradoId}/registrar-pago")
    public ResponseEntity<Map<String, Object>> registrarPagoServicio(
            @PathVariable Integer concentradoId,
            @Valid @RequestBody RegistrarPagoServicioDto pagoDto,
            @RequestHeader("Authorization") String token,
            HttpServletRequest request
    ) {
        Map<String, Object> response = new HashMap<>();

        try {
            Integer usuarioId = extractUsuarioId(token);
            String ipOrigen = HttpUtils.obtenerIpCliente(request);

            ConcentradoResponseDto concentrado = liquidacionServicioIngenioBl.registrarPagoServicio(
                    concentradoId, pagoDto, usuarioId, ipOrigen
            );

            response.put("success", true);
            response.put("message", "Pago de servicio registrado exitosamente");
            response.put("data", concentrado);
            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error interno del servidor: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    private Integer extractUsuarioId(String token) {
        String cleanToken = token.replace("Bearer ", "");
        return jwtUtil.extractUsuarioId(cleanToken);
    }
}