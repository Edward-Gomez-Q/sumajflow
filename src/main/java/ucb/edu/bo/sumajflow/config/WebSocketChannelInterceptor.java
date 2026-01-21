package ucb.edu.bo.sumajflow.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;
import ucb.edu.bo.sumajflow.utils.JwtUtil;

import java.util.Collections;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketChannelInterceptor implements ChannelInterceptor {

    private final JwtUtil jwtUtil;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
            String authHeader = accessor.getFirstNativeHeader("Authorization");

            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);

                try {
                    if (jwtUtil.validateToken(token)) {
                        String correo = jwtUtil.extractCorreo(token);
                        String rol = jwtUtil.extractRol(token);
                        Integer usuarioId = jwtUtil.extractUsuarioId(token);

                        UsernamePasswordAuthenticationToken authentication =
                                new UsernamePasswordAuthenticationToken(
                                        correo,
                                        null,
                                        Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + rol.toUpperCase()))
                                );

                        authentication.setDetails(usuarioId);
                        accessor.setUser(authentication);

                        log.info("✅ WebSocket autenticado - Usuario ID: {}, Rol: {}", usuarioId, rol);
                    } else {
                        log.warn("⚠️ Token JWT inválido en WebSocket");
                    }
                } catch (Exception e) {
                    log.error("❌ Error validando token JWT en WebSocket: {}", e.getMessage());
                }
            }
        }

        return message;
    }
}