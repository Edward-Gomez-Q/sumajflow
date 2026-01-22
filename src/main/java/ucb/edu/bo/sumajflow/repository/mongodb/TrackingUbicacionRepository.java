package ucb.edu.bo.sumajflow.repository.mongodb;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;
import ucb.edu.bo.sumajflow.document.TrackingUbicacion;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TrackingUbicacionRepository extends MongoRepository<TrackingUbicacion, String> {

    Optional<TrackingUbicacion> findByAsignacionCamionId(Integer asignacionCamionId);

    List<TrackingUbicacion> findByLoteId(Integer loteId);

    @Query("{ 'transportistaId': ?0, 'estadoViaje': { $nin: ['completado', 'cancelado'] } }")
    List<TrackingUbicacion> findActiveByTransportistaId(Integer transportistaId);

    List<TrackingUbicacion> findByEstadoViaje(String estadoViaje);

    @Query("{ 'estadoViaje': { $nin: ['completado', 'cancelado'] } }")
    List<TrackingUbicacion> findAllActive();

    @Query("{ 'loteId': ?0, 'estadoViaje': { $nin: ['completado', 'cancelado'] } }")
    List<TrackingUbicacion> findActiveByLoteId(Integer loteId);

    boolean existsByAsignacionCamionId(Integer asignacionCamionId);

    @Query("{ 'ultimaSincronizacion': { $lt: ?0 }, 'estadoViaje': { $nin: ['completado', 'cancelado'] } }")
    List<TrackingUbicacion> findOfflineTrackings(LocalDateTime threshold);

    @Query("{ 'ubicacionActual.location': { $near: { $geometry: { type: 'Point', coordinates: [?1, ?0] }, $maxDistance: ?2 } } }")
    List<TrackingUbicacion> findNearLocation(Double lat, Double lng, Double maxDistanceMeters);

    @Query(value = "{ 'loteId': ?0, 'estadoViaje': { $nin: ['completado', 'cancelado'] } }", count = true)
    long countActiveByLoteId(Integer loteId);

    void deleteByAsignacionCamionId(Integer asignacionCamionId);
}