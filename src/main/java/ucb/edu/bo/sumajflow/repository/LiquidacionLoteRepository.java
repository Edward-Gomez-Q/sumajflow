package ucb.edu.bo.sumajflow.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ucb.edu.bo.sumajflow.entity.Liquidacion;
import ucb.edu.bo.sumajflow.entity.LiquidacionLote;
import ucb.edu.bo.sumajflow.entity.Lotes;

import java.util.List;
import java.util.Optional;

public interface LiquidacionLoteRepository extends JpaRepository<LiquidacionLote, Integer> {
   List<LiquidacionLote> findByLotesId(Lotes lote);
   void deleteByLiquidacionId(Liquidacion liquidacion);
}