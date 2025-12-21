package ucb.edu.bo.sumajflow.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ucb.edu.bo.sumajflow.entity.InvitacionTransportista;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository para invitaciones de transportistas con QR
 * ACTUALIZADO: Sin referencia directa a cooperativa
 */
@Repository
public interface InvitacionTransportistaRepository extends JpaRepository<InvitacionTransportista, Integer> {

    /**
     * Buscar invitación por token
     */
    Optional<InvitacionTransportista> findByTokenInvitacion(String token);

    /**
     * ✅ ACTUALIZADO: Buscar invitación activa por número de celular y cooperativa
     * Ahora usa la tabla intermedia invitacion_cooperativa
     */
    @Query("SELECT i FROM InvitacionTransportista i " +
            "JOIN i.invitacionesCooperativa ic " +
            "WHERE ic.cooperativa.id = :cooperativaId " +
            "AND i.numeroCelular = :celular " +
            "AND i.estado IN ('pendiente_qr', 'codigo_enviado', 'verificado') " +
            "AND i.fechaExpiracion > :ahora")
    Optional<InvitacionTransportista> findInvitacionActivaPorCelularYCooperativa(
            @Param("cooperativaId") Integer cooperativaId,
            @Param("celular") String celular,
            @Param("ahora") LocalDateTime ahora
    );

    /**
     * ✅ ACTUALIZADO: Listar invitaciones de una cooperativa con filtros
     * Ahora usa JOIN con invitacion_cooperativa
     */
    @Query(value = "SELECT DISTINCT i.* FROM invitacion_transportista i " +
            "INNER JOIN invitacion_cooperativa ic ON i.id = ic.invitacion_transportista_id " +
            "WHERE ic.cooperativa_id = :cooperativaId " +
            "AND (:estado = '' OR :estado IS NULL OR i.estado = :estado) " +
            "AND (:busqueda = '' OR :busqueda IS NULL OR " +
            "     LOWER(i.primer_nombre) LIKE LOWER(CONCAT('%', :busqueda, '%')) OR " +
            "     LOWER(i.primer_apellido) LIKE LOWER(CONCAT('%', :busqueda, '%')) OR " +
            "     i.numero_celular LIKE CONCAT('%', :busqueda, '%')) " +
            "ORDER BY i.created_at DESC",
            countQuery = "SELECT COUNT(DISTINCT i.id) FROM invitacion_transportista i " +
                    "INNER JOIN invitacion_cooperativa ic ON i.id = ic.invitacion_transportista_id " +
                    "WHERE ic.cooperativa_id = :cooperativaId " +
                    "AND (:estado = '' OR :estado IS NULL OR i.estado = :estado) " +
                    "AND (:busqueda = '' OR :busqueda IS NULL OR " +
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
     * ✅ ACTUALIZADO: Contar invitaciones pendientes de una cooperativa
     */
    @Query("SELECT COUNT(DISTINCT i) FROM InvitacionTransportista i " +
            "JOIN i.invitacionesCooperativa ic " +
            "WHERE ic.cooperativa.id = :cooperativaId " +
            "AND i.estado IN ('pendiente_qr', 'codigo_enviado')")
    Long countPendientesByCooperativa(@Param("cooperativaId") Integer cooperativaId);

    /**
     * Buscar invitaciones expiradas
     */
    @Query("SELECT i FROM InvitacionTransportista i " +
            "WHERE i.estado IN ('pendiente_qr', 'codigo_enviado', 'verificado') " +
            "AND i.fechaExpiracion < :ahora")
    List<InvitacionTransportista> findInvitacionesExpiradas(@Param("ahora") LocalDateTime ahora);

    /**
     * ✅ ACTUALIZADO: Verificar si existe invitación reciente para un celular
     */
    @Query("SELECT CASE WHEN COUNT(i) > 0 THEN true ELSE false END " +
            "FROM InvitacionTransportista i " +
            "JOIN i.invitacionesCooperativa ic " +
            "WHERE i.numeroCelular = :celular " +
            "AND ic.cooperativa.id = :cooperativaId " +
            "AND i.fechaEnvio > :hace24horas")
    boolean existeInvitacionReciente(
            @Param("cooperativaId") Integer cooperativaId,
            @Param("celular") String celular,
            @Param("hace24horas") LocalDateTime hace24horas
    );

    /**
     * Buscar por código de verificación (para debugging)
     */
    Optional<InvitacionTransportista> findByCodigoVerificacion(String codigo);

    /**
     * ✅ ACTUALIZADO: Contar invitaciones por estado para estadísticas
     */
    @Query("SELECT i.estado, COUNT(DISTINCT i) FROM InvitacionTransportista i " +
            "JOIN i.invitacionesCooperativa ic " +
            "WHERE ic.cooperativa.id = :cooperativaId " +
            "GROUP BY i.estado")
    List<Object[]> countByEstado(@Param("cooperativaId") Integer cooperativaId);
}