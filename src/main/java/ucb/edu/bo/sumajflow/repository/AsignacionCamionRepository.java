package ucb.edu.bo.sumajflow.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ucb.edu.bo.sumajflow.entity.AsignacionCamion;
import ucb.edu.bo.sumajflow.entity.Lotes;
import ucb.edu.bo.sumajflow.entity.Transportista;

import java.math.BigDecimal;
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
  // Obtener asignaciones por estado
  @Query("SELECT a FROM AsignacionCamion a WHERE a.estado = :estado ORDER BY a.fechaAsignacion DESC")
  List<AsignacionCamion> findByEstado(@Param("estado") String estado);

  // Verificar si todas las asignaciones de un lote están en un estado específico
  @Query("SELECT CASE WHEN COUNT(a) = " +
          "(SELECT COUNT(a2) FROM AsignacionCamion a2 WHERE a2.lotesId = :lote) " +
          "THEN true ELSE false END " +
          "FROM AsignacionCamion a " +
          "WHERE a.lotesId = :lote AND a.estado = :estado")
  boolean todasEnEstado(@Param("lote") Lotes lote, @Param("estado") String estado);

  // Obtener peso total real de un lote (suma de pesajes netos)
  @Query("SELECT COALESCE(SUM(p.pesoNeto), 0) " +
          "FROM Pesajes p " +
          "WHERE p.asignacionCamionId IN " +
          "(SELECT a FROM AsignacionCamion a WHERE a.lotesId = :lote)")
  BigDecimal calcularPesoTotalReal(@Param("lote") Lotes lote);

  /**
   * Buscar asignaciones de un transportista excluyendo ciertos estados
   */
  @Query("SELECT a FROM AsignacionCamion a WHERE a.transportistaId = :transportista AND a.estado NOT IN :estados ORDER BY a.fechaAsignacion DESC")
  List<AsignacionCamion> findByTransportistaIdAndEstadoNotIn(
          @Param("transportista") Transportista transportista,
          @Param("estados") List<String> estados
  );

  /**
   * Buscar asignaciones de un transportista con un estado específico
   */
  List<AsignacionCamion> findByTransportistaIdAndEstado(Transportista transportista, String estado);

  /**
   * Buscar todas las asignaciones de un transportista
   */
  List<AsignacionCamion> findByTransportistaId(Transportista transportista);


}