package ucb.edu.bo.sumajflow.bl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ucb.edu.bo.sumajflow.entity.Auditoria;
import ucb.edu.bo.sumajflow.entity.Usuarios;
import ucb.edu.bo.sumajflow.repository.AuditoriaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Servicio para gestionar auditoría del sistema
 * Cumple con ISO/IEC 27001 para trazabilidad completa
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuditoriaBl {

    private final AuditoriaRepository auditoriaRepository;
    private final ObjectMapper objectMapper;

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
        log.debug("Registrando auditoría - Tabla: {}, Acción: {}, Usuario: {}",
                tabla, accion, usuario.getCorreo());

        Auditoria auditoria = Auditoria.builder()
                .usuariosId(usuario)
                .tipoUsuario(obtenerTipoUsuario(usuario))
                .tablaAfectada(tabla)
                .accion(accion)
                .registroId(registroId)
                .descripcion(descripcion)
                .datosAnteriores(convertirAJson(datosAnteriores))
                .datosNuevos(convertirAJson(datosNuevos))
                .camposModificados(convertirListaAJson(camposModificados))
                .ipOrigen(ipOrigen)
                .userAgent(userAgent)
                .metodoHttp(metodoHttp)
                .endpoint(endpoint)
                .fechaOperacion(LocalDateTime.now())
                .modulo(modulo)
                .nivelCriticidad(nivelCriticidad != null ? nivelCriticidad : determinarCriticidad(tabla, accion))
                .operacionExitosa(true) // Siempre exitoso si no hay rollback
                .build();

        auditoriaRepository.save(auditoria);

        log.info("Auditoría registrada - ID: {}, Tabla: {}, Acción: {}",
                auditoria.getId(), tabla, accion);
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
        log.debug("Registrando auditoría simple - Tabla: {}, Acción: {}", tabla, accion);

        registrar(usuario, tabla, accion, descripcion, null, null, null, null,
                ipOrigen, null, metodoHttp, endpoint, modulo, null);
    }

    /**
     * Registra un login exitoso
     */
    @Transactional
    public void registrarLogin(Usuarios usuario, String ipOrigen, String userAgent) {
        log.info("Registrando login - Usuario: {}, IP: {}", usuario.getCorreo(), ipOrigen);

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
        log.info("Registrando nuevo usuario - Tipo: {}, Email: {}", tipoUsuario, usuario.getCorreo());

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
        log.info("Registrando creación de sector - Nombre: {}, ID: {}", nombreSector, sectorId);

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
                crearDatosSector(sectorId, nombreSector, numeroCoordenadas),
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
        log.info("Registrando actualización de sector - ID: {}, Nombre anterior: {}, Nombre nuevo: {}",
                sectorId, nombreAnterior, nombreNuevo);

        String descripcion = String.format(
                "Sector actualizado: '%s' -> '%s' (ID: %d) con %d coordenadas",
                nombreAnterior, nombreNuevo, sectorId, numeroCoordenadas
        );

        registrar(
                usuario,
                "sectores",
                "UPDATE",
                descripcion,
                sectorId,
                crearDatosSector(sectorId, nombreAnterior, numeroCoordenadas),
                crearDatosSector(sectorId, nombreNuevo, numeroCoordenadas),
                List.of("nombre", "color", "coordenadas"),
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
        log.warn("Registrando eliminación de sector - ID: {}, Nombre: {}", sectorId, nombreSector);

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
                crearDatosSector(sectorId, nombreSector, 0),
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
        log.info("Registrando aprobación/rechazo de socio - ID: {}, Estado: {}",
                cooperativaSocioId, estado);

        String accionTexto = estado.equals("aprobado") ? "aprobada" : "rechazada";
        String descripcion = String.format(
                "Solicitud de %s %s%s",
                nombreSocio,
                accionTexto,
                observaciones != null ? ". " + observaciones : ""
        );

        registrar(
                usuario,
                "cooperativa_socio",
                "APPROVAL",
                descripcion,
                cooperativaSocioId,
                Map.of("estado", "pendiente"),
                Map.of("estado", estado, "observaciones", observaciones != null ? observaciones : ""),
                List.of("estado", "observaciones"),
                ipOrigen,
                null,
                metodoHttp,
                endpoint,
                "RF02",
                "CRÍTICO"
        );
    }

    /**
     * Registra creación de minas
     */
    @Transactional
    public void registrarCreacionMina(
            Usuarios usuario,
            Integer minaId,
            String nombreMina,
            Integer sectorId,
            String ipOrigen,
            String metodoHttp,
            String endpoint
    ) {
        log.info("Registrando creación de mina - Nombre: {}, ID: {}", nombreMina, minaId);

        String descripcion = String.format(
                "Mina creada: %s (ID: %d) en sector ID: %d",
                nombreMina, minaId, sectorId
        );

        registrar(
                usuario,
                "minas",
                "INSERT",
                descripcion,
                minaId,
                null,
                Map.of(
                        "id", minaId,
                        "nombre", nombreMina,
                        "sectorId", sectorId,
                        "estado", "activo"
                ),
                null,
                ipOrigen,
                null,
                metodoHttp,
                endpoint,
                "RF03",
                "MEDIO"
        );
    }

    /**
     * Registra actualización de minas
     */
    @Transactional
    public void registrarActualizacionMina(
            Usuarios usuario,
            Integer minaId,
            String nombreMina,
            List<String> camposModificados,
            String ipOrigen,
            String metodoHttp,
            String endpoint
    ) {
        log.info("Registrando actualización de mina - ID: {}, Nombre: {}", minaId, nombreMina);

        String descripcion = String.format(
                "Mina actualizada: %s (ID: %d)",
                nombreMina, minaId
        );

        registrar(
                usuario,
                "minas",
                "UPDATE",
                descripcion,
                minaId,
                null,
                null,
                camposModificados,
                ipOrigen,
                null,
                metodoHttp,
                endpoint,
                "RF03",
                "MEDIO"
        );
    }

    /**
     * Registra eliminación lógica de minas
     */
    @Transactional
    public void registrarEliminacionMina(
            Usuarios usuario,
            Integer minaId,
            String nombreMina,
            String ipOrigen,
            String metodoHttp,
            String endpoint
    ) {
        log.warn("Registrando eliminación de mina - ID: {}, Nombre: {}", minaId, nombreMina);

        String descripcion = String.format(
                "Mina eliminada (lógicamente): '%s' (ID: %d)",
                nombreMina, minaId
        );

        registrar(
                usuario,
                "minas",
                "DELETE_LOGICO",
                descripcion,
                minaId,
                Map.of("estado", "activo"),
                Map.of("estado", "inactivo"),
                List.of("estado"),
                ipOrigen,
                null,
                metodoHttp,
                endpoint,
                "RF03",
                "ALTO"
        );
    }

    // ==================== MÉTODOS AUXILIARES PRIVADOS ====================

    /**
     * Obtiene el tipo de usuario
     */
    private String obtenerTipoUsuario(Usuarios usuario) {
        return usuario.getTipoUsuarioId() != null ?
                usuario.getTipoUsuarioId().getTipoUsuario() : null;
    }

    /**
     * Convierte un objeto a JSON string (null-safe)
     */
    private String convertirAJson(Object objeto) {
        if (objeto == null) {
            return null;
        }

        try {
            return objectMapper.writeValueAsString(objeto);
        } catch (JsonProcessingException e) {
            log.error("Error al serializar objeto a JSON", e);
            return "{\"error\":\"No se pudo serializar\"}";
        }
    }

    /**
     * Convierte una lista a JSON string (null-safe)
     */
    private String convertirListaAJson(List<String> lista) {
        if (lista == null || lista.isEmpty()) {
            return null;
        }
        return convertirAJson(lista);
    }

    /**
     * Determina la criticidad según la tabla y acción
     */
    private String determinarCriticidad(String tabla, String accion) {
        // Operaciones críticas en tablas sensibles
        if (esTablaCritica(tabla)) {
            return "CRÍTICO";
        }

        // Eliminaciones siempre son de alta criticidad
        if ("DELETE".equals(accion) || "DELETE_LOGICO".equals(accion)) {
            return "ALTO";
        }

        // Aprobaciones son críticas
        if ("APPROVAL".equals(accion)) {
            return "CRÍTICO";
        }

        // Inserciones y actualizaciones son de media criticidad
        if ("INSERT".equals(accion) || "UPDATE".equals(accion)) {
            return "MEDIO";
        }

        return "BAJO";
    }

    /**
     * Verifica si una tabla es crítica
     */
    private boolean esTablaCritica(String tabla) {
        return List.of(
                "liquidacion",
                "concentrado",
                "pesajes",
                "reporte_quimico",
                "cooperativa_socio"
        ).contains(tabla);
    }

    /**
     * Crea un objeto con datos básicos del usuario para auditoría
     */
    private Map<String, Object> crearDatosUsuario(Usuarios usuario) {
        return Map.of(
                "id", usuario.getId(),
                "correo", usuario.getCorreo(),
                "tipoUsuario", obtenerTipoUsuario(usuario)
        );
    }

    /**
     * Crea un objeto con datos del sector para auditoría
     */
    private Map<String, Object> crearDatosSector(Integer sectorId, String nombreSector, int numeroCoordenadas) {
        return Map.of(
                "id", sectorId,
                "nombre", nombreSector,
                "numeroCoordenadas", numeroCoordenadas
        );
    }

    /**
     * Registra un error en auditoría
     */
    @Transactional
    public void registrarError(
            Usuarios usuario,
            String tabla,
            String accion,
            String descripcionError,
            String mensajeError,
            String ipOrigen,
            String metodoHttp,
            String endpoint
    ) {
        log.error("Registrando error en auditoría - Tabla: {}, Acción: {}, Error: {}",
                tabla, accion, mensajeError);

        Auditoria auditoria = Auditoria.builder()
                .usuariosId(usuario)
                .tipoUsuario(obtenerTipoUsuario(usuario))
                .tablaAfectada(tabla)
                .accion(accion)
                .descripcion(descripcionError)
                .ipOrigen(ipOrigen)
                .metodoHttp(metodoHttp)
                .endpoint(endpoint)
                .fechaOperacion(LocalDateTime.now())
                .operacionExitosa(false)
                .mensajeError(mensajeError)
                .nivelCriticidad("ALTO")
                .build();

        auditoriaRepository.save(auditoria);
    }

    /**
     * Obtiene auditorías recientes de un usuario
     */
    @Transactional(readOnly = true)
    public List<Auditoria> obtenerAuditoriasRecientes(Integer usuarioId, int limite) {
        log.debug("Obteniendo {} auditorías recientes para usuario ID: {}", limite, usuarioId);
        return auditoriaRepository.findTopNByUsuarioIdOrderByFechaOperacionDesc(usuarioId, limite);
    }

    /**
     * Obtiene auditorías por tabla y acción
     */
    @Transactional(readOnly = true)
    public List<Auditoria> obtenerAuditoriasPorTablaYAccion(
            String tabla,
            String accion,
            LocalDateTime fechaInicio,
            LocalDateTime fechaFin
    ) {
        log.debug("Obteniendo auditorías - Tabla: {}, Acción: {}, Rango: {} a {}",
                tabla, accion, fechaInicio, fechaFin);
        return auditoriaRepository.findByTablaAndAccionAndFechaRange(
                tabla, accion, fechaInicio, fechaFin);
    }

    /**
     * Cuenta operaciones por nivel de criticidad
     */
    @Transactional(readOnly = true)
    public Map<String, Long> contarPorCriticidad(LocalDateTime fechaInicio, LocalDateTime fechaFin) {
        log.debug("Contando operaciones por criticidad - Rango: {} a {}", fechaInicio, fechaFin);

        List<Auditoria> auditorias = auditoriaRepository.findByFechaOperacionBetween(fechaInicio, fechaFin);

        return auditorias.stream()
                .collect(java.util.stream.Collectors.groupingBy(
                        Auditoria::getNivelCriticidad,
                        java.util.stream.Collectors.counting()
                ));
    }
}