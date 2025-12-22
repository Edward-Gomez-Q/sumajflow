package ucb.edu.bo.sumajflow.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ucb.edu.bo.sumajflow.entity.Liquidacion;
import ucb.edu.bo.sumajflow.entity.LiquidacionDeduccion;

import java.math.BigDecimal;
import java.util.List;

public interface LiquidacionDeduccionRepository extends JpaRepository<LiquidacionDeduccion, Integer> {

  // Buscar por liquidación
  List<LiquidacionDeduccion> findByLiquidacionId(Liquidacion liquidacion);

  // Calcular total de deducciones
  @Query("SELECT COALESCE(SUM(d.monto), 0) FROM LiquidacionDeduccion d WHERE d.liquidacionId = :liquidacion")
  BigDecimal calcularTotalDeducciones(@Param("liquidacion") Liquidacion liquidacion);

  // Eliminar todas las deducciones de una liquidación
  void deleteByLiquidacionId(Liquidacion liquidacion);
}