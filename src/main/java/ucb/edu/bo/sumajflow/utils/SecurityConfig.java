package ucb.edu.bo.sumajflow.utils;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

/**
 * Configuración de seguridad de Spring Security con JWT
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter,
                          JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.jwtAuthenticationEntryPoint = jwtAuthenticationEntryPoint;
    }

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowCredentials(true);
        configuration.setAllowedOrigins(Arrays.asList(
                "http://localhost:5173",
                "http://localhost:5174",
                "http://localhost:3000",
                "http://127.0.0.1:5173",
                "http://localhost:8081"
        ));
        configuration.setAllowedHeaders(Arrays.asList(
                "Origin",
                "Content-Type",
                "Accept",
                "Authorization",
                "X-Requested-With",
                "Access-Control-Request-Method",
                "Access-Control-Request-Headers"
        ));
        configuration.setExposedHeaders(Arrays.asList(
                "Access-Control-Allow-Origin",
                "Access-Control-Allow-Credentials",
                "Authorization"
        ));
        configuration.setAllowedMethods(Arrays.asList(
                "GET",
                "POST",
                "PUT",
                "PATCH",
                "DELETE",
                "OPTIONS"
        ));
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // Habilitar CORS con la configuración definida arriba
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                // Deshabilitar CSRF (no es necesario con JWT)
                .csrf(csrf -> csrf.disable())

                // Configuración de sesiones (stateless con JWT)
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                // Manejo de excepciones de autenticación
                .exceptionHandling(exception ->
                        exception.authenticationEntryPoint(jwtAuthenticationEntryPoint)
                )

                // Configuración de autorización de endpoints
                .authorizeHttpRequests(auth -> auth
                        // =============== ENDPOINTS PÚBLICOS ===============
                        // Autenticación
                        .requestMatchers("/auth/login", "/auth/register", "/auth/refresh", "/auth/health")
                        .permitAll()

                        // Swagger/OpenAPI UI
                        .requestMatchers(
                                "/v3/api-docs/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/swagger-resources/**",
                                "/webjars/**"
                        ).permitAll()

                        // Catálogos públicos
                        .requestMatchers("/public/**")
                        .permitAll()

                        // Archivos (MinIO)
                        .requestMatchers("/files/**")
                        .permitAll()

                        // Actuator health
                        .requestMatchers("/actuator/health")
                        .permitAll()

                        // =============== ENDPOINTS POR ROL ===============
                        // Cooperativa
                        .requestMatchers("/cooperativa/**")
                        .hasRole("COOPERATIVA")

                        // Socio
                        .requestMatchers("/socio/**")
                        .hasRole("SOCIO")

                        // Ingenio
                        .requestMatchers("/ingenio/**")
                        .hasRole("INGENIO")

                        // Comercializadora
                        .requestMatchers("/comercializadora/**")
                        .hasRole("COMERCIALIZADORA")

                        // Transportista
                        .requestMatchers("/transportista/**")
                        .hasRole("TRANSPORTISTA")

                        // Administrador (acceso a todo)
                        .requestMatchers("/admin/**")
                        .hasRole("ADMINISTRADOR")

                        // WebSocket endpoint
                        .requestMatchers("/ws/**").permitAll()

                        // =============== RESTO DE ENDPOINTS ===============
                        // Cualquier otra petición requiere autenticación
                        .anyRequest()
                        .authenticated()
                )

                // Agregar filtro JWT antes del filtro de autenticación de Spring
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}