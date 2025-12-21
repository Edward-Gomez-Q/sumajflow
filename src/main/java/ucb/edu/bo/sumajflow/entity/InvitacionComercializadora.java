package ucb.edu.bo.sumajflow.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Tabla intermedia para relación many-to-many entre InvitacionTransportista y Comercializadora
 */
@Entity
@Table(name = "invitacion_comercializadora")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = {"comercializadora", "invitacionTransportista"})
public class InvitacionComercializadora {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "comercializadora_id", nullable = false)
    private Comercializadora comercializadora;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invitacion_transportista_id", nullable = false)
    private InvitacionTransportista invitacionTransportista;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}