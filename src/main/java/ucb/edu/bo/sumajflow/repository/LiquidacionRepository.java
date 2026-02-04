package ucb.edu.bo.sumajflow.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ucb.edu.bo.sumajflow.entity.Liquidacion;
import ucb.edu.bo.sumajflow.entity.Socio;

import java.time.LocalDate;
import java.util.List;

public interface LiquidacionRepository extends JpaRepository<Liquidacion, Integer> {
    List<Liquidacion> findBySocioIdOrderByCreatedAtDesc(Socio socio);
    List<Liquidacion> findBySocioIdAndTipoLiquidacionOrderByCreatedAtDesc(Socio socio, String tipoLiquidacion);

}