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
@Table(name = "liquidacion")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = { "cotizacionesList", "deduccionesList", "liquidacionLoteList", "liquidacionConcentradoList"})
public class Liquidacion implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @NotNull
    @Size(min = 1, max = 50)
    @Column(name = "tipo_liquidacion", nullable = false, length = 50)
    private String tipoLiquidacion;

    @Column(name = "peso_total_entrada", precision = 12, scale = 2)
    private BigDecimal pesoTotalEntrada;

    @Column(name = "peso_tmh", precision = 12, scale = 4)
    private BigDecimal pesoTmh;

    @Column(name = "peso_tms", precision = 12, scale = 4)
    private BigDecimal pesoTms;

    @Column(name = "peso_final_tms", precision = 12, scale = 4)
    private BigDecimal pesoFinalTms;

    @Column(name = "costo_por_tonelada", precision = 12, scale = 4)
    private BigDecimal costoPorTonelada;

    @Column(name = "costo_procesamiento_total", precision = 15, scale = 4)
    private BigDecimal costoProcesamientoTotal;

    @Column(name = "servicios_adicionales", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private String serviciosAdicionales;

    @Column(name = "total_servicios_adicionales", precision = 15, scale = 4)
    @Builder.Default
    private BigDecimal totalServiciosAdicionales = BigDecimal.ZERO;

    @Column(name = "valor_bruto_usd", precision = 15, scale = 4)
    private BigDecimal valorBrutoUsd;

    @Column(name = "valor_neto_usd", precision = 15, scale = 4)
    private BigDecimal valorNetoUsd;

    @Column(name = "tipo_cambio", precision = 8, scale = 4)
    private BigDecimal tipoCambio;

    @Column(name = "valor_neto_bob", precision = 15, scale = 4)
    private BigDecimal valorNetoBob;

    @Size(max = 10)
    @Column(name = "moneda", length = 10)
    @Builder.Default
    private String moneda = "USD";

    @Column(name = "fecha_aprobacion")
    private LocalDateTime fechaAprobacion;

    @Column(name = "fecha_pago")
    private LocalDateTime fechaPago;

    @Size(max = 50)
    @Column(name = "metodo_pago", length = 50)
    private String metodoPago;

    @Size(max = 100)
    @Column(name = "numero_comprobante", length = 100)
    private String numeroComprobante;

    @Size(max = 200)
    @Column(name = "url_comprobante", length = 200)
    private String urlComprobante;

    @Column(name = "observaciones", columnDefinition = "text")
    private String observaciones;

    @Size(max = 50)
    @Column(name = "estado", length = 50)
    private String estado;

    // Auditoría
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // Relaciones
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "socio_id", nullable = false)
    private Socio socioId;

    // NUEVO: Relación con comercializadora (nullable - solo para ventas)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "comercializadora_id")
    private Comercializadora comercializadoraId;

    @OneToMany(mappedBy = "liquidacionId", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    @Builder.Default
    private List<LiquidacionLote> liquidacionLoteList = new ArrayList<>();

    @OneToMany(mappedBy = "liquidacionId", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    @Builder.Default
    private List<LiquidacionConcentrado> liquidacionConcentradoList = new ArrayList<>();

    @OneToMany(mappedBy = "liquidacionId", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    @Builder.Default
    private List<LiquidacionCotizacion> cotizacionesList = new ArrayList<>();

    @OneToMany(mappedBy = "liquidacionId", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    @Builder.Default
    private List<LiquidacionDeduccion> deduccionesList = new ArrayList<>();

    // Métodos helper para lotes
    public void addLiquidacionLote(LiquidacionLote liquidacionLote) {
        liquidacionLoteList.add(liquidacionLote);
        liquidacionLote.setLiquidacionId(this);
    }

    public void removeLiquidacionLote(LiquidacionLote liquidacionLote) {
        liquidacionLoteList.remove(liquidacionLote);
        liquidacionLote.setLiquidacionId(null);
    }

    // Métodos helper para concentrados
    public void addLiquidacionConcentrado(LiquidacionConcentrado liquidacionConcentrado) {
        liquidacionConcentradoList.add(liquidacionConcentrado);
        liquidacionConcentrado.setLiquidacionId(this);
    }

    public void removeLiquidacionConcentrado(LiquidacionConcentrado liquidacionConcentrado) {
        liquidacionConcentradoList.remove(liquidacionConcentrado);
        liquidacionConcentrado.setLiquidacionId(null);
    }

    // Métodos helper para cotizaciones
    public void addCotizacion(LiquidacionCotizacion cotizacion) {
        cotizacionesList.add(cotizacion);
        cotizacion.setLiquidacionId(this);
    }

    public void removeCotizacion(LiquidacionCotizacion cotizacion) {
        cotizacionesList.remove(cotizacion);
        cotizacion.setLiquidacionId(null);
    }

    // Métodos helper para deducciones
    public void addDeduccion(LiquidacionDeduccion deduccion) {
        deduccionesList.add(deduccion);
        deduccion.setLiquidacionId(this);
    }

    public void removeDeduccion(LiquidacionDeduccion deduccion) {
        deduccionesList.remove(deduccion);
        deduccion.setLiquidacionId(null);
    }

    @PrePersist
    protected void onCreate() {
        if (moneda == null) {
            moneda = "USD";
        }
        if (estado == null) {
            estado = "borrador";
        }
        if (totalServiciosAdicionales == null) {
            totalServiciosAdicionales = BigDecimal.ZERO;
        }
    }
}