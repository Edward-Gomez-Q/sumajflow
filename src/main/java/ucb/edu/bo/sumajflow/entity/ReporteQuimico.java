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
    @Column(name = "numero_reporte", nullable = false, length = 100, unique = true)
    private String numeroReporte;

    // Tipo de quien subió el reporte
    @NotNull
    @Size(max = 50)
    @Column(name = "tipo_reporte", nullable = false, length = 50)
    @Builder.Default
    private String tipoReporte = "socio"; // socio | comercializadora

    // Tipo de venta (determina qué campos son obligatorios)
    @NotNull
    @Size(max = 50)
    @Column(name = "tipo_venta", nullable = false, length = 50)
    private String tipoVenta; // venta_concentrado | venta_lote_complejo

    @Size(max = 100)
    @Column(name = "laboratorio", length = 100)
    private String laboratorio;

    // ===== FECHAS =====
    @Column(name = "fecha_empaquetado")
    private LocalDateTime fechaEmpaquetado;

    @Column(name = "fecha_recepcion_laboratorio")
    private LocalDateTime fechaRecepcionLaboratorio;

    @Column(name = "fecha_salida_laboratorio")
    private LocalDateTime fechaSalidaLaboratorio;

    @NotNull
    @Column(name = "fecha_analisis", nullable = false)
    private LocalDateTime fechaAnalisis;

    // ===== LEYES DE MINERALES =====

    // SOLO para venta_concentrado
    @Column(name = "ley_mineral_principal", precision = 8, scale = 4)
    private BigDecimal leyMineralPrincipal;

    // PLATA - diferentes unidades según tipo_venta
    @Column(name = "ley_ag_gmt", precision = 12, scale = 4)
    private BigDecimal leyAgGmt; // g/MT - SOLO venta_concentrado

    @Column(name = "ley_ag_dm", precision = 8, scale = 4)
    private BigDecimal leyAgDm; // DM - SOLO venta_lote_complejo

    // METALES BASE
    @Column(name = "ley_pb", precision = 8, scale = 4)
    private BigDecimal leyPb; // Plomo - AMBOS tipos

    @Column(name = "ley_zn", precision = 8, scale = 4)
    private BigDecimal leyZn; // Zinc - SOLO venta_lote_complejo

    // HUMEDAD - SOLO venta_concentrado
    @Column(name = "porcentaje_h2o", precision = 5, scale = 2)
    private BigDecimal porcentajeH2o;

    // ===== DOCUMENTACIÓN =====
    @Size(max = 200)
    @Column(name = "url_pdf", length = 200)
    private String urlPdf;

    @Column(name = "observaciones_laboratorio", columnDefinition = "text")
    private String observacionesLaboratorio;

    // ===== CARACTERÍSTICAS EMPAQUETADO (solo venta_concentrado) =====
    @Column(name = "numero_sacos")
    private Integer numeroSacos;

    @Column(name = "peso_por_saco", precision = 10, scale = 3)
    private BigDecimal pesoPorSaco;

    @Size(max = 100)
    @Column(name = "tipo_empaque", length = 100)
    private String tipoEmpaque;

    // ===== VALIDACIONES =====
    @Size(max = 50)
    @Column(name = "estado", length = 50)
    @Builder.Default
    private String estado = "pendiente";

    @Column(name = "subido_por_socio")
    @Builder.Default
    private Boolean subidoPorSocio = false;

    @Column(name = "subido_por_comercializadora")
    @Builder.Default
    private Boolean subidoPorComercializadora = false;

    @Column(name = "fecha_subida_socio")
    private LocalDateTime fechaSubidaSocio;

    @Column(name = "fecha_subida_comercializadora")
    private LocalDateTime fechaSubidaComercializadora;

    // ===== AUDITORÍA =====
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // ===== RELACIONES =====
    @OneToMany(mappedBy = "reporteQuimicoId", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<LiquidacionLote> liquidacionLoteList = new ArrayList<>();

    @OneToMany(mappedBy = "reporteQuimicoId", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<LiquidacionConcentrado> liquidacionConcentradoList = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        if (tipoReporte == null) {
            tipoReporte = "socio";
        }
        if (estado == null) {
            estado = "pendiente";
        }
        if (subidoPorSocio == null) {
            subidoPorSocio = false;
        }
        if (subidoPorComercializadora == null) {
            subidoPorComercializadora = false;
        }
    }

    // ===== MÉTODOS DE VALIDACIÓN =====

    /**
     * Valida que los campos obligatorios estén presentes según el tipo de venta
     */
    public void validarCamposSegunTipoVenta() {
        if ("venta_concentrado".equals(tipoVenta)) {
            if (leyMineralPrincipal == null) {
                throw new IllegalStateException("La ley del mineral principal es requerida para venta de concentrado");
            }
            if (leyAgGmt == null) {
                throw new IllegalStateException("La ley de Ag (g/MT) es requerida para venta de concentrado");
            }
            if (porcentajeH2o == null) {
                throw new IllegalStateException("El porcentaje de humedad es requerido para venta de concentrado");
            }
        } else if ("venta_lote_complejo".equals(tipoVenta)) {
            if (leyAgDm == null) {
                throw new IllegalStateException("La ley de Ag (DM) es requerida para venta de lote complejo");
            }
            if (leyPb == null) {
                throw new IllegalStateException("La ley de Pb es requerida para venta de lote complejo");
            }
            if (leyZn == null) {
                throw new IllegalStateException("La ley de Zn es requerida para venta de lote complejo");
            }
        }
    }
}