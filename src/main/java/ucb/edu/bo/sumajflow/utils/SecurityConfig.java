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
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
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