// src/main/java/ucb/edu/bo/sumajflow/repository/SectoresRepository.java
package ucb.edu.bo.sumajflow.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ucb.edu.bo.sumajflow.entity.Cooperativa;
import ucb.edu.bo.sumajflow.entity.Sectores;

import java.util.List;
import java.util.Optional;

public interface SectoresRepository extends JpaRepository<Sectores, Integer> {
  /**
   * Obtiene todos los sectores activos de una cooperativa
   */
  @Query("SELECT s FROM Sectores s WHERE s.cooperativaId = :cooperativa AND s.estado = 'activo'")
  List<Sectores> findByCooperativaIdAndEstadoActivo(@Param("cooperativa") Cooperativa cooperativa);

  /**
   * Obtiene todos los sectores de una cooperativa (activos e inactivos)
   */
  List<Sectores> findByCooperativaId(Cooperativa cooperativa);

  /**
   * Busca un sector por ID y verifica que esté activo
   */
  @Query("SELECT s FROM Sectores s WHERE s.id = :id AND s.estado = 'activo'")
  Optional<Sectores> findByIdAndEstadoActivo(@Param("id") Integer id);

  /**
   * Busca un sector activo por nombre y cooperativa
   */
  @Query("SELECT s FROM Sectores s WHERE s.nombre = :nombre AND s.cooperativaId = :cooperativa AND s.estado = 'activo'")
  Optional<Sectores> findByNombreAndCooperativaIdAndEstadoActivo(
          @Param("nombre") String nombre,
          @Param("cooperativa") Cooperativa cooperativa
  );

  /**
   * Busca un sector activo por color y cooperativa
   */
  @Query("SELECT s FROM Sectores s WHERE s.color = :color AND s.cooperativaId = :cooperativa AND s.estado = 'activo'")
  Optional<Sectores> findByColorAndCooperativaIdAndEstadoActivo(
          @Param("color") String color,
          @Param("cooperativa") Cooperativa cooperativa
  );

  /**
   * Verifica si existe un sector activo con el nombre dado en la cooperativa
   */
  @Query("SELECT CASE WHEN COUNT(s) > 0 THEN true ELSE false END FROM Sectores s " +
          "WHERE s.nombre = :nombre AND s.cooperativaId = :cooperativa AND s.estado = 'activo'")
  boolean existsByNombreAndCooperativaIdAndEstadoActivo(
          @Param("nombre") String nombre,
          @Param("cooperativa") Cooperativa cooperativa
  );

  /**
   * Verifica si existe un sector activo con el color dado en la cooperativa
   */
  @Query("SELECT CASE WHEN COUNT(s) > 0 THEN true ELSE false END FROM Sectores s " +
          "WHERE s.color = :color AND s.cooperativaId = :cooperativa AND s.estado = 'activo'")
  boolean existsByColorAndCooperativaIdAndEstadoActivo(
          @Param("color") String color,
          @Param("cooperativa") Cooperativa cooperativa
  );

  /**
   * @deprecated Usar findByNombreAndCooperativaIdAndEstadoActivo
   */
  @Deprecated
  Optional<Sectores> findByNombreAndCooperativaId(String nombre, Cooperativa cooperativa);

  /**
   * @deprecated Usar findByColorAndCooperativaIdAndEstadoActivo
   */
  @Deprecated
  Optional<Sectores> findByColorAndCooperativaId(String color, Cooperativa cooperativa);

  /**
   * @deprecated Usar existsByNombreAndCooperativaIdAndEstadoActivo
   */
  @Deprecated
  boolean existsByNombreAndCooperativaId(String nombre, Cooperativa cooperativa);

  /**
   * @deprecated Usar existsByColorAndCooperativaIdAndEstadoActivo
   */
  @Deprecated
  boolean existsByColorAndCooperativaId(String color, Cooperativa cooperativa);

  // ==================== CONSULTAS DE ESTADÍSTICAS ====================

  /**
   * Cuenta sectores activos de una cooperativa
   */
  @Query("SELECT COUNT(s) FROM Sectores s WHERE s.cooperativaId = :cooperativa AND s.estado = 'activo'")
  long countByCooperativaIdAndEstadoActivo(@Param("cooperativa") Cooperativa cooperativa);

  /**
   * Cuenta sectores inactivos de una cooperativa
   */
  @Query("SELECT COUNT(s) FROM Sectores s WHERE s.cooperativaId = :cooperativa AND s.estado = 'inactivo'")
  long countByCooperativaIdAndEstadoInactivo(@Param("cooperativa") Cooperativa cooperativa);
}