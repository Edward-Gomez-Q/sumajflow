package ucb.edu.bo.sumajflow.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ucb.edu.bo.sumajflow.entity.IngenioMinero;
import ucb.edu.bo.sumajflow.entity.LoteIngenio;
import ucb.edu.bo.sumajflow.entity.Lotes;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface LoteIngenioRepository extends JpaRepository<LoteIngenio, Integer> {

  Optional<LoteIngenio> findByLotesId(Lotes lote);

  List<LoteIngenio> findByIngenioMineroId(IngenioMinero ingenio);

  /**
   * Obtiene los lotes de un ingenio con filtros y paginación
   */
  @Query(value = """
        SELECT li.*
        FROM lote_ingenio li
        INNER JOIN lotes l ON li.lotes_id = l.id
        INNER JOIN minas m ON l.minas_id = m.id
        INNER JOIN sectores s ON m.sectores_id = s.id
        INNER JOIN cooperativa c ON s.cooperativa_id = c.id
        WHERE li.ingenio_minero_id = :ingenioId
        AND (CAST(:estado AS TEXT) IS NULL OR l.estado = CAST(:estado AS TEXT))
        AND (CAST(:tipoMineral AS TEXT) IS NULL OR l.tipo_mineral = CAST(:tipoMineral AS TEXT))
        AND (CAST(:cooperativaNombre AS TEXT) IS NULL OR c.razon_social ILIKE CONCAT('%', CAST(:cooperativaNombre AS TEXT), '%'))
        AND (CAST(:fechaDesde AS TIMESTAMP) IS NULL OR l.fecha_creacion >= CAST(:fechaDesde AS TIMESTAMP))
        AND (CAST(:fechaHasta AS TIMESTAMP) IS NULL OR l.fecha_creacion <= CAST(:fechaHasta AS TIMESTAMP))
        """,
          countQuery = """
        SELECT COUNT(li.id)
        FROM lote_ingenio li
        INNER JOIN lotes l ON li.lotes_id = l.id
        INNER JOIN minas m ON l.minas_id = m.id
        INNER JOIN sectores s ON m.sectores_id = s.id
        INNER JOIN cooperativa c ON s.cooperativa_id = c.id
        WHERE li.ingenio_minero_id = :ingenioId
        AND (CAST(:estado AS TEXT) IS NULL OR l.estado = CAST(:estado AS TEXT))
        AND (CAST(:tipoMineral AS TEXT) IS NULL OR l.tipo_mineral = CAST(:tipoMineral AS TEXT))
        AND (CAST(:cooperativaNombre AS TEXT) IS NULL OR c.razon_social ILIKE CONCAT('%', CAST(:cooperativaNombre AS TEXT), '%'))
        AND (CAST(:fechaDesde AS TIMESTAMP) IS NULL OR l.fecha_creacion >= CAST(:fechaDesde AS TIMESTAMP))
        AND (CAST(:fechaHasta AS TIMESTAMP) IS NULL OR l.fecha_creacion <= CAST(:fechaHasta AS TIMESTAMP))
        """,
          nativeQuery = true)
  Page<LoteIngenio> findLotesByIngenioWithFilters(
          @Param("ingenioId") Integer ingenioId,
          @Param("estado") String estado,
          @Param("tipoMineral") String tipoMineral,
          @Param("cooperativaNombre") String cooperativaNombre,
          @Param("fechaDesde") LocalDateTime fechaDesde,
          @Param("fechaHasta") LocalDateTime fechaHasta,
          Pageable pageable
  );
}