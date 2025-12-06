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
@Table(name = "liquidacion")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"loteId", "socioId", "cotizacionesList", "deduccionesList"})
public class Liquidacion implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @NotNull
    @Size(min = 1, max = 50)
    @Column(name = "tipo_liquidacion", nullable = false, length = 50)
    private String tipoLiquidacion;

    @NotNull
    @Column(name = "fecha_liquidacion", nullable = false)
    private LocalDate fechaLiquidacion;

    @Size(max = 10)
    @Column(name = "moneda", length = 10)
    private String moneda;

    @Column(name = "peso_liquidado", precision = 12, scale = 2)
    private BigDecimal pesoLiquidado;

    @Column(name = "valor_bruto", precision = 15, scale = 2)
    private BigDecimal valorBruto;

    @Column(name = "valor_neto", precision = 15, scale = 2)
    private BigDecimal valorNeto;

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
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lote_id")
    private Lotes loteId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "socio_id", nullable = false)
    private Socio socioId;

    @OneToMany(mappedBy = "liquidacionId", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    @Builder.Default
    private List<LiquidacionCotizacion> cotizacionesList = new ArrayList<>();

    @OneToMany(mappedBy = "liquidacionId", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    @Builder.Default
    private List<LiquidacionDeduccion> deduccionesList = new ArrayList<>();

    // Métodos helper
    public void addCotizacion(LiquidacionCotizacion cotizacion) {
        cotizacionesList.add(cotizacion);
        cotizacion.setLiquidacionId(this);
    }

    public void removeCotizacion(LiquidacionCotizacion cotizacion) {
        cotizacionesList.remove(cotizacion);
        cotizacion.setLiquidacionId(null);
    }

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
            moneda = "bob";
        }
        if (estado == null) {
            estado = "borrador";
        }
    }
}