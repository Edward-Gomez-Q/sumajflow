package ucb.edu.bo.sumajflow.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ucb.edu.bo.sumajflow.entity.LoteIngenio;
import ucb.edu.bo.sumajflow.entity.Lotes;

import java.util.Optional;

public interface LoteIngenioRepository extends JpaRepository<LoteIngenio, Integer> {

  // Buscar por lote
  Optional<LoteIngenio> findByLotesId(Lotes lote);
}