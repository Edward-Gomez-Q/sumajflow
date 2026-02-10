package ucb.edu.bo.sumajflow.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ucb.edu.bo.sumajflow.entity.Lotes;
import ucb.edu.bo.sumajflow.entity.Minas;
import ucb.edu.bo.sumajflow.entity.Socio;

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
  /**
   * Verifica si existen lotes en proceso (no finalizados) para un ingenio específico
   * Los estados finales son: 'Completado' y 'Rechazado'
   */
  @Query(value = """
        SELECT COUNT(l.id) > 0
        FROM lotes l
        INNER JOIN lote_ingenio li ON l.id = li.lotes_id
        WHERE li.ingenio_minero_id = :ingenioId
        AND l.estado NOT IN ('Completado', 'Rechazado')
        """, nativeQuery = true)
  boolean existenLotesEnProcesoParaIngenio(@Param("ingenioId") Integer ingenioId);

  /**
   * Verifica si existen lotes en proceso (no finalizados) para una comercializadora específica
   * Los estados finales son: 'Completado' y 'Rechazado'
   */
  @Query(value = """
        SELECT COUNT(l.id) > 0
        FROM lotes l
        INNER JOIN lote_comercializadora lc ON l.id = lc.lotes_id
        WHERE lc.comercializadora_id = :comercializadoraId
        AND l.estado NOT IN ('Completado', 'Rechazado')
        """, nativeQuery = true)
  boolean existenLotesEnProcesoParaComercializadora(@Param("comercializadoraId") Integer comercializadoraId);

  /**
   * Obtiene los lotes en proceso para un ingenio (para mostrar detalles)
   */
  @Query(value = """
        SELECT l.*
        FROM lotes l
        INNER JOIN lote_ingenio li ON l.id = li.lotes_id
        WHERE li.ingenio_minero_id = :ingenioId
        AND l.estado NOT IN ('Completado', 'Rechazado')
        ORDER BY l.fecha_creacion DESC
        """, nativeQuery = true)
  List<Lotes> obtenerLotesEnProcesoParaIngenio(@Param("ingenioId") Integer ingenioId);

  /**
   * Obtiene los lotes en proceso para una comercializadora (para mostrar detalles)
   */
  @Query(value = """
        SELECT l.*
        FROM lotes l
        INNER JOIN lote_comercializadora lc ON l.id = lc.lotes_id
        WHERE lc.comercializadora_id = :comercializadoraId
        AND l.estado NOT IN ('Completado', 'Rechazado')
        ORDER BY l.fecha_creacion DESC
        """, nativeQuery = true)
  List<Lotes> obtenerLotesEnProcesoParaComercializadora(@Param("comercializadoraId") Integer comercializadoraId);

  /**
   * Calcula la ocupación total del almacén de un ingenio
   * Suma el peso de todos los lotes que están almacenados (estados que indican presencia física)
   */
  @Query(value = """
        SELECT COALESCE(SUM(l.peso_total_real), 0)
        FROM lotes l
        INNER JOIN lote_ingenio li ON l.id = li.lotes_id
        WHERE li.ingenio_minero_id = :ingenioId
        AND l.estado IN ('En Almacén', 'En Procesamiento', 'Procesado')
        """, nativeQuery = true)
  Double calcularOcupacionAlmacenIngenio(@Param("ingenioId") Integer ingenioId);

  /**
   * Calcula la ocupación total del almacén de una comercializadora
   * Suma el peso de todos los lotes que están almacenados
   */
  @Query(value = """
        SELECT COALESCE(SUM(l.peso_total_real), 0)
        FROM lotes l
        INNER JOIN lote_comercializadora lc ON l.id = lc.lotes_id
        WHERE lc.comercializadora_id = :comercializadoraId
        AND l.estado IN ('En Almacén', 'Pendiente Despacho')
        """, nativeQuery = true)
  Double calcularOcupacionAlmacenComercializadora(@Param("comercializadoraId") Integer comercializadoraId);

  /**
   * Cuenta los lotes almacenados en un ingenio
   */
  @Query(value = """
        SELECT COUNT(l.id)
        FROM lotes l
        INNER JOIN lote_ingenio li ON l.id = li.lotes_id
        WHERE li.ingenio_minero_id = :ingenioId
        AND l.estado IN ('En Almacén', 'En Procesamiento', 'Procesado')
        """, nativeQuery = true)
  Integer contarLotesAlmacenadosIngenio(@Param("ingenioId") Integer ingenioId);

  /**
   * Cuenta los lotes almacenados en una comercializadora
   */
  @Query(value = """
        SELECT COUNT(l.id)
        FROM lotes l
        INNER JOIN lote_comercializadora lc ON l.id = lc.lotes_id
        WHERE lc.comercializadora_id = :comercializadoraId
        AND l.estado IN ('En Almacén', 'Pendiente Despacho')
        """, nativeQuery = true)
  Integer contarLotesAlmacenadosComercializadora(@Param("comercializadoraId") Integer comercializadoraId);

  /**
   * Obtiene los lotes de una cooperativa con filtros y paginación
   * Busca todos los lotes cuyas minas pertenezcan a sectores de la cooperativa
   */
  @Query(value = """
        SELECT l.*
        FROM lotes l
        INNER JOIN minas m ON l.minas_id = m.id
        INNER JOIN sectores s ON m.sectores_id = s.id
        WHERE s.cooperativa_id = :cooperativaId
        AND (CAST(:estado AS TEXT) IS NULL OR l.estado = CAST(:estado AS TEXT))
        AND (CAST(:tipoOperacion AS TEXT) IS NULL OR l.tipo_operacion = CAST(:tipoOperacion AS TEXT))
        AND (CAST(:tipoMineral AS TEXT) IS NULL OR l.tipo_mineral = CAST(:tipoMineral AS TEXT))
        AND (CAST(:fechaDesde AS TIMESTAMP) IS NULL OR l.fecha_creacion >= CAST(:fechaDesde AS TIMESTAMP))
        AND (CAST(:fechaHasta AS TIMESTAMP) IS NULL OR l.fecha_creacion <= CAST(:fechaHasta AS TIMESTAMP))
        AND (CAST(:socioId AS INTEGER) IS NULL OR m.socio_id = CAST(:socioId AS INTEGER))
        AND (CAST(:minaId AS INTEGER) IS NULL OR l.minas_id = CAST(:minaId AS INTEGER))
        AND (CAST(:sectorId AS INTEGER) IS NULL OR s.id = CAST(:sectorId AS INTEGER))
        """,
          countQuery = """
        SELECT COUNT(l.id)
        FROM lotes l
        INNER JOIN minas m ON l.minas_id = m.id
        INNER JOIN sectores s ON m.sectores_id = s.id
        WHERE s.cooperativa_id = :cooperativaId
        AND (CAST(:estado AS TEXT) IS NULL OR l.estado = CAST(:estado AS TEXT))
        AND (CAST(:tipoOperacion AS TEXT) IS NULL OR l.tipo_operacion = CAST(:tipoOperacion AS TEXT))
        AND (CAST(:tipoMineral AS TEXT) IS NULL OR l.tipo_mineral = CAST(:tipoMineral AS TEXT))
        AND (CAST(:fechaDesde AS TIMESTAMP) IS NULL OR l.fecha_creacion >= CAST(:fechaDesde AS TIMESTAMP))
        AND (CAST(:fechaHasta AS TIMESTAMP) IS NULL OR l.fecha_creacion <= CAST(:fechaHasta AS TIMESTAMP))
        AND (CAST(:socioId AS INTEGER) IS NULL OR m.socio_id = CAST(:socioId AS INTEGER))
        AND (CAST(:minaId AS INTEGER) IS NULL OR l.minas_id = CAST(:minaId AS INTEGER))
        AND (CAST(:sectorId AS INTEGER) IS NULL OR s.id = CAST(:sectorId AS INTEGER))
        """,
          nativeQuery = true)
  Page<Lotes> findLotesByCooperativaWithFilters(
          @Param("cooperativaId") Integer cooperativaId,
          @Param("estado") String estado,
          @Param("tipoOperacion") String tipoOperacion,
          @Param("tipoMineral") String tipoMineral,
          @Param("fechaDesde") LocalDateTime fechaDesde,
          @Param("fechaHasta") LocalDateTime fechaHasta,
          @Param("socioId") Integer socioId,
          @Param("minaId") Integer minaId,
          @Param("sectorId") Integer sectorId,
          Pageable pageable
  );

  @Query("SELECT l FROM Lotes l WHERE l.minasId.socioId = :socio")
  List<Lotes> findByMinasSocioId(@Param("socio") Socio socio);

  @Query("SELECT l FROM Lotes l WHERE l.minasId.socioId = :socio AND l.estado = :estado")
  List<Lotes> findByMinasSocioIdAndEstado(Socio socio, String estado);
}