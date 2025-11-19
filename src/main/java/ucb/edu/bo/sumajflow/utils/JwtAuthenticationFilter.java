package ucb.edu.bo.sumajflow.utils;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    public JwtAuthenticationFilter(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }
    // Esta clase filtra cada solicitud HTTP para validar el token JWT
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        try {
            String token = extractToken(request);

            if (token != null && jwtUtil.validateToken(token) && jwtUtil.isAccessToken(token)) {
                String correo = jwtUtil.extractCorreo(token);
                String rol = jwtUtil.extractRol(token);
                Integer usuarioId = jwtUtil.extractUsuarioId(token);

                // Crear autenticación
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                correo,
                                null,
                                Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + rol.toUpperCase()))
                        );

                // Agregar detalles adicionales (ID del usuario)
                authentication.setDetails(usuarioId);

                // Establecer en el contexto de seguridad
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        } catch (Exception e) {
            logger.error("Error al procesar el token JWT: " + e.getMessage());
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Extrae el token del header Authorization
     */
    private String extractToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");

        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }

        return null;
    }
}



































