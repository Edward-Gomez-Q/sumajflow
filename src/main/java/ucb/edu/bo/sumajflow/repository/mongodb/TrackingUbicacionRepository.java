package ucb.edu.bo.sumajflow.repository.mongodb;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.mongodb.repository.Update;
import org.springframework.stereotype.Repository;
import ucb.edu.bo.sumajflow.document.TrackingUbicacion;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repositorio MongoDB para operaciones de tracking de ubicaciones
 */
@Repository
public interface TrackingUbicacionRepository extends MongoRepository<TrackingUbicacion, String> {

    /**
     * Buscar tracking por asignación de camión
     */
    Optional<TrackingUbicacion> findByAsignacionCamionId(Integer asignacionCamionId);

    /**
     * Buscar todos los trackings de un lote
     */
    List<TrackingUbicacion> findByLoteId(Integer loteId);

    /**
     * Buscar trackings activos de un transportista
     */
    @Query("{ 'transportistaId': ?0, 'estadoViaje': { $nin: ['completado', 'cancelado'] } }")
    List<TrackingUbicacion> findActiveByTransportistaId(Integer transportistaId);

    /**
     * Buscar trackings por estado de viaje
     */
    List<TrackingUbicacion> findByEstadoViaje(String estadoViaje);

    /**
     * Buscar trackings activos (no completados ni cancelados)
     */
    @Query("{ 'estadoViaje': { $nin: ['completado', 'cancelado'] } }")
    List<TrackingUbicacion> findAllActive();

    /**
     * Buscar trackings de un lote que están activos
     */
    @Query("{ 'loteId': ?0, 'estadoViaje': { $nin: ['completado', 'cancelado'] } }")
    List<TrackingUbicacion> findActiveByLoteId(Integer loteId);

    /**
     * Verificar si existe tracking para una asignación
     */
    boolean existsByAsignacionCamionId(Integer asignacionCamionId);

    /**
     * Buscar trackings con ubicaciones pendientes de sincronizar
     */
    @Query("{ 'ubicacionesPendientesSincronizar': { $exists: true, $ne: [] } }")
    List<TrackingUbicacion> findWithPendingSync();

    /**
     * Buscar trackings offline (sin actualización reciente)
     */
    @Query("{ 'ultimaSincronizacion': { $lt: ?0 }, 'estadoViaje': { $nin: ['completado', 'cancelado'] } }")
    List<TrackingUbicacion> findOfflineTrackings(LocalDateTime threshold);

    /**
     * Buscar trackings cerca de una ubicación (para monitoreo)
     * Usa índice geoespacial
     */
    @Query("{ 'ubicacionActual.location': { $near: { $geometry: { type: 'Point', coordinates: [?1, ?0] }, $maxDistance: ?2 } } }")
    List<TrackingUbicacion> findNearLocation(Double lat, Double lng, Double maxDistanceMeters);

    /**
     * Contar trackings activos por lote
     */
    @Query(value = "{ 'loteId': ?0, 'estadoViaje': { $nin: ['completado', 'cancelado'] } }", count = true)
    long countActiveByLoteId(Integer loteId);

    /**
     * Eliminar tracking por asignación (para casos de cancelación)
     */
    void deleteByAsignacionCamionId(Integer asignacionCamionId);
}