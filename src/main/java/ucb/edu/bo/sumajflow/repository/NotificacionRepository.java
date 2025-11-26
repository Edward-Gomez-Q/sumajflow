package ucb.edu.bo.sumajflow.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ucb.edu.bo.sumajflow.entity.Notificacion;
import ucb.edu.bo.sumajflow.entity.Usuarios;

import java.util.List;

public interface NotificacionRepository extends JpaRepository<Notificacion, Integer> {

    // Obtener notificaciones de un usuario ordenadas por fecha (más recientes primero)
    List<Notificacion> findByUsuariosIdOrderByFechaCreacionDesc(Usuarios usuario);

    // Obtener notificaciones no leídas
    List<Notificacion> findByUsuariosIdAndLeidoOrderByFechaCreacionDesc(Usuarios usuario, Boolean leido);

    // Obtener últimas N notificaciones
    List<Notificacion> findTop20ByUsuariosIdOrderByFechaCreacionDesc(Usuarios usuario);

    // Contar no leídas
    Long countByUsuariosIdAndLeido(Usuarios usuario, Boolean leido);

    // Marcar como leída
    @Modifying
    @Query("UPDATE Notificacion n SET n.leido = true, n.fechaLectura = CURRENT_TIMESTAMP WHERE n.id = :id")
    void marcarComoLeida(@Param("id") Integer id);

    // Marcar todas como leídas
    @Modifying
    @Query("UPDATE Notificacion n SET n.leido = true, n.fechaLectura = CURRENT_TIMESTAMP " +
            "WHERE n.usuariosId = :usuario AND n.leido = false")
    void marcarTodasComoLeidas(@Param("usuario") Usuarios usuario);
}
