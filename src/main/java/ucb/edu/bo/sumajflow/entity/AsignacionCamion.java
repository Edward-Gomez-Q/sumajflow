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
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "asignacion_camion")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"lotesId", "transportistaId", "pesajesList"})
public class AsignacionCamion implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @NotNull
    @Column(name = "numero_camion", nullable = false)
    private Integer numeroCamion;

    @Size(max = 50)
    @Column(name = "estado", length = 50)
    private String estado;

    @Column(name = "fecha_asignacion")
    private LocalDateTime fechaAsignacion;

    @Column(name = "fecha_inicio")
    private LocalDateTime fechaInicio;

    @Column(name = "fecha_fin")
    private LocalDateTime fechaFin;

    @Column(name = "peso_bruto", precision = 12, scale = 2)
    private BigDecimal pesoBruto;

    @Column(name = "peso_neto", precision = 12, scale = 2)
    private BigDecimal pesoNeto;

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
    @JoinColumn(name = "lotes_id", nullable = false)
    private Lotes lotesId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "transportista_id", nullable = false)
    private Transportista transportistaId;

    @OneToMany(mappedBy = "asignacionCamionId", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    @Builder.Default
    private List<Pesajes> pesajesList = new ArrayList<>();

    // Método helper para sincronización bidireccional
    public void addPesaje(Pesajes pesaje) {
        pesajesList.add(pesaje);
        pesaje.setAsignacionCamionId(this);
    }

    public void removePesaje(Pesajes pesaje) {
        pesajesList.remove(pesaje);
        pesaje.setAsignacionCamionId(null);
    }

    @PrePersist
    protected void onCreate() {
        if (fechaAsignacion == null) {
            fechaAsignacion = LocalDateTime.now();
        }
        if (estado == null) {
            estado = "asignado";
        }
    }
}