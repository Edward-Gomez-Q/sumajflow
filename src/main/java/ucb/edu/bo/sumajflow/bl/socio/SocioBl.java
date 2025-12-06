package ucb.edu.bo.sumajflow.bl.socio;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ucb.edu.bo.sumajflow.dto.socio.CooperativaInfoDto;
import ucb.edu.bo.sumajflow.dto.socio.SocioEstadoDto;
import ucb.edu.bo.sumajflow.entity.Cooperativa;
import ucb.edu.bo.sumajflow.entity.CooperativaSocio;
import ucb.edu.bo.sumajflow.entity.Socio;
import ucb.edu.bo.sumajflow.entity.Usuarios;
import ucb.edu.bo.sumajflow.repository.CooperativaSocioRepository;
import ucb.edu.bo.sumajflow.repository.SocioRepository;
import ucb.edu.bo.sumajflow.repository.UsuariosRepository;

import java.time.LocalDateTime;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class SocioBl {

    private final SocioRepository socioRepository;
    private final CooperativaSocioRepository cooperativaSocioRepository;
    private final UsuariosRepository usuariosRepository;

    // Mensajes predefinidos según estado
    private static final Map<String, String> MENSAJES_ESTADO = Map.of(
            "pendiente", "Tu solicitud está siendo revisada por la cooperativa",
            "aprobado", "Tu cuenta ha sido aprobada. ¡Bienvenido!",
            "rechazado", "Tu solicitud ha sido rechazada. Contacta con la cooperativa para más información"
    );

    /**
     * Obtiene el estado de aprobación del socio y toda la información relevante
     */
    @Transactional(readOnly = true)
    public SocioEstadoDto obtenerEstadoSocio(Integer usuarioId) {
        log.debug("Obteniendo estado de socio - Usuario ID: {}", usuarioId);

        // 1. Buscar y validar usuario
        Usuarios usuario = obtenerUsuario(usuarioId);
        validarTipoUsuarioSocio(usuario);

        // 2. Buscar datos del socio
        Socio socio = obtenerSocio(usuario);

        // 3. Buscar relación cooperativa-socio
        CooperativaSocio cooperativaSocio = obtenerCooperativaSocio(socio);

        // 4. Obtener datos de la cooperativa
        Cooperativa cooperativa = cooperativaSocio.getCooperativaId();

        // 5. Crear DTO de cooperativa
        CooperativaInfoDto cooperativaInfo = crearCooperativaInfo(cooperativa);

        // 6. Generar ID de solicitud
        String solicitudId = String.valueOf(socio.getId());

        // 7. Determinar mensaje según el estado
        String mensaje = obtenerMensajePorEstado(socio.getEstado());

        // 8. Crear y retornar el DTO
        SocioEstadoDto resultado = new SocioEstadoDto(
                socio.getId(),
                socio.getEstado(),
                socio.getFechaEnvio(),
                cooperativaSocio.getFechaAfiliacion(),
                solicitudId,
                cooperativaInfo,
                mensaje
        );

        log.info("Estado de socio obtenido - Socio ID: {}, Estado: {}", socio.getId(), socio.getEstado());
        return resultado;
    }

    /**
     * Verifica si un socio está aprobado
     */
    @Transactional(readOnly = true)
    public boolean estaSocioAprobado(Integer usuarioId) {
        log.debug("Verificando aprobación de socio - Usuario ID: {}", usuarioId);

        Usuarios usuario = obtenerUsuario(usuarioId);
        Socio socio = obtenerSocio(usuario);

        boolean aprobado = "aprobado".equalsIgnoreCase(socio.getEstado());
        log.debug("Socio {} - Aprobado: {}", socio.getId(), aprobado);

        return aprobado;
    }

    /**
     * Obtiene información completa del socio incluyendo cooperativa
     */
    @Transactional(readOnly = true)
    public Map<String, Object> obtenerInformacionCompleta(Integer usuarioId) {
        log.debug("Obteniendo información completa de socio - Usuario ID: {}", usuarioId);

        Usuarios usuario = obtenerUsuario(usuarioId);
        validarTipoUsuarioSocio(usuario);

        Socio socio = obtenerSocio(usuario);
        CooperativaSocio cooperativaSocio = obtenerCooperativaSocio(socio);
        Cooperativa cooperativa = cooperativaSocio.getCooperativaId();

        return Map.of(
                "socioId", socio.getId(),
                "estado", socio.getEstado(),
                "fechaEnvio", socio.getFechaEnvio(),
                "fechaAfiliacion", cooperativaSocio.getFechaAfiliacion() != null
                        ? cooperativaSocio.getFechaAfiliacion()
                        : "",
                "cooperativaId", cooperativa.getId(),
                "cooperativaNombre", cooperativa.getRazonSocial(),
                "cooperativaNit", cooperativa.getNit(),
                "observaciones", cooperativaSocio.getObservaciones() != null
                        ? cooperativaSocio.getObservaciones()
                        : "",
                "carnetAfiliacionUrl", socio.getCarnetAfiliacionUrl() != null
                        ? socio.getCarnetAfiliacionUrl()
                        : "",
                "carnetIdentidadUrl", socio.getCarnetIdentidadUrl() != null
                        ? socio.getCarnetIdentidadUrl()
                        : ""
        );
    }

    /**
     * Verifica si puede reenviar solicitud (solo si fue rechazado)
     */
    @Transactional(readOnly = true)
    public boolean puedeReenviarSolicitud(Integer usuarioId) {
        log.debug("Verificando si puede reenviar solicitud - Usuario ID: {}", usuarioId);

        Usuarios usuario = obtenerUsuario(usuarioId);
        Socio socio = obtenerSocio(usuario);

        boolean puedeReenviar = "rechazado".equalsIgnoreCase(socio.getEstado());
        log.debug("Socio {} - Puede reenviar: {}", socio.getId(), puedeReenviar);

        return puedeReenviar;
    }

    /**
     * Obtiene el tiempo transcurrido desde el envío de la solicitud
     */
    @Transactional(readOnly = true)
    public String obtenerTiempoDesdeEnvio(Integer usuarioId) {
        log.debug("Calculando tiempo desde envío - Usuario ID: {}", usuarioId);

        Usuarios usuario = obtenerUsuario(usuarioId);
        Socio socio = obtenerSocio(usuario);

        return calcularTiempoTranscurrido(socio.getFechaEnvio());
    }

    // ==================== MÉTODOS AUXILIARES PRIVADOS ====================

    /**
     * Obtiene usuario por ID con validación
     */
    private Usuarios obtenerUsuario(Integer usuarioId) {
        return usuariosRepository.findById(usuarioId)
                .orElseThrow(() -> {
                    log.error("Usuario no encontrado - ID: {}", usuarioId);
                    return new IllegalArgumentException("Usuario no encontrado");
                });
    }

    /**
     * Valida que el usuario sea de tipo socio
     */
    private void validarTipoUsuarioSocio(Usuarios usuario) {
        String tipoUsuario = usuario.getTipoUsuarioId().getTipoUsuario();
        if (!"socio".equals(tipoUsuario)) {
            log.warn("Usuario no es socio - Usuario ID: {}, Tipo: {}", usuario.getId(), tipoUsuario);
            throw new IllegalArgumentException("El usuario no es un socio");
        }
    }

    /**
     * Obtiene datos del socio
     */
    private Socio obtenerSocio(Usuarios usuario) {
        return socioRepository.findByUsuariosId(usuario)
                .orElseThrow(() -> {
                    log.error("Datos de socio no encontrados - Usuario ID: {}", usuario.getId());
                    return new IllegalArgumentException("Datos de socio no encontrados");
                });
    }

    /**
     * Obtiene relación cooperativa-socio
     */
    private CooperativaSocio obtenerCooperativaSocio(Socio socio) {
        return cooperativaSocioRepository.findBySocioId(socio)
                .orElseThrow(() -> {
                    log.error("Relación cooperativa-socio no encontrada - Socio ID: {}", socio.getId());
                    return new IllegalArgumentException("No se encontró la relación con la cooperativa");
                });
    }

    /**
     * Crea DTO con información de la cooperativa
     */
    private CooperativaInfoDto crearCooperativaInfo(Cooperativa cooperativa) {
        return new CooperativaInfoDto(
                cooperativa.getId(),
                cooperativa.getRazonSocial(),
                cooperativa.getNit(),
                cooperativa.getCorreoContacto(),
                cooperativa.getNumeroTelefonoMovil()
        );
    }

    /**
     * Obtiene mensaje apropiado según el estado
     */
    private String obtenerMensajePorEstado(String estado) {
        return MENSAJES_ESTADO.getOrDefault(
                estado.toLowerCase(),
                "Estado desconocido"
        );
    }

    /**
     * Calcula tiempo transcurrido desde una fecha
     */
    private String calcularTiempoTranscurrido(LocalDateTime fechaEnvio) {
        if (fechaEnvio == null) {
            return "Fecha no disponible";
        }

        LocalDateTime ahora = LocalDateTime.now();
        long dias = java.time.Duration.between(fechaEnvio, ahora).toDays();

        if (dias == 0) {
            long horas = java.time.Duration.between(fechaEnvio, ahora).toHours();
            if (horas == 0) {
                long minutos = java.time.Duration.between(fechaEnvio, ahora).toMinutes();
                return "Hace " + minutos + " minuto" + (minutos != 1 ? "s" : "");
            }
            return "Hace " + horas + " hora" + (horas != 1 ? "s" : "");
        }

        if (dias < 30) {
            return "Hace " + dias + " día" + (dias != 1 ? "s" : "");
        }

        long meses = dias / 30;
        return "Hace " + meses + " mes" + (meses != 1 ? "es" : "");
    }
}