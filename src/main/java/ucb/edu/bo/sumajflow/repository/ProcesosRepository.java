package ucb.edu.bo.sumajflow.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ucb.edu.bo.sumajflow.entity.Procesos;

import java.util.List;

public interface ProcesosRepository extends JpaRepository<Procesos, Integer> {

  // Obtener todos los procesos ordenados por nombre
  List<Procesos> findAllByOrderByNombre();
}