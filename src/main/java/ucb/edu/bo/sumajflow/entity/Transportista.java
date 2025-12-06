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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "transportista")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"usuariosId", "asignacionCamionList"})
public class Transportista implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @NotNull
    @Size(min = 1, max = 20)
    @Column(name = "ci", nullable = false, length = 20)
    private String ci;

    @NotNull
    @Size(min = 1, max = 50)
    @Column(name = "licencia_conducir", nullable = false, length = 50)
    private String licenciaConducir;

    @NotNull
    @Size(min = 1, max = 10)
    @Column(name = "categoria_licencia", nullable = false, length = 10)
    private String categoriaLicencia;

    @NotNull
    @Column(name = "fecha_vencimiento_licencia", nullable = false)
    private LocalDate fechaVencimientoLicencia;

    @NotNull
    @Size(min = 1, max = 20)
    @Column(name = "placa_vehiculo", nullable = false, length = 20)
    private String placaVehiculo;

    @Size(max = 50)
    @Column(name = "marca_vehiculo", length = 50)
    private String marcaVehiculo;

    @Size(max = 50)
    @Column(name = "modelo_vehiculo", length = 50)
    private String modeloVehiculo;

    @Size(max = 30)
    @Column(name = "color_vehiculo", length = 30)
    private String colorVehiculo;

    @Column(name = "peso_tara", precision = 12, scale = 2)
    private BigDecimal pesoTara;

    @Column(name = "capacidad_carga", precision = 12, scale = 2)
    private BigDecimal capacidadCarga;

    @Size(max = 50)
    @Column(name = "estado", length = 50)
    private String estado;

    @Column(name = "fecha_aprobacion")
    private LocalDateTime fechaAprobacion;

    @Column(name = "viajes_completados")
    private Integer viajesCompletados;

    @Column(name = "calificacion_promedio", precision = 3, scale = 2)
    private BigDecimal calificacionPromedio;

    // Auditoría
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // Relaciones
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuarios_id", nullable = false, unique = true)
    private Usuarios usuariosId;

    @OneToMany(mappedBy = "transportistaId", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    @Builder.Default
    private List<AsignacionCamion> asignacionCamionList = new ArrayList<>();

    // Métodos helper
    public void addAsignacionCamion(AsignacionCamion asignacion) {
        asignacionCamionList.add(asignacion);
        asignacion.setTransportistaId(this);
    }

    public void removeAsignacionCamion(AsignacionCamion asignacion) {
        asignacionCamionList.remove(asignacion);
        asignacion.setTransportistaId(null);
    }

    @PrePersist
    protected void onCreate() {
        if (estado == null) {
            estado = "pendiente";
        }
        if (viajesCompletados == null) {
            viajesCompletados = 0;
        }
        if (calificacionPromedio == null) {
            calificacionPromedio = BigDecimal.ZERO;
        }
    }
}