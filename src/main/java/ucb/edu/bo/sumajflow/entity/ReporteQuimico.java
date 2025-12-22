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
@Table(name = "reporte_quimico")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"liquidacionLoteList", "liquidacionConcentradoList"})
public class ReporteQuimico implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @NotNull
    @Size(min = 1, max = 100)
    @Column(name = "numero_reporte", nullable = false, length = 100)
    private String numeroReporte;

    @Size(max = 100)
    @Column(name = "laboratorio", length = 100)
    private String laboratorio;

    @NotNull
    @Column(name = "fecha_analisis", nullable = false)
    private LocalDate fechaAnalisis;

    @Column(name = "ley_ag", precision = 8, scale = 4)
    private BigDecimal leyAg;

    @Column(name = "ley_pb", precision = 8, scale = 4)
    private BigDecimal leyPb;

    @Column(name = "ley_zn", precision = 8, scale = 4)
    private BigDecimal leyZn;

    @Column(name = "humedad", precision = 5, scale = 2)
    private BigDecimal humedad;

    @Size(max = 50)
    @Column(name = "tipo_analisis", length = 50)
    private String tipoAnalisis;

    @Size(max = 200)
    @Column(name = "url_pdf", length = 200)
    private String urlPdf;

    // Auditoría
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // ELIMINADO: loteId (ya no tiene relación directa con lotes)

    // NUEVO: Relaciones inversas con liquidacion_lote
    @OneToMany(mappedBy = "reporteQuimicoId", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<LiquidacionLote> liquidacionLoteList = new ArrayList<>();

    // NUEVO: Relaciones inversas con liquidacion_concentrado
    @OneToMany(mappedBy = "reporteQuimicoId", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<LiquidacionConcentrado> liquidacionConcentradoList = new ArrayList<>();

    // Métodos helper para liquidacion lote
    public void addLiquidacionLote(LiquidacionLote liquidacionLote) {
        liquidacionLoteList.add(liquidacionLote);
        liquidacionLote.setReporteQuimicoId(this);
    }

    public void removeLiquidacionLote(LiquidacionLote liquidacionLote) {
        liquidacionLoteList.remove(liquidacionLote);
        liquidacionLote.setReporteQuimicoId(null);
    }

    // Métodos helper para liquidacion concentrado
    public void addLiquidacionConcentrado(LiquidacionConcentrado liquidacionConcentrado) {
        liquidacionConcentradoList.add(liquidacionConcentrado);
        liquidacionConcentrado.setReporteQuimicoId(this);
    }

    public void removeLiquidacionConcentrado(LiquidacionConcentrado liquidacionConcentrado) {
        liquidacionConcentradoList.remove(liquidacionConcentrado);
        liquidacionConcentrado.setReporteQuimicoId(null);
    }
}