package ucb.edu.bo.sumajflow.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ucb.edu.bo.sumajflow.entity.Persona;
import ucb.edu.bo.sumajflow.entity.Usuarios;

import java.util.Optional;

public interface PersonaRepository extends JpaRepository<Persona, Integer> {


    Optional<Persona> findByUsuariosId(Usuarios usuario);
}