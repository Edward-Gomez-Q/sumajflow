package ucb.edu.bo.sumajflow.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ucb.edu.bo.sumajflow.entity.BalanzaComercializadora;
import ucb.edu.bo.sumajflow.entity.Comercializadora;

import java.util.Optional;

public interface BalanzaComercializadoraRepository extends JpaRepository<BalanzaComercializadora, Integer> {
  Optional<BalanzaComercializadora> findByComercializadoraId(Comercializadora comercializadora);
}