package ucb.edu.bo.sumajflow.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "lote_concentrado_relacion")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"loteComplejoId", "concentradoId"})
public class LoteConcentradoRelacion implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "fecha_creacion")
    private LocalDateTime fechaCreacion;

    @NotNull
    @Column(name = "peso_entrada", nullable = false, precision = 12, scale = 2)
    private BigDecimal pesoEntrada;

    @Column(name = "peso_salida", precision = 12, scale = 2)
    private BigDecimal pesoSalida;

    @Column(name = "porcentaje_recuperacion", precision = 5, scale = 2)
    private BigDecimal porcentajeRecuperacion;

    // Auditoría (solo updated_at, ya tiene fecha_creacion)
    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // Relaciones
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "lote_complejo_id", nullable = false)
    private Lotes loteComplejoId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "concentrado_id", nullable = false)
    private Concentrado concentradoId;

    @PrePersist
    protected void onCreate() {
        if (fechaCreacion == null) {
            fechaCreacion = LocalDateTime.now();
        }
    }
}