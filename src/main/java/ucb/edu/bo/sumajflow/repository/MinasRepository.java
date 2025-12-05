// src/main/java/ucb/edu/bo/sumajflow/repository/MinasRepository.java
package ucb.edu.bo.sumajflow.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ucb.edu.bo.sumajflow.entity.Minas;
import ucb.edu.bo.sumajflow.entity.Sectores;

import java.math.BigDecimal;
import java.util.List;

public interface MinasRepository extends JpaRepository<Minas, Integer> {

  /**
   * Obtiene todas las minas activas de un sector
   */
  @Query("SELECT m FROM Minas m WHERE m.sectoresId = :sector AND m.estado = 'activo'")
  List<Minas> findMinasActivasBySector(@Param("sector") Sectores sector);

  /**
   * Verifica si un sector tiene minas activas
   */
  @Query("SELECT CASE WHEN COUNT(m) > 0 THEN true ELSE false END FROM Minas m " +
          "WHERE m.sectoresId = :sector AND m.estado = 'activo'")
  boolean existsMinasActivasInSector(@Param("sector") Sectores sector);

  /**
   * Cuenta minas activas en un sector
   */
  @Query("SELECT COUNT(m) FROM Minas m WHERE m.sectoresId = :sector AND m.estado = 'activo'")
  long countMinasActivasBySector(@Param("sector") Sectores sector);

  /**
   * Obtiene todas las minas (activas e inactivas) de un sector
   */
  List<Minas> findBySectoresId(Sectores sector);

  /**
   * Verifica si una coordenada está dentro de un polígono usando Ray Casting Algorithm
   */
  @Query("SELECT m FROM Minas m WHERE m.sectoresId = :sector")
  List<Minas> findAllMinasBySector(@Param("sector") Sectores sector);
}