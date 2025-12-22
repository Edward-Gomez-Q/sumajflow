package ucb.edu.bo.sumajflow.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ucb.edu.bo.sumajflow.entity.Liquidacion;
import ucb.edu.bo.sumajflow.entity.Socio;

import java.time.LocalDate;
import java.util.List;

public interface LiquidacionRepository extends JpaRepository<Liquidacion, Integer> {

  // Buscar por socio
  List<Liquidacion> findBySocioIdOrderByFechaLiquidacionDesc(Socio socio);

  // Buscar por tipo de liquidación
  @Query("SELECT l FROM Liquidacion l WHERE l.tipoLiquidacion = :tipo ORDER BY l.fechaLiquidacion DESC")
  List<Liquidacion> findByTipo(@Param("tipo") String tipo);

  // Buscar por estado
  @Query("SELECT l FROM Liquidacion l WHERE l.estado = :estado ORDER BY l.fechaLiquidacion DESC")
  List<Liquidacion> findByEstado(@Param("estado") String estado);

  // Buscar por socio y estado
  @Query("SELECT l FROM Liquidacion l WHERE l.socioId = :socio AND l.estado = :estado ORDER BY l.fechaLiquidacion DESC")
  List<Liquidacion> findBySocioAndEstado(
          @Param("socio") Socio socio,
          @Param("estado") String estado
  );

  // Buscar por rango de fechas
  @Query("SELECT l FROM Liquidacion l WHERE l.fechaLiquidacion BETWEEN :fechaInicio AND :fechaFin ORDER BY l.fechaLiquidacion DESC")
  List<Liquidacion> findByRangoFechas(
          @Param("fechaInicio") LocalDate fechaInicio,
          @Param("fechaFin") LocalDate fechaFin
  );

  // Calcular total liquidado por socio
  @Query("SELECT COALESCE(SUM(l.valorNeto), 0) FROM Liquidacion l WHERE l.socioId = :socio AND l.estado = 'pagado'")
  java.math.BigDecimal calcularTotalLiquidadoPorSocio(@Param("socio") Socio socio);

  // Contar liquidaciones por estado
  @Query("SELECT COUNT(l) FROM Liquidacion l WHERE l.estado = :estado")
  long countByEstado(@Param("estado") String estado);
}