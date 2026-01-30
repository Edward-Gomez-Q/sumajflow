package ucb.edu.bo.sumajflow.bl.ingenio;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ucb.edu.bo.sumajflow.bl.ConcentradoBl;
import ucb.edu.bo.sumajflow.bl.NotificacionBl;
import ucb.edu.bo.sumajflow.dto.ingenio.ConcentradoResponseDto;
import ucb.edu.bo.sumajflow.dto.ingenio.ReporteQuimicoCreateDto;
import ucb.edu.bo.sumajflow.entity.*;
import ucb.edu.bo.sumajflow.repository.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Servicio de gestión de Reportes Químicos
 * Responsabilidades:
 * - Registrar reportes químicos del laboratorio
 * - Validar reportes químicos
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReporteQuimicoIngenioBl {

    // Repositorios
    private final ConcentradoRepository concentradoRepository;
    private final IngenioMineroRepository ingenioMineroRepository;
    private final UsuariosRepository usuariosRepository;
    private final ReporteQuimicoRepository reporteQuimicoRepository;

    // Servicios
    private final ConcentradoBl concentradoBl;
    private final NotificacionBl notificacionBl;

    // ==================== REGISTRAR REPORTE QUÍMICO ====================

    /**
     * Registrar reporte químico (PDF del laboratorio)
     */
    @Transactional
    public ConcentradoResponseDto registrarReporteQuimico(
            Integer concentradoId,
            ReporteQuimicoCreateDto reporteDto,
            Integer usuarioId,
            String ipOrigen
    ) {
        log.info("Registrando reporte químico - Concentrado ID: {}", concentradoId);

        IngenioMinero ingenio = obtenerIngenioDelUsuario(usuarioId);
        Concentrado concentrado = obtenerConcentradoConPermisos(concentradoId, ingenio);

        // Validar que el concentrado esté esperando reporte químico
        concentradoBl.validarEstado(concentrado, "esperando_reporte_quimico");

        // Crear reporte químico
        ReporteQuimico reporte = ReporteQuimico.builder()
                .numeroReporte(reporteDto.getNumeroReporte())
                .laboratorio(reporteDto.getLaboratorio())
                .fechaAnalisis(reporteDto.getFechaAnalisis())
                .leyAg(reporteDto.getLeyAg())
                .leyPb(reporteDto.getLeyPb())
                .leyZn(reporteDto.getLeyZn())
                .humedad(reporteDto.getHumedad())
                .tipoAnalisis(reporteDto.getTipoAnalisis())
                .urlPdf(reporteDto.getUrlPdf())
                .build();

        reporte = reporteQuimicoRepository.save(reporte);

        // Cambiar estado del concentrado
        concentradoBl.transicionarEstado(
                concentrado,
                "reporte_quimico_registrado",
                "Reporte químico registrado - Nro: " + reporteDto.getNumeroReporte(),
                "Laboratorio: " + reporteDto.getLaboratorio(),
                usuarioId,
                ipOrigen
        );

        // Registrar auditoría
        registrarAuditoriaReporteQuimico(concentrado, reporte, usuarioId, ipOrigen);

        // Notificaciones
        notificarReporteRegistrado(concentrado, reporte);

        // WebSocket
        concentradoBl.publicarEventoWebSocket(concentrado, "reporte_quimico_registrado");

        return concentradoBl.convertirAResponseDto(concentrado);
    }

    // ==================== VALIDAR REPORTE QUÍMICO ====================

    /**
     * Validar reporte químico (cambiar estado a "listo_para_liquidacion")
     */
    @Transactional
    public ConcentradoResponseDto validarReporteQuimico(
            Integer concentradoId,
            Integer usuarioId,
            String ipOrigen
    ) {
        log.info("Validando reporte químico - Concentrado ID: {}", concentradoId);

        IngenioMinero ingenio = obtenerIngenioDelUsuario(usuarioId);
        Concentrado concentrado = obtenerConcentradoConPermisos(concentradoId, ingenio);

        // Validar que el concentrado tenga reporte registrado
        concentradoBl.validarEstado(concentrado, "reporte_quimico_registrado");

        // Cambiar estado del concentrado
        concentradoBl.transicionarEstado(
                concentrado,
                "listo_para_liquidacion",
                "Reporte químico validado. Listo para solicitar liquidación del servicio.",
                null,
                usuarioId,
                ipOrigen
        );

        // Registrar auditoría
        List<Map<String, Object>> historial = concentradoBl.obtenerHistorial(concentrado);
        Map<String, Object> registro = new HashMap<>();
        registro.put("accion", "VALIDAR_REPORTE");
        registro.put("descripcion", "Reporte químico validado por operador del ingenio");
        registro.put("usuario_id", usuarioId);
        registro.put("ip_origen", ipOrigen);
        registro.put("timestamp", LocalDateTime.now().toString());
        historial.add(registro);
        concentrado.setObservaciones(concentradoBl.convertirHistorialAJson(historial));
        concentradoRepository.save(concentrado);

        // Notificaciones
        notificarReporteValidado(concentrado);

        // WebSocket
        concentradoBl.publicarEventoWebSocket(concentrado, "listo_para_liquidacion");

        return concentradoBl.convertirAResponseDto(concentrado);
    }

    // ==================== MÉTODOS AUXILIARES PRIVADOS ====================

    private IngenioMinero obtenerIngenioDelUsuario(Integer usuarioId) {
        Usuarios usuario = usuariosRepository.findById(usuarioId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        return ingenioMineroRepository.findByUsuariosId(usuario)
                .orElseThrow(() -> new IllegalArgumentException("Ingenio minero no encontrado"));
    }

    private Concentrado obtenerConcentradoConPermisos(Integer concentradoId, IngenioMinero ingenio) {
        Concentrado concentrado = concentradoBl.obtenerConcentrado(concentradoId);

        if (!concentrado.getIngenioMineroId().getId().equals(ingenio.getId())) {
            throw new IllegalArgumentException("No tienes permiso para acceder a este concentrado");
        }

        return concentrado;
    }

    private void registrarAuditoriaReporteQuimico(
            Concentrado concentrado,
            ReporteQuimico reporte,
            Integer usuarioId,
            String ipOrigen
    ) {
        List<Map<String, Object>> historial = concentradoBl.obtenerHistorial(concentrado);
        Map<String, Object> registro = new HashMap<>();
        registro.put("accion", "REGISTRAR_REPORTE");
        registro.put("reporte_id", reporte.getId());
        registro.put("numero_reporte", reporte.getNumeroReporte());
        registro.put("laboratorio", reporte.getLaboratorio());
        registro.put("ley_ag", reporte.getLeyAg());
        registro.put("ley_pb", reporte.getLeyPb());
        registro.put("ley_zn", reporte.getLeyZn());
        registro.put("usuario_id", usuarioId);
        registro.put("ip_origen", ipOrigen);
        registro.put("timestamp", LocalDateTime.now().toString());

        historial.add(registro);
        concentrado.setObservaciones(concentradoBl.convertirHistorialAJson(historial));
        concentradoRepository.save(concentrado);
    }

    private void notificarReporteRegistrado(Concentrado concentrado, ReporteQuimico reporte) {
        Integer socioUsuarioId = concentrado.getSocioPropietarioId().getUsuariosId().getId();

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("concentradoId", concentrado.getId());
        metadata.put("codigoConcentrado", concentrado.getCodigoConcentrado());
        metadata.put("reporteId", reporte.getId());
        metadata.put("numeroReporte", reporte.getNumeroReporte());

        notificacionBl.crearNotificacion(
                socioUsuarioId,
                "info",
                "Reporte químico registrado",
                "Se ha registrado el reporte químico " + reporte.getNumeroReporte() + " para el concentrado " + concentrado.getCodigoConcentrado(),
                metadata
        );
    }

    private void notificarReporteValidado(Concentrado concentrado) {
        Integer socioUsuarioId = concentrado.getSocioPropietarioId().getUsuariosId().getId();

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("concentradoId", concentrado.getId());
        metadata.put("codigoConcentrado", concentrado.getCodigoConcentrado());
        metadata.put("estado", concentrado.getEstado());

        notificacionBl.crearNotificacion(
                socioUsuarioId,
                "success",
                "Solicitar liquidación de servicio",
                "El reporte químico del concentrado " + concentrado.getCodigoConcentrado() + " ha sido validado. Ya puedes solicitar la liquidación del servicio de procesamiento.",
                metadata
        );
    }
}