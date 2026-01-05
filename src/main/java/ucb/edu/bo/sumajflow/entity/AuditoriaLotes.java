package ucb.edu.bo.sumajflow.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.io.Serializable;
import java.time.LocalDateTime;

@Entity
@Table(name = "auditoria_lotes")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = "loteId")
public class AuditoriaLotes implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Size(max = 50)
    @Column(name = "tipo_usuario", length = 50)
    private String tipoUsuario;

    @Size(max = 70)
    @Column(name = "estado_anterior", length = 70)
    private String estadoAnterior;

    @NotNull
    @Size(min = 1, max = 70)
    @Column(name = "estado_nuevo", nullable = false, length = 70)
    private String estadoNuevo;

    @NotNull
    @Size(min = 1, max = 50)
    @Column(name = "accion", nullable = false, length = 50)
    private String accion;

    @Column(name = "descripcion", columnDefinition = "text")
    private String descripcion;

    @Column(name = "observaciones", columnDefinition = "text")
    private String observaciones;

    @Column(name = "metadata", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private String metadata;

    @Size(max = 45)
    @Column(name = "ip_origen", length = 45)
    private String ipOrigen;

    @NotNull
    @Column(name = "fecha_registro", nullable = false)
    private LocalDateTime fechaRegistro;

    // Auditoría (solo updated_at)
    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // Relaciones
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "lote_id", nullable = false)
    private Lotes loteId;

    @PrePersist
    protected void onCreate() {
        if (fechaRegistro == null) {
            fechaRegistro = LocalDateTime.now();
        }
    }
}