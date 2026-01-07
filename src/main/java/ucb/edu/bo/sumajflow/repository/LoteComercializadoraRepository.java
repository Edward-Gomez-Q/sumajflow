package ucb.edu.bo.sumajflow.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ucb.edu.bo.sumajflow.entity.Comercializadora;
import ucb.edu.bo.sumajflow.entity.LoteComercializadora;
import ucb.edu.bo.sumajflow.entity.Lotes;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface LoteComercializadoraRepository extends JpaRepository<LoteComercializadora, Integer> {

  Optional<LoteComercializadora> findByLotesId(Lotes lote);

  List<LoteComercializadora> findByComercializadoraId(Comercializadora comercializadora);

  /**
   * Obtiene los lotes de una comercializadora con filtros y paginación
   */
  @Query(value = """
        SELECT lc.*
        FROM lote_comercializadora lc
        INNER JOIN lotes l ON lc.lotes_id = l.id
        INNER JOIN minas m ON l.minas_id = m.id
        INNER JOIN sectores s ON m.sectores_id = s.id
        INNER JOIN cooperativa c ON s.cooperativa_id = c.id
        WHERE lc.comercializadora_id = :comercializadoraId
        AND (CAST(:estado AS TEXT) IS NULL OR l.estado = CAST(:estado AS TEXT))
        AND (CAST(:tipoMineral AS TEXT) IS NULL OR l.tipo_mineral = CAST(:tipoMineral AS TEXT))
        AND (CAST(:cooperativaNombre AS TEXT) IS NULL OR c.razon_social ILIKE CONCAT('%', CAST(:cooperativaNombre AS TEXT), '%'))
        AND (CAST(:fechaDesde AS TIMESTAMP) IS NULL OR l.fecha_creacion >= CAST(:fechaDesde AS TIMESTAMP))
        AND (CAST(:fechaHasta AS TIMESTAMP) IS NULL OR l.fecha_creacion <= CAST(:fechaHasta AS TIMESTAMP))
        """,
          countQuery = """
        SELECT COUNT(lc.id)
        FROM lote_comercializadora lc
        INNER JOIN lotes l ON lc.lotes_id = l.id
        INNER JOIN minas m ON l.minas_id = m.id
        INNER JOIN sectores s ON m.sectores_id = s.id
        INNER JOIN cooperativa c ON s.cooperativa_id = c.id
        WHERE lc.comercializadora_id = :comercializadoraId
        AND (CAST(:estado AS TEXT) IS NULL OR l.estado = CAST(:estado AS TEXT))
        AND (CAST(:tipoMineral AS TEXT) IS NULL OR l.tipo_mineral = CAST(:tipoMineral AS TEXT))
        AND (CAST(:cooperativaNombre AS TEXT) IS NULL OR c.razon_social ILIKE CONCAT('%', CAST(:cooperativaNombre AS TEXT), '%'))
        AND (CAST(:fechaDesde AS TIMESTAMP) IS NULL OR l.fecha_creacion >= CAST(:fechaDesde AS TIMESTAMP))
        AND (CAST(:fechaHasta AS TIMESTAMP) IS NULL OR l.fecha_creacion <= CAST(:fechaHasta AS TIMESTAMP))
        """,
          nativeQuery = true)
  Page<LoteComercializadora> findLotesByComercializadoraWithFilters(
          @Param("comercializadoraId") Integer comercializadoraId,
          @Param("estado") String estado,
          @Param("tipoMineral") String tipoMineral,
          @Param("cooperativaNombre") String cooperativaNombre,
          @Param("fechaDesde") LocalDateTime fechaDesde,
          @Param("fechaHasta") LocalDateTime fechaHasta,
          Pageable pageable
  );
}