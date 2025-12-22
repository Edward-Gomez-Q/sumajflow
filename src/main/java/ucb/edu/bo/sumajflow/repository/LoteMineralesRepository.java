package ucb.edu.bo.sumajflow.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ucb.edu.bo.sumajflow.entity.LoteMinerales;
import ucb.edu.bo.sumajflow.entity.Lotes;

import java.util.List;

public interface LoteMineralesRepository extends JpaRepository<LoteMinerales, Integer> {

  // Obtener minerales de un lote
  List<LoteMinerales> findByLotesId(Lotes lote);

  // Eliminar minerales de un lote (para actualización)
  void deleteByLotesId(Lotes lote);
}