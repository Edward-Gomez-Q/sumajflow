package ucb.edu.bo.sumajflow.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ucb.edu.bo.sumajflow.entity.Sectores;
import ucb.edu.bo.sumajflow.entity.SectoresCoordenadas;

public interface SectoresCoordenadasRepository extends JpaRepository<SectoresCoordenadas, Integer> {
  void deleteBySectoresId(Sectores sector);
}