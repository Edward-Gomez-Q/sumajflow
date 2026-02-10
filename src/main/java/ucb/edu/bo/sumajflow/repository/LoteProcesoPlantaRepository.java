package ucb.edu.bo.sumajflow.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ucb.edu.bo.sumajflow.entity.Concentrado;
import ucb.edu.bo.sumajflow.entity.LoteProcesoPlanta;
import ucb.edu.bo.sumajflow.entity.Procesos;

import java.util.List;
import java.util.Optional;

public interface LoteProcesoPlantaRepository extends JpaRepository<LoteProcesoPlanta, Integer> {

    // Buscar procesos de un concentrado ordenados por orden
    List<LoteProcesoPlanta> findByConcentradoIdOrderByOrden(Concentrado concentrado);

    // Verificar si todos los procesos están completados
    @Query("SELECT CASE WHEN COUNT(lpp) = " +
            "(SELECT COUNT(lpp2) FROM LoteProcesoPlanta lpp2 WHERE lpp2.concentradoId = :concentrado) " +
            "THEN true ELSE false END " +
            "FROM LoteProcesoPlanta lpp " +
            "WHERE lpp.concentradoId = :concentrado AND lpp.estado = 'completado'")
    boolean todosLosProceosCompletados(@Param("concentrado") Concentrado concentrado);

    // Obtener proceso actual (primer proceso no completado)
    @Query("SELECT lpp FROM LoteProcesoPlanta lpp " +
            "WHERE lpp.concentradoId = :concentrado " +
            "AND lpp.estado != 'completado' " +
            "ORDER BY lpp.orden ASC")
    List<LoteProcesoPlanta> findProcesosPendientes(@Param("concentrado") Concentrado concentrado);

    // Obtener procesos de un concentrado ordenados
    List<LoteProcesoPlanta> findByConcentradoIdOrderByOrdenAsc(Concentrado concentrado);

    // Obtener proceso actual (primer pendiente)
    @Query("SELECT lpp FROM LoteProcesoPlanta lpp WHERE lpp.concentradoId = :concentrado AND lpp.estado = 'pendiente' ORDER BY lpp.orden ASC")
    Optional<LoteProcesoPlanta> findPrimerPendiente(@Param("concentrado") Concentrado concentrado);

    // Verificar si todos los procesos están completados
    @Query("SELECT CASE WHEN COUNT(lpp) = 0 THEN true ELSE false END FROM LoteProcesoPlanta lpp WHERE lpp.concentradoId = :concentrado AND lpp.estado != 'completado'")
    boolean todosCompletados(@Param("concentrado") Concentrado concentrado);

    List<LoteProcesoPlanta> findByConcentradoId(Concentrado concentradoId);

    List<LoteProcesoPlanta> findByProcesoIdAndEstado(Procesos procesoId, String estado);

}