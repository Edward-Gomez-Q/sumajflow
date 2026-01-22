package ucb.edu.bo.sumajflow.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.*;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketEventListener {

    private final Map<String, String> userSessions = new ConcurrentHashMap<>();
    private final Map<String, ScheduledExecutorService> disconnectSchedulers = new ConcurrentHashMap<>();
    private final SimpMessagingTemplate messagingTemplate;

    @EventListener
    public void handleWebSocketConnectListener(SessionConnectedEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = headerAccessor.getSessionId();
        String username = getUsername(headerAccessor);

        // Cancelar cualquier cierre programado para este usuario
        ScheduledExecutorService oldScheduler = disconnectSchedulers.remove(username);
        if (oldScheduler != null) {
            oldScheduler.shutdownNow();
            log.debug("Cancelado cierre programado para usuario: {}", username);
        }

        String oldSession = userSessions.put(username, sessionId);

        if (oldSession != null && !oldSession.equals(sessionId)) {
            log.warn("🔄 RECONEXIÓN detectada - Usuario: {}, Nueva sesión: {}, Sesión anterior: {}",
                    username, sessionId, oldSession);

            // Dar tiempo para que la sesión anterior se cierre naturalmente
            // antes de intentar cerrarla manualmente
        } else {
            log.info("✅ WebSocket CONECTADO - Usuario: {}, Sesión: {}", username, sessionId);
        }
    }

    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = headerAccessor.getSessionId();
        String username = getUsername(headerAccessor);

        // Programar limpieza después de un pequeño delay para evitar race conditions
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        disconnectSchedulers.put(username, scheduler);

        scheduler.schedule(() -> {
            String activeSession = userSessions.get(username);
            if (sessionId.equals(activeSession)) {
                userSessions.remove(username);
                log.info("🔌 WebSocket DESCONECTADO - Usuario: {}, Sesión: {}", username, sessionId);
            } else {
                log.debug("Sesión {} ya fue reemplazada por {} para usuario {}",
                        sessionId, activeSession, username);
            }
            disconnectSchedulers.remove(username);
            scheduler.shutdown();
        }, 500, TimeUnit.MILLISECONDS);
    }

    @EventListener
    public void handleSubscribeEvent(SessionSubscribeEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String username = getUsername(headerAccessor);
        String destination = headerAccessor.getDestination();

        log.info("📬 SUSCRIPCIÓN - Usuario: {}, Destino: {}", username, destination);
    }

    private String getUsername(StompHeaderAccessor headerAccessor) {
        return headerAccessor.getUser() != null ?
                headerAccessor.getUser().getName() : "anonymous";
    }
}