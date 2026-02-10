package ucb.edu.bo.sumajflow.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ucb.edu.bo.sumajflow.entity.Comercializadora;
import ucb.edu.bo.sumajflow.entity.Concentrado;
import ucb.edu.bo.sumajflow.entity.Liquidacion;
import ucb.edu.bo.sumajflow.entity.LiquidacionConcentrado;

import java.util.List;
import java.util.Optional;

public interface LiquidacionConcentradoRepository extends JpaRepository<LiquidacionConcentrado, Integer> {
    List<LiquidacionConcentrado> findByConcentradoId(Concentrado concentrado);
    @Query("SELECT DISTINCT lc.concentradoId FROM LiquidacionConcentrado lc " +
            "WHERE lc.liquidacionId.comercializadoraId = :comercializadora " +
            "AND lc.liquidacionId.estado NOT IN ('pendiente_aprobacion', 'rechazado') " +
            "ORDER BY lc.concentradoId.createdAt DESC")
    List<Concentrado> findDistinctConcentradosByComercializadoraAprobados(
            @Param("comercializadora") Comercializadora comercializadora);

    List<LiquidacionConcentrado> findByLiquidacionId(Liquidacion liquidacion);
}