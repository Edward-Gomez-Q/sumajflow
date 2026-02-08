package ucb.edu.bo.sumajflow.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ucb.edu.bo.sumajflow.entity.Comercializadora;
import ucb.edu.bo.sumajflow.entity.DeduccionConfiguracion;
import ucb.edu.bo.sumajflow.entity.TablaPreciosMineral;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface TablaPreciosMineralRepository extends JpaRepository<TablaPreciosMineral, Integer> {

    @Query("SELECT t FROM TablaPreciosMineral t " +
            "WHERE t.comercializadoraId = :comercializadora " +
            "AND t.mineral = :mineral " +
            "AND t.activo = true " +
            "AND t.fechaInicio <= :fecha " +
            "AND (t.fechaFin IS NULL OR t.fechaFin >= :fecha) " +
            "AND :valor >= t.rangoMinimo " +
            "AND :valor <= t.rangoMaximo")
    Optional<TablaPreciosMineral> findPrecioVigente(
            @Param("comercializadora") Comercializadora comercializadora,
            @Param("mineral") String mineral,
            @Param("valor") BigDecimal valor,
            @Param("fecha") LocalDate fecha
    );


    @Query("SELECT t FROM TablaPreciosMineral t " +
            "WHERE t.comercializadoraId.id = :comercializadoraId " +
            "AND t.activo = true " +
            "AND t.fechaInicio <= :fecha " +
            "AND (t.fechaFin IS NULL OR t.fechaFin >= :fecha) " +
            "ORDER BY t.mineral, t.rangoMinimo")
    List<TablaPreciosMineral> findPreciosVigentes(
            @Param("comercializadoraId") Integer comercializadoraId,
            @Param("fecha") LocalDate fecha
    );

}