package ucb.edu.bo.sumajflow.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ucb.edu.bo.sumajflow.entity.ReporteQuimico;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ReporteQuimicoRepository extends JpaRepository<ReporteQuimico, Integer> {

  // Buscar por número de reporte
  Optional<ReporteQuimico> findByNumeroReporte(String numeroReporte);

  // Verificar si existe un número de reporte
  @Query("SELECT CASE WHEN COUNT(r) > 0 THEN true ELSE false END FROM ReporteQuimico r WHERE r.numeroReporte = :numero")
  boolean existsByNumeroReporte(@Param("numero") String numeroReporte);

  // Buscar por tipo de análisis
  List<ReporteQuimico> findByTipoAnalisisOrderByFechaAnalisisDesc(String tipoAnalisis);

  // Buscar por laboratorio
  List<ReporteQuimico> findByLaboratorioOrderByFechaAnalisisDesc(String laboratorio);

  // Buscar por rango de fechas
  @Query("SELECT r FROM ReporteQuimico r WHERE r.fechaAnalisis BETWEEN :fechaInicio AND :fechaFin ORDER BY r.fechaAnalisis DESC")
  List<ReporteQuimico> findByRangoFechas(
          @Param("fechaInicio") LocalDate fechaInicio,
          @Param("fechaFin") LocalDate fechaFin
  );

  // Obtener reportes recientes
  @Query(value = "SELECT * FROM reporte_quimico ORDER BY fecha_analisis DESC LIMIT :limit", nativeQuery = true)
  List<ReporteQuimico> findTopNRecientes(@Param("limit") int limit);

  // Buscar reportes con leyes superiores a un valor
  @Query("SELECT r FROM ReporteQuimico r WHERE " +
          "(r.leyAg IS NOT NULL AND r.leyAg >= :ley) OR " +
          "(r.leyPb IS NOT NULL AND r.leyPb >= :ley) OR " +
          "(r.leyZn IS NOT NULL AND r.leyZn >= :ley)")
  List<ReporteQuimico> findByLeyMinima(@Param("ley") java.math.BigDecimal ley);
}