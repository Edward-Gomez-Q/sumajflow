package ucb.edu.bo.sumajflow.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ucb.edu.bo.sumajflow.entity.BalanzaCooperativa;
import ucb.edu.bo.sumajflow.entity.Cooperativa;

import java.util.Optional;


public interface BalanzaCooperativaRepository extends JpaRepository<BalanzaCooperativa, Integer> {

  Optional<BalanzaCooperativa> findByCooperativaId(Cooperativa cooperativa);
}