package ucb.edu.bo.sumajflow.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.Arrays;

/**
 * Configuración CORS (Cross-Origin Resource Sharing)
 * Permite que el frontend (Vue en puerto 5173) se comunique con el backend (Spring Boot en puerto 8080)
 */
@Configuration
public class CorsConfig {

    @Bean
    public CorsFilter corsFilter() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        CorsConfiguration config = new CorsConfiguration();

        // Permitir credenciales (cookies, authorization headers, etc.)
        config.setAllowCredentials(true);

        // Orígenes permitidos (frontend)
        config.setAllowedOrigins(Arrays.asList(
                "http://localhost:5173",        // Vite dev server (Vue)
                "http://localhost:5174",        // Vite alternativo
                "http://localhost:3000",        // React/Next.js (si lo usas en el futuro)
                "http://127.0.0.1:5173",        // Variante con 127.0.0.1
                "http://localhost:8081"         // Por si cambias el puerto del frontend
        ));

        // Headers permitidos
        config.setAllowedHeaders(Arrays.asList(
                "Origin",
                "Content-Type",
                "Accept",
                "Authorization",
                "X-Requested-With",
                "Access-Control-Request-Method",
                "Access-Control-Request-Headers"
        ));

        // Headers expuestos (que el cliente puede leer)
        config.setExposedHeaders(Arrays.asList(
                "Access-Control-Allow-Origin",
                "Access-Control-Allow-Credentials",
                "Authorization"
        ));

        // Métodos HTTP permitidos
        config.setAllowedMethods(Arrays.asList(
                "GET",
                "POST",
                "PUT",
                "PATCH",
                "DELETE",
                "OPTIONS"
        ));

        // Tiempo de cache para preflight requests (en segundos)
        config.setMaxAge(3600L);

        // Aplicar configuración a todos los endpoints
        source.registerCorsConfiguration("/**", config);

        return new CorsFilter(source);
    }
}