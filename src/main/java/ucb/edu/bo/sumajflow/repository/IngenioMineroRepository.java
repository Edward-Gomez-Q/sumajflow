package ucb.edu.bo.sumajflow.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import ucb.edu.bo.sumajflow.entity.IngenioMinero;
import ucb.edu.bo.sumajflow.entity.Usuarios;

import java.util.List;
import java.util.Optional;

public interface IngenioMineroRepository extends JpaRepository<IngenioMinero, Integer> {

  // Obtener todos los ingenios activos (puedes agregar campo 'estado' si lo necesitas)
  @Query("SELECT i FROM IngenioMinero i ORDER BY i.razonSocial")
  List<IngenioMinero> findAllActive();

   Optional<IngenioMinero> findByUsuariosId(Usuarios usuario);
}