package ucb.edu.bo.sumajflow.bl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ucb.edu.bo.sumajflow.entity.Auditoria;
import ucb.edu.bo.sumajflow.entity.Usuarios;
import ucb.edu.bo.sumajflow.repository.AuditoriaRepository;

import java.util.Date;
import java.util.List;

/**
 * Servicio para gestionar auditoría del sistema
 * Cumple con ISO/IEC 27001 para trazabilidad completa
 */
@Service
public class AuditoriaBl {

    private final AuditoriaRepository auditoriaRepository;
    private final ObjectMapper objectMapper;

    public AuditoriaBl(AuditoriaRepository auditoriaRepository) {
        this.auditoriaRepository = auditoriaRepository;
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Registra una acción completa en la auditoría con todos los campos
     */
    @Transactional
    public void registrar(
            Usuarios usuario,
            String tabla,
            String accion,
            String descripcion,
            Integer registroId,
            Object datosAnteriores,
            Object datosNuevos,
            List<String> camposModificados,
            String ipOrigen,
            String userAgent,
            String metodoHttp,
            String endpoint,
            String modulo,
            String nivelCriticidad
    ) {
        Auditoria auditoria = new Auditoria();

        // Usuario y tipo
        auditoria.setUsuariosId(usuario);
        auditoria.setTipoUsuario(usuario.getTipoUsuarioId() != null ?
                usuario.getTipoUsuarioId().getTipoUsuario() : null);

        // Operación
        auditoria.setTablaAfectada(tabla);
        auditoria.setAccion(accion);
        auditoria.setRegistroId(registroId);
        auditoria.setDescripcion(descripcion);

        // Datos del cambio
        if (datosAnteriores != null) {
            auditoria.setDatosAnteriores(convertirAJson(datosAnteriores));
        }
        if (datosNuevos != null) {
            auditoria.setDatosNuevos(convertirAJson(datosNuevos));
        }
        if (camposModificados != null && !camposModificados.isEmpty()) {
            auditoria.setCamposModificados(convertirAJson(camposModificados));
        }

        // Contexto HTTP
        auditoria.setIpOrigen(ipOrigen);
        auditoria.setUserAgent(userAgent);
        auditoria.setMetodoHttp(metodoHttp);
        auditoria.setEndpoint(endpoint);

        // Metadata
        auditoria.setModulo(modulo);
        auditoria.setNivelCriticidad(nivelCriticidad != null ? nivelCriticidad : determinarCriticidad(tabla, accion));

        // Timestamp
        auditoria.setFechaOperacion(new Date());

        // Siempre exitoso porque si llega aquí, no hubo rollback
        auditoria.setOperacionExitosa(true);

        auditoriaRepository.save(auditoria);
    }

    /**
     * Versión simplificada para operaciones sin datos complejos
     */
    @Transactional
    public void registrarSimple(
            Usuarios usuario,
            String tabla,
            String accion,
            String descripcion,
            String ipOrigen,
            String metodoHttp,
            String endpoint,
            String modulo
    ) {
        registrar(usuario, tabla, accion, descripcion, null, null, null, null,
                ipOrigen, null, metodoHttp, endpoint, modulo, null);
    }

    /**
     * Registra un login exitoso
     */
    @Transactional
    public void registrarLogin(Usuarios usuario, String ipOrigen, String userAgent) {
        registrar(
                usuario,
                "usuarios",
                "LOGIN",
                "Inicio de sesión exitoso: " + usuario.getCorreo(),
                usuario.getId(),
                null,
                null,
                null,
                ipOrigen,
                userAgent,
                "POST",
                "/auth/login",
                "RF01",
                "MEDIO"
        );
    }

    /**
     * Registra un registro de usuario (onboarding)
     */
    @Transactional
    public void registrarRegistro(
            Usuarios usuario,
            String tipoUsuario,
            String ipOrigen,
            String userAgent
    ) {
        registrar(
                usuario,
                "usuarios",
                "INSERT",
                "Registro de nuevo usuario tipo " + tipoUsuario + ": " + usuario.getCorreo(),
                usuario.getId(),
                null,
                crearDatosUsuario(usuario),
                null,
                ipOrigen,
                userAgent,
                "POST",
                "/auth/register",
                "RF01",
                "ALTO"
        );
    }

    /**
     * Registra creación de sectores
     */
    @Transactional
    public void registrarCreacionSector(
            Usuarios usuario,
            Integer sectorId,
            String nombreSector,
            int numeroCoordenadas,
            String ipOrigen,
            String metodoHttp,
            String endpoint
    ) {
        String descripcion = String.format(
                "Sector creado: %s (ID: %d) con %d coordenadas",
                nombreSector, sectorId, numeroCoordenadas
        );

        registrar(
                usuario,
                "sectores",
                "INSERT",
                descripcion,
                sectorId,
                null,
                null,
                null,
                ipOrigen,
                null,
                metodoHttp,
                endpoint,
                "RF02",
                "MEDIO"
        );
    }

    /**
     * Registra actualización de sectores
     */
    @Transactional
    public void registrarActualizacionSector(
            Usuarios usuario,
            Integer sectorId,
            String nombreAnterior,
            String nombreNuevo,
            int numeroCoordenadas,
            String ipOrigen,
            String metodoHttp,
            String endpoint
    ) {
        String descripcion = String.format(
                "Sector actualizado: '%s' -> '%s' (ID: %d) con %d coordenadas",
                nombreAnterior, nombreNuevo, sectorId, numeroCoordenadas
        );

        List<String> camposModificados = List.of("nombre", "color", "coordenadas");

        registrar(
                usuario,
                "sectores",
                "UPDATE",
                descripcion,
                sectorId,
                null,
                null,
                camposModificados,
                ipOrigen,
                null,
                metodoHttp,
                endpoint,
                "RF02",
                "MEDIO"
        );
    }

    /**
     * Registra eliminación de sectores
     */
    @Transactional
    public void registrarEliminacionSector(
            Usuarios usuario,
            Integer sectorId,
            String nombreSector,
            String ipOrigen,
            String metodoHttp,
            String endpoint
    ) {
        String descripcion = String.format(
                "Sector eliminado: '%s' (ID: %d)",
                nombreSector, sectorId
        );

        registrar(
                usuario,
                "sectores",
                "DELETE",
                descripcion,
                sectorId,
                null,
                null,
                null,
                ipOrigen,
                null,
                metodoHttp,
                endpoint,
                "RF02",
                "ALTO"
        );
    }

    /**
     * Registra aprobación/rechazo de socios
     */
    @Transactional
    public void registrarAprobacionSocio(
            Usuarios usuario,
            Integer cooperativaSocioId,
            String nombreSocio,
            String estado,
            String observaciones,
            String ipOrigen,
            String metodoHttp,
            String endpoint
    ) {
        String descripcion = String.format(
                "Solicitud de %s %s. %s",
                nombreSocio,
                estado.equals("aprobado") ? "aprobada" : "rechazada",
                observaciones != null ? observaciones : ""
        );

        registrar(
                usuario,
                "cooperativa_socio",
                "APPROVAL",
                descripcion,
                cooperativaSocioId,
                null,
                null,
                List.of("estado", "observaciones"),
                ipOrigen,
                null,
                metodoHttp,
                endpoint,
                "RF02",
                "CRÍTICO"
        );
    }

    // ==================== MÉTODOS AUXILIARES ====================

    /**
     * Convierte un objeto a JSON string
     */
    private String convertirAJson(Object objeto) {
        try {
            return objectMapper.writeValueAsString(objeto);
        } catch (JsonProcessingException e) {
            return "{\"error\":\"No se pudo serializar\"}";
        }
    }

    /**
     * Determina la criticidad según la tabla y acción
     */
    private String determinarCriticidad(String tabla, String accion) {
        // Operaciones críticas en tablas sensibles
        if (tabla.equals("liquidacion") ||
                tabla.equals("concentrado") ||
                tabla.equals("pesajes") ||
                tabla.equals("reporte_quimico")) {
            return "CRÍTICO";
        }

        // Eliminaciones siempre son de alta criticidad
        if (accion.equals("DELETE")) {
            return "ALTO";
        }

        // Aprobaciones son críticas
        if (accion.equals("APPROVAL")) {
            return "CRÍTICO";
        }

        // Inserciones y actualizaciones son de media criticidad
        if (accion.equals("INSERT") || accion.equals("UPDATE")) {
            return "MEDIO";
        }

        return "BAJO";
    }

    /**
     * Crea un objeto con datos básicos del usuario para auditoría
     */
    private Object crearDatosUsuario(Usuarios usuario) {
        return new Object() {
            public final Integer id = usuario.getId();
            public final String correo = usuario.getCorreo();
            public final String tipoUsuario = usuario.getTipoUsuarioId() != null ?
                    usuario.getTipoUsuarioId().getTipoUsuario() : null;
        };
    }
}