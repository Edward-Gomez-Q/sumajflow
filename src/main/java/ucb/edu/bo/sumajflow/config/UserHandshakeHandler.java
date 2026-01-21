package ucb.edu.bo.sumajflow.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;

import java.security.Principal;
import java.util.List;
import java.util.Map;

@Slf4j
public class UserHandshakeHandler extends DefaultHandshakeHandler {

    @Override
    protected Principal determineUser(
            ServerHttpRequest request,
            WebSocketHandler wsHandler,
            Map<String, Object> attributes) {

        // Extraer token del header Authorization
        List<String> authHeaders = request.getHeaders().get("Authorization");

        if (authHeaders != null && !authHeaders.isEmpty()) {
            String authHeader = authHeaders.get(0);

            if (authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                String userId = extractUserIdFromToken(token);

                if (userId != null) {
                    log.info("🔐 WebSocket Handshake - Usuario ID: {}", userId);
                    return new StompPrincipal(userId);
                }
            }
        }

        log.warn("⚠️ WebSocket Handshake sin autenticación válida");
        return null;
    }

    private String extractUserIdFromToken(String token) {
        try {
            // Decodificar payload del JWT (sin verificar firma para handshake)
            String[] parts = token.split("\\.");
            if (parts.length >= 2) {
                String payload = new String(
                        java.util.Base64.getUrlDecoder().decode(parts[1])
                );

                // Parsear JSON manualmente para extraer usuarioId
                if (payload.contains("\"usuarioId\"")) {
                    int start = payload.indexOf("\"usuarioId\":") + 12;
                    int end = payload.indexOf(",", start);
                    if (end == -1) end = payload.indexOf("}", start);
                    String userIdStr = payload.substring(start, end).trim();
                    return userIdStr;
                }
            }
        } catch (Exception e) {
            log.error("Error extrayendo usuarioId del token: {}", e.getMessage());
        }
        return null;
    }

    private static class StompPrincipal implements Principal {
        private final String name;

        StompPrincipal(String name) {
            this.name = name;
        }

        @Override
        public String getName() {
            return name;
        }
    }
}