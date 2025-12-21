package ucb.edu.bo.sumajflow.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ucb.edu.bo.sumajflow.entity.Comercializadora;
import ucb.edu.bo.sumajflow.entity.InvitacionComercializadora;
import ucb.edu.bo.sumajflow.entity.InvitacionTransportista;

import java.util.List;
import java.util.Optional;

@Repository
public interface InvitacionComercializadoraRepository extends JpaRepository<InvitacionComercializadora, Integer> {

    /**
     * Buscar por comercializadora e invitación
     */
    Optional<InvitacionComercializadora> findByComercializadoraAndInvitacionTransportista(
            Comercializadora comercializadora,
            InvitacionTransportista invitacion
    );

    /**
     * Buscar todas las invitaciones de una comercializadora
     */
    List<InvitacionComercializadora> findByComercializadora(Comercializadora comercializadora);

    /**
     * Buscar todas las comercializadoras que hicieron una invitación
     */
    List<InvitacionComercializadora> findByInvitacionTransportista(InvitacionTransportista invitacion);

    /**
     * Verificar si existe relación
     */
    boolean existsByComercializadoraAndInvitacionTransportista(
            Comercializadora comercializadora,
            InvitacionTransportista invitacion
    );

    /**
     * Contar invitaciones de una comercializadora
     */
    long countByComercializadora(Comercializadora comercializadora);

    /**
     * Eliminar por comercializadora e invitación
     */
    void deleteByComercializadoraAndInvitacionTransportista(
            Comercializadora comercializadora,
            InvitacionTransportista invitacion
    );
}