package ucb.edu.bo.sumajflow.bl.cooperativa;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ucb.edu.bo.sumajflow.entity.AuditoriaLotes;
import ucb.edu.bo.sumajflow.entity.Lotes;
import ucb.edu.bo.sumajflow.repository.AuditoriaLotesRepository;
import ucb.edu.bo.sumajflow.repository.LotesRepository;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditoriaLotesBl {

    private final AuditoriaLotesRepository auditoriaLotesRepository;
    private final LotesRepository lotesRepository;
    private final ObjectMapper objectMapper; // Inyectar ObjectMapper

    /**
     * Registrar acción en auditoría de lotes
     */
    @Transactional
    public void registrarAuditoria(
            Integer loteId,
            String tipoUsuario,
            String estadoAnterior,
            String estadoNuevo,
            String accion,
            String descripcion,
            String observaciones,
            Map<String, Object> metadata,
            String ipOrigen
    ) {
        log.debug("Registrando auditoría para lote ID: {}", loteId);

        Lotes lote = lotesRepository.findById(loteId)
                .orElseThrow(() -> new IllegalArgumentException("Lote no encontrado con ID: " + loteId));

        AuditoriaLotes auditoria = AuditoriaLotes.builder()
                .loteId(lote)
                .tipoUsuario(tipoUsuario)
                .estadoAnterior(estadoAnterior)
                .estadoNuevo(estadoNuevo)
                .accion(accion)
                .descripcion(descripcion)
                .observaciones(observaciones)
                .metadata(metadata != null ? convertMapToJsonb(metadata) : null)
                .ipOrigen(ipOrigen)
                .fechaRegistro(LocalDateTime.now())
                .build();

        auditoriaLotesRepository.save(auditoria);

        log.info("Auditoría registrada para lote ID: {} - Acción: {}", loteId, accion);
    }

    /**
     * Registrar aprobación de cooperativa
     */
    @Transactional
    public void registrarAprobacionCooperativa(
            Integer loteId,
            String estadoAnterior,
            String estadoNuevo,
            Integer cantidadTransportistas,
            String ipOrigen
    ) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("transportistas_asignados", cantidadTransportistas);
        metadata.put("tipo_accion", "aprobacion_cooperativa");

        registrarAuditoria(
                loteId,
                "cooperativa",
                estadoAnterior,
                estadoNuevo,
                "APROBAR_COOPERATIVA",
                "Lote aprobado por la cooperativa y transportistas asignados",
                null,
                metadata,
                ipOrigen
        );
    }

    /**
     * Registrar rechazo de cooperativa
     */
    @Transactional
    public void registrarRechazoCooperativa(
            Integer loteId,
            String estadoAnterior,
            String motivoRechazo,
            String ipOrigen
    ) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("motivo_rechazo", motivoRechazo);
        metadata.put("tipo_accion", "rechazo_cooperativa");

        registrarAuditoria(
                loteId,
                "cooperativa",
                estadoAnterior,
                "Rechazado",
                "RECHAZAR_COOPERATIVA",
                "Lote rechazado por la cooperativa",
                motivoRechazo,
                metadata,
                ipOrigen
        );
    }

    /**
     * Registrar asignación de transportista
     */
    @Transactional
    public void registrarAsignacionTransportista(
            Integer loteId,
            Integer transportistaId,
            String placaVehiculo,
            Integer numeroCamion,
            String ipOrigen
    ) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("transportista_id", transportistaId);
        metadata.put("placa_vehiculo", placaVehiculo);
        metadata.put("numero_camion", numeroCamion);
        metadata.put("tipo_accion", "asignacion_transportista");

        registrarAuditoria(
                loteId,
                "cooperativa",
                null,
                null,
                "ASIGNAR_TRANSPORTISTA",
                "Transportista asignado al lote - Camión #" + numeroCamion,
                null,
                metadata,
                ipOrigen
        );
    }

    // Método auxiliar para convertir Map a JSON (simplificado)
    private String convertMapToJsonb(Map<String, Object> map) {
        try {
            return objectMapper.writeValueAsString(map);
        } catch (Exception e) {
            log.error("Error convirtiendo metadata a JSON", e);
            return "{}";
        }
    }
}