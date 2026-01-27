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


    
    // Buscar por liquidación
    List<LiquidacionConcentrado> findByLiquidacionId(Liquidacion liquidacion);

    // Buscar por concentrado
    List<LiquidacionConcentrado> findByConcentradoId(Concentrado concentrado);

    // Verificar si un concentrado ya tiene liquidación de venta
    @Query("SELECT CASE WHEN COUNT(lc) > 0 THEN true ELSE false END " +
            "FROM LiquidacionConcentrado lc " +
            "JOIN lc.liquidacionId l " +
            "WHERE lc.concentradoId = :concentrado " +
            "AND l.tipoLiquidacion = 'venta_concentrado'")
    boolean concentradoYaTieneLiquidacionVenta(@Param("concentrado") Concentrado concentrado);

    // Verificar si un concentrado ya tiene liquidación de cobro ingenio
    @Query("SELECT CASE WHEN COUNT(lc) > 0 THEN true ELSE false END " +
            "FROM LiquidacionConcentrado lc " +
            "JOIN lc.liquidacionId l " +
            "WHERE lc.concentradoId = :concentrado " +
            "AND l.tipoLiquidacion = 'cobro_ingenio'")
    boolean concentradoYaTieneLiquidacionIngenio(@Param("concentrado") Concentrado concentrado);
}