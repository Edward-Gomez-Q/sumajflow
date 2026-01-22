package ucb.edu.bo.sumajflow.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.web.socket.config.annotation.*;
import org.springframework.web.socket.server.standard.ServletServerContainerFactoryBean;

@Configuration
@EnableWebSocketMessageBroker
@EnableScheduling
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final WebSocketChannelInterceptor webSocketChannelInterceptor;

    @Bean
    public TaskScheduler heartBeatScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(2); // Aumentado para manejar mejor múltiples conexiones
        scheduler.setThreadNamePrefix("wss-heartbeat-");
        scheduler.setAwaitTerminationSeconds(60);
        scheduler.setWaitForTasksToCompleteOnShutdown(true);
        scheduler.initialize();
        return scheduler;
    }

    /**
     * Configuración del contenedor de WebSocket para evitar errores de estado
     */
    @Bean
    public ServletServerContainerFactoryBean createWebSocketContainer() {
        ServletServerContainerFactoryBean container = new ServletServerContainerFactoryBean();
        container.setMaxTextMessageBufferSize(512 * 1024); // 512KB
        container.setMaxBinaryMessageBufferSize(512 * 1024);
        container.setMaxSessionIdleTimeout(30000L); // 30 segundos
        container.setAsyncSendTimeout(5000L); // 5 segundos para envíos asíncronos
        return container;
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic", "/queue")
                .setHeartbeatValue(new long[]{20000, 20000}) // Aumentado a 20s para reducir colisiones
                .setTaskScheduler(heartBeatScheduler());

        config.setApplicationDestinationPrefixes("/app");
        config.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")
                .withSockJS()
                .setClientLibraryUrl("https://cdn.jsdelivr.net/npm/sockjs-client@1/dist/sockjs.min.js")
                .setDisconnectDelay(5000L) // Delay antes de considerar desconectado
                .setHeartbeatTime(20000L) // Consistente con el broker
                .setSessionCookieNeeded(false)
                .setStreamBytesLimit(512 * 1024)
                .setHttpMessageCacheSize(1000);

        registry.addEndpoint("/ws-native")
                .setAllowedOriginPatterns("*")
                .setHandshakeHandler(new UserHandshakeHandler());
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration
                .interceptors(webSocketChannelInterceptor)
                .taskExecutor()
                .corePoolSize(4)
                .maxPoolSize(8)
                .keepAliveSeconds(60);
    }

    @Override
    public void configureClientOutboundChannel(ChannelRegistration registration) {
        registration
                .taskExecutor()
                .corePoolSize(4)
                .maxPoolSize(8)
                .keepAliveSeconds(60);
    }

    @Override
    public void configureWebSocketTransport(WebSocketTransportRegistration registry) {
        registry
                .setMessageSizeLimit(256 * 1024) // 256KB (aumentado)
                .setSendBufferSizeLimit(1024 * 1024) // 1MB (aumentado)
                .setSendTimeLimit(30000) // 30 segundos (aumentado)
                .setTimeToFirstMessage(60000); // 60 segundos (aumentado)
    }
}