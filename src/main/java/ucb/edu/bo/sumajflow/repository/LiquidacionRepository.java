package ucb.edu.bo.sumajflow.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ucb.edu.bo.sumajflow.entity.*;

import java.util.List;
import java.util.Optional;

public interface LiquidacionRepository extends JpaRepository<Liquidacion, Integer> {
    List<Liquidacion> findBySocioIdOrderByCreatedAtDesc(Socio socio);
    List<Liquidacion> findBySocioIdAndTipoLiquidacionOrderByCreatedAtDesc(Socio socio, String tipoLiquidacion);
    List<Liquidacion> findByTipoLiquidacionOrderByCreatedAtDesc(String tipoLiquidacion);
    List<Liquidacion> findBySocioIdAndTipoLiquidacionInOrderByCreatedAtDesc(Socio socio, List<String> tiposLiquidacion);
    List<Liquidacion> findByComercializadoraIdAndTipoLiquidacionInOrderByCreatedAtDesc(Comercializadora comercializadora, List<String> tiposLiquidacion);

    List<Liquidacion> findBySocioId(Socio socioId);

    @Query("SELECT l FROM Liquidacion l " +
            "WHERE l.socioId = :socio " +
            "AND l.tipoLiquidacion = :tipo")
    List<Liquidacion> findBySocioIdAndTipoLiquidacion(
            @Param("socio") Socio socio,
            @Param("tipo") String tipoLiquidacion
    );
    List<Liquidacion> findByComercializadoraId(Comercializadora comercializadoraId);

    List<Liquidacion> findByComercializadoraIdAndEstado(Comercializadora comercializadoraId, String estado);

    @Query("SELECT DISTINCT l FROM Liquidacion l " +
            "JOIN l.liquidacionLoteList ll " +
            "JOIN ll.lotesId lote " +
            "JOIN lote.loteIngenioList li " +
            "WHERE li.ingenioMineroId = :ingenio " +
            "ORDER BY l.createdAt DESC")
    List<Liquidacion> findByIngenioMineroId(@Param("ingenio") IngenioMinero ingenio);


    @Query("SELECT DISTINCT l FROM Liquidacion l " +
            "JOIN l.liquidacionLoteList ll " +
            "WHERE ll.lotesId = :lote " +
            "ORDER BY l.createdAt DESC")
    Optional<Liquidacion> findByLote(@Param("lote") Lotes lote);

    @Query("SELECT DISTINCT l FROM Liquidacion l " +
            "JOIN l.liquidacionConcentradoList lc " +
            "WHERE lc.concentradoId = :concentrado " +
            "ORDER BY l.createdAt DESC")
    Optional<Liquidacion> findByConcentradoId(@Param("concentrado") Concentrado concentrado);
}