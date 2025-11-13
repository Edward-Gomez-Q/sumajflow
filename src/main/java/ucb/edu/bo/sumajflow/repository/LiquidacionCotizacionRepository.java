package ucb.edu.bo.sumajflow.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ucb.edu.bo.sumajflow.entity.LiquidacionCotizacion;

public interface LiquidacionCotizacionRepository extends JpaRepository<LiquidacionCotizacion, Integer> {
  }