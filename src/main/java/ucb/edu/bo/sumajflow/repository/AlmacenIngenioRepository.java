package ucb.edu.bo.sumajflow.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ucb.edu.bo.sumajflow.entity.AlmacenIngenio;
import ucb.edu.bo.sumajflow.entity.IngenioMinero;

import java.util.Optional;

@Repository
public interface AlmacenIngenioRepository extends JpaRepository<AlmacenIngenio, Integer> {

  Optional<AlmacenIngenio> findByIngenioMineroId(IngenioMinero ingenioMinero);
}