package ucb.edu.bo.sumajflow.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ucb.edu.bo.sumajflow.entity.Transportista;
import ucb.edu.bo.sumajflow.entity.Usuarios;

import java.util.Optional;

/**
 * Repository para transportistas con queries personalizados
 */
@Repository
public interface TransportistaRepository extends JpaRepository<Transportista, Integer> {

  /**
   * Buscar transportista por usuario
   */
  Optional<Transportista> findByUsuariosId(Usuarios usuario);

  /**
   * Buscar transportista por número de celular (a través de usuarios)
   */
  @Query("SELECT t FROM Transportista t " +
          "JOIN Usuarios u ON t.usuariosId = u " +
          "JOIN Persona p ON p.usuariosId = u " +
          "WHERE p.numeroCelular = :celular")
  Optional<Transportista> findByNumeroCelular(@Param("celular") String celular);

  /**
   * Buscar transportista por placa
   */
  Optional<Transportista> findByPlacaVehiculo(String placa);

  /**
   * Listar transportistas con filtros
   * Esta query busca transportistas que han sido invitados por esta cooperativa
   */
  @Query("SELECT DISTINCT t FROM InvitacionTransportista t " +
          "LEFT JOIN Transportista i ON i.invitacionTransportista = t " +
          "WHERE t.cooperativaId.id = :cooperativaId " +
          "AND (:estado IS NULL OR i.estado = :estado) " +
          "AND (:busqueda IS NULL OR " +
          "     LOWER(i.usuariosId.correo) LIKE LOWER(CONCAT('%', :busqueda, '%')))")
  Page<Transportista> findByCooperativaWithFilters(
          @Param("cooperativaId") Integer cooperativaId,
          @Param("estado") String estado,
          @Param("busqueda") String busqueda,
          Pageable pageable
  );

  /**
   * Contar transportistas por estado para una cooperativa
   */
  @Query("SELECT COUNT(DISTINCT t) FROM InvitacionTransportista t " +
          "LEFT JOIN Transportista i ON i.invitacionTransportista = t " +
          "WHERE t.cooperativaId.id = :cooperativaId " +
          "AND i.estado = :estado")
  Long countByCooperativaAndEstado(
          @Param("cooperativaId") Integer cooperativaId,
          @Param("estado") String estado
  );

  /**
   * Verificar si existe transportista con CI
   */
  boolean existsByCi(String ci);

  /**
   * Verificar si existe transportista con placa
   */
  boolean existsByPlacaVehiculo(String placa);
}