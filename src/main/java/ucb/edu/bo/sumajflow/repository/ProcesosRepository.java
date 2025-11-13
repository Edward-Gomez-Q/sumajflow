package ucb.edu.bo.sumajflow.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ucb.edu.bo.sumajflow.entity.Procesos;

public interface ProcesosRepository extends JpaRepository<Procesos, Integer> {
  }