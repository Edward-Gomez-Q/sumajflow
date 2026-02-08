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

@Entity
@Table(name = "tabla_precios_mineral")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TablaPreciosMineral implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "comercializadora_id", nullable = false)
    private Comercializadora comercializadoraId;

    @NotNull
    @Size(max = 10)
    @Column(name = "mineral", nullable = false, length = 10)
    private String mineral; // Pb, Zn, Ag

    @NotNull
    @Size(max = 20)
    @Column(name = "unidad_medida", nullable = false, length = 20)
    private String unidadMedida; // % para Pb/Zn, DM para Ag

    @NotNull
    @Column(name = "rango_minimo", nullable = false, precision = 8, scale = 4)
    private BigDecimal rangoMinimo;

    @NotNull
    @Column(name = "rango_maximo", nullable = false, precision = 8, scale = 4)
    private BigDecimal rangoMaximo;

    @NotNull
    @Column(name = "precio_usd", nullable = false, precision = 10, scale = 4)
    private BigDecimal precioUsd;

    @NotNull
    @Column(name = "fecha_inicio", nullable = false)
    private LocalDate fechaInicio;

    @Column(name = "fecha_fin")
    private LocalDate fechaFin;

    @NotNull
    @Column(name = "activo", nullable = false)
    @Builder.Default
    private Boolean activo = true;

    @Column(name = "observaciones", columnDefinition = "text")
    private String observaciones;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * Verifica si el precio está vigente en una fecha específica
     */
    public boolean esVigente(LocalDate fecha) {
        if (!activo) return false;
        if (fecha.isBefore(fechaInicio)) return false;
        return fechaFin == null || !fecha.isAfter(fechaFin);
    }

    /**
     * Verifica si un valor está dentro del rango
     */
    public boolean contieneValor(BigDecimal valor) {
        return valor.compareTo(rangoMinimo) >= 0 && valor.compareTo(rangoMaximo) <= 0;
    }
}