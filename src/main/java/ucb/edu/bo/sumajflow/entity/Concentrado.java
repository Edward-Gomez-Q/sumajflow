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
@Table(name = "concentrado")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"ingenioMineroId", "loteOrigenId", "socioPropietarioId", "loteConcentradoRelacionList"})
public class Concentrado implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @NotNull
    @Size(min = 1, max = 50)
    @Column(name = "codigo_concentrado", nullable = false, length = 50)
    private String codigoConcentrado;

    @NotNull
    @Column(name = "peso_inicial", nullable = false, precision = 12, scale = 2)
    private BigDecimal pesoInicial;

    @Column(name = "peso_final", precision = 12, scale = 2)
    private BigDecimal pesoFinal;

    @Column(name = "merma", precision = 12, scale = 2)
    private BigDecimal merma;

    @Size(max = 50)
    @Column(name = "mineral_principal", length = 50)
    private String mineralPrincipal;

    @Size(max = 50)
    @Column(name = "estado", length = 50)
    private String estado;

    @Column(name = "fecha_inicio")
    private LocalDateTime fechaInicio;

    @Column(name = "fecha_fin")
    private LocalDateTime fechaFin;

    // Auditoría
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // Relaciones
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ingenio_minero_id", nullable = false)
    private IngenioMinero ingenioMineroId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lote_origen_id")
    private Lotes loteOrigenId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "socio_propietario_id")
    private Socio socioPropietarioId;

    @OneToMany(mappedBy = "concentradoId", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    @Builder.Default
    private List<LoteConcentradoRelacion> loteConcentradoRelacionList = new ArrayList<>();

    // Métodos helper
    public void addLoteConcentradoRelacion(LoteConcentradoRelacion relacion) {
        loteConcentradoRelacionList.add(relacion);
        relacion.setConcentradoId(this);
    }

    public void removeLoteConcentradoRelacion(LoteConcentradoRelacion relacion) {
        loteConcentradoRelacionList.remove(relacion);
        relacion.setConcentradoId(null);
    }

    @PrePersist
    protected void onCreate() {
        if (estado == null) {
            estado = "creado";
        }
    }
}