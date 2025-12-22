package ucb.edu.bo.sumajflow.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ucb.edu.bo.sumajflow.entity.AsignacionCamion;
import ucb.edu.bo.sumajflow.entity.Lotes;

import java.util.List;

public interface AsignacionCamionRepository extends JpaRepository<AsignacionCamion, Integer> {

  // Obtener todas las asignaciones de un lote
  List<AsignacionCamion> findByLotesId(Lotes lote);

  // Contar asignaciones de un lote
  @Query("SELECT COUNT(a) FROM AsignacionCamion a WHERE a.lotesId = :lote")
  long countByLote(@Param("lote") Lotes lote);

  // Verificar si un transportista ya está asignado a un lote
  @Query("SELECT CASE WHEN COUNT(a) > 0 THEN true ELSE false END " +
          "FROM AsignacionCamion a " +
          "WHERE a.lotesId = :lote AND a.transportistaId.id = :transportistaId")
  boolean existsByLoteAndTransportista(
          @Param("lote") Lotes lote,
          @Param("transportistaId") Integer transportistaId
  );

  // Obtener asignaciones por estado
  @Query("SELECT a FROM AsignacionCamion a WHERE a.lotesId = :lote AND a.estado = :estado")
  List<AsignacionCamion> findByLoteAndEstado(
          @Param("lote") Lotes lote,
          @Param("estado") String estado
  );

  // Contar asignaciones por estado
  @Query("SELECT COUNT(a) FROM AsignacionCamion a WHERE a.lotesId = :lote AND a.estado = :estado")
  long countByLoteAndEstado(
          @Param("lote") Lotes lote,
          @Param("estado") String estado
  );
}