package ucb.edu.bo.sumajflow.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ucb.edu.bo.sumajflow.entity.Lotes;

public interface LotesRepository extends JpaRepository<Lotes, Integer> {
  }