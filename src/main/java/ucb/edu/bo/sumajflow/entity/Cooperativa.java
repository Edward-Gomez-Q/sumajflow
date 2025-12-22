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
@Table(name = "cooperativa")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"usuariosId", "sectoresList", "cooperativaSocioList", "balanzaCooperativaList"})
public class Cooperativa implements Serializable {

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
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuarios_id", nullable = false)
    private Usuarios usuariosId;

    @OneToMany(mappedBy = "cooperativaId", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    @Builder.Default
    private List<Sectores> sectoresList = new ArrayList<>();

    @OneToMany(mappedBy = "cooperativaId", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    @Builder.Default
    private List<CooperativaSocio> cooperativaSocioList = new ArrayList<>();

    @OneToMany(mappedBy = "cooperativaId", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    @Builder.Default
    private List<BalanzaCooperativa> balanzaCooperativaList = new ArrayList<>();

    // Métodos helper para sincronización bidireccional
    public void addSector(Sectores sector) {
        sectoresList.add(sector);
        sector.setCooperativaId(this);
    }

    public void removeSector(Sectores sector) {
        sectoresList.remove(sector);
        sector.setCooperativaId(null);
    }

    public void addCooperativaSocio(CooperativaSocio cooperativaSocio) {
        cooperativaSocioList.add(cooperativaSocio);
        cooperativaSocio.setCooperativaId(this);
    }

    public void removeCooperativaSocio(CooperativaSocio cooperativaSocio) {
        cooperativaSocioList.remove(cooperativaSocio);
        cooperativaSocio.setCooperativaId(null);
    }

    public void addBalanza(BalanzaCooperativa balanza) {
        balanzaCooperativaList.add(balanza);
        balanza.setCooperativaId(this);
    }

    public void removeBalanza(BalanzaCooperativa balanza) {
        balanzaCooperativaList.remove(balanza);
        balanza.setCooperativaId(null);
    }
}