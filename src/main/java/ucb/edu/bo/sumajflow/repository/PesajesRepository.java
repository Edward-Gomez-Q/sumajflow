package ucb.edu.bo.sumajflow.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ucb.edu.bo.sumajflow.entity.AsignacionCamion;
import ucb.edu.bo.sumajflow.entity.Pesajes;

import java.util.List;

public interface PesajesRepository extends JpaRepository<Pesajes, Integer> {

  // Obtener pesajes de una asignación de camión
  List<Pesajes> findByAsignacionCamionId(AsignacionCamion asignacionCamion);

  // Obtener pesajes por tipo
  @Query("SELECT p FROM Pesajes p WHERE p.asignacionCamionId = :asignacion AND p.tipoPesaje = :tipo")
  List<Pesajes> findByAsignacionAndTipo(
          @Param("asignacion") AsignacionCamion asignacion,
          @Param("tipo") String tipo
  );

  // Verificar si existe un pesaje de un tipo específico para una asignación
  @Query("SELECT CASE WHEN COUNT(p) > 0 THEN true ELSE false END " +
          "FROM Pesajes p " +
          "WHERE p.asignacionCamionId = :asignacion AND p.tipoPesaje = :tipo")
  boolean existsByAsignacionAndTipo(
          @Param("asignacion") AsignacionCamion asignacion,
          @Param("tipo") String tipo
  );

  // Contar pesajes de una asignación
  @Query("SELECT COUNT(p) FROM Pesajes p WHERE p.asignacionCamionId = :asignacion")
  long countByAsignacion(@Param("asignacion") AsignacionCamion asignacion);
}