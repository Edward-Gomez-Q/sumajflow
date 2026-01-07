package ucb.edu.bo.sumajflow.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ucb.edu.bo.sumajflow.entity.Transportista;
import ucb.edu.bo.sumajflow.entity.Usuarios;

import java.util.List;
import java.util.Optional;

@Repository
public interface TransportistaRepository extends JpaRepository<Transportista, Integer> {

    Optional<Transportista> findByUsuariosId(Usuarios usuario);

    Optional<Transportista> findByPlacaVehiculo(String placa);

    boolean existsByCi(String ci);

    boolean existsByPlacaVehiculo(String placa);

    @Query(value =
            "SELECT t.* FROM transportista t " +
                    "INNER JOIN invitacion_transportista it ON t.invitacion_transportista_id = it.id " +
                    "INNER JOIN invitacion_cooperativa ic ON it.id = ic.invitacion_transportista_id " +
                    "WHERE ic.cooperativa_id = :cooperativaId " +
                    "AND (:estado = '' OR t.estado = :estado) " +
                    "AND (:busqueda = '' OR " +
                    "     LOWER(t.placa_vehiculo) LIKE LOWER(:busquedaPattern) OR " +
                    "     LOWER(t.ci) LIKE LOWER(:busquedaPattern)) " +
                    "ORDER BY " +
                    "CASE WHEN :ordenarPor = 'created_at' AND :direccion = 'desc' THEN t.created_at END DESC, " +
                    "CASE WHEN :ordenarPor = 'created_at' AND :direccion = 'asc' THEN t.created_at END ASC, " +
                    "CASE WHEN :ordenarPor = 'placa_vehiculo' AND :direccion = 'desc' THEN t.placa_vehiculo END DESC, " +
                    "CASE WHEN :ordenarPor = 'placa_vehiculo' AND :direccion = 'asc' THEN t.placa_vehiculo END ASC, " +
                    "CASE WHEN :ordenarPor = 'viajes_completados' AND :direccion = 'desc' THEN t.viajes_completados END DESC, " +
                    "CASE WHEN :ordenarPor = 'viajes_completados' AND :direccion = 'asc' THEN t.viajes_completados END ASC, " +
                    "t.created_at DESC " +
                    "OFFSET :offset LIMIT :limit",
            nativeQuery = true)
    List<Transportista> findByCooperativaWithFiltersNative(
            @Param("cooperativaId") Integer cooperativaId,
            @Param("estado") String estado,
            @Param("busqueda") String busqueda,
            @Param("busquedaPattern") String busquedaPattern,
            @Param("ordenarPor") String ordenarPor,
            @Param("direccion") String direccion,
            @Param("offset") int offset,
            @Param("limit") int limit
    );


    /**
     * ✅ ACTUALIZADO: Contar total
     * Ahora usa JOIN con invitacion_cooperativa
     */
    @Query(value =
            "SELECT COUNT(DISTINCT t.id) FROM transportista t " +
                    "INNER JOIN invitacion_transportista it ON t.invitacion_transportista_id = it.id " +
                    "INNER JOIN invitacion_cooperativa ic ON it.id = ic.invitacion_transportista_id " +
                    "WHERE ic.cooperativa_id = :cooperativaId " +
                    "AND (:estado = '' OR t.estado = :estado) " +
                    "AND (:busqueda = '' OR " +
                    "     LOWER(t.placa_vehiculo) LIKE LOWER(:busquedaPattern) OR " +
                    "     LOWER(t.ci) LIKE LOWER(:busquedaPattern))",
            nativeQuery = true)
    Long countByCooperativaWithFilters(
            @Param("cooperativaId") Integer cooperativaId,
            @Param("estado") String estado,
            @Param("busqueda") String busqueda,
            @Param("busquedaPattern") String busquedaPattern
    );
    // Obtener transportistas disponibles para una cooperativa
    @Query("SELECT t FROM Transportista t " +
            "WHERE t.estado = 'aprobado' " +
            "AND EXISTS (" +
            "    SELECT 1 FROM InvitacionCooperativa ic " +
            "    WHERE ic.invitacionTransportista.id = t.invitacionTransportista.id " +
            "    AND ic.cooperativa.id = :cooperativaId" +
            ") " +
            "AND NOT EXISTS (" +
            "    SELECT 1 FROM AsignacionCamion ac " +
            "    WHERE ac.transportistaId.id = t.id " +
            "    AND ac.estado IN ('Esperando iniciar', 'En camino a la mina', 'Esperando carguío', " +
            "                      'En camino balanza cooperativa', 'En camino balanza destino', " +
            "                      'En camino almacén destino', 'Descargando')" +
            ") " +
            "ORDER BY t.calificacionPromedio DESC, t.viajesCompletados DESC")
    List<Transportista> findDisponiblesByCooperativa(@Param("cooperativaId") Integer cooperativaId);

    // Verificar si está disponible (no tiene viajes activos)
    @Query("SELECT CASE WHEN COUNT(ac) = 0 THEN true ELSE false END " +
            "FROM AsignacionCamion ac " +
            "WHERE ac.transportistaId.id = :transportistaId " +
            "AND ac.estado NOT IN ('viaje_terminado', 'cancelado')")
    boolean isDisponible(@Param("transportistaId") Integer transportistaId);
}