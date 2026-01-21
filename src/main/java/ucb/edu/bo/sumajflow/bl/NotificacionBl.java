package ucb.edu.bo.sumajflow.bl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ucb.edu.bo.sumajflow.dto.NotificacionDto;
import ucb.edu.bo.sumajflow.entity.Notificacion;
import ucb.edu.bo.sumajflow.entity.Usuarios;
import ucb.edu.bo.sumajflow.repository.NotificacionRepository;
import ucb.edu.bo.sumajflow.repository.UsuariosRepository;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificacionBl {

    private final NotificacionRepository notificacionRepository;
    private final UsuariosRepository usuariosRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper;


    /**
     * Obtiene notificaciones paginadas con filtros
     * @param usuarioId ID del usuario
     * @param soloNoLeidas Filtrar solo no leídas
     * @param tipo Filtrar por tipo (info, success, warning, error)
     * @param page Número de página (0-indexed)
     * @param size Elementos por página
     * @return Map con notificaciones y metadata de paginación
     */
    @Transactional(readOnly = true)
    public Map<String, Object> obtenerNotificacionesPaginadas(
            Integer usuarioId,
            Boolean soloNoLeidas,
            String tipo,
            Integer page,
            Integer size) {

        log.debug("Obteniendo notificaciones paginadas - Usuario: {}, Página: {}, Tamaño: {}",
                usuarioId, page, size);

        Usuarios usuario = usuariosRepository.findById(usuarioId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        // Obtener todas las notificaciones (sin paginación en repository)
        List<Notificacion> todasLasNotificaciones = notificacionRepository
                .findByUsuariosIdOrderByFechaCreacionDesc(usuario);

        // Aplicar filtros
        List<Notificacion> notificacionesFiltradas = todasLasNotificaciones.stream()
                .filter(n -> {
                    // Filtro de leído/no leído
                    if (Boolean.TRUE.equals(soloNoLeidas) && n.getLeido()) {
                        return false;
                    }
                    // Filtro por tipo
                    if (tipo != null && !tipo.isEmpty() && !tipo.equals(n.getTipo())) {
                        return false;
                    }
                    return true;
                })
                .collect(Collectors.toList());

        // Calcular paginación
        int totalElementos = notificacionesFiltradas.size();
        int totalPaginas = (int) Math.ceil((double) totalElementos / size);
        int inicio = page * size;
        int fin = Math.min(inicio + size, totalElementos);

        // Obtener página actual
        List<NotificacionDto> notificacionesPagina = notificacionesFiltradas.stream()
                .skip(inicio)
                .limit(size)
                .map(this::convertirADto)
                .collect(Collectors.toList());

        // Construir respuesta
        Map<String, Object> resultado = new HashMap<>();
        resultado.put("notificaciones", notificacionesPagina);
        resultado.put("totalElementos", totalElementos);
        resultado.put("totalPaginas", totalPaginas);
        resultado.put("paginaActual", page);
        resultado.put("elementosPorPagina", size);
        resultado.put("tieneSiguiente", page < totalPaginas - 1);
        resultado.put("tieneAnterior", page > 0);

        return resultado;
    }


    /**
     * Crea y envía una notificación en tiempo real
     */
    @Transactional
    public void crearNotificacion(
            Integer usuarioId,
            String tipo,
            String titulo,
            String mensaje,
            Map<String, Object> metadata) {

        log.info("Creando notificación - Usuario ID: {}, Tipo: {}", usuarioId, tipo);

        Usuarios usuario = usuariosRepository.findById(usuarioId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        Notificacion notificacion = Notificacion.builder()
                .usuariosId(usuario)
                .tipo(tipo)
                .titulo(titulo)
                .mensaje(mensaje)
                .leido(false)
                .fechaCreacion(LocalDateTime.now())
                .metadata(convertirMetadataAJson(metadata))
                .build();

        notificacion = notificacionRepository.save(notificacion);

        // Enviar por WebSocket en tiempo real
        enviarNotificacionWebSocket(usuario.getId(), convertirADto(notificacion));

        log.info("Notificación creada y enviada - ID: {}", notificacion.getId());
    }

    /**
     * Envía notificación por WebSocket
     */
    private void enviarNotificacionWebSocket(Integer usuarioId, NotificacionDto dto) {
        try {
            String destination = "/queue/notificaciones";
            String userIdStr = usuarioId.toString();

            log.info("📤 Enviando notificación WebSocket - Usuario: {}, Destino: {}", userIdStr, destination);

            messagingTemplate.convertAndSendToUser(
                    userIdStr,
                    destination,
                    dto
            );

            log.info("✅ Notificación enviada por WebSocket - Usuario ID: {}", usuarioId);
        } catch (Exception e) {
            log.error("❌ Error al enviar notificación por WebSocket - Usuario ID: {}", usuarioId, e);
        }
    }

    /**
     * Obtiene notificaciones de un usuario
     */
    @Transactional(readOnly = true)
    public List<NotificacionDto> obtenerNotificaciones(Integer usuarioId, Boolean soloNoLeidas) {
        log.debug("Obteniendo notificaciones - Usuario ID: {}, Solo no leídas: {}", usuarioId, soloNoLeidas);

        Usuarios usuario = usuariosRepository.findById(usuarioId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        List<Notificacion> notificaciones = Boolean.TRUE.equals(soloNoLeidas)
                ? notificacionRepository.findByUsuariosIdAndLeidoOrderByFechaCreacionDesc(usuario, false)
                : notificacionRepository.findTop20ByUsuariosIdOrderByFechaCreacionDesc(usuario);

        return notificaciones.stream()
                .map(this::convertirADto)
                .collect(Collectors.toList());
    }

    /**
     * Marca una notificación como leída
     * ⚠️ VALIDACIÓN DE SEGURIDAD: Verifica que la notificación pertenezca al usuario
     */
    @Transactional
    public void marcarComoLeida(Integer notificacionId, Integer usuarioId) {
        log.info("Marcando notificación como leída - ID: {}, Usuario ID: {}", notificacionId, usuarioId);

        Notificacion notificacion = notificacionRepository.findById(notificacionId)
                .orElseThrow(() -> new IllegalArgumentException("Notificación no encontrada"));

        // ⚠️ VALIDAR que la notificación pertenezca al usuario autenticado
        if (!notificacion.getUsuariosId().getId().equals(usuarioId)) {
            log.warn("Intento de acceso no autorizado - Notificación ID: {}, Usuario ID: {}",
                    notificacionId, usuarioId);
            throw new SecurityException("No tienes permiso para modificar esta notificación");
        }

        notificacionRepository.marcarComoLeida(notificacionId);
        log.debug("Notificación marcada como leída - ID: {}", notificacionId);
    }

    /**
     * Marca todas las notificaciones de un usuario como leídas
     */
    @Transactional
    public void marcarTodasComoLeidas(Integer usuarioId) {
        log.info("Marcando todas las notificaciones como leídas - Usuario ID: {}", usuarioId);

        Usuarios usuario = usuariosRepository.findById(usuarioId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        notificacionRepository.marcarTodasComoLeidas(usuario);
        log.debug("Todas las notificaciones marcadas como leídas - Usuario ID: {}", usuarioId);
    }

    /**
     * Cuenta notificaciones no leídas
     */
    @Transactional(readOnly = true)
    public Long contarNoLeidas(Integer usuarioId) {
        log.debug("Contando notificaciones no leídas - Usuario ID: {}", usuarioId);

        Usuarios usuario = usuariosRepository.findById(usuarioId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        return notificacionRepository.countByUsuariosIdAndLeido(usuario, false);
    }

    /**
     * Elimina una notificación
     * VALIDACIÓN DE SEGURIDAD: Verifica que la notificación pertenezca al usuario
     */
    @Transactional
    public void eliminarNotificacion(Integer notificacionId, Integer usuarioId) {
        log.info("Eliminando notificación - ID: {}, Usuario ID: {}", notificacionId, usuarioId);

        Notificacion notificacion = notificacionRepository.findById(notificacionId)
                .orElseThrow(() -> new IllegalArgumentException("Notificación no encontrada"));

        //VALIDAR que la notificación pertenezca al usuario autenticado
        if (!notificacion.getUsuariosId().getId().equals(usuarioId)) {
            log.warn("Intento de eliminación no autorizado - Notificación ID: {}, Usuario ID: {}",
                    notificacionId, usuarioId);
            throw new SecurityException("No tienes permiso para eliminar esta notificación");
        }

        notificacionRepository.delete(notificacion);
        log.info("Notificación eliminada - ID: {}", notificacionId);
    }

    // ==================== MÉTODOS AUXILIARES PRIVADOS ====================

    /**
     * Convierte metadata a JSON string
     */
    private String convertirMetadataAJson(Map<String, Object> metadata) {
        if (metadata == null || metadata.isEmpty()) {
            return null;
        }

        try {
            return objectMapper.writeValueAsString(metadata);
        } catch (JsonProcessingException e) {
            log.error("Error al serializar metadata", e);
            return null;
        }
    }

    /**
     * Convierte entidad a DTO
     */
    private NotificacionDto convertirADto(Notificacion notificacion) {
        NotificacionDto dto = new NotificacionDto();
        dto.setId(notificacion.getId());
        dto.setTipo(notificacion.getTipo());
        dto.setTitulo(notificacion.getTitulo());
        dto.setMensaje(notificacion.getMensaje());
        dto.setLeido(notificacion.getLeido());
        dto.setFechaCreacion(notificacion.getFechaCreacion());
        dto.setTime(calcularTiempoRelativo(notificacion.getFechaCreacion()));

        // Parsear metadata si existe
        if (notificacion.getMetadata() != null) {
            try {
                dto.setMetadata(objectMapper.readValue(notificacion.getMetadata(), Map.class));
            } catch (JsonProcessingException e) {
                log.error("Error al deserializar metadata - Notificación ID: {}", notificacion.getId(), e);
            }
        }

        return dto;
    }

    /**
     * Calcula tiempo relativo para mostrar en UI (mejorado con LocalDateTime)
     */
    private String calcularTiempoRelativo(LocalDateTime fechaCreacion) {
        Duration duracion = Duration.between(fechaCreacion, LocalDateTime.now());

        long segundos = duracion.getSeconds();
        if (segundos < 60) return "Justo ahora";

        long minutos = duracion.toMinutes();
        if (minutos < 60) return "Hace " + minutos + " minuto" + (minutos > 1 ? "s" : "");

        long horas = duracion.toHours();
        if (horas < 24) return "Hace " + horas + " hora" + (horas > 1 ? "s" : "");

        long dias = duracion.toDays();
        if (dias < 30) return "Hace " + dias + " día" + (dias > 1 ? "s" : "");

        long meses = dias / 30;
        return "Hace " + meses + " mes" + (meses > 1 ? "es" : "");
    }
}