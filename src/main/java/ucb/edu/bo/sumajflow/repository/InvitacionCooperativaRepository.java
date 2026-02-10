package ucb.edu.bo.sumajflow.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ucb.edu.bo.sumajflow.entity.Cooperativa;
import ucb.edu.bo.sumajflow.entity.InvitacionCooperativa;
import ucb.edu.bo.sumajflow.entity.InvitacionTransportista;

import java.util.List;
import java.util.Optional;

@Repository
public interface InvitacionCooperativaRepository extends JpaRepository<InvitacionCooperativa, Integer> {

    /**
     * Buscar por cooperativa e invitación
     */
    Optional<InvitacionCooperativa> findByCooperativaAndInvitacionTransportista(
            Cooperativa cooperativa,
            InvitacionTransportista invitacion
    );

    /**
     * Buscar todas las invitaciones de una cooperativa
     */
    List<InvitacionCooperativa> findByCooperativa(Cooperativa cooperativa);

    /**
     * Buscar todas las cooperativas que hicieron una invitación
     */
    List<InvitacionCooperativa> findByInvitacionTransportista(InvitacionTransportista invitacion);

    /**
     * Verificar si existe relación
     */
    boolean existsByCooperativaAndInvitacionTransportista(
            Cooperativa cooperativa,
            InvitacionTransportista invitacion
    );

    /**
     * Contar invitaciones de una cooperativa
     */
    long countByCooperativa(Cooperativa cooperativa);

    /**
     * Eliminar por cooperativa e invitación
     */
    void deleteByCooperativaAndInvitacionTransportista(
            Cooperativa cooperativa,
            InvitacionTransportista invitacion
    );

}