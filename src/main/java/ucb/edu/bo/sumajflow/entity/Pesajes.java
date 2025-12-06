package ucb.edu.bo.sumajflow.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "pesajes")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = "asignacionCamionId")
public class Pesajes implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @NotNull
    @Size(min = 1, max = 50)
    @Column(name = "tipo_pesaje", nullable = false, length = 50)
    private String tipoPesaje;

    @NotNull
    @Column(name = "peso_bruto", nullable = false, precision = 12, scale = 2)
    private BigDecimal pesoBruto;

    @NotNull
    @Column(name = "peso_tara", nullable = false, precision = 12, scale = 2)
    private BigDecimal pesoTara;

    @Column(name = "peso_neto", precision = 12, scale = 2)
    private BigDecimal pesoNeto;

    @Column(name = "fecha_pesaje")
    private LocalDateTime fechaPesaje;

    @Size(max = 255)
    @Column(name = "observaciones", length = 255)
    private String observaciones;

    // Auditoría
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // Relaciones
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "asignacion_camion_id", nullable = false)
    private AsignacionCamion asignacionCamionId;

    @PrePersist
    protected void onCreate() {
        if (fechaPesaje == null) {
            fechaPesaje = LocalDateTime.now();
        }
        // Calcular peso neto automáticamente
        if (pesoNeto == null && pesoBruto != null && pesoTara != null) {
            pesoNeto = pesoBruto.subtract(pesoTara);
        }
    }

    @PreUpdate
    protected void onUpdate() {
        // Recalcular peso neto si cambian bruto o tara
        if (pesoBruto != null && pesoTara != null) {
            pesoNeto = pesoBruto.subtract(pesoTara);
        }
    }
}