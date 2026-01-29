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

  // Buscar por código
  Optional<Concentrado> findByCodigoConcentrado(String codigoConcentrado);

  // Buscar por ingenio
  List<Concentrado> findByIngenioMineroIdOrderByCreatedAtDesc(IngenioMinero ingenio);

  // Buscar por socio propietario
  List<Concentrado> findBySocioPropietarioIdOrderByCreatedAtDesc(Socio socio);

  // Buscar por estado
  @Query("SELECT c FROM Concentrado c WHERE c.ingenioMineroId = :ingenio AND c.estado = :estado ORDER BY c.createdAt DESC")
  List<Concentrado> findByIngenioAndEstado(
          @Param("ingenio") IngenioMinero ingenio,
          @Param("estado") String estado
  );

  // Verificar si código ya existe
  @Query("SELECT CASE WHEN COUNT(c) > 0 THEN true ELSE false END FROM Concentrado c WHERE c.codigoConcentrado = :codigo")
  boolean existsByCodigo(@Param("codigo") String codigo);

  // Obtener concentrados activos (no completados ni vendidos)
  @Query("SELECT c FROM Concentrado c WHERE c.ingenioMineroId = :ingenio AND c.estado NOT IN ('vendido', 'liquidado') ORDER BY c.createdAt DESC")
  List<Concentrado> findActivosByIngenio(@Param("ingenio") IngenioMinero ingenio);

  // Paginación con filtros
  @Query("SELECT c FROM Concentrado c " +
          "WHERE c.ingenioMineroId = :ingenio " +
          "AND (:estado IS NULL OR c.estado = :estado) " +
          "AND (:mineralPrincipal IS NULL OR c.mineralPrincipal = :mineralPrincipal) " +
          "AND (CAST(:fechaDesde AS timestamp) IS NULL OR c.createdAt >= :fechaDesde) " +
          "AND (CAST(:fechaHasta AS timestamp) IS NULL OR c.createdAt <= :fechaHasta)")
  Page<Concentrado> findConcentradosConFiltros(
          @Param("ingenio") IngenioMinero ingenio,
          @Param("estado") String estado,
          @Param("mineralPrincipal") String mineralPrincipal,
          @Param("fechaDesde") LocalDateTime fechaDesde,
          @Param("fechaHasta") LocalDateTime fechaHasta,
          Pageable pageable
  );
}