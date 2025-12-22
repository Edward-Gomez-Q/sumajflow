package ucb.edu.bo.sumajflow.bl.ingenio;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ucb.edu.bo.sumajflow.bl.NotificacionBl;
import ucb.edu.bo.sumajflow.bl.cooperativa.AuditoriaLotesBl;
import ucb.edu.bo.sumajflow.dto.ingenio.ActualizarProcesoDto;
import ucb.edu.bo.sumajflow.dto.ingenio.ProcesoPlantaDto;
import ucb.edu.bo.sumajflow.entity.Concentrado;
import ucb.edu.bo.sumajflow.entity.IngenioMinero;
import ucb.edu.bo.sumajflow.entity.LoteProcesoPlanta;
import ucb.edu.bo.sumajflow.entity.Usuarios;
import ucb.edu.bo.sumajflow.repository.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProcesoPlantaBl {

    private final LoteProcesoPlantaRepository loteProcesoPlantaRepository;
    private final ConcentradoRepository concentradoRepository;
    private final IngenioMineroRepository ingenioMineroRepository;
    private final UsuariosRepository usuariosRepository;
    private final AuditoriaLotesBl auditoriaLotesBl;
    private final NotificacionBl notificacionBl;

    /**
     * Obtener procesos de un concentrado
     */
    @Transactional(readOnly = true)
    public List<ProcesoPlantaDto> getProcesosByConcentrado(Integer concentradoId, Integer usuarioId) {
        log.debug("Obteniendo procesos para concentrado ID: {}", concentradoId);

        IngenioMinero ingenio = obtenerIngenioDelUsuario(usuarioId);
        Concentrado concentrado = obtenerConcentradoConPermisos(concentradoId, ingenio);

        List<LoteProcesoPlanta> procesos =
                loteProcesoPlantaRepository.findByConcentradoIdOrderByOrden(concentrado);

        return procesos.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    /**
     * Actualizar estado de un proceso (tipo Kanban)
     */
    @Transactional
    public ProcesoPlantaDto actualizarEstadoProceso(
            Integer procesoId,
            ActualizarProcesoDto dto,
            Integer usuarioId,
            String ipOrigen
    ) {
        log.info("Actualizando proceso ID: {} a estado: {}", procesoId, dto.getNuevoEstado());

        // 1. Validaciones
        validarNuevoEstado(dto.getNuevoEstado());

        LoteProcesoPlanta proceso = loteProcesoPlantaRepository.findById(procesoId)
                .orElseThrow(() -> new IllegalArgumentException("Proceso no encontrado"));

        IngenioMinero ingenio = obtenerIngenioDelUsuario(usuarioId);
        validarPermisosConcentrado(proceso.getConcentradoId(), ingenio);

        String estadoAnterior = proceso.getEstado();

        // 2. Validar orden de procesos (no se puede completar un proceso si el anterior no está completado)
        if (dto.getNuevoEstado().equals("completado")) {
            validarOrdenProcesos(proceso);
        }

        // 3. Actualizar proceso
        proceso.setEstado(dto.getNuevoEstado());
        if (dto.getObservaciones() != null) {
            proceso.setObservaciones(dto.getObservaciones());
        }

        // Actualizar fechas
        if (dto.getNuevoEstado().equals("en_proceso") && proceso.getFechaInicio() == null) {
            proceso.setFechaInicio(LocalDateTime.now());
        }
        if (dto.getNuevoEstado().equals("completado")) {
            proceso.setFechaFin(LocalDateTime.now());
        }

        loteProcesoPlantaRepository.save(proceso);

        // 4. Verificar si todos los procesos están completados
        boolean todosCompletados = verificarTodosProcesosCompletados(proceso.getConcentradoId());

        // 5. Si todos están completados, actualizar estado del concentrado
        if (todosCompletados) {
            actualizarEstadoConcentrado(proceso.getConcentradoId());
        }

        // 6. Registrar en auditoría
        registrarAuditoriaProceso(proceso, estadoAnterior, ipOrigen);

        // 7. Notificar
        notificarCambioProceso(proceso, estadoAnterior, todosCompletados);

        log.info("Proceso actualizado exitosamente - ID: {}", procesoId);

        return convertToDto(proceso);
    }

    /**
     * Finalizar concentrado y registrar peso final
     */
    @Transactional
    public void finalizarConcentrado(
            Integer concentradoId,
            BigDecimal pesoFinal,
            Integer usuarioId,
            String ipOrigen
    ) {
        log.info("Finalizando concentrado ID: {} con peso final: {}", concentradoId, pesoFinal);

        IngenioMinero ingenio = obtenerIngenioDelUsuario(usuarioId);
        Concentrado concentrado = obtenerConcentradoConPermisos(concentradoId, ingenio);

        // Validar que todos los procesos estén completados
        boolean todosCompletados = loteProcesoPlantaRepository.todosLosProceosCompletados(concentrado);
        if (!todosCompletados) {
            throw new IllegalArgumentException(
                    "No se puede finalizar el concentrado hasta que todos los procesos estén completados"
            );
        }

        if (pesoFinal == null || pesoFinal.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("El peso final debe ser mayor a 0");
        }

        // Actualizar peso final y calcular merma
        concentrado.setPesoFinal(pesoFinal);
        BigDecimal merma = concentrado.getPesoInicial().subtract(pesoFinal);
        concentrado.setMerma(merma);
        concentrado.setEstado("procesado");
        concentrado.setFechaFin(LocalDateTime.now());

        concentradoRepository.save(concentrado);

        // Registrar en auditoría
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("peso_final", pesoFinal);
        metadata.put("merma", merma);

        auditoriaLotesBl.registrarAuditoria(
                null,
                "ingenio",
                "en_proceso",
                "procesado",
                "FINALIZAR_CONCENTRADO",
                "Concentrado " + concentrado.getCodigoConcentrado() + " finalizado. Peso final: " + pesoFinal + " kg",
                null,
                metadata,
                ipOrigen
        );

        // Notificar al socio
        Integer socioUsuarioId = concentrado.getSocioPropietarioId().getUsuariosId().getId();
        Map<String, Object> notifMetadata = new HashMap<>();
        notifMetadata.put("concentradoId", concentrado.getId());
        notifMetadata.put("pesoFinal", pesoFinal);
        notifMetadata.put("merma", merma);

        notificacionBl.crearNotificacion(
                socioUsuarioId,
                "success",
                "Concentrado procesado",
                "El concentrado " + concentrado.getCodigoConcentrado() +
                        " ha sido procesado. Peso final: " + pesoFinal + " kg",
                notifMetadata
        );

        log.info("Concentrado finalizado - ID: {}", concentradoId);
    }

    // ==================== MÉTODOS DE VALIDACIÓN ====================

    private void validarNuevoEstado(String estado) {
        if (!List.of("pendiente", "en_proceso", "completado").contains(estado)) {
            throw new IllegalArgumentException(
                    "Estado inválido. Debe ser: pendiente, en_proceso o completado"
            );
        }
    }

    private void validarOrdenProcesos(LoteProcesoPlanta procesoActual) {
        // Obtener todos los procesos del concentrado
        List<LoteProcesoPlanta> procesos = loteProcesoPlantaRepository
                .findByConcentradoIdOrderByOrden(procesoActual.getConcentradoId());

        // Verificar que todos los procesos anteriores estén completados
        for (LoteProcesoPlanta proceso : procesos) {
            if (proceso.getOrden() < procesoActual.getOrden()) {
                if (!"completado".equals(proceso.getEstado())) {
                    throw new IllegalArgumentException(
                            "No se puede completar este proceso. Primero debe completar el proceso: " +
                                    proceso.getProcesoId().getNombre()
                    );
                }
            }
        }
    }

    private boolean verificarTodosProcesosCompletados(Concentrado concentrado) {
        return loteProcesoPlantaRepository.todosLosProceosCompletados(concentrado);
    }

    private void actualizarEstadoConcentrado(Concentrado concentrado) {
        concentrado.setEstado("en_proceso");
        concentradoRepository.save(concentrado);

        log.info("Estado del concentrado actualizado a 'en_proceso' - ID: {}", concentrado.getId());
    }

    private void validarPermisosConcentrado(Concentrado concentrado, IngenioMinero ingenio) {
        if (!concentrado.getIngenioMineroId().getId().equals(ingenio.getId())) {
            throw new IllegalArgumentException("No tienes permiso para modificar este concentrado");
        }
    }

    // ==================== MÉTODOS DE AUDITORÍA ====================

    private void registrarAuditoriaProceso(
            LoteProcesoPlanta proceso,
            String estadoAnterior,
            String ipOrigen
    ) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("proceso_id", proceso.getId());
        metadata.put("proceso_nombre", proceso.getProcesoId().getNombre());
        metadata.put("concentrado_id", proceso.getConcentradoId().getId());

        auditoriaLotesBl.registrarAuditoria(
                null,
                "ingenio",
                estadoAnterior,
                proceso.getEstado(),
                "ACTUALIZAR_PROCESO_PLANTA",
                "Proceso " + proceso.getProcesoId().getNombre() +
                        " cambió de " + estadoAnterior + " a " + proceso.getEstado(),
                proceso.getObservaciones(),
                metadata,
                ipOrigen
        );
    }

    // ==================== MÉTODOS DE NOTIFICACIÓN ====================

    private void notificarCambioProceso(
            LoteProcesoPlanta proceso,
            String estadoAnterior,
            boolean todosCompletados
    ) {
        Integer socioUsuarioId = proceso.getConcentradoId()
                .getSocioPropietarioId().getUsuariosId().getId();

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("concentradoId", proceso.getConcentradoId().getId());
        metadata.put("procesoNombre", proceso.getProcesoId().getNombre());
        metadata.put("estadoAnterior", estadoAnterior);
        metadata.put("estadoNuevo", proceso.getEstado());

        String mensaje = todosCompletados
                ? "Todos los procesos del concentrado " + proceso.getConcentradoId().getCodigoConcentrado() +
                " han sido completados"
                : "El proceso " + proceso.getProcesoId().getNombre() +
                " está ahora en estado: " + proceso.getEstado();

        notificacionBl.crearNotificacion(
                socioUsuarioId,
                "info",
                "Actualización de proceso",
                mensaje,
                metadata
        );
    }

    // ==================== MÉTODOS AUXILIARES ====================

    private IngenioMinero obtenerIngenioDelUsuario(Integer usuarioId) {
        Usuarios usuario = usuariosRepository.findById(usuarioId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        return ingenioMineroRepository.findByUsuariosId(usuario)
                .orElseThrow(() -> new IllegalArgumentException("Ingenio minero no encontrado"));
    }

    private Concentrado obtenerConcentradoConPermisos(Integer concentradoId, IngenioMinero ingenio) {
        Concentrado concentrado = concentradoRepository.findById(concentradoId)
                .orElseThrow(() -> new IllegalArgumentException("Concentrado no encontrado"));

        validarPermisosConcentrado(concentrado, ingenio);

        return concentrado;
    }

    // ==================== MÉTODOS DE CONVERSIÓN ====================

    private ProcesoPlantaDto convertToDto(LoteProcesoPlanta procesoPlanta) {
        ProcesoPlantaDto dto = new ProcesoPlantaDto();

        dto.setId(procesoPlanta.getId());
        dto.setProcesoId(procesoPlanta.getProcesoId().getId());
        dto.setProcesoNombre(procesoPlanta.getProcesoId().getNombre());
        dto.setOrden(procesoPlanta.getOrden());
        dto.setEstado(procesoPlanta.getEstado());
        dto.setFechaInicio(procesoPlanta.getFechaInicio());
        dto.setFechaFin(procesoPlanta.getFechaFin());
        dto.setObservaciones(procesoPlanta.getObservaciones());

        return dto;
    }
}