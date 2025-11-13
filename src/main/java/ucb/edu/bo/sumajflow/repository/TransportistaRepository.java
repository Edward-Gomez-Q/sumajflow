package ucb.edu.bo.sumajflow.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ucb.edu.bo.sumajflow.entity.Transportista;

public interface TransportistaRepository extends JpaRepository<Transportista, Integer> {
  }