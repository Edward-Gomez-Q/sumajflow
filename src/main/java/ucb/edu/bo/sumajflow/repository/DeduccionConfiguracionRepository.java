package ucb.edu.bo.sumajflow.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ucb.edu.bo.sumajflow.entity.DeduccionConfiguracion;

import java.time.LocalDate;
import java.util.List;

public interface DeduccionConfiguracionRepository extends JpaRepository<DeduccionConfiguracion, Integer> {

    /**
     * Obtener deducciones vigentes para una fecha específica
     */
    @Query("SELECT d FROM DeduccionConfiguracion d " +
            "WHERE d.activo = true " +
            "AND d.fechaInicio <= :fecha " +
            "AND (d.fechaFin IS NULL OR d.fechaFin >= :fecha) " +
            "ORDER BY d.orden ASC")
    List<DeduccionConfiguracion> findDeduccionesVigentes(@Param("fecha") LocalDate fecha);

    /**
     * Obtener deducciones aplicables a un tipo de liquidación y mineral
     */
    @Query("SELECT d FROM DeduccionConfiguracion d " +
            "WHERE d.activo = true " +
            "AND d.fechaInicio <= :fecha " +
            "AND (d.fechaFin IS NULL OR d.fechaFin >= :fecha) " +
            "AND (d.aplicaATipoLiquidacion IS NULL OR d.aplicaATipoLiquidacion = 'todos' OR d.aplicaATipoLiquidacion = :tipoLiquidacion) " +
            "ORDER BY d.orden ASC")
    List<DeduccionConfiguracion> findDeduccionesAplicables(
            @Param("fecha") LocalDate fecha,
            @Param("tipoLiquidacion") String tipoLiquidacion
    );
}