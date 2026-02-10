package ucb.edu.bo.sumajflow.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ucb.edu.bo.sumajflow.entity.Minas;
import ucb.edu.bo.sumajflow.entity.Sectores;
import ucb.edu.bo.sumajflow.entity.Socio;

import java.util.List;
import java.util.Optional;

@Repository
public interface MinasRepository extends JpaRepository<Minas, Integer> {

  /**
   * Encuentra minas ACTIVAS de un socio
   */
  @Query("SELECT m FROM Minas m WHERE m.socioId = :socio AND m.estado = 'activo'")
  List<Minas> findByActiveSocio(@Param("socio") Socio socio);

  /**
   * Encuentra una mina por ID solo si está ACTIVA
   */
  @Query("SELECT m FROM Minas m WHERE m.id = :id AND m.estado = 'activo'")
  Optional<Minas> findByIdAndEstadoActivo(@Param("id") Integer id);

  /**
   * Encuentra minas ACTIVAS en un sector específico
   */
  @Query("SELECT m FROM Minas m WHERE m.sectoresId = :sector AND m.estado = 'activo'")
  List<Minas> findMinasActivasBySector(@Param("sector") Sectores sector);

  /**
   * Verifica si existen minas ACTIVAS en un sector
   */
  @Query("SELECT CASE WHEN COUNT(m) > 0 THEN true ELSE false END " +
          "FROM Minas m WHERE m.sectoresId = :sector AND m.estado = 'activo'")
  boolean existsMinasActivasInSector(@Param("sector") Sectores sector);

  /**
   * Cuenta minas ACTIVAS en un sector
   */
  @Query("SELECT COUNT(m) FROM Minas m WHERE m.sectoresId = :sector AND m.estado = 'activo'")
  long countMinasActivasBySector(@Param("sector") Sectores sector);

  /**
   * Verifica si ya existe una mina ACTIVA con el mismo nombre para un socio
   */
  @Query("SELECT m FROM Minas m WHERE m.nombre = :nombre AND m.socioId = :socio AND m.estado = 'activo'")
  Optional<Minas> findByNombreAndActiveSocio(@Param("nombre") String nombre, @Param("socio") Socio socio);

  /**
   * Cuenta minas ACTIVAS de un socio
   */
  @Query("SELECT COUNT(m) FROM Minas m WHERE m.socioId = :socio AND m.estado = 'activo'")
  long countMinasActivasBySocio(@Param("socio") Socio socio);

  /**
   * Cuenta minas INACTIVAS de un socio
   */
  @Query("SELECT COUNT(m) FROM Minas m WHERE m.socioId = :socio AND m.estado = 'inactivo'")
  long countMinasInactivasBySocio(@Param("socio") Socio socio);

  /**
   * Encuentra todas las minas (activas e inactivas) de un socio
   */
  @Query("SELECT m FROM Minas m WHERE m.socioId = :socio ORDER BY m.estado DESC, m.createdAt DESC")
  List<Minas> findAllBySocio(@Param("socio") Socio socio);

  /**
   * Verifica si una mina tiene lotes ACTIVOS asociados
   * Estados de lote considerados ACTIVOS para esta validación:
   * - pendiente
   * - esperando_aprobacion_destino
   * - en_proceso
   * - en_camino_mina
   * - en_camino_balanza_cooperativa
   * - en_camino_balanza_destino
   * - en_camino_almacen
   * - transporte_terminado
   * - planta (todos los subprocesos)
   */
  @Query("SELECT CASE WHEN COUNT(l) > 0 THEN true ELSE false END " +
          "FROM Lotes l WHERE l.minasId = :mina AND l.estado NOT IN ('completado', 'rechazado', 'cancelado')")
  boolean hasLotesActivos(@Param("mina") Minas mina);

  /**
   * Cuenta lotes activos de una mina
   */
  @Query("SELECT COUNT(l) FROM Lotes l WHERE l.minasId = :mina AND l.estado NOT IN ('completado', 'rechazado', 'cancelado')")
  long countLotesActivosByMina(@Param("mina") Minas mina);

  List<Minas> findBySectoresId(Sectores sectoresId);
}