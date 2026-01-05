package ucb.edu.bo.sumajflow.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "lotes")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {
        "minasId",
        "asignacionCamionList",
        "loteIngenioList",
        "loteComercializadoraList",
        "loteMineralesList",
        "loteConcentradoRelacionList",
        "liquidacionLoteList",
        "auditoriaLotesList"
})
public class Lotes implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @NotNull
    @Column(name = "camiones_solicitados", nullable = false)
    private Integer camionesSolicitados;

    @NotNull
    @Size(min = 1, max = 50)
    @Column(name = "tipo_operacion", nullable = false, length = 50)
    private String tipoOperacion;

    @NotNull
    @Size(min = 1, max = 50)
    @Column(name = "tipo_mineral", nullable = false, length = 50)
    private String tipoMineral;

    @NotNull
    @Size(min = 1, max = 70)
    @Column(name = "estado", nullable = false, length = 70)
    private String estado;

    @Column(name = "fecha_creacion")
    private LocalDateTime fechaCreacion;

    @Column(name = "fecha_aprobacion_cooperativa")
    private LocalDateTime fechaAprobacionCooperativa;

    @Column(name = "fecha_aprobacion_destino")
    private LocalDateTime fechaAprobacionDestino;

    @Column(name = "fecha_inicio_transporte")
    private LocalDateTime fechaInicioTransporte;

    @Column(name = "fecha_fin_transporte")
    private LocalDateTime fechaFinTransporte;

    @Column(name = "peso_total_estimado", precision = 12, scale = 2)
    private BigDecimal pesoTotalEstimado;

    @Column(name = "peso_total_real", precision = 12, scale = 2)
    private BigDecimal pesoTotalReal;

    @Column(name = "observaciones", columnDefinition = "text")
    private String observaciones;

    // Auditoría (solo updated_at, ya tiene fecha_creacion)
    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // Relaciones
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "minas_id", nullable = false)
    private Minas minasId;

    @OneToMany(mappedBy = "lotesId", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    @Builder.Default
    private List<AsignacionCamion> asignacionCamionList = new ArrayList<>();

    @OneToMany(mappedBy = "lotesId", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    @Builder.Default
    private List<LoteIngenio> loteIngenioList = new ArrayList<>();

    @OneToMany(mappedBy = "lotesId", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    @Builder.Default
    private List<LoteComercializadora> loteComercializadoraList = new ArrayList<>();

    @OneToMany(mappedBy = "lotesId", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    @Builder.Default
    private List<LoteMinerales> loteMineralesList = new ArrayList<>();

    @OneToMany(mappedBy = "loteComplejoId", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    @Builder.Default
    private List<LoteConcentradoRelacion> loteConcentradoRelacionList = new ArrayList<>();

    // ELIMINADO: reporteQuimicoList (ahora está en las tablas intermedias)

    @OneToMany(mappedBy = "lotesId", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    @Builder.Default
    private List<LiquidacionLote> liquidacionLoteList = new ArrayList<>();

    @OneToMany(mappedBy = "loteId", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    @Builder.Default
    private List<AuditoriaLotes> auditoriaLotesList = new ArrayList<>();

    // Métodos helper para sincronización bidireccional
    public void addAsignacionCamion(AsignacionCamion asignacion) {
        asignacionCamionList.add(asignacion);
        asignacion.setLotesId(this);
    }

    public void removeAsignacionCamion(AsignacionCamion asignacion) {
        asignacionCamionList.remove(asignacion);
        asignacion.setLotesId(null);
    }

    public void addLoteIngenio(LoteIngenio loteIngenio) {
        loteIngenioList.add(loteIngenio);
        loteIngenio.setLotesId(this);
    }

    public void removeLoteIngenio(LoteIngenio loteIngenio) {
        loteIngenioList.remove(loteIngenio);
        loteIngenio.setLotesId(null);
    }

    public void addLoteComercializadora(LoteComercializadora loteComercializadora) {
        loteComercializadoraList.add(loteComercializadora);
        loteComercializadora.setLotesId(this);
    }

    public void removeLoteComercializadora(LoteComercializadora loteComercializadora) {
        loteComercializadoraList.remove(loteComercializadora);
        loteComercializadora.setLotesId(null);
    }

    public void addLoteMineral(LoteMinerales loteMineral) {
        loteMineralesList.add(loteMineral);
        loteMineral.setLotesId(this);
    }

    public void removeLoteMineral(LoteMinerales loteMineral) {
        loteMineralesList.remove(loteMineral);
        loteMineral.setLotesId(null);
    }

    public void addLiquidacionLote(LiquidacionLote liquidacionLote) {
        liquidacionLoteList.add(liquidacionLote);
        liquidacionLote.setLotesId(this);
    }

    public void removeLiquidacionLote(LiquidacionLote liquidacionLote) {
        liquidacionLoteList.remove(liquidacionLote);
        liquidacionLote.setLotesId(null);
    }

    @PrePersist
    protected void onCreate() {
        if (fechaCreacion == null) {
            fechaCreacion = LocalDateTime.now();
        }
    }
}