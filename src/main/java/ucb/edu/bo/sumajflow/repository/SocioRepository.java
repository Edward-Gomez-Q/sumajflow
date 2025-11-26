package ucb.edu.bo.sumajflow.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ucb.edu.bo.sumajflow.entity.Socio;
import ucb.edu.bo.sumajflow.entity.Usuarios;

import java.util.Optional;

public interface SocioRepository extends JpaRepository<Socio, Integer> {

  Optional<Socio> findByUsuariosId(Usuarios usuarioId);
}