package ucb.edu.bo.sumajflow.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ucb.edu.bo.sumajflow.entity.Concentrado;
import ucb.edu.bo.sumajflow.entity.IngenioMinero;
import ucb.edu.bo.sumajflow.entity.Socio;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ConcentradoRepository extends JpaRepository<Concentrado, Integer> {

  // Buscar por ingenio
  List<Concentrado> findByIngenioMineroIdOrderByCreatedAtDesc(IngenioMinero ingenio);
  // Buscar por socio propietario
  List<Concentrado> findBySocioPropietarioIdOrderByCreatedAtDesc(Socio socio);

  // Verificar si código ya existe
  @Query("SELECT CASE WHEN COUNT(c) > 0 THEN true ELSE false END FROM Concentrado c WHERE c.codigoConcentrado = :codigo")
  boolean existsByCodigo(@Param("codigo") String codigo);
}