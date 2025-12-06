package ucb.edu.bo.sumajflow.bl.cooperativa;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ucb.edu.bo.sumajflow.bl.AuditoriaBl;
import ucb.edu.bo.sumajflow.bl.NotificacionBl;
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
@Slf4j
@Service
@RequiredArgsConstructor
public class CooperativaBl {

    // Inyección de dependencias por constructor con @RequiredArgsConstructor
    private final CooperativaRepository cooperativaRepository;
    private final CooperativaSocioRepository cooperativaSocioRepository;
    private final SocioRepository socioRepository;
    private final PersonaRepository personaRepository;
    private final UsuariosRepository usuariosRepository;
    private final AuditoriaBl auditoriaBl;
    private final NotificacionBl notificacionBl;

    /**
     * Obtiene la cooperativa asociada a un usuario
     */
    @Transactional(readOnly = true)
    public Cooperativa obtenerCooperativaPorUsuario(Integer usuarioId) {
        log.debug("Obteniendo cooperativa para usuario ID: {}", usuarioId);

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
        log.info("Listando socios - Usuario: {}, Estado: {}, Búsqueda: {}, Página: {}",
                usuarioId, estado, busqueda, pagina);

        // 1. Obtener cooperativa
        Cooperativa cooperativa = obtenerCooperativaPorUsuario(usuarioId);

        // 2. Determinar si necesitamos ordenar por nombre
        boolean ordenarPorNombre = "nombre".equals(ordenarPor);

        // 3. Configurar ordenamiento (si no es por nombre, usar el sort de base de datos)
        Sort sort = ordenarPorNombre ? Sort.unsorted() : configurarOrdenamiento(ordenarPor, direccion);

        // 4. Crear pageable
        Pageable pageable = PageRequest.of(pagina, tamanoPagina, sort);

        // 5. Buscar socios según filtros
        Page<CooperativaSocio> pageCooperativaSocio = obtenerSociosPaginados(
                cooperativa, estado, busqueda, pageable);

        // 6. Convertir a DTOs
        List<SocioResponseDto> sociosDto = pageCooperativaSocio.getContent().stream()
                .map(this::convertirADto)
                .collect(Collectors.toList());

        // 7. Si se requiere ordenar por nombre, hacerlo en memoria
        if (ordenarPorNombre) {
            sociosDto = ordenarPorNombreEnMemoria(sociosDto, direccion);
        }

        // 8. Obtener estadísticas
        Map<String, Long> estadisticas = obtenerEstadisticas(cooperativa);

        // 9. Crear respuesta paginada
        return crearRespuestaPaginada(pageCooperativaSocio, sociosDto, estadisticas);
    }

    /**
     * Aprueba o rechaza una solicitud de socio CON CONTEXTO HTTP
     */
    @Transactional
    public void procesarSolicitud(
            Integer usuarioId,
            SocioAprobacionDto dto,
            String ipOrigen,
            String metodoHttp,
            String endpoint
    ) {
        log.info("Procesando solicitud de socio - CooperativaSocioId: {}, Estado: {}",
                dto.getCooperativaSocioId(), dto.getEstado());

        // 1. Validar datos
        validarEstadoAprobacion(dto);

        // 2. Obtener cooperativa del usuario
        Cooperativa cooperativa = obtenerCooperativaPorUsuario(usuarioId);

        // 3. Buscar relación cooperativa-socio
        CooperativaSocio cooperativaSocio = cooperativaSocioRepository
                .findById(dto.getCooperativaSocioId())
                .orElseThrow(() -> new IllegalArgumentException("Solicitud no encontrada"));

        // 4. Validar permisos y estado
        validarSolicitud(cooperativaSocio, cooperativa);

        // 5. Actualizar estados
        actualizarEstadosSocio(cooperativaSocio, dto);

        // 6. Obtener datos para auditoría y notificación
        Usuarios usuarioCooperativa = usuariosRepository.findById(usuarioId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        Socio socio = cooperativaSocio.getSocioId();
        Persona personaSocio = personaRepository.findByUsuariosId(socio.getUsuariosId())
                .orElseThrow(() -> new IllegalArgumentException("Datos de persona no encontrados"));

        String nombreCompleto = construirNombreCompleto(personaSocio);

        // 7. Registrar en auditoría CON CONTEXTO COMPLETO
        auditoriaBl.registrarAprobacionSocio(
                usuarioCooperativa,
                cooperativaSocio.getId(),
                nombreCompleto,
                dto.getEstado(),
                dto.getObservaciones(),
                ipOrigen,
                metodoHttp,
                endpoint
        );

        // 8. Enviar notificación al socio
        enviarNotificacionAprobacion(cooperativa, socio, dto, cooperativaSocio.getId());

        log.info("Solicitud procesada exitosamente - Estado: {}", dto.getEstado());
    }

    /**
     * Obtiene el detalle completo de un socio
     */
    @Transactional(readOnly = true)
    public SocioResponseDto obtenerDetalleSocio(Integer usuarioId, Integer cooperativaSocioId) {
        log.debug("Obteniendo detalle de socio - CooperativaSocioId: {}", cooperativaSocioId);

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

    // ==================== MÉTODOS AUXILIARES PRIVADOS ====================

    /**
     * Obtiene socios paginados según filtros
     */
    private Page<CooperativaSocio> obtenerSociosPaginados(
            Cooperativa cooperativa,
            String estado,
            String busqueda,
            Pageable pageable
    ) {
        boolean tieneEstado = estado != null && !estado.isEmpty();
        boolean tieneBusqueda = busqueda != null && !busqueda.isEmpty();

        if (tieneEstado && tieneBusqueda) {
            return cooperativaSocioRepository.findByCooperativaAndEstadoAndBusqueda(
                    cooperativa, estado, busqueda.toLowerCase(), pageable);
        } else if (tieneEstado) {
            return cooperativaSocioRepository.findByCooperativaIdAndEstado(
                    cooperativa, estado, pageable);
        } else if (tieneBusqueda) {
            return cooperativaSocioRepository.findByCooperativaAndBusqueda(
                    cooperativa, busqueda.toLowerCase(), pageable);
        } else {
            return cooperativaSocioRepository.findByCooperativaId(cooperativa, pageable);
        }
    }

    /**
     * Ordena socios por nombre en memoria
     */
    private List<SocioResponseDto> ordenarPorNombreEnMemoria(
            List<SocioResponseDto> socios,
            String direccion
    ) {
        Comparator<SocioResponseDto> comparador = Comparator.comparing(
                dto -> dto.getNombres() != null ? dto.getNombres().toLowerCase() : ""
        );

        if ("desc".equalsIgnoreCase(direccion)) {
            comparador = comparador.reversed();
        }

        return socios.stream()
                .sorted(comparador)
                .collect(Collectors.toList());
    }

    /**
     * Obtiene estadísticas de socios por estado
     */
    private Map<String, Long> obtenerEstadisticas(Cooperativa cooperativa) {
        Map<String, Long> stats = new HashMap<>();
        stats.put("aprobados", cooperativaSocioRepository.countByCooperativaIdAndEstado(cooperativa, "aprobado"));
        stats.put("pendientes", cooperativaSocioRepository.countByCooperativaIdAndEstado(cooperativa, "pendiente"));
        stats.put("rechazados", cooperativaSocioRepository.countByCooperativaIdAndEstado(cooperativa, "rechazado"));
        return stats;
    }

    /**
     * Crea respuesta paginada con estadísticas
     */
    private SociosPaginadosDto crearRespuestaPaginada(
            Page<CooperativaSocio> page,
            List<SocioResponseDto> sociosDto,
            Map<String, Long> estadisticas
    ) {
        SociosPaginadosDto respuesta = new SociosPaginadosDto(
                sociosDto,
                page.getNumber(),
                page.getTotalPages(),
                page.getTotalElements(),
                page.getSize()
        );

        respuesta.setTotalAprobados(estadisticas.get("aprobados"));
        respuesta.setTotalPendientes(estadisticas.get("pendientes"));
        respuesta.setTotalRechazados(estadisticas.get("rechazados"));

        return respuesta;
    }

    /**
     * Valida el estado de aprobación
     */
    private void validarEstadoAprobacion(SocioAprobacionDto dto) {
        if (dto.getEstado() == null ||
                (!dto.getEstado().equals("aprobado") && !dto.getEstado().equals("rechazado"))) {
            throw new IllegalArgumentException("Estado debe ser 'aprobado' o 'rechazado'");
        }
    }

    /**
     * Valida la solicitud y permisos
     */
    private void validarSolicitud(CooperativaSocio cooperativaSocio, Cooperativa cooperativa) {
        if (!cooperativaSocio.getCooperativaId().getId().equals(cooperativa.getId())) {
            throw new SecurityException("No tienes permiso para procesar esta solicitud");
        }

        if (!"pendiente".equals(cooperativaSocio.getEstado())) {
            throw new IllegalArgumentException("Solo se pueden procesar solicitudes pendientes");
        }
    }

    /**
     * Actualiza estados de cooperativa-socio y socio
     */
    private void actualizarEstadosSocio(CooperativaSocio cooperativaSocio, SocioAprobacionDto dto) {
        // Actualizar estado en cooperativa_socio
        cooperativaSocio.setEstado(dto.getEstado());
        cooperativaSocio.setObservaciones(dto.getObservaciones());
        cooperativaSocioRepository.save(cooperativaSocio);

        // Actualizar estado en socio
        Socio socio = cooperativaSocio.getSocioId();
        socio.setEstado(dto.getEstado());
        socioRepository.save(socio);
    }

    /**
     * Construye nombre completo de la persona
     */
    private String construirNombreCompleto(Persona persona) {
        return persona.getNombres() + " " + persona.getPrimerApellido();
    }

    /**
     * Envía notificación de aprobación/rechazo al socio
     */
    private void enviarNotificacionAprobacion(
            Cooperativa cooperativa,
            Socio socio,
            SocioAprobacionDto dto,
            Integer cooperativaSocioId
    ) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("tipo", "aprobacion_socio");
        metadata.put("socioId", socio.getId());
        metadata.put("cooperativaId", cooperativa.getId());
        metadata.put("cooperativaNombre", cooperativa.getRazonSocial());
        metadata.put("estado", dto.getEstado());
        metadata.put("cooperativaSocioId", cooperativaSocioId);

        if (dto.getObservaciones() != null) {
            metadata.put("observaciones", dto.getObservaciones());
        }

        boolean esAprobado = dto.getEstado().equals("aprobado");
        String tipoNotificacion = esAprobado ? "success" : "warning";
        String titulo = esAprobado ? "¡Solicitud aprobada!" : "Solicitud rechazada";

        String mensaje = construirMensajeNotificacion(cooperativa, dto, esAprobado);

        notificacionBl.crearNotificacion(
                socio.getUsuariosId().getId(),
                tipoNotificacion,
                titulo,
                mensaje,
                metadata
        );
    }

    /**
     * Construye mensaje de notificación
     */
    private String construirMensajeNotificacion(
            Cooperativa cooperativa,
            SocioAprobacionDto dto,
            boolean esAprobado
    ) {
        StringBuilder mensaje = new StringBuilder();

        if (esAprobado) {
            mensaje.append("Tu solicitud para unirte a ")
                    .append(cooperativa.getRazonSocial())
                    .append(" ha sido aprobada. ¡Bienvenido!");
        } else {
            mensaje.append("Tu solicitud para unirte a ")
                    .append(cooperativa.getRazonSocial())
                    .append(" ha sido rechazada.");
        }

        if (dto.getObservaciones() != null && !dto.getObservaciones().isEmpty()) {
            mensaje.append(" Motivo: ").append(dto.getObservaciones());
        }

        return mensaje.toString();
    }

    /**
     * Configura el ordenamiento según los parámetros
     * NOTA: Para ordenar por nombre, se debe hacer en memoria después de la consulta
     */
    private Sort configurarOrdenamiento(String ordenarPor, String direccion) {
        String campo = switch (ordenarPor != null ? ordenarPor : "fechaAfiliacion") {
            case "fechaEnvio" -> "socioId.fechaEnvio";
            default -> "fechaAfiliacion";
        };

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
            mapearDatosPersona(dto, persona);
        }

        // Información de socio
        mapearDatosSocio(dto, socio);

        // Información de cooperativa-socio
        mapearDatosCooperativaSocio(dto, cooperativaSocio);

        // Información adicional
        dto.setCorreo(socio.getUsuariosId().getCorreo());

        return dto;
    }

    /**
     * Mapea datos de persona al DTO
     */
    private void mapearDatosPersona(SocioResponseDto dto, Persona persona) {
        dto.setNombres(persona.getNombres());
        dto.setPrimerApellido(persona.getPrimerApellido());
        dto.setSegundoApellido(persona.getSegundoApellido());
        dto.setNombreCompleto(construirNombreCompletoDto(persona));
        dto.setCi(persona.getCi());

        if (persona.getFechaNacimiento() != null) {
            dto.setFechaNacimiento(persona.getFechaNacimiento());
        }

        dto.setNumeroCelular(persona.getNumeroCelular());
        dto.setGenero(persona.getGenero());
    }

    /**
     * Construye nombre completo para el DTO
     */
    private String construirNombreCompletoDto(Persona persona) {
        StringBuilder nombreCompleto = new StringBuilder();
        nombreCompleto.append(persona.getNombres())
                .append(" ")
                .append(persona.getPrimerApellido());

        if (persona.getSegundoApellido() != null) {
            nombreCompleto.append(" ").append(persona.getSegundoApellido());
        }

        return nombreCompleto.toString();
    }

    /**
     * Mapea datos de socio al DTO
     */
    private void mapearDatosSocio(SocioResponseDto dto, Socio socio) {
        dto.setEstado(socio.getEstado());
        dto.setFechaEnvio(socio.getFechaEnvio());
        dto.setCarnetAfiliacionUrl(socio.getCarnetAfiliacionUrl());
        dto.setCarnetIdentidadUrl(socio.getCarnetIdentidadUrl());
    }

    /**
     * Mapea datos de cooperativa-socio al DTO
     */
    private void mapearDatosCooperativaSocio(SocioResponseDto dto, CooperativaSocio cooperativaSocio) {
        if (cooperativaSocio.getFechaAfiliacion() != null) {
            dto.setFechaAfiliacion(cooperativaSocio.getFechaAfiliacion());
        }
        dto.setObservaciones(cooperativaSocio.getObservaciones());
    }
}