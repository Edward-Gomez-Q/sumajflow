package ucb.edu.bo.sumajflow.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ucb.edu.bo.sumajflow.entity.AlmacenComercializadora;
import ucb.edu.bo.sumajflow.entity.Comercializadora;

import java.util.Optional;

@Repository
public interface AlmacenComercializadoraRepository extends JpaRepository<AlmacenComercializadora, Integer> {

  Optional<AlmacenComercializadora> findByComercializadoraId(Comercializadora comercializadora);
}