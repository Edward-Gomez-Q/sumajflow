package ucb.edu.bo.sumajflow.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "usuarios")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {
        "tipoUsuarioId",
        "persona",
        "cooperativa",
        "socio",
        "ingenioMinero",
        "comercializadora",
        "transportista",
        "auditoriaList",
        "notificacionesList"
})
public class Usuarios implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @NotNull
    @Size(min = 1, max = 50)
    @Column(name = "correo", nullable = false, length = 50, unique = true)
    private String correo;

    @NotNull
    @Size(min = 1, max = 250)
    @Column(name = "contrasena", nullable = false, length = 250)
    private String contrasena;

    // Auditoría
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // Relaciones
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tipo_usuario_id", nullable = false)
    private TipoUsuario tipoUsuarioId;

    // Relaciones uno-a-uno (cada usuario puede tener solo una de estas)
    @OneToOne(mappedBy = "usuariosId", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private Persona persona;

    @OneToOne(mappedBy = "usuariosId", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private Cooperativa cooperativa;

    @OneToOne(mappedBy = "usuariosId", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private Socio socio;

    @OneToOne(mappedBy = "usuariosId", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private IngenioMinero ingenioMinero;

    @OneToOne(mappedBy = "usuariosId", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private Comercializadora comercializadora;

    @OneToOne(mappedBy = "usuariosId", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private Transportista transportista;

    // Otras relaciones
    @OneToMany(mappedBy = "usuariosId", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<Auditoria> auditoriaList = new ArrayList<>();

    @OneToMany(mappedBy = "usuariosId", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    @Builder.Default
    private List<Notificacion> notificacionesList = new ArrayList<>();

    // Métodos helper
    public void addAuditoria(Auditoria auditoria) {
        auditoriaList.add(auditoria);
        auditoria.setUsuariosId(this);
    }

    public void removeAuditoria(Auditoria auditoria) {
        auditoriaList.remove(auditoria);
        auditoria.setUsuariosId(null);
    }

    public void addNotificacion(Notificacion notificacion) {
        notificacionesList.add(notificacion);
        notificacion.setUsuariosId(this);
    }

    public void removeNotificacion(Notificacion notificacion) {
        notificacionesList.remove(notificacion);
        notificacion.setUsuariosId(null);
    }
}