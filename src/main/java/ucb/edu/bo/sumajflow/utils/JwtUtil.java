package ucb.edu.bo.sumajflow.utils;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Utilidad para generar y validar tokens JWT
 */
@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private Long expiration;

    @Value("${jwt.refresh-expiration}")
    private Long refreshExpiration;

    @Value("${jwt.issuer}")
    private String issuer;

    /**
     * Genera una clave secreta a partir del secret string
     */
    private SecretKey getSigningKey() {
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * Genera un token de acceso JWT
     * @param usuarioId ID del usuario
     * @param correo Correo del usuario
     * @param rol Rol del usuario
     * @return Token JWT
     */
    public String generateAccessToken(Integer usuarioId, String correo, String rol) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("id", usuarioId);
        claims.put("correo", correo);
        claims.put("rol", rol);
        claims.put("type", "access");

        return Jwts.builder()
                .claims(claims)
                .subject(correo)
                .issuer(issuer)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * Genera un token de refresco JWT
     * @param usuarioId ID del usuario
     * @param correo Correo del usuario
     * @return Refresh token JWT
     */
    public String generateRefreshToken(Integer usuarioId, String correo) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("id", usuarioId);
        claims.put("correo", correo);
        claims.put("type", "refresh");

        return Jwts.builder()
                .claims(claims)
                .subject(correo)
                .issuer(issuer)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + refreshExpiration))
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * Extrae el correo del token
     */
    public String extractCorreo(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    /**
     * Extrae el ID del usuario del token
     */
    public Integer extractUsuarioId(String token) {
        return extractClaim(token, claims -> claims.get("id", Integer.class));
    }

    /**
     * Extrae el rol del usuario del token
     */
    public String extractRol(String token) {
        return extractClaim(token, claims -> claims.get("rol", String.class));
    }

    /**
     * Extrae el tipo de token (access o refresh)
     */
    public String extractTokenType(String token) {
        return extractClaim(token, claims -> claims.get("type", String.class));
    }

    /**
     * Extrae la fecha de expiración del token
     */
    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    /**
     * Extrae un claim específico del token
     */
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    /**
     * Extrae todos los claims del token
     */
    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * Verifica si el token ha expirado
     */
    public Boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    /**
     * Valida el token
     * @param token Token JWT
     * @param correo Correo del usuario para verificar
     * @return true si el token es válido
     */
    public Boolean validateToken(String token, String correo) {
        try {
            final String extractedCorreo = extractCorreo(token);
            return (extractedCorreo.equals(correo) && !isTokenExpired(token));
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * Valida el token sin verificar el correo (para el filtro)
     */
    public Boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token);
            return !isTokenExpired(token);
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * Verifica si es un token de acceso
     */
    public Boolean isAccessToken(String token) {
        String type = extractTokenType(token);
        return "access".equals(type);
    }

    /**
     * Verifica si es un refresh token
     */
    public Boolean isRefreshToken(String token) {
        String type = extractTokenType(token);
        return "refresh".equals(type);
    }
}