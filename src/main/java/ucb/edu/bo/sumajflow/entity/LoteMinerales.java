package ucb.edu.bo.sumajflow.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.io.Serializable;
import java.time.LocalDateTime;

@Entity
@Table(name = "lote_minerales")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"lotesId", "mineralesId"})
public class LoteMinerales implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    // Auditoría
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // Relaciones (muchos a muchos)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "lotes_id", nullable = false)
    private Lotes lotesId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "minerales_id", nullable = false)
    private Minerales mineralesId;
}