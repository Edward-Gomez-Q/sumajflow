package ucb.edu.bo.sumajflow.bl.ingenio;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ucb.edu.bo.sumajflow.bl.ConcentradoBl;
import ucb.edu.bo.sumajflow.bl.NotificacionBl;
import ucb.edu.bo.sumajflow.dto.ingenio.*;
import ucb.edu.bo.sumajflow.entity.*;
import ucb.edu.bo.sumajflow.repository.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Servicio de gestión del Kanban de procesos de planta
 * Maneja 3 operaciones principales:
 * 1. Iniciar procesamiento (primer proceso)
 * 2. Mover entre procesos (procesos intermedios)
 * 3. Finalizar procesamiento (completar)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KanbanIngenioBl {

    private final ConcentradoRepository concentradoRepository;
    private final IngenioMineroRepository ingenioMineroRepository;
    private final UsuariosRepository usuariosRepository;
    private final LoteProcesoPlantaRepository loteProcesoPlantaRepository;
    private final ConcentradoBl concentradoBl;
    private final NotificacionBl notificacionBl;
    private final SimpMessagingTemplate messagingTemplate;

    // ==================== OBTENER PROCESOS ====================

    /**
     * Obtener procesos del concentrado (Kanban)
     */
    @Transactional(readOnly = true)
    public ProcesosConcentradoResponseDto obtenerProcesos(Integer concentradoId, Integer usuarioId) {
        log.debug("Obteniendo procesos del concentrado ID: {}", concentradoId);

        IngenioMinero ingenio = obtenerIngenioDelUsuario(usuarioId);
        Concentrado concentrado = obtenerConcentradoConPermisos(concentradoId, ingenio);

        return concentradoBl.construirProcesosResponseDto(concentrado);
    }

    // ==================== 1. INICIAR PROCESAMIENTO ====================

    /**
     * Iniciar procesamiento: pasar de "Por Iniciar" al primer proceso
     * Estado: en_camino_a_planta → en_proceso
     */
    @Transactional
    public ProcesosConcentradoResponseDto iniciarProcesamiento(
            Integer concentradoId,
            ProcesoIniciarDto iniciarDto,
            Integer usuarioId,
            String ipOrigen
    ) {
        log.info("Iniciando procesamiento del concentrado ID: {}", concentradoId);

        IngenioMinero ingenio = obtenerIngenioDelUsuario(usuarioId);
        Concentrado concentrado = obtenerConcentradoConPermisos(concentradoId, ingenio);

        // Validar estado del concentrado
        if (!"en_camino_a_planta".equals(concentrado.getEstado())) {
            throw new IllegalArgumentException("El concentrado debe estar en estado 'en_camino_a_planta' para iniciar procesamiento");
        }

        // Obtener el primer proceso
        List<LoteProcesoPlanta> procesos = loteProcesoPlantaRepository
                .findByConcentradoIdOrderByOrdenAsc(concentrado);

        if (procesos.isEmpty()) {
            throw new IllegalArgumentException("No hay procesos configurados para este concentrado");
        }

        LoteProcesoPlanta primerProceso = procesos.getFirst();

        if (!"pendiente".equals(primerProceso.getEstado())) {
            throw new IllegalArgumentException("El primer proceso ya fue iniciado");
        }

        // Iniciar el primer proceso
        primerProceso.setEstado("en_proceso");
        primerProceso.setFechaInicio(LocalDateTime.now());
        primerProceso.setObservaciones(iniciarDto.getObservacionesInicioProceso());
        loteProcesoPlantaRepository.save(primerProceso);

        // Cambiar estado del concentrado
        concentradoBl.transicionarEstado(
                concentrado,
                "en_proceso",
                "Procesamiento iniciado - " + primerProceso.getProcesoId().getNombre(),
                null,
                usuarioId,
                ipOrigen
        );

        // Guardar observaciones en JSONB del concentrado
        Map<String, Object> detalles = new HashMap<>();
        detalles.put("proceso_id", primerProceso.getId());
        detalles.put("proceso_nombre", primerProceso.getProcesoId().getNombre());
        detalles.put("proceso_orden", primerProceso.getOrden());
        detalles.put("observaciones_inicio", iniciarDto.getObservacionesInicioProceso() != null ?
                iniciarDto.getObservacionesInicioProceso() : "");

        guardarObservacionesEnConcentrado(
                concentrado,
                "INICIAR_PROCESAMIENTO",
                detalles,
                usuarioId,
                ipOrigen
        );

        // Publicar evento WebSocket
        concentradoBl.publicarEventoWebSocket(concentrado, "procesamiento_iniciado");

        log.info("Procesamiento iniciado exitosamente para concentrado ID: {}", concentradoId);

        return concentradoBl.construirProcesosResponseDto(concentrado);
    }

    // ==================== 2. MOVER ENTRE PROCESOS ====================

    /**
     * Mover concentrado entre procesos intermedios
     */
    @Transactional
    public ProcesosConcentradoResponseDto moverAProceso(
            Integer concentradoId,
            ProcesoMoverDto moverDto,
            Integer usuarioId,
            String ipOrigen
    ) {
        log.info("Moviendo concentrado {} al proceso {}", concentradoId, moverDto.getProcesoDestinoId());

        IngenioMinero ingenio = obtenerIngenioDelUsuario(usuarioId);
        Concentrado concentrado = obtenerConcentradoConPermisos(concentradoId, ingenio);

        if (!"en_proceso".equals(concentrado.getEstado())) {
            throw new IllegalArgumentException("El concentrado debe estar en procesamiento");
        }

        LoteProcesoPlanta procesoDestino = loteProcesoPlantaRepository.findById(moverDto.getProcesoDestinoId())
                .orElseThrow(() -> new IllegalArgumentException("Proceso destino no encontrado"));

        if (!procesoDestino.getConcentradoId().getId().equals(concentradoId)) {
            throw new IllegalArgumentException("El proceso destino no pertenece a este concentrado");
        }

        List<LoteProcesoPlanta> todosProcesos = loteProcesoPlantaRepository
                .findByConcentradoIdOrderByOrdenAsc(concentrado);

        // Determinar proceso actual (el que está en_proceso)
        LoteProcesoPlanta procesoActual = todosProcesos.stream()
                .filter(p -> "en_proceso".equals(p.getEstado()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No hay proceso activo actualmente"));

        // Validar que no se intente retroceder
        if (procesoDestino.getOrden() <= procesoActual.getOrden()) {
            throw new IllegalArgumentException("No puedes retroceder a un proceso anterior o al mismo proceso");
        }

        // 1. Completar proceso actual
        procesoActual.setEstado("completado");
        procesoActual.setFechaFin(LocalDateTime.now());

        // Agregar observaciones de fin al proceso
        String obsActuales = procesoActual.getObservaciones();
        String obsFin = moverDto.getObservacionesFinProceso();
        if (obsFin != null && !obsFin.isBlank()) {
            procesoActual.setObservaciones(
                    obsActuales != null && !obsActuales.isBlank()
                            ? obsActuales + " | FIN: " + obsFin
                            : "FIN: " + obsFin
            );
        }
        loteProcesoPlantaRepository.save(procesoActual);

        // 2. Auto-completar procesos intermedios si hay saltos
        List<LoteProcesoPlanta> procesosIntermedios = todosProcesos.stream()
                .filter(p -> p.getOrden() > procesoActual.getOrden() &&
                        p.getOrden() < procesoDestino.getOrden() &&
                        !"completado".equals(p.getEstado()))
                .toList();

        for (LoteProcesoPlanta intermedio : procesosIntermedios) {
            if ("pendiente".equals(intermedio.getEstado())) {
                intermedio.setFechaInicio(LocalDateTime.now());
            }
            intermedio.setEstado("completado");
            intermedio.setFechaFin(LocalDateTime.now());
            intermedio.setObservaciones("Auto-completado por salto en Kanban");
            loteProcesoPlantaRepository.save(intermedio);
        }

        // 3. Iniciar proceso destino
        procesoDestino.setEstado("en_proceso");
        procesoDestino.setFechaInicio(LocalDateTime.now());
        procesoDestino.setObservaciones(moverDto.getObservacionesInicioProceso());
        loteProcesoPlantaRepository.save(procesoDestino);

        // 4. Guardar observaciones en JSONB del concentrado
        Map<String, Object> detalles = new HashMap<>();
        detalles.put("proceso_origen", Map.of(
                "id", procesoActual.getId(),
                "nombre", procesoActual.getProcesoId().getNombre(),
                "orden", procesoActual.getOrden()
        ));
        detalles.put("proceso_destino", Map.of(
                "id", procesoDestino.getId(),
                "nombre", procesoDestino.getProcesoId().getNombre(),
                "orden", procesoDestino.getOrden()
        ));
        detalles.put("observaciones_fin_proceso", moverDto.getObservacionesFinProceso() != null ?
                moverDto.getObservacionesFinProceso() : "");
        detalles.put("observaciones_inicio_proceso", moverDto.getObservacionesInicioProceso() != null ?
                moverDto.getObservacionesInicioProceso() : "");

        if (!procesosIntermedios.isEmpty()) {
            detalles.put("procesos_auto_completados",
                    procesosIntermedios.stream()
                            .map(p -> Map.of(
                                    "id", p.getId(),
                                    "nombre", p.getProcesoId().getNombre(),
                                    "orden", p.getOrden()
                            ))
                            .collect(Collectors.toList())
            );
        }

        guardarObservacionesEnConcentrado(
                concentrado,
                "MOVER_PROCESO",
                detalles,
                usuarioId,
                ipOrigen
        );

        // Publicar evento WebSocket
        concentradoBl.publicarEventoWebSocket(concentrado, "kanban_actualizado");

        log.info("Concentrado movido exitosamente de {} a {}",
                procesoActual.getProcesoId().getNombre(),
                procesoDestino.getProcesoId().getNombre());

        return concentradoBl.construirProcesosResponseDto(concentrado);
    }

    // ==================== 3. FINALIZAR PROCESAMIENTO ====================

    /**
     * Finalizar procesamiento: completar el último proceso
     * Estado: en_proceso → esperando_reporte_quimico
     */
    @Transactional
    public ProcesosConcentradoResponseDto finalizarProcesamiento(
            Integer concentradoId,
            ProcesoFinalizarDto finalizarDto,
            Integer usuarioId,
            String ipOrigen
    ) {
        log.info("Finalizando procesamiento del concentrado ID: {}", concentradoId);

        IngenioMinero ingenio = obtenerIngenioDelUsuario(usuarioId);
        Concentrado concentrado = obtenerConcentradoConPermisos(concentradoId, ingenio);

        // Validar que el concentrado esté en procesamiento
        if (!"en_proceso".equals(concentrado.getEstado())) {
            throw new IllegalArgumentException("El concentrado debe estar en procesamiento");
        }

        // Obtener todos los procesos
        List<LoteProcesoPlanta> todosProcesos = loteProcesoPlantaRepository
                .findByConcentradoIdOrderByOrdenAsc(concentrado);

        if (todosProcesos.isEmpty()) {
            throw new IllegalArgumentException("No hay procesos configurados");
        }

        // Obtener el último proceso
        LoteProcesoPlanta ultimoProceso = todosProcesos.getLast();

        // Validar que el último proceso esté en_proceso
        if (!"en_proceso".equals(ultimoProceso.getEstado())) {
            throw new IllegalArgumentException("El último proceso debe estar en curso para finalizar");
        }

        // Completar el último proceso
        ultimoProceso.setEstado("completado");
        ultimoProceso.setFechaFin(LocalDateTime.now());

        // Agregar observaciones de fin
        String obsActuales = ultimoProceso.getObservaciones();
        String obsFin = finalizarDto.getObservacionesFinProceso();
        if (obsFin != null && !obsFin.isBlank()) {
            ultimoProceso.setObservaciones(
                    obsActuales != null && !obsActuales.isBlank()
                            ? obsActuales + " | FIN: " + obsFin
                            : "FIN: " + obsFin
            );
        }
        loteProcesoPlantaRepository.save(ultimoProceso);

        // Cambiar estado del concentrado
        concentradoBl.transicionarEstado(
                concentrado,
                "esperando_reporte_quimico",
                "Procesamiento completado. Esperando reporte químico.",
                null,
                usuarioId,
                ipOrigen
        );

        // Guardar observaciones en JSONB del concentrado
        Map<String, Object> detalles = new HashMap<>();
        detalles.put("ultimo_proceso", Map.of(
                "id", ultimoProceso.getId(),
                "nombre", ultimoProceso.getProcesoId().getNombre(),
                "orden", ultimoProceso.getOrden()
        ));
        detalles.put("observaciones_fin_proceso", finalizarDto.getObservacionesFinProceso() != null ?
                finalizarDto.getObservacionesFinProceso() : "");
        detalles.put("observaciones_generales", finalizarDto.getObservacionesGenerales() != null ?
                finalizarDto.getObservacionesGenerales() : "");
        detalles.put("total_procesos_completados", todosProcesos.size());

        guardarObservacionesEnConcentrado(
                concentrado,
                "FINALIZAR_PROCESAMIENTO",
                detalles,
                usuarioId,
                ipOrigen
        );

        // Notificar al socio propietario
        notificarProcesamientoCompleto(concentrado);

        // Publicar evento WebSocket
        concentradoBl.publicarEventoWebSocket(concentrado, "procesamiento_completo");

        log.info("Procesamiento finalizado exitosamente para concentrado ID: {}", concentradoId);

        return concentradoBl.construirProcesosResponseDto(concentrado);
    }

    // ==================== MÉTODOS AUXILIARES ====================

    /**
     * Guardar observaciones en el campo JSONB del concentrado
     */
    private void guardarObservacionesEnConcentrado(
            Concentrado concentrado,
            String accion,
            Map<String, Object> detalles,
            Integer usuarioId,
            String ipOrigen
    ) {
        List<Map<String, Object>> historial = concentradoBl.obtenerHistorial(concentrado);

        Map<String, Object> registro = new HashMap<>();
        registro.put("accion", accion);
        registro.put("timestamp", LocalDateTime.now().toString());
        registro.put("usuario_id", usuarioId);
        registro.put("ip_origen", ipOrigen);
        registro.put("detalles", detalles);

        historial.add(registro);

        concentrado.setObservaciones(concentradoBl.convertirHistorialAJson(historial));
        concentradoRepository.save(concentrado);
    }

    /**
     * Obtener ingenio del usuario autenticado
     */
    private IngenioMinero obtenerIngenioDelUsuario(Integer usuarioId) {
        Usuarios usuario = usuariosRepository.findById(usuarioId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        return ingenioMineroRepository.findByUsuariosId(usuario)
                .orElseThrow(() -> new IllegalArgumentException("Ingenio minero no encontrado"));
    }

    /**
     * Obtener concentrado con validación de permisos
     */
    private Concentrado obtenerConcentradoConPermisos(Integer concentradoId, IngenioMinero ingenio) {
        Concentrado concentrado = concentradoBl.obtenerConcentrado(concentradoId);

        if (!concentrado.getIngenioMineroId().getId().equals(ingenio.getId())) {
            throw new IllegalArgumentException("No tienes permiso para acceder a este concentrado");
        }

        return concentrado;
    }

    /**
     * Notificar al socio propietario que el procesamiento finalizó
     */
    private void notificarProcesamientoCompleto(Concentrado concentrado) {
        Integer socioUsuarioId = concentrado.getSocioPropietarioId().getUsuariosId().getId();

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("concentradoId", concentrado.getId());
        metadata.put("codigoConcentrado", concentrado.getCodigoConcentrado());
        metadata.put("estado", concentrado.getEstado());

        notificacionBl.crearNotificacion(
                socioUsuarioId,
                "success",
                "Procesamiento completado",
                "El procesamiento del concentrado " + concentrado.getCodigoConcentrado() +
                        " ha finalizado. Se espera el reporte químico.",
                metadata
        );
    }
}