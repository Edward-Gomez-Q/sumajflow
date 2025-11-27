// src/main/java/ucb/edu/bo/sumajflow/repository/SectoresRepository.java
package ucb.edu.bo.sumajflow.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ucb.edu.bo.sumajflow.entity.Cooperativa;
import ucb.edu.bo.sumajflow.entity.Sectores;

import java.util.List;
import java.util.Optional;

public interface SectoresRepository extends JpaRepository<Sectores, Integer> {
  List<Sectores> findByCooperativaId(Cooperativa cooperativa);
  Optional<Sectores> findByNombreAndCooperativaId(String nombre, Cooperativa cooperativa);
  Optional<Sectores> findByColorAndCooperativaId(String color, Cooperativa cooperativa);
  boolean existsByNombreAndCooperativaId(String nombre, Cooperativa cooperativa);
  boolean existsByColorAndCooperativaId(String color, Cooperativa cooperativa);
}