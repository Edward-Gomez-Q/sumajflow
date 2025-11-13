package ucb.edu.bo.sumajflow.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ucb.edu.bo.sumajflow.entity.Planta;

public interface PlantaRepository extends JpaRepository<Planta, Integer> {
  }