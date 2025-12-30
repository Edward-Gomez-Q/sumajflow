package ucb.edu.bo.sumajflow.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ucb.edu.bo.sumajflow.entity.Lotes;
import ucb.edu.bo.sumajflow.entity.Minas;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface LotesRepository extends JpaRepository<Lotes, Integer> {

  List<Lotes> findByMinasId(Minas mina);

  @Query("SELECT l FROM Lotes l WHERE l.id = :id AND l.estado != 'eliminado'")
  Optional<Lotes> findByIdAndNotDeleted(@Param("id") Integer id);

  @Query("SELECT COUNT(l) FROM Lotes l WHERE l.minasId = :mina AND l.estado NOT IN ('completado', 'rechazado', 'cancelado')")
  long countLotesActivosByMina(@Param("mina") Minas mina);

  @Query("SELECT CASE WHEN COUNT(l) > 0 THEN true ELSE false END FROM Lotes l WHERE l.minasId = :mina AND l.estado NOT IN ('completado', 'rechazado', 'cancelado')")
  boolean existsLotesActivosByMina(@Param("mina") Minas mina);

  List<Lotes> findByMinasIdIn(List<Minas> minas);

  // Query NATIVA con CAST() función
  @Query(value = "SELECT * FROM lotes " +
          "WHERE minas_id = ANY(CAST(:minaIds AS INTEGER[])) " +
          "AND (CAST(:estado AS TEXT) IS NULL OR estado = CAST(:estado AS TEXT)) " +
          "AND (CAST(:tipoOperacion AS TEXT) IS NULL OR tipo_operacion = CAST(:tipoOperacion AS TEXT)) " +
          "AND (CAST(:tipoMineral AS TEXT) IS NULL OR tipo_mineral = CAST(:tipoMineral AS TEXT)) " +
          "AND (CAST(:fechaDesde AS TIMESTAMP) IS NULL OR fecha_creacion >= CAST(:fechaDesde AS TIMESTAMP)) " +
          "AND (CAST(:fechaHasta AS TIMESTAMP) IS NULL OR fecha_creacion <= CAST(:fechaHasta AS TIMESTAMP)) " +
          "AND (CAST(:minaId AS INTEGER) IS NULL OR minas_id = CAST(:minaId AS INTEGER))",
          countQuery = "SELECT COUNT(*) FROM lotes " +
                  "WHERE minas_id = ANY(CAST(:minaIds AS INTEGER[])) " +
                  "AND (CAST(:estado AS TEXT) IS NULL OR estado = CAST(:estado AS TEXT)) " +
                  "AND (CAST(:tipoOperacion AS TEXT) IS NULL OR tipo_operacion = CAST(:tipoOperacion AS TEXT)) " +
                  "AND (CAST(:tipoMineral AS TEXT) IS NULL OR tipo_mineral = CAST(:tipoMineral AS TEXT)) " +
                  "AND (CAST(:fechaDesde AS TIMESTAMP) IS NULL OR fecha_creacion >= CAST(:fechaDesde AS TIMESTAMP)) " +
                  "AND (CAST(:fechaHasta AS TIMESTAMP) IS NULL OR fecha_creacion <= CAST(:fechaHasta AS TIMESTAMP)) " +
                  "AND (CAST(:minaId AS INTEGER) IS NULL OR minas_id = CAST(:minaId AS INTEGER))",
          nativeQuery = true)
  Page<Lotes> findByMinasIdInWithFilters(
          @Param("minaIds") Integer[] minaIds,
          @Param("estado") String estado,
          @Param("tipoOperacion") String tipoOperacion,
          @Param("tipoMineral") String tipoMineral,
          @Param("fechaDesde") LocalDateTime fechaDesde,
          @Param("fechaHasta") LocalDateTime fechaHasta,
          @Param("minaId") Integer minaId,
          Pageable pageable
  );
}