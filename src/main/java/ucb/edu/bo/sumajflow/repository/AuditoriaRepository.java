package ucb.edu.bo.sumajflow.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ucb.edu.bo.sumajflow.entity.Auditoria;
import ucb.edu.bo.sumajflow.entity.Usuarios;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AuditoriaRepository extends JpaRepository<Auditoria, Integer> {

  /**
   * Encuentra auditorías por usuario ordenadas por fecha descendente
   */
  List<Auditoria> findByUsuariosIdOrderByFechaOperacionDesc(Usuarios usuario);

  /**
   * Encuentra las N auditorías más recientes de un usuario
   */
  @Query("SELECT a FROM Auditoria a WHERE a.usuariosId.id = :usuarioId " +
          "ORDER BY a.fechaOperacion DESC")
  List<Auditoria> findTopNByUsuarioIdOrderByFechaOperacionDesc(
          @Param("usuarioId") Integer usuarioId,
          @Param("limite") int limite
  );

  /**
   * Encuentra auditorías por tabla y acción en un rango de fechas
   */
  @Query("SELECT a FROM Auditoria a WHERE a.tablaAfectada = :tabla " +
          "AND a.accion = :accion " +
          "AND a.fechaOperacion BETWEEN :fechaInicio AND :fechaFin " +
          "ORDER BY a.fechaOperacion DESC")
  List<Auditoria> findByTablaAndAccionAndFechaRange(
          @Param("tabla") String tabla,
          @Param("accion") String accion,
          @Param("fechaInicio") LocalDateTime fechaInicio,
          @Param("fechaFin") LocalDateTime fechaFin
  );

  /**
   * Encuentra auditorías en un rango de fechas
   */
  List<Auditoria> findByFechaOperacionBetween(
          LocalDateTime fechaInicio,
          LocalDateTime fechaFin
  );

  /**
   * Encuentra auditorías por tabla
   */
  List<Auditoria> findByTablaAfectadaOrderByFechaOperacionDesc(String tabla);

  /**
   * Encuentra auditorías por nivel de criticidad
   */
  List<Auditoria> findByNivelCriticidadOrderByFechaOperacionDesc(String nivelCriticidad);

  /**
   * Encuentra auditorías por registro específico
   */
  List<Auditoria> findByTablaAfectadaAndRegistroIdOrderByFechaOperacionDesc(
          String tabla,
          Integer registroId
  );

  /**
   * Cuenta operaciones por usuario en un período
   */
  @Query("SELECT COUNT(a) FROM Auditoria a WHERE a.usuariosId.id = :usuarioId " +
          "AND a.fechaOperacion BETWEEN :fechaInicio AND :fechaFin")
  Long countByUsuarioIdAndFechaBetween(
          @Param("usuarioId") Integer usuarioId,
          @Param("fechaInicio") LocalDateTime fechaInicio,
          @Param("fechaFin") LocalDateTime fechaFin
  );

  /**
   * Encuentra auditorías fallidas
   */
  List<Auditoria> findByOperacionExitosaFalseOrderByFechaOperacionDesc();

  /**
   * Encuentra auditorías por IP de origen
   */
  List<Auditoria> findByIpOrigenOrderByFechaOperacionDesc(String ipOrigen);

  /**
   * Encuentra auditorías por módulo
   */
  List<Auditoria> findByModuloOrderByFechaOperacionDesc(String modulo);

  /**
   * Cuenta auditorías por tabla y acción
   */
  Long countByTablaAfectadaAndAccion(String tabla, String accion);

  /**
   * Encuentra auditorías críticas recientes (últimas 24 horas)
   */
  @Query("SELECT a FROM Auditoria a WHERE a.nivelCriticidad = 'CRÍTICO' " +
          "AND a.fechaOperacion >= :hace24Horas " +
          "ORDER BY a.fechaOperacion DESC")
  List<Auditoria> findAuditoriasCriticasRecientes(
          @Param("hace24Horas") LocalDateTime hace24Horas
  );

  /**
   * Estadísticas de operaciones por tipo de usuario
   */
  @Query("SELECT a.tipoUsuario, COUNT(a) FROM Auditoria a " +
          "WHERE a.fechaOperacion BETWEEN :fechaInicio AND :fechaFin " +
          "GROUP BY a.tipoUsuario")
  List<Object[]> countOperacionesPorTipoUsuario(
          @Param("fechaInicio") LocalDateTime fechaInicio,
          @Param("fechaFin") LocalDateTime fechaFin
  );

  /**
   * Encuentra últimas operaciones de un registro específico
   */
  @Query("SELECT a FROM Auditoria a WHERE a.tablaAfectada = :tabla " +
          "AND a.registroId = :registroId " +
          "ORDER BY a.fechaOperacion DESC")
  List<Auditoria> findHistorialRegistro(
          @Param("tabla") String tabla,
          @Param("registroId") Integer registroId
  );
}