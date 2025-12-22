package ucb.edu.bo.sumajflow.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ucb.edu.bo.sumajflow.entity.Planta;
import ucb.edu.bo.sumajflow.entity.ProcesosPlanta;

import java.util.List;

public interface ProcesosPlantaRepository extends JpaRepository<ProcesosPlanta, Integer> {

  // Obtener procesos de una planta ordenados por orden
  List<ProcesosPlanta> findByPlantaIdOrderByOrden(Planta planta);
}