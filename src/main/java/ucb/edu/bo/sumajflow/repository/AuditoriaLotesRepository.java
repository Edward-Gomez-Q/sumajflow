package ucb.edu.bo.sumajflow.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ucb.edu.bo.sumajflow.entity.AuditoriaLotes;

import java.util.List;

public interface AuditoriaLotesRepository extends JpaRepository<AuditoriaLotes, Integer> {

    // Obtener auditoría de un lote ordenada por fecha
    @Query("SELECT a FROM AuditoriaLotes a WHERE a.loteId = :loteId ORDER BY a.fechaRegistro DESC")
    List<AuditoriaLotes> findByLoteIdOrderByFechaDesc(@Param("loteId") Integer loteId);

    // Obtener últimas N acciones de un lote
    @Query(value = "SELECT * FROM auditoria_lotes WHERE lote_id = :loteId " +
            "ORDER BY fecha_registro DESC LIMIT :limit",
            nativeQuery = true)
    List<AuditoriaLotes> findTopNByLoteId(
            @Param("loteId") Integer loteId,
            @Param("limit") int limit
    );
}