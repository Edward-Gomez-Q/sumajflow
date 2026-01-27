package ucb.edu.bo.sumajflow.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ucb.edu.bo.sumajflow.entity.Concentrado;
import ucb.edu.bo.sumajflow.entity.LoteConcentradoRelacion;
import ucb.edu.bo.sumajflow.entity.Lotes;

import java.math.BigDecimal;
import java.util.List;

public interface LoteConcentradoRelacionRepository extends JpaRepository<LoteConcentradoRelacion, Integer> {

    // Buscar relaciones por concentrado
    List<LoteConcentradoRelacion> findByConcentradoId(Concentrado concentrado);

    // Buscar relaciones por lote
    List<LoteConcentradoRelacion> findByLoteComplejoId(Lotes lote);

    // Verificar si un lote ya está en un concentrado
    @Query("SELECT CASE WHEN COUNT(lcr) > 0 THEN true ELSE false END FROM LoteConcentradoRelacion lcr WHERE lcr.loteComplejoId = :lote")
    boolean loteYaEnConcentrado(@Param("lote") Lotes lote);

    // Calcular peso total de entrada de un concentrado
    @Query("SELECT COALESCE(SUM(lcr.pesoEntrada), 0) FROM LoteConcentradoRelacion lcr WHERE lcr.concentradoId = :concentrado")
    BigDecimal calcularPesoTotalEntrada(@Param("concentrado") Concentrado concentrado);

    // Contar lotes de un concentrado
    @Query("SELECT COUNT(lcr) FROM LoteConcentradoRelacion lcr WHERE lcr.concentradoId = :concentrado")
    long contarLotesDeConcentrado(@Param("concentrado") Concentrado concentrado);


    // Verificar si un lote ya está en un concentrado
    @Query("SELECT CASE WHEN COUNT(lcr) > 0 THEN true ELSE false END FROM LoteConcentradoRelacion lcr WHERE lcr.loteComplejoId = :lote")
    boolean existsByLoteComplejoId(@Param("lote") Lotes lote);
}