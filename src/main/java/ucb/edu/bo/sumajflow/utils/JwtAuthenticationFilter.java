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

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            String token = extractToken(request);

            if (token != null && jwtUtil.validateToken(token) && jwtUtil.isAccessToken(token)) {
                String correo = jwtUtil.extractCorreo(token);
                String rol = jwtUtil.extractRol(token);
                Integer usuarioId = jwtUtil.extractUsuarioId(token);
                Boolean aprobado = jwtUtil.extractAprobado(token);

                //  Si es socio no aprobad
                if ("socio".equalsIgnoreCase(rol) && !Boolean.TRUE.equals(aprobado)) {
                    String uri = request.getRequestURI();
                    if (!uri.startsWith("/auth/") &&
                            !uri.startsWith("/public/") &&
                            !uri.equals("/socio/estado") &&
                            !uri.equals("/socio/verificar-aprobacion") &&
                            !uri.startsWith("/socio/perfil")) {
                        response.setContentType("application/json");
                        response.setCharacterEncoding("UTF-8");
                        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                        response.getWriter().write(
                                "{\"success\":false,\"message\":\"Tu solicitud está pendiente de aprobación\",\"codigo\":\"SOCIO_PENDIENTE\"}"
                        );
                        return;
                    }
                }

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