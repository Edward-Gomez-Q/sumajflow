package ucb.edu.bo.sumajflow.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ucb.edu.bo.sumajflow.entity.Cooperativa;
import ucb.edu.bo.sumajflow.entity.InvitacionTransportista;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository para invitaciones de transportistas con QR
 */
@Repository
public interface InvitacionTransportistaRepository extends JpaRepository<InvitacionTransportista, Integer> {

    /**
     * Buscar invitación por token
     */
    Optional<InvitacionTransportista> findByTokenInvitacion(String token);

    /**
     * Buscar invitación activa por número de celular y cooperativa
     */
    @Query("SELECT i FROM InvitacionTransportista i " +
            "WHERE i.cooperativaId = :cooperativa " +
            "AND i.numeroCelular = :celular " +
            "AND i.estado IN ('pendiente_qr', 'codigo_enviado', 'verificado') " +
            "AND i.fechaExpiracion > :ahora")
    Optional<InvitacionTransportista> findInvitacionActivaPorCelular(
            @Param("cooperativa") Cooperativa cooperativa,
            @Param("celular") String celular,
            @Param("ahora") LocalDateTime ahora
    );

    /**
     * Listar invitaciones de una cooperativa con filtros
     */
    @Query(value = "SELECT * FROM invitacion_transportista i " +
            "WHERE i.cooperativa_id = :cooperativaId " +
            "AND (:estado IS NULL OR i.estado = :estado) " +
            "AND (:busqueda IS NULL OR " +
            "     LOWER(i.primer_nombre) LIKE LOWER(CONCAT('%', :busqueda, '%')) OR " +
            "     LOWER(i.primer_apellido) LIKE LOWER(CONCAT('%', :busqueda, '%')) OR " +
            "     i.numero_celular LIKE CONCAT('%', :busqueda, '%')) " +
            "ORDER BY i.created_at DESC",
            countQuery = "SELECT COUNT(*) FROM invitacion_transportista i " +
                    "WHERE i.cooperativa_id = :cooperativaId " +
                    "AND (:estado IS NULL OR i.estado = :estado) " +
                    "AND (:busqueda IS NULL OR " +
                    "     LOWER(i.primer_nombre) LIKE LOWER(CONCAT('%', :busqueda, '%')) OR " +
                    "     LOWER(i.primer_apellido) LIKE LOWER(CONCAT('%', :busqueda, '%')) OR " +
                    "     i.numero_celular LIKE CONCAT('%', :busqueda, '%'))",
            nativeQuery = true)
    Page<InvitacionTransportista> findByCooperativaWithFilters(
            @Param("cooperativaId") Integer cooperativaId,
            @Param("estado") String estado,
            @Param("busqueda") String busqueda,
            Pageable pageable
    );

    /**
     * Contar invitaciones pendientes de una cooperativa
     */
    @Query("SELECT COUNT(i) FROM InvitacionTransportista i " +
            "WHERE i.cooperativaId = :cooperativa " +
            "AND i.estado IN ('pendiente_qr', 'codigo_enviado')")
    Long countPendientesByCooperativa(@Param("cooperativa") Cooperativa cooperativa);

    /**
     * Buscar invitaciones expiradas
     */
    @Query("SELECT i FROM InvitacionTransportista i " +
            "WHERE i.estado IN ('pendiente_qr', 'codigo_enviado', 'verificado') " +
            "AND i.fechaExpiracion < :ahora")
    List<InvitacionTransportista> findInvitacionesExpiradas(@Param("ahora") LocalDateTime ahora);

    /**
     * Verificar si existe invitación reciente para un celular
     */
    @Query("SELECT CASE WHEN COUNT(i) > 0 THEN true ELSE false END " +
            "FROM InvitacionTransportista i " +
            "WHERE i.numeroCelular = :celular " +
            "AND i.cooperativaId = :cooperativa " +
            "AND i.fechaEnvio > :hace24horas")
    boolean existeInvitacionReciente(
            @Param("cooperativa") Cooperativa cooperativa,
            @Param("celular") String celular,
            @Param("hace24horas") LocalDateTime hace24horas
    );

    /**
     * Buscar por código de verificación (para debugging)
     */
    Optional<InvitacionTransportista> findByCodigoVerificacion(String codigo);

    /**
     * Contar invitaciones por estado para estadísticas
     */
    @Query("SELECT i.estado, COUNT(i) FROM InvitacionTransportista i " +
            "WHERE i.cooperativaId = :cooperativa " +
            "GROUP BY i.estado")
    List<Object[]> countByEstado(@Param("cooperativa") Cooperativa cooperativa);
}