package ucb.edu.bo.sumajflow.bl;

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

@Service
public class SocioBl {
    private final SocioRepository socioRepository;
    private final CooperativaSocioRepository cooperativaSocioRepository;
    private final UsuariosRepository usuariosRepository;

    public SocioBl(SocioRepository socioRepository,
                   CooperativaSocioRepository cooperativaSocioRepository,
                   UsuariosRepository usuariosRepository) {
        this.socioRepository = socioRepository;
        this.cooperativaSocioRepository = cooperativaSocioRepository;
        this.usuariosRepository = usuariosRepository;
    }

    /**
     * Obtiene el estado de aprobación del socio y toda la información relevante
     */
    @Transactional(readOnly = true)
    public SocioEstadoDto obtenerEstadoSocio(Integer usuarioId) {
        // 1. Buscar usuario
        Usuarios usuario = usuariosRepository.findById(usuarioId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        // 2. Verificar que sea un socio
        if (!"socio".equals(usuario.getTipoUsuarioId().getTipoUsuario())) {
            throw new IllegalArgumentException("El usuario no es un socio");
        }

        // 3. Buscar datos del socio
        Socio socio = socioRepository.findByUsuariosId(usuario)
                .orElseThrow(() -> new IllegalArgumentException("Datos de socio no encontrados"));

        // 4. Buscar relación cooperativa-socio
        CooperativaSocio cooperativaSocio = cooperativaSocioRepository.findBySocioId((socio))
                .orElseThrow(() -> new IllegalArgumentException(
                        "No se encontró la relación con la cooperativa"));

        // 5. Obtener datos de la cooperativa
        Cooperativa cooperativa = cooperativaSocio.getCooperativaId();

        // 6. Crear DTO de cooperativa
        CooperativaInfoDto cooperativaInfo = new CooperativaInfoDto(
                cooperativa.getId(),
                cooperativa.getRazonSocial(),
                cooperativa.getNit(),
                cooperativa.getCorreoContacto(),
                cooperativa.getNumeroTelefonoMovil()
        );
        // 7. Generar ID de solicitud
        String solicitudId = "" + socio.getId();

        // 8. Determinar mensaje según el estado
        String mensaje;
        switch (socio.getEstado().toLowerCase()) {
            case "pendiente":
                mensaje = "Tu solicitud está siendo revisada por la cooperativa";
                break;
            case "aprobado":
                mensaje = "Tu cuenta ha sido aprobada. ¡Bienvenido!";
                break;
            case "rechazado":
                mensaje = "Tu solicitud ha sido rechazada. Contacta con la cooperativa para más información";
                break;
            default:
                mensaje = "Estado desconocido";
        }

        // 9. Crear y retornar el DTO
        return new SocioEstadoDto(
                socio.getId(),
                socio.getEstado(),
                socio.getFechaEnvio(),
                cooperativaSocio.getFechaAfiliacion(),
                solicitudId,
                cooperativaInfo,
                mensaje
        );
    }

    /**
     * Verifica si un socio está aprobado
     */
    @Transactional(readOnly = true)
    public boolean estaSocioAprobado(Integer usuarioId) {
        Usuarios usuario = usuariosRepository.findById(usuarioId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        Socio socio = socioRepository.findByUsuariosId(usuario)
                .orElseThrow(() -> new IllegalArgumentException("Datos de socio no encontrados"));

        return "aprobado".equalsIgnoreCase(socio.getEstado());
    }
}
