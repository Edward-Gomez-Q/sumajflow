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

@Entity
@Table(name = "liquidacion_deduccion")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = "liquidacionId")
public class LiquidacionDeduccion implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Size(max = 100)
    @Column(name = "concepto", length = 100)
    private String concepto;

    @Column(name = "monto_fijo", precision = 15, scale = 4)
    private BigDecimal monto;

    @Column(name = "porcentaje", precision = 5, scale = 4)
    private BigDecimal porcentaje;

    @NotNull
    @Size(min = 1, max = 50)
    @Column(name = "tipo_deduccion", nullable = false, length = 50)
    private String tipoDeduccion;

    @Size(max = 200)
    @Column(name = "descripcion", length = 200)
    private String descripcion;

    @NotNull
    @Column(name = "monto_deducido", nullable = false, precision = 15, scale = 4)
    private BigDecimal montoDeducido;

    @NotNull
    @Size(max = 50)
    @Column(name = "base_calculo", nullable = false, length = 50)
    @Builder.Default
    private String baseCalculo = "valor_bruto";

    @NotNull
    @Column(name = "orden", nullable = false)
    @Builder.Default
    private Integer orden = 0;

    @Size(max = 10)
    @Column(name = "moneda", length = 10)
    @Builder.Default
    private String moneda = "USD";

    // Auditoría
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // Relaciones
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "liquidacion_id", nullable = false)
    private Liquidacion liquidacionId;
}