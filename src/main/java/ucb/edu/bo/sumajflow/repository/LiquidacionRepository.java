package ucb.edu.bo.sumajflow.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ucb.edu.bo.sumajflow.entity.Liquidacion;

public interface LiquidacionRepository extends JpaRepository<Liquidacion, Integer> {
  }