package ucb.edu.bo.sumajflow.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ucb.edu.bo.sumajflow.entity.IngenioMinero;
import ucb.edu.bo.sumajflow.entity.Planta;

import java.util.Optional;

public interface PlantaRepository extends JpaRepository<Planta, Integer> {

  // Buscar planta por ingenio
  Optional<Planta> findByIngenioMineroId(IngenioMinero ingenio);

  // Verificar si un ingenio tiene planta configurada
  @Query("SELECT CASE WHEN COUNT(p) > 0 THEN true ELSE false END FROM Planta p WHERE p.ingenioMineroId = :ingenio")
  boolean ingenioTienePlanta(@Param("ingenio") IngenioMinero ingenio);
}