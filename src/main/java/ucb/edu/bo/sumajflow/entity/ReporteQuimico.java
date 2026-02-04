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

    @NotNull
    @Size(max = 50)
    @Column(name = "tipo_reporte", nullable = false, length = 50)
    @Builder.Default
    private String tipoReporte = "ingenio";

    @Size(max = 100)
    @Column(name = "laboratorio", length = 100)
    private String laboratorio;

    @Column(name = "fecha_muestreo")
    private LocalDate fechaMuestreo;

    @Column(name = "ley_mineral_principal", precision = 8, scale = 4)
    private BigDecimal leyMineralPrincipal;

    @Column(name = "ley_ag_gmt", precision = 12, scale = 4)
    private BigDecimal leyAgGmt;

    @Column(name = "ley_ag_dm", precision = 8, scale = 4)
    private BigDecimal leyAgDm;

    @NotNull
    @Column(name = "fecha_analisis", nullable = false)
    private LocalDateTime fechaAnalisis;


    @Column(name = "ley_pb", precision = 8, scale = 4)
    private BigDecimal leyPb;

    @Column(name = "ley_zn", precision = 8, scale = 4)
    private BigDecimal leyZn;

    @Column(name = "porcentaje_h2o", precision = 5, scale = 2)
    private BigDecimal porcentajeH2o;

    @Size(max = 50)
    @Column(name = "tipo_analisis", length = 50)
    private String tipoAnalisis;

    @Size(max = 200)
    @Column(name = "url_pdf", length = 200)
    private String urlPdf;

    @Size(max = 20)
    @Column(name = "unidad_traza", length = 20)
    private String unidadTraza;

    @Column(name = "observaciones_laboratorio", columnDefinition = "text")
    private String observacionesLaboratorio;

    @Size(max = 50)
    @Column(name = "estado", length = 50)
    @Builder.Default
    private String estado = "pendiente";

    @Column(name = "validado_por_socio")
    @Builder.Default
    private Boolean validadoPorSocio = false;

    @Column(name = "validado_por_ingenio")
    @Builder.Default
    private Boolean validadoPorIngenio = false;

    @Column(name = "validado_por_comercializadora")
    @Builder.Default
    private Boolean validadoPorComercializadora = false;

    @Column(name = "fecha_validacion_socio")
    private LocalDateTime fechaValidacionSocio;

    @Column(name = "fecha_validacion_ingenio")
    private LocalDateTime fechaValidacionIngenio;

    @Column(name = "fecha_validacion_comercializadora")
    private LocalDateTime fechaValidacionComercializadora;

    // Auditoría
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "reporteQuimicoId", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<LiquidacionLote> liquidacionLoteList = new ArrayList<>();

    @OneToMany(mappedBy = "reporteQuimicoId", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<LiquidacionConcentrado> liquidacionConcentradoList = new ArrayList<>();


    @PrePersist
    protected void onCreate() {
        if (tipoReporte == null) {
            tipoReporte = "ingenio";
        }
        if (estado == null) {
            estado = "pendiente";
        }
        if (validadoPorSocio == null) {
            validadoPorSocio = false;
        }
        if (validadoPorIngenio == null) {
            validadoPorIngenio = false;
        }
        if (validadoPorComercializadora == null) {
            validadoPorComercializadora = false;
        }
    }
}