package ucb.edu.bo.sumajflow.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ucb.edu.bo.sumajflow.entity.Cooperativa;
import ucb.edu.bo.sumajflow.entity.CooperativaSocio;
import ucb.edu.bo.sumajflow.entity.Socio;
import java.util.List;
import java.util.Optional;

/**
 * Repositorio para la entidad CooperativaSocio
 */
public interface CooperativaSocioRepository extends JpaRepository<CooperativaSocio, Integer> {

    Optional<CooperativaSocio> findBySocioId(Socio socioId);

    /**
     * Busca todos los socios de una cooperativa
     */
    @Query("SELECT cs FROM CooperativaSocio cs " +
            "LEFT JOIN FETCH cs.socioId s " +
            "LEFT JOIN FETCH s.usuariosId u " +
            "WHERE cs.cooperativaId = :cooperativa")
    Page<CooperativaSocio> findByCooperativaId(
            @Param("cooperativa") Cooperativa cooperativa,
            Pageable pageable
    );

    List<CooperativaSocio> findByCooperativaId(Cooperativa cooperativa);
    List<CooperativaSocio> findByCooperativaIdAndEstado(Cooperativa cooperativa, String estado);

    Optional<CooperativaSocio> findByCooperativaIdAndSocioId(Cooperativa cooperativaId, Socio socioId);
    /**
     * Busca socios por cooperativa y estado
     */
    @Query("SELECT cs FROM CooperativaSocio cs " +
            "LEFT JOIN FETCH cs.socioId s " +
            "LEFT JOIN FETCH s.usuariosId u " +
            "WHERE cs.cooperativaId = :cooperativa " +
            "AND cs.estado = :estado")
    Page<CooperativaSocio> findByCooperativaIdAndEstado(
            @Param("cooperativa") Cooperativa cooperativa,
            @Param("estado") String estado,
            Pageable pageable
    );

    /**
     * Busca socios por cooperativa con búsqueda en nombre, apellido o CI
     */
    @Query("SELECT DISTINCT cs FROM CooperativaSocio cs " +
            "JOIN FETCH cs.socioId s " +
            "JOIN FETCH s.usuariosId u " +
            "JOIN Persona p ON p.usuariosId = u " +
            "WHERE cs.cooperativaId = :cooperativa " +
            "AND (LOWER(p.nombres) LIKE %:busqueda% " +
            "OR LOWER(p.primerApellido) LIKE %:busqueda% " +
            "OR LOWER(p.segundoApellido) LIKE %:busqueda% " +
            "OR LOWER(p.ci) LIKE %:busqueda%)")
    Page<CooperativaSocio> findByCooperativaAndBusqueda(
            @Param("cooperativa") Cooperativa cooperativa,
            @Param("busqueda") String busqueda,
            Pageable pageable
    );

    /**
     * Busca socios por cooperativa, estado y búsqueda
     */
    @Query("SELECT DISTINCT cs FROM CooperativaSocio cs " +
            "JOIN FETCH cs.socioId s " +
            "JOIN FETCH s.usuariosId u " +
            "JOIN Persona p ON p.usuariosId = u " +
            "WHERE cs.cooperativaId = :cooperativa " +
            "AND cs.estado = :estado " +
            "AND (LOWER(p.nombres) LIKE %:busqueda% " +
            "OR LOWER(p.primerApellido) LIKE %:busqueda% " +
            "OR LOWER(p.segundoApellido) LIKE %:busqueda% " +
            "OR LOWER(p.ci) LIKE %:busqueda%)")
    Page<CooperativaSocio> findByCooperativaAndEstadoAndBusqueda(
            @Param("cooperativa") Cooperativa cooperativa,
            @Param("estado") String estado,
            @Param("busqueda") String busqueda,
            Pageable pageable
    );

    /**
     * Cuenta socios por cooperativa y estado
     */
    Long countByCooperativaIdAndEstado(Cooperativa cooperativa, String estado);

    /**
     * Encuentra la relación cooperativa-socio donde el socio esté en un estado específico
     */
    @Query("SELECT cs FROM CooperativaSocio cs WHERE cs.socioId = :socio AND cs.estado = :estado")
    Optional<CooperativaSocio> findBySocioIdAndEstado(
            @Param("socio") Socio socio,
            @Param("estado") String estado
    );

    /**
     * Verifica si un socio está aprobado en una cooperativa específica
     */
    @Query("SELECT CASE WHEN COUNT(cs) > 0 THEN true ELSE false END " +
            "FROM CooperativaSocio cs WHERE cs.socioId = :socio " +
            "AND cs.cooperativaId = :cooperativa AND cs.estado = 'aprobado'")
    boolean isSocioAprobadoEnCooperativa(
            @Param("socio") Socio socio,
            @Param("cooperativa") Cooperativa cooperativa
    );

    /**
     * Obtiene la cooperativa donde un socio está aprobado
     */
    @Query("SELECT cs.cooperativaId FROM CooperativaSocio cs " +
            "WHERE cs.socioId = :socio AND cs.estado = 'aprobado'")
    Optional<Cooperativa> findCooperativaByApprovedSocio(@Param("socio") Socio socio);

    /**
     * Obtiene todos los socios aprobados de una cooperativa
     */
    @Query("SELECT cs FROM CooperativaSocio cs WHERE cs.cooperativaId = :cooperativa AND cs.estado = 'aprobado'")
    List<CooperativaSocio> findApprovedSociosByCooperativa(@Param("cooperativa") Cooperativa cooperativa);

    /**
     * Obtiene todos los socios pendientes de aprobación de una cooperativa
     */
    @Query("SELECT cs FROM CooperativaSocio cs WHERE cs.cooperativaId = :cooperativa AND cs.estado = 'pendiente'")
    List<CooperativaSocio> findPendingSociosByCooperativa(@Param("cooperativa") Cooperativa cooperativa);

    /**
     * Cuenta socios aprobados en una cooperativa
     */
    @Query("SELECT COUNT(cs) FROM CooperativaSocio cs WHERE cs.cooperativaId = :cooperativa AND cs.estado = 'aprobado'")
    long countApprovedSociosByCooperativa(@Param("cooperativa") Cooperativa cooperativa);

    /**
     * Encuentra todas las relaciones de un socio (para ver su historial)
     */
    @Query("SELECT cs FROM CooperativaSocio cs WHERE cs.socioId = :socio ORDER BY cs.fechaAfiliacion DESC")
    List<CooperativaSocio> findAllBySocio(@Param("socio") Socio socio);

    /**
     * Buscar Cooperativa por Socio
     */
    @Query("SELECT cs.cooperativaId FROM CooperativaSocio cs WHERE cs.socioId = :socio")
    Optional<Cooperativa> findCooperativaBySocio(@Param("socio") Socio socio);
}