package ucb.edu.bo.sumajflow.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ucb.edu.bo.sumajflow.entity.Lotes;
import ucb.edu.bo.sumajflow.entity.Minas;

import java.util.List;
import java.util.Optional;

public interface LotesRepository extends JpaRepository<Lotes, Integer> {

  // Buscar lotes por mina
  List<Lotes> findByMinasId(Minas mina);

  // Buscar lote por ID con validación de estado no eliminado
  @Query("SELECT l FROM Lotes l WHERE l.id = :id AND l.estado != 'eliminado'")
  Optional<Lotes> findByIdAndNotDeleted(@Param("id") Integer id);

  // Contar lotes activos por mina (para validar eliminación de mina)
  @Query("SELECT COUNT(l) FROM Lotes l WHERE l.minasId = :mina AND l.estado NOT IN ('completado', 'rechazado', 'cancelado')")
  long countLotesActivosByMina(@Param("mina") Minas mina);

  // Verificar si hay lotes activos por mina
  @Query("SELECT CASE WHEN COUNT(l) > 0 THEN true ELSE false END FROM Lotes l WHERE l.minasId = :mina AND l.estado NOT IN ('completado', 'rechazado', 'cancelado')")
  boolean existsLotesActivosByMina(@Param("mina") Minas mina);
}