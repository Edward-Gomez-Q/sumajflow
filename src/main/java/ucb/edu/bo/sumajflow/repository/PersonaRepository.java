package ucb.edu.bo.sumajflow.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ucb.edu.bo.sumajflow.entity.Persona;

public interface PersonaRepository extends JpaRepository<Persona, Integer> {
}