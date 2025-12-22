package ucb.edu.bo.sumajflow.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ucb.edu.bo.sumajflow.entity.LoteComercializadora;
import ucb.edu.bo.sumajflow.entity.Lotes;

import java.util.Optional;

public interface LoteComercializadoraRepository extends JpaRepository<LoteComercializadora, Integer> {

  // Buscar por lote
  Optional<LoteComercializadora> findByLotesId(Lotes lote);
}