package ucb.edu.bo.sumajflow.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.*;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class WebSocketEventListener {

    private final Map<String, String> userSessions = new ConcurrentHashMap<>();

    @EventListener
    public void handleWebSocketConnectListener(SessionConnectedEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = headerAccessor.getSessionId();
        String username = getUsername(headerAccessor);

        String oldSession = userSessions.put(username, sessionId);

        if (oldSession != null && !oldSession.equals(sessionId)) {
            log.warn("🔄 RECONEXIÓN detectada - Usuario: {}, Nueva sesión: {}", username, sessionId);
        } else {
            log.info("✅ WebSocket CONECTADO - Usuario: {}, Sesión: {}", username, sessionId);
        }
    }

    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = headerAccessor.getSessionId();
        String username = getUsername(headerAccessor);

        String activeSession = userSessions.get(username);
        if (sessionId.equals(activeSession)) {
            userSessions.remove(username);
            log.info("🔌 WebSocket DESCONECTADO - Usuario: {}", username);
        }
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