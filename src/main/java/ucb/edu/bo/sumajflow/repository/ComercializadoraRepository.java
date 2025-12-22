package ucb.edu.bo.sumajflow.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import ucb.edu.bo.sumajflow.entity.Comercializadora;
import ucb.edu.bo.sumajflow.entity.Usuarios;

import java.util.List;
import java.util.Optional;

public interface ComercializadoraRepository extends JpaRepository<Comercializadora, Integer> {

  // Obtener todas las comercializadoras activas
  @Query("SELECT c FROM Comercializadora c ORDER BY c.razonSocial")
  List<Comercializadora> findAllActive();

    Optional<Comercializadora> findByUsuariosId(Usuarios usuario);
}