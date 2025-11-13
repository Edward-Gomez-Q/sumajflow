package ucb.edu.bo.sumajflow.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ucb.edu.bo.sumajflow.entity.Minerales;

public interface MineralesRepository extends JpaRepository<Minerales, Integer> {
  }