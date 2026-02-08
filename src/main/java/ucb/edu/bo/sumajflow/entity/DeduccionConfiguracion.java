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
@Table(name = "deducciones_configuracion")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeduccionConfiguracion implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @NotNull
    @Size(max = 50)
    @Column(name = "codigo", nullable = false, length = 50, unique = true)
    private String codigo;

    @NotNull
    @Size(max = 100)
    @Column(name = "concepto", nullable = false, length = 100)
    private String concepto;

    @Size(max = 200)
    @Column(name = "descripcion", length = 200)
    private String descripcion;

    @NotNull
    @Size(max = 50)
    @Column(name = "tipo_deduccion", nullable = false, length = 50)
    private String tipoDeduccion;

    @Size(max = 50)
    @Column(name = "categoria", length = 50)
    private String categoria;

    @Size(max = 50)
    @Column(name = "aplica_a_mineral", length = 50)
    private String aplicaAMineral;

    @Size(max = 50)
    @Column(name = "aplica_a_tipo_liquidacion", length = 50)
    private String aplicaATipoLiquidacion;

    @NotNull
    @Column(name = "porcentaje", nullable = false, precision = 5, scale = 4)
    private BigDecimal porcentaje;

    @NotNull
    @Size(max = 50)
    @Column(name = "base_calculo", nullable = false, length = 50)
    @Builder.Default
    private String baseCalculo = "valor_bruto_total";

    @NotNull
    @Column(name = "activo", nullable = false)
    @Builder.Default
    private Boolean activo = true;

    @NotNull
    @Column(name = "orden", nullable = false)
    @Builder.Default
    private Integer orden = 0;

    @NotNull
    @Column(name = "fecha_inicio", nullable = false)
    private LocalDate fechaInicio;

    @Column(name = "fecha_fin")
    private LocalDate fechaFin;

    @Column(name = "notas", columnDefinition = "text")
    private String notas;

    @Size(max = 200)
    @Column(name = "fuente_legal", length = 200)
    private String fuenteLegal;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * Verifica si la deducción está vigente en una fecha específica
     */
    public boolean esVigente(LocalDate fecha) {
        if (!activo) return false;
        if (fecha.isBefore(fechaInicio)) return false;
        return fechaFin == null || !fecha.isAfter(fechaFin);
    }

    /**
     * Verifica si aplica a un mineral específico
     */
    public boolean aplicaAMineral(String mineral) {
        return aplicaAMineral == null
                || "todos".equalsIgnoreCase(aplicaAMineral)
                || mineral.equalsIgnoreCase(aplicaAMineral);
    }

    /**
     * Verifica si aplica a un tipo de liquidación específico
     */
    public boolean aplicaATipoLiquidacion(String tipoLiquidacion) {
        return aplicaATipoLiquidacion == null
                || "todos".equalsIgnoreCase(aplicaATipoLiquidacion)
                || tipoLiquidacion.equalsIgnoreCase(aplicaATipoLiquidacion);
    }
}