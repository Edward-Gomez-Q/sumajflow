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
@Table(name = "reporte_quimico")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = "loteId")
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

    // Relaciones
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lote_id")
    private Lotes loteId;
}