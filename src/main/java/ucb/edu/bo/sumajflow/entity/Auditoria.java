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
@Table(name = "auditoria")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = "usuariosId")
public class Auditoria implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Size(max = 50)
    @Column(name = "tipo_usuario", length = 50)
    private String tipoUsuario;

    @NotNull
    @Size(min = 1, max = 100)
    @Column(name = "tabla_afectada", nullable = false, length = 100)
    private String tablaAfectada;

    @NotNull
    @Size(min = 1, max = 20)
    @Column(name = "accion", nullable = false, length = 20)
    private String accion;

    @Column(name = "registro_id")
    private Integer registroId;

    @Column(name = "datos_anteriores", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private String datosAnteriores;

    @Column(name = "datos_nuevos", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private String datosNuevos;

    @Column(name = "campos_modificados", columnDefinition = "text")
    private String camposModificados;

    @Size(max = 45)
    @Column(name = "ip_origen", length = 45)
    private String ipOrigen;

    @Column(name = "user_agent", columnDefinition = "text")
    private String userAgent;

    @Size(max = 10)
    @Column(name = "metodo_http", length = 10)
    private String metodoHttp;

    @Size(max = 255)
    @Column(name = "endpoint", length = 255)
    private String endpoint;

    @NotNull
    @Column(name = "fecha_operacion", nullable = false)
    private LocalDateTime fechaOperacion;

    @Column(name = "descripcion", columnDefinition = "text")
    private String descripcion;

    @Size(max = 20)
    @Column(name = "nivel_criticidad", length = 20)
    private String nivelCriticidad;

    @Size(max = 50)
    @Column(name = "modulo", length = 50)
    private String modulo;

    @Column(name = "operacion_exitosa")
    private Boolean operacionExitosa;

    @Column(name = "mensaje_error", columnDefinition = "text")
    private String mensajeError;

    // Auditoría (solo updated_at)
    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // Relaciones
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuarios_id")
    private Usuarios usuariosId;

    @PrePersist
    protected void onCreate() {
        if (fechaOperacion == null) {
            fechaOperacion = LocalDateTime.now();
        }
        if (nivelCriticidad == null) {
            nivelCriticidad = "medio";
        }
        if (operacionExitosa == null) {
            operacionExitosa = true;
        }
    }
}