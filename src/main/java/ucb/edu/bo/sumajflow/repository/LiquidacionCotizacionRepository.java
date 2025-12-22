package ucb.edu.bo.sumajflow.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ucb.edu.bo.sumajflow.entity.Liquidacion;
import ucb.edu.bo.sumajflow.entity.LiquidacionCotizacion;

import java.util.List;

public interface LiquidacionCotizacionRepository extends JpaRepository<LiquidacionCotizacion, Integer> {

  // Buscar por liquidación
  List<LiquidacionCotizacion> findByLiquidacionId(Liquidacion liquidacion);

  // Eliminar todas las cotizaciones de una liquidación
  void deleteByLiquidacionId(Liquidacion liquidacion);
}