package ucb.edu.bo.sumajflow.bl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ucb.edu.bo.sumajflow.dto.NotificacionDto;
import ucb.edu.bo.sumajflow.entity.Notificacion;
import ucb.edu.bo.sumajflow.entity.Usuarios;
import ucb.edu.bo.sumajflow.repository.NotificacionRepository;
import ucb.edu.bo.sumajflow.repository.UsuariosRepository;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class NotificacionBl {

    private final NotificacionRepository notificacionRepository;
    private final UsuariosRepository usuariosRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper;

    public NotificacionBl(
            NotificacionRepository notificacionRepository,
            UsuariosRepository usuariosRepository,
            SimpMessagingTemplate messagingTemplate,
            ObjectMapper objectMapper) {
        this.notificacionRepository = notificacionRepository;
        this.usuariosRepository = usuariosRepository;
        this.messagingTemplate = messagingTemplate;
        this.objectMapper = objectMapper;
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

        Usuarios usuario = usuariosRepository.findById(usuarioId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        Notificacion notificacion = new Notificacion();
        notificacion.setUsuariosId(usuario);
        notificacion.setTipo(tipo);
        notificacion.setTitulo(titulo);
        notificacion.setMensaje(mensaje);
        notificacion.setLeido(false);
        notificacion.setFechaCreacion(new Date());

        // Convertir metadata a JSON string
        if (metadata != null && !metadata.isEmpty()) {
            try {
                notificacion.setMetadata(objectMapper.writeValueAsString(metadata));
            } catch (JsonProcessingException e) {
                System.err.println("Error al serializar metadata: " + e.getMessage());
            }
        }

        notificacion = notificacionRepository.save(notificacion);

        // Enviar por WebSocket en tiempo real
        enviarNotificacionWebSocket(usuario.getId(), convertirADto(notificacion));
    }

    /**
     * Envía notificación por WebSocket
     */
    private void enviarNotificacionWebSocket(Integer usuarioId, NotificacionDto dto) {
        try {
            messagingTemplate.convertAndSendToUser(
                    usuarioId.toString(),
                    "/queue/notificaciones",
                    dto
            );
        } catch (Exception e) {
            System.err.println("Error al enviar notificación por WebSocket: " + e.getMessage());
        }
    }

    /**
     * Obtiene notificaciones de un usuario
     */
    @Transactional(readOnly = true)
    public List<NotificacionDto> obtenerNotificaciones(Integer usuarioId, Boolean soloNoLeidas) {
        Usuarios usuario = usuariosRepository.findById(usuarioId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        List<Notificacion> notificaciones;
        if (Boolean.TRUE.equals(soloNoLeidas)) {
            notificaciones = notificacionRepository
                    .findByUsuariosIdAndLeidoOrderByFechaCreacionDesc(usuario, false);
        } else {
            notificaciones = notificacionRepository
                    .findTop20ByUsuariosIdOrderByFechaCreacionDesc(usuario);
        }

        return notificaciones.stream()
                .map(this::convertirADto)
                .collect(Collectors.toList());
    }

    /**
     * Marca una notificación como leída
     *  VALIDACIÓN DE SEGURIDAD: Verifica que la notificación pertenezca al usuario
     */
    @Transactional
    public void marcarComoLeida(Integer notificacionId, Integer usuarioId) {
        // Buscar la notificación
        Notificacion notificacion = notificacionRepository.findById(notificacionId)
                .orElseThrow(() -> new IllegalArgumentException("Notificación no encontrada"));

        //  VALIDAR que la notificación pertenezca al usuario autenticado
        if (!notificacion.getUsuariosId().getId().equals(usuarioId)) {
            throw new SecurityException("No tienes permiso para modificar esta notificación");
        }

        // Marcar como leída
        notificacionRepository.marcarComoLeida(notificacionId);
    }

    /**
     * Marca todas las notificaciones de un usuario como leídas
     */
    @Transactional
    public void marcarTodasComoLeidas(Integer usuarioId) {
        Usuarios usuario = usuariosRepository.findById(usuarioId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));
        notificacionRepository.marcarTodasComoLeidas(usuario);
    }

    /**
     * Cuenta notificaciones no leídas
     */
    @Transactional(readOnly = true)
    public Long contarNoLeidas(Integer usuarioId) {
        Usuarios usuario = usuariosRepository.findById(usuarioId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));
        return notificacionRepository.countByUsuariosIdAndLeido(usuario, false);
    }

    /**
     * Elimina una notificación
     *  VALIDACIÓN DE SEGURIDAD: Verifica que la notificación pertenezca al usuario
     */
    @Transactional
    public void eliminarNotificacion(Integer notificacionId, Integer usuarioId) {
        // Buscar la notificación
        Notificacion notificacion = notificacionRepository.findById(notificacionId)
                .orElseThrow(() -> new IllegalArgumentException("Notificación no encontrada"));

        //  VALIDAR que la notificación pertenezca al usuario autenticado
        if (!notificacion.getUsuariosId().getId().equals(usuarioId)) {
            throw new SecurityException("No tienes permiso para eliminar esta notificación");
        }

        // Eliminar
        notificacionRepository.delete(notificacion);
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
                dto.setMetadata(objectMapper.readValue(
                        notificacion.getMetadata(), Map.class));
            } catch (JsonProcessingException e) {
                System.err.println("Error al deserializar metadata: " + e.getMessage());
            }
        }

        return dto;
    }

    /**
     * Calcula tiempo relativo para mostrar en UI
     */
    private String calcularTiempoRelativo(Date fecha) {
        long diff = System.currentTimeMillis() - fecha.getTime();
        long segundos = diff / 1000;

        if (segundos < 60) return "Justo ahora";

        long minutos = segundos / 60;
        if (minutos < 60) return "Hace " + minutos + " minuto" + (minutos > 1 ? "s" : "");

        long horas = minutos / 60;
        if (horas < 24) return "Hace " + horas + " hora" + (horas > 1 ? "s" : "");

        long dias = horas / 24;
        if (dias < 30) return "Hace " + dias + " día" + (dias > 1 ? "s" : "");

        long meses = dias / 30;
        return "Hace " + meses + " mes" + (meses > 1 ? "es" : "");
    }
}