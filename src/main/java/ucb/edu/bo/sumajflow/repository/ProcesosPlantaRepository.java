package ucb.edu.bo.sumajflow.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ucb.edu.bo.sumajflow.entity.ProcesosPlanta;

public interface ProcesosPlantaRepository extends JpaRepository<ProcesosPlanta, Integer> {
  }