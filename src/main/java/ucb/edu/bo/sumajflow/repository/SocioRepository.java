package ucb.edu.bo.sumajflow.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ucb.edu.bo.sumajflow.entity.Socio;

public interface SocioRepository extends JpaRepository<Socio, Integer> {
  }