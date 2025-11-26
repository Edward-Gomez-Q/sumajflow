package ucb.edu.bo.sumajflow.bl;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ucb.edu.bo.sumajflow.dto.socio.SocioAprobacionDto;
import ucb.edu.bo.sumajflow.dto.socio.SocioResponseDto;
import ucb.edu.bo.sumajflow.dto.socio.SociosPaginadosDto;
import ucb.edu.bo.sumajflow.entity.*;
import ucb.edu.bo.sumajflow.repository.*;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Servicio para gestionar operaciones de cooperativa
 */
@Service
public class CooperativaBl {

    private final CooperativaRepository cooperativaRepository;
    private final CooperativaSocioRepository cooperativaSocioRepository;
    private final SocioRepository socioRepository;
    private final PersonaRepository personaRepository;
    private final UsuariosRepository usuariosRepository;
    private final AuditoriaBl auditoriaBl;
    private final NotificacionBl notificacionBl;

    public CooperativaBl(
            CooperativaRepository cooperativaRepository,
            CooperativaSocioRepository cooperativaSocioRepository,
            SocioRepository socioRepository,
            PersonaRepository personaRepository,
            UsuariosRepository usuariosRepository,
            AuditoriaBl auditoriaBl,
            NotificacionBl notificacionBl
    ) {
        this.cooperativaRepository = cooperativaRepository;
        this.cooperativaSocioRepository = cooperativaSocioRepository;
        this.socioRepository = socioRepository;
        this.personaRepository = personaRepository;
        this.usuariosRepository = usuariosRepository;
        this.auditoriaBl = auditoriaBl;
        this.notificacionBl = notificacionBl;
    }

    /**
     * Obtiene la cooperativa asociada a un usuario
     */
    @Transactional(readOnly = true)
    public Cooperativa obtenerCooperativaPorUsuario(Integer usuarioId) {
        Usuarios usuario = usuariosRepository.findById(usuarioId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        return cooperativaRepository.findByUsuariosId(usuario)
                .orElseThrow(() -> new IllegalArgumentException("Cooperativa no encontrada para este usuario"));
    }

    /**
     * Lista todos los socios de una cooperativa con paginación y filtros
     */
    @Transactional(readOnly = true)
    public SociosPaginadosDto listarSocios(
            Integer usuarioId,
            String estado,
            String busqueda,
            String ordenarPor,
            String direccion,
            Integer pagina,
            Integer tamanoPagina
    ) {
        // 1. Obtener cooperativa
        Cooperativa cooperativa = obtenerCooperativaPorUsuario(usuarioId);

        // 2. Determinar si necesitamos ordenar por nombre
        boolean ordenarPorNombre = "nombre".equals(ordenarPor);

        // 3. Configurar ordenamiento (si no es por nombre, usar el sort de base de datos)
        Sort sort = ordenarPorNombre ? Sort.unsorted() : configurarOrdenamiento(ordenarPor, direccion);

        // 4. Crear pageable
        Pageable pageable = PageRequest.of(pagina, tamanoPagina, sort);

        // 5. Buscar socios según filtros
        Page<CooperativaSocio> pageCooperativaSocio;

        if (estado != null && !estado.isEmpty() && busqueda != null && !busqueda.isEmpty()) {
            // Con estado y búsqueda
            pageCooperativaSocio = cooperativaSocioRepository
                    .findByCooperativaAndEstadoAndBusqueda(
                            cooperativa, estado, busqueda.toLowerCase(), pageable);
        } else if (estado != null && !estado.isEmpty()) {
            // Solo con estado
            pageCooperativaSocio = cooperativaSocioRepository
                    .findByCooperativaIdAndEstado(cooperativa, estado, pageable);
        } else if (busqueda != null && !busqueda.isEmpty()) {
            // Solo con búsqueda
            pageCooperativaSocio = cooperativaSocioRepository
                    .findByCooperativaAndBusqueda(cooperativa, busqueda.toLowerCase(), pageable);
        } else {
            // Sin filtros
            pageCooperativaSocio = cooperativaSocioRepository
                    .findByCooperativaId(cooperativa, pageable);
        }

        // 6. Convertir a DTOs
        List<SocioResponseDto> sociosDto = pageCooperativaSocio.getContent().stream()
                .map(this::convertirADto)
                .collect(Collectors.toList());

        // 7. Si se requiere ordenar por nombre, hacerlo en memoria
        if (ordenarPorNombre) {
            Comparator<SocioResponseDto> comparador = Comparator.comparing(
                    dto -> dto.getNombres() != null ? dto.getNombres().toLowerCase() : ""
            );

            if ("desc".equalsIgnoreCase(direccion)) {
                comparador = comparador.reversed();
            }

            sociosDto = sociosDto.stream()
                    .sorted(comparador)
                    .collect(Collectors.toList());
        }

        // 8. Obtener estadísticas
        Long totalAprobados = cooperativaSocioRepository
                .countByCooperativaIdAndEstado(cooperativa, "aprobado");
        Long totalPendientes = cooperativaSocioRepository
                .countByCooperativaIdAndEstado(cooperativa, "pendiente");
        Long totalRechazados = cooperativaSocioRepository
                .countByCooperativaIdAndEstado(cooperativa, "rechazado");

        // 9. Crear respuesta paginada
        SociosPaginadosDto respuesta = new SociosPaginadosDto(
                sociosDto,
                pageCooperativaSocio.getNumber(),
                pageCooperativaSocio.getTotalPages(),
                pageCooperativaSocio.getTotalElements(),
                pageCooperativaSocio.getSize()
        );

        respuesta.setTotalAprobados(totalAprobados);
        respuesta.setTotalPendientes(totalPendientes);
        respuesta.setTotalRechazados(totalRechazados);

        return respuesta;
    }

    /**
     * Aprueba o rechaza una solicitud de socio
     */
    @Transactional
    public void procesarSolicitud(Integer usuarioId, SocioAprobacionDto dto) {
        // 1. Validar datos
        if (dto.getEstado() == null ||
                (!dto.getEstado().equals("aprobado") && !dto.getEstado().equals("rechazado"))) {
            throw new IllegalArgumentException("Estado debe ser 'aprobado' o 'rechazado'");
        }

        // 2. Obtener cooperativa del usuario
        Cooperativa cooperativa = obtenerCooperativaPorUsuario(usuarioId);

        // 3. Buscar relación cooperativa-socio
        CooperativaSocio cooperativaSocio = cooperativaSocioRepository
                .findById(dto.getCooperativaSocioId())
                .orElseThrow(() -> new IllegalArgumentException("Solicitud no encontrada"));

        // 4. Validar que la solicitud pertenece a la cooperativa
        if (!cooperativaSocio.getCooperativaId().getId().equals(cooperativa.getId())) {
            throw new SecurityException("No tienes permiso para procesar esta solicitud");
        }

        // 5. Validar que la solicitud esté pendiente
        if (!"pendiente".equals(cooperativaSocio.getEstado())) {
            throw new IllegalArgumentException("Solo se pueden procesar solicitudes pendientes");
        }

        // 6. Actualizar estado en cooperativa_socio
        cooperativaSocio.setEstado(dto.getEstado());
        cooperativaSocio.setObservaciones(dto.getObservaciones());
        cooperativaSocioRepository.save(cooperativaSocio);

        // 7. Actualizar estado en socio
        Socio socio = cooperativaSocio.getSocioId();
        socio.setEstado(dto.getEstado());
        socioRepository.save(socio);

        // 8. Obtener datos para notificación
        Usuarios usuarioCooperativa = usuariosRepository.findById(usuarioId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        Persona personaSocio = personaRepository.findByUsuariosId(socio.getUsuariosId())
                .orElseThrow(() -> new IllegalArgumentException("Datos de persona no encontrados"));

        // 9. Registrar en auditoría
        String descripcion = String.format(
                "Solicitud de %s %s %s. %s",
                personaSocio.getNombres(),
                personaSocio.getPrimerApellido(),
                dto.getEstado().equals("aprobado") ? "aprobada" : "rechazada",
                dto.getObservaciones() != null ? dto.getObservaciones() : ""
        );

        auditoriaBl.registrarAprobacion(
                usuarioCooperativa,
                "cooperativa_socio",
                "Solicitud ID: " + cooperativaSocio.getId(),
                dto.getEstado(),
                descripcion
        );

        // 10. Enviar notificación al socio
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("tipo", "aprobacion_socio"); // Para identificar el tipo de notificación
        metadata.put("socioId", socio.getId());
        metadata.put("cooperativaId", cooperativa.getId());
        metadata.put("cooperativaNombre", cooperativa.getRazonSocial());
        metadata.put("estado", dto.getEstado());
        metadata.put("cooperativaSocioId", cooperativaSocio.getId());

        if (dto.getObservaciones() != null) {
            metadata.put("observaciones", dto.getObservaciones());
        }

        String tipoNotificacion = dto.getEstado().equals("aprobado") ? "success" : "warning";
        String titulo = dto.getEstado().equals("aprobado")
                ? "¡Solicitud aprobada!"
                : "Solicitud rechazada";

        String mensaje = dto.getEstado().equals("aprobado")
                ? "Tu solicitud para unirte a " + cooperativa.getRazonSocial() + " ha sido aprobada. ¡Bienvenido!"
                : "Tu solicitud para unirte a " + cooperativa.getRazonSocial() + " ha sido rechazada.";

        if (dto.getObservaciones() != null && !dto.getObservaciones().isEmpty()) {
            mensaje += " Motivo: " + dto.getObservaciones();
        }

        notificacionBl.crearNotificacion(
                socio.getUsuariosId().getId(),
                tipoNotificacion,
                titulo,
                mensaje,
                metadata
        );
    }

    /**
     * Obtiene el detalle completo de un socio
     */
    @Transactional(readOnly = true)
    public SocioResponseDto obtenerDetalleSocio(Integer usuarioId, Integer cooperativaSocioId) {
        // 1. Obtener cooperativa
        Cooperativa cooperativa = obtenerCooperativaPorUsuario(usuarioId);

        // 2. Buscar relación cooperativa-socio
        CooperativaSocio cooperativaSocio = cooperativaSocioRepository
                .findById(cooperativaSocioId)
                .orElseThrow(() -> new IllegalArgumentException("Socio no encontrado"));

        // 3. Validar que pertenece a la cooperativa
        if (!cooperativaSocio.getCooperativaId().getId().equals(cooperativa.getId())) {
            throw new SecurityException("No tienes permiso para ver este socio");
        }

        // 4. Convertir a DTO
        return convertirADto(cooperativaSocio);
    }

    // ==================== MÉTODOS AUXILIARES ====================

    /**
     * Configura el ordenamiento según los parámetros
     * NOTA: Para ordenar por nombre, se debe hacer en memoria después de la consulta
     */
    private Sort configurarOrdenamiento(String ordenarPor, String direccion) {
        // Mapeo de campos de ordenamiento
        String campo;
        switch (ordenarPor != null ? ordenarPor : "fechaAfiliacion") {
            case "fechaAfiliacion":
                campo = "fechaAfiliacion";
                break;
            case "fechaEnvio":
                campo = "socioId.fechaEnvio";
                break;
            default:
                campo = "fechaAfiliacion";
        }

        // Dirección de ordenamiento
        Sort.Direction dir = "asc".equalsIgnoreCase(direccion)
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;

        return Sort.by(dir, campo);
    }

    /**
     * Convierte una entidad CooperativaSocio a DTO
     */
    private SocioResponseDto convertirADto(CooperativaSocio cooperativaSocio) {
        Socio socio = cooperativaSocio.getSocioId();
        Persona persona = personaRepository.findByUsuariosId(socio.getUsuariosId())
                .orElse(null);

        SocioResponseDto dto = new SocioResponseDto();

        // IDs
        dto.setId(socio.getId());
        dto.setUsuarioId(socio.getUsuariosId().getId());
        dto.setCooperativaSocioId(cooperativaSocio.getId());

        // Información personal
        if (persona != null) {
            dto.setNombres(persona.getNombres());
            dto.setPrimerApellido(persona.getPrimerApellido());
            dto.setSegundoApellido(persona.getSegundoApellido());
            dto.setNombreCompleto(
                    persona.getNombres() + " " +
                            persona.getPrimerApellido() +
                            (persona.getSegundoApellido() != null ? " " + persona.getSegundoApellido() : "")
            );
            dto.setCi(persona.getCi());

            // Convertir java.util.Date a java.sql.Date si es necesario
            if (persona.getFechaNacimiento() != null) {
                dto.setFechaNacimiento(
                        persona.getFechaNacimiento()
                );
            }

            dto.setNumeroCelular(persona.getNumeroCelular());
            dto.setGenero(persona.getGenero());
        }

        // Información de socio
        dto.setEstado(socio.getEstado());
        dto.setFechaEnvio(socio.getFechaEnvio());
        dto.setCarnetAfiliacionUrl(socio.getCarnetAfiliacionUrl());
        dto.setCarnetIdentidadUrl(socio.getCarnetIdentidadUrl());

        // Información de cooperativa-socio
        if (cooperativaSocio.getFechaAfiliacion() != null) {
            dto.setFechaAfiliacion(
                    cooperativaSocio.getFechaAfiliacion()
            );
        }
        dto.setObservaciones(cooperativaSocio.getObservaciones());

        // Información adicional
        dto.setCorreo(socio.getUsuariosId().getCorreo());

        return dto;
    }
}