package ucb.edu.bo.sumajflow.bl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ucb.edu.bo.sumajflow.entity.Auditoria;
import ucb.edu.bo.sumajflow.entity.Usuarios;
import ucb.edu.bo.sumajflow.repository.AuditoriaRepository;

import java.sql.Timestamp;

/**
 * Servicio para gestionar auditoría del sistema
 */
@Service
public class AuditoriaBl {

    private final AuditoriaRepository auditoriaRepository;

    public AuditoriaBl(AuditoriaRepository auditoriaRepository) {
        this.auditoriaRepository = auditoriaRepository;
    }

    /**
     * Registra una acción en la auditoría
     */
    @Transactional
    public void registrar(Usuarios usuario, String tabla, String accion, String descripcion) {
        Auditoria auditoria = new Auditoria();
        auditoria.setUsuariosId(usuario);
        auditoria.setTablaAfectada(tabla);
        auditoria.setAccion(accion);
        auditoria.setDescripcion(descripcion);
        auditoria.setFecha(new Timestamp(System.currentTimeMillis()));
        auditoriaRepository.save(auditoria);
    }

    /**
     * Registra un login exitoso
     */
    @Transactional
    public void registrarLogin(Usuarios usuario) {
        registrar(usuario, "usuarios", "LOGIN",
                "Inicio de sesión exitoso: " + usuario.getCorreo());
    }

    /**
     * Registra un registro de usuario
     */
    @Transactional
    public void registrarRegistro(Usuarios usuario, String tipoUsuario) {
        registrar(usuario, "usuarios", "INSERT",
                "Registro de nuevo usuario tipo " + tipoUsuario + ": " + usuario.getCorreo());
    }

    /**
     * Registra una actualización
     */
    @Transactional
    public void registrarActualizacion(Usuarios usuario, String tabla, String descripcion) {
        registrar(usuario, tabla, "UPDATE", descripcion);
    }

    /**
     * Registra una eliminación
     */
    @Transactional
    public void registrarEliminacion(Usuarios usuario, String tabla, String descripcion) {
        registrar(usuario, tabla, "DELETE", descripcion);
    }

    /**
     * Registra una aprobación/rechazo
     */
    @Transactional
    public void registrarAprobacion(Usuarios usuario, String tabla, String entidad,
                                    String estado, String observaciones) {
        String descripcion = String.format("Cambio de estado de %s a '%s'. %s",
                entidad, estado, observaciones != null ? observaciones : "");
        registrar(usuario, tabla, "APPROVAL", descripcion);
    }
}