package ucb.edu.bo.sumajflow.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ucb.edu.bo.sumajflow.entity.Auditoria;

public interface AuditoriaRepository extends JpaRepository<Auditoria, Integer> {
  }