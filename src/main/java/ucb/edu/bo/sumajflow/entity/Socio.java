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
@Table(name = "socio")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {
        "usuariosId",
        "cooperativaSocioList",
        "minasList",
        "concentradoList",
        "liquidacionList"
})
public class Socio implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @NotNull
    @Column(name = "fecha_envio", nullable = false)
    private LocalDateTime fechaEnvio;

    @NotNull
    @Size(min = 1, max = 100)
    @Column(name = "estado", nullable = false, length = 100)
    private String estado;

    @Size(max = 200)
    @Column(name = "carnet_afiliacion_url", length = 200)
    private String carnetAfiliacionUrl;

    @Size(max = 200)
    @Column(name = "carnet_identidad_url", length = 200)
    private String carnetIdentidadUrl;

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

    @OneToMany(mappedBy = "socioId", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    @Builder.Default
    private List<CooperativaSocio> cooperativaSocioList = new ArrayList<>();

    @OneToMany(mappedBy = "socioId", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    @Builder.Default
    private List<Minas> minasList = new ArrayList<>();

    @OneToMany(mappedBy = "socioPropietarioId", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<Concentrado> concentradoList = new ArrayList<>();

    @OneToMany(mappedBy = "socioId", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<Liquidacion> liquidacionList = new ArrayList<>();

    // Métodos helper
    public void addCooperativaSocio(CooperativaSocio cooperativaSocio) {
        cooperativaSocioList.add(cooperativaSocio);
        cooperativaSocio.setSocioId(this);
    }

    public void removeCooperativaSocio(CooperativaSocio cooperativaSocio) {
        cooperativaSocioList.remove(cooperativaSocio);
        cooperativaSocio.setSocioId(null);
    }

    public void addMina(Minas mina) {
        minasList.add(mina);
        mina.setSocioId(this);
    }

    public void removeMina(Minas mina) {
        minasList.remove(mina);
        mina.setSocioId(null);
    }

    public void addConcentrado(Concentrado concentrado) {
        concentradoList.add(concentrado);
        concentrado.setSocioPropietarioId(this);
    }

    public void removeConcentrado(Concentrado concentrado) {
        concentradoList.remove(concentrado);
        concentrado.setSocioPropietarioId(null);
    }

    public void addLiquidacion(Liquidacion liquidacion) {
        liquidacionList.add(liquidacion);
        liquidacion.setSocioId(this);
    }

    public void removeLiquidacion(Liquidacion liquidacion) {
        liquidacionList.remove(liquidacion);
        liquidacion.setSocioId(null);
    }

    @PrePersist
    protected void onCreate() {
        if (fechaEnvio == null) {
            fechaEnvio = LocalDateTime.now();
        }
        if (estado == null) {
            estado = "pendiente";
        }
    }
}