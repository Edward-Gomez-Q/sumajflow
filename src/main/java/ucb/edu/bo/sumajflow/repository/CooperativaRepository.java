package ucb.edu.bo.sumajflow.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ucb.edu.bo.sumajflow.entity.Cooperativa;
import ucb.edu.bo.sumajflow.entity.Usuarios;

import java.util.Optional;

public interface CooperativaRepository extends JpaRepository<Cooperativa, Integer> {

  Optional<Cooperativa> findByUsuariosId(Usuarios usuario);
}