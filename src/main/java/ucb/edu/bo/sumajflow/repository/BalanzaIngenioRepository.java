package ucb.edu.bo.sumajflow.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ucb.edu.bo.sumajflow.entity.BalanzaIngenio;
import ucb.edu.bo.sumajflow.entity.IngenioMinero;
import java.util.Optional;

public interface BalanzaIngenioRepository extends JpaRepository<BalanzaIngenio, Integer> {
    Optional<BalanzaIngenio> findByIngenioMineroId(IngenioMinero ingenio);
}