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
@Table(name = "ingenio_minero")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"usuariosId", "plantaList", "almacenesIngenioList", "balanzasIngenioList", "concentradoList", "loteIngenioList"})
public class IngenioMinero implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @NotNull
    @Size(min = 1, max = 100)
    @Column(name = "razon_social", nullable = false, length = 100)
    private String razonSocial;

    @NotNull
    @Size(min = 1, max = 50)
    @Column(name = "nit", nullable = false, length = 50)
    private String nit;

    @NotNull
    @Column(name = "nim", nullable = false)
    private Integer nim;

    @NotNull
    @Size(min = 1, max = 50)
    @Column(name = "correo_contacto", nullable = false, length = 50)
    private String correoContacto;

    @Size(max = 50)
    @Column(name = "numero_telefono_fijo", length = 50)
    private String numeroTelefonoFijo;

    @Size(max = 50)
    @Column(name = "numero_telefono_movil", length = 50)
    private String numeroTelefonoMovil;

    @NotNull
    @Size(min = 1, max = 50)
    @Column(name = "departamento", nullable = false, length = 50)
    private String departamento;

    @NotNull
    @Size(min = 1, max = 100)
    @Column(name = "provincia", nullable = false, length = 100)
    private String provincia;

    @NotNull
    @Size(min = 1, max = 100)
    @Column(name = "municipio", nullable = false, length = 100)
    private String municipio;

    @NotNull
    @Size(min = 1, max = 250)
    @Column(name = "direccion", nullable = false, length = 250)
    private String direccion;

    @Column(name = "latitud", precision = 10, scale = 7)
    private BigDecimal latitud;

    @Column(name = "longitud", precision = 10, scale = 7)
    private BigDecimal longitud;

    // Auditoría
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // Relaciones
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuarios_id", nullable = false)
    private Usuarios usuariosId;

    @OneToMany(mappedBy = "ingenioMineroId", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    @Builder.Default
    private List<Planta> plantaList = new ArrayList<>();

    @OneToMany(mappedBy = "ingenioMineroId", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    @Builder.Default
    private List<AlmacenIngenio> almacenesIngenioList = new ArrayList<>();

    @OneToMany(mappedBy = "ingenioMineroId", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    @Builder.Default
    private List<BalanzaIngenio> balanzasIngenioList = new ArrayList<>();

    @OneToMany(mappedBy = "ingenioMineroId", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    @Builder.Default
    private List<Concentrado> concentradoList = new ArrayList<>();

    @OneToMany(mappedBy = "ingenioMineroId", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    @Builder.Default
    private List<LoteIngenio> loteIngenioList = new ArrayList<>();

    // Métodos helper para sincronización bidireccional
    public void addPlanta(Planta planta) {
        plantaList.add(planta);
        planta.setIngenioMineroId(this);
    }

    public void removePlanta(Planta planta) {
        plantaList.remove(planta);
        planta.setIngenioMineroId(null);
    }

    public void addAlmacen(AlmacenIngenio almacen) {
        almacenesIngenioList.add(almacen);
        almacen.setIngenioMineroId(this);
    }

    public void removeAlmacen(AlmacenIngenio almacen) {
        almacenesIngenioList.remove(almacen);
        almacen.setIngenioMineroId(null);
    }

    public void addBalanza(BalanzaIngenio balanza) {
        balanzasIngenioList.add(balanza);
        balanza.setIngenioMineroId(this);
    }

    public void removeBalanza(BalanzaIngenio balanza) {
        balanzasIngenioList.remove(balanza);
        balanza.setIngenioMineroId(null);
    }

    public void addConcentrado(Concentrado concentrado) {
        concentradoList.add(concentrado);
        concentrado.setIngenioMineroId(this);
    }

    public void removeConcentrado(Concentrado concentrado) {
        concentradoList.remove(concentrado);
        concentrado.setIngenioMineroId(null);
    }

    public void addLoteIngenio(LoteIngenio loteIngenio) {
        loteIngenioList.add(loteIngenio);
        loteIngenio.setIngenioMineroId(this);
    }

    public void removeLoteIngenio(LoteIngenio loteIngenio) {
        loteIngenioList.remove(loteIngenio);
        loteIngenio.setIngenioMineroId(null);
    }
}