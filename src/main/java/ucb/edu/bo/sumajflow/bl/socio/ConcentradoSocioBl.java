package ucb.edu.bo.sumajflow.bl.socio;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ucb.edu.bo.sumajflow.bl.ConcentradoBl;
import ucb.edu.bo.sumajflow.bl.NotificacionBl;
import ucb.edu.bo.sumajflow.dto.ingenio.*;
import ucb.edu.bo.sumajflow.entity.*;
import ucb.edu.bo.sumajflow.repository.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Servicio de Concentrados para el ROL SOCIO
 * Responsabilidades:
 * - Ver sus propios concentrados (filtrado por socio propietario)
 * - Ver detalle de concentrado (solo lectura)
 * - Ver Kanban de procesos (solo lectura)
 * - Ver reporte químico cuando esté disponible
 * - Solicitar liquidación de servicio
 * - Solicitar venta a comercializadora
 * - Ver sus liquidaciones
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ConcentradoSocioBl {

    // Repositorios específicos
    private final UsuariosRepository usuariosRepository;
    private final SocioRepository socioRepository;
    private final ConcentradoRepository concentradoRepository;
    private final ComercializadoraRepository comercializadoraRepository;
    private final LiquidacionRepository liquidacionRepository;
    private final PersonaRepository personaRepository;

    // Servicios
    private final ConcentradoBl concentradoBl;
    private final NotificacionBl notificacionBl;

    // ==================== LISTAR CONCENTRADOS DEL SOCIO ====================

    /**
     * Listar todos los concentrados del socio con filtros opcionales y paginación
     */
    @Transactional(readOnly = true)
    public Page<ConcentradoResponseDto> listarMisConcentrados(
            Integer usuarioId,
            String estado,
            String mineralPrincipal,
            LocalDateTime fechaDesde,
            LocalDateTime fechaHasta,
            int page,
            int size
    ) {
        log.debug("Listando concentrados del socio - Usuario ID: {}, Página: {}, Tamaño: {}",
                usuarioId, page, size);

        Socio socio = obtenerSocioDelUsuario(usuarioId);

        // Obtener concentrados del socio
        List<Concentrado> concentradosSocio = concentradoRepository
                .findBySocioPropietarioIdOrderByCreatedAtDesc(socio);

        // Usar mEtodo común del BL base para filtrar y paginar
        return concentradoBl.listarConcentradosConFiltros(
                concentradosSocio,
                estado,
                mineralPrincipal,
                fechaDesde,
                fechaHasta,
                page,
                size
        );
    }

    /**
     * Obtener detalle de un concentrado específico (solo si es propietario)
     */
    @Transactional(readOnly = true)
    public ConcentradoResponseDto obtenerMiConcentrado(Integer concentradoId, Integer usuarioId) {
        log.debug("Obteniendo detalle del concentrado ID: {}", concentradoId);

        Socio socio = obtenerSocioDelUsuario(usuarioId);
        Concentrado concentrado = obtenerConcentradoConPermisos(concentradoId, socio);

        return concentradoBl.convertirAResponseDto(concentrado);
    }

    /**
     * Obtener estadísticas personales del socio
     */
    @Transactional(readOnly = true)
    public Map<String, Object> obtenerMiDashboard(Integer usuarioId) {
        log.debug("Obteniendo dashboard del socio - Usuario ID: {}", usuarioId);

        Socio socio = obtenerSocioDelUsuario(usuarioId);

        List<Concentrado> misConcentrados = concentradoRepository.findBySocioPropietarioIdOrderByCreatedAtDesc(socio);

        Map<String, Object> dashboard = new HashMap<>();
        dashboard.put("totalConcentrados", misConcentrados.size());
        dashboard.put("enProceso", misConcentrados.stream().filter(c -> c.getEstado().contains("proceso")).count());
        dashboard.put("esperandoReporte", misConcentrados.stream().filter(c -> "esperando_reporte_quimico".equals(c.getEstado())).count());
        dashboard.put("pendientePago", misConcentrados.stream().filter(c -> c.getEstado().contains("liquidado") && !c.getEstado().contains("pagado")).count());
        dashboard.put("listoVenta", misConcentrados.stream().filter(c -> "listo_para_venta".equals(c.getEstado())).count());
        dashboard.put("vendidos", misConcentrados.stream().filter(c -> "vendido_a_comercializadora".equals(c.getEstado())).count());

        return dashboard;
    }

    /**
     * Obtener procesos del concentrado (solo lectura para el socio)
     */
    @Transactional(readOnly = true)
    public ProcesosConcentradoResponseDto verProcesos(Integer concentradoId, Integer usuarioId) {
        log.debug("Viendo procesos del concentrado ID: {} (solo lectura)", concentradoId);

        Socio socio = obtenerSocioDelUsuario(usuarioId);
        Concentrado concentrado = obtenerConcentradoConPermisos(concentradoId, socio);

        return concentradoBl.construirProcesosResponseDto(concentrado);
    }


    // ==================== MÉTODOS AUXILIARES PRIVADOS ====================

    private Socio obtenerSocioDelUsuario(Integer usuarioId) {
        Usuarios usuario = usuariosRepository.findById(usuarioId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        return socioRepository.findByUsuariosId(usuario)
                .orElseThrow(() -> new IllegalArgumentException("Socio no encontrado"));
    }

    private Concentrado obtenerConcentradoConPermisos(Integer concentradoId, Socio socio) {
        Concentrado concentrado = concentradoBl.obtenerConcentrado(concentradoId);

        // Validar que el concentrado pertenezca al socio
        if (!concentrado.getSocioPropietarioId().getId().equals(socio.getId())) {
            throw new IllegalArgumentException("No tienes permiso para acceder a este concentrado");
        }

        return concentrado;
    }

}