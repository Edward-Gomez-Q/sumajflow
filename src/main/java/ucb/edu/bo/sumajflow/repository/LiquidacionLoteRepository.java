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

    // Buscar por liquidación
    List<LiquidacionLote> findByLiquidacionId(Liquidacion liquidacion);

    // Buscar por lote
    Optional<LiquidacionLote> findByLotesId(Lotes lote);

    // Verificar si un lote ya tiene liquidación
    @Query("SELECT CASE WHEN COUNT(ll) > 0 THEN true ELSE false END FROM LiquidacionLote ll WHERE ll.lotesId = :lote")
    boolean loteYaTieneLiquidacion(@Param("lote") Lotes lote);
}