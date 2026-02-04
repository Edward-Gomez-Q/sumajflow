package ucb.edu.bo.sumajflow.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ucb.edu.bo.sumajflow.entity.Concentrado;
import ucb.edu.bo.sumajflow.entity.Liquidacion;
import ucb.edu.bo.sumajflow.entity.LiquidacionConcentrado;

import java.util.List;
import java.util.Optional;

public interface LiquidacionConcentradoRepository extends JpaRepository<LiquidacionConcentrado, Integer> {

}