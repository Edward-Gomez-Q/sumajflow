package ucb.edu.bo.sumajflow.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
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
@ToString(exclude = {
        "ingenioMineroId",
        "socioPropietarioId",
        "loteConcentradoRelacionList",
        "liquidacionConcentradoList",
        "loteProcesoPlantaList"
})
public class Concentrado implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @NotNull
    @Size(min = 1, max = 50)
    @Column(name = "codigo_concentrado", nullable = false, length = 50, unique = true)
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

    @Size(max = 200)
    @Column(name = "minerales_secundarios", length = 200)
    private String mineralesSecundarios;

    @Column(name = "lote_origen_multiple")
    @Builder.Default
    private Boolean loteOrigenMultiple = false;

    @Size(max = 50)
    @Column(name = "estado", length = 50)
    private String estado;

    @Column(name = "numero_sacos")
    private Integer numeroSacos;

    @Column(name = "observaciones", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private String observaciones;

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
    @JoinColumn(name = "socio_propietario_id")
    private Socio socioPropietarioId;

    // Relación con lotes a través de tabla intermedia
    @OneToMany(mappedBy = "concentradoId", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    @Builder.Default
    private List<LoteConcentradoRelacion> loteConcentradoRelacionList = new ArrayList<>();

    // Relación con liquidaciones a través de tabla intermedia
    @OneToMany(mappedBy = "concentradoId", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    @Builder.Default
    private List<LiquidacionConcentrado> liquidacionConcentradoList = new ArrayList<>();

    // Relación con procesos de planta (Kanban)
    @OneToMany(mappedBy = "concentradoId", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    @Builder.Default
    private List<LoteProcesoPlanta> loteProcesoPlantaList = new ArrayList<>();

    // Métodos helper para lote concentrado relacion
    public void addLoteConcentradoRelacion(LoteConcentradoRelacion relacion) {
        loteConcentradoRelacionList.add(relacion);
        relacion.setConcentradoId(this);
    }

    public void removeLoteConcentradoRelacion(LoteConcentradoRelacion relacion) {
        loteConcentradoRelacionList.remove(relacion);
        relacion.setConcentradoId(null);
    }

    // Métodos helper para liquidacion concentrado
    public void addLiquidacionConcentrado(LiquidacionConcentrado liquidacionConcentrado) {
        liquidacionConcentradoList.add(liquidacionConcentrado);
        liquidacionConcentrado.setConcentradoId(this);
    }

    public void removeLiquidacionConcentrado(LiquidacionConcentrado liquidacionConcentrado) {
        liquidacionConcentradoList.remove(liquidacionConcentrado);
        liquidacionConcentrado.setConcentradoId(null);
    }

    // Métodos helper para lote proceso planta
    public void addLoteProcesoPlanta(LoteProcesoPlanta loteProcesoPlanta) {
        loteProcesoPlantaList.add(loteProcesoPlanta);
        loteProcesoPlanta.setConcentradoId(this);
    }

    public void removeLoteProcesoPlanta(LoteProcesoPlanta loteProcesoPlanta) {
        loteProcesoPlantaList.remove(loteProcesoPlanta);
        loteProcesoPlanta.setConcentradoId(null);
    }

    @PrePersist
    protected void onCreate() {
        if (estado == null) {
            estado = "creado";
        }
        if (fechaInicio == null) {
            fechaInicio = LocalDateTime.now();
        }
        if (loteOrigenMultiple == null) {
            loteOrigenMultiple = false;
        }
    }
}