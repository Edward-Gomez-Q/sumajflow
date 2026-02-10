package ucb.edu.bo.sumajflow.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import ucb.edu.bo.sumajflow.entity.BalanzaCooperativa;
import ucb.edu.bo.sumajflow.entity.Cooperativa;

import java.util.List;
import java.util.Optional;


public interface BalanzaCooperativaRepository extends JpaRepository<BalanzaCooperativa, Integer> {

  Optional<BalanzaCooperativa> findByCooperativaId(Cooperativa cooperativa);

  @Query("SELECT b FROM BalanzaCooperativa b WHERE b.cooperativaId = :cooperativaId ORDER BY b.createdAt DESC")
  List<BalanzaCooperativa> findByCooperativaIdQuery(Cooperativa cooperativaId);
}