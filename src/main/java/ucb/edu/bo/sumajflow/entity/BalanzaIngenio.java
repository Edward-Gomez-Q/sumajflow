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
@Table(name = "balanza_ingenio")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = "ingenioMineroId")
public class BalanzaIngenio implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @NotNull
    @Size(min = 1, max = 100)
    @Column(name = "nombre", nullable = false, length = 100)
    private String nombre;

    @NotNull
    @Size(min = 1, max = 50)
    @Column(name = "marca", nullable = false, length = 50)
    private String marca;

    @NotNull
    @Size(min = 1, max = 50)
    @Column(name = "modelo", nullable = false, length = 50)
    private String modelo;

    @NotNull
    @Size(min = 1, max = 100)
    @Column(name = "numero_serie", nullable = false, length = 100)
    private String numeroSerie;

    @NotNull
    @Column(name = "capacidad_maxima", nullable = false, precision = 12, scale = 3)
    private BigDecimal capacidadMaxima;

    @NotNull
    @Column(name = "precision_minima", nullable = false, precision = 8, scale = 3)
    private BigDecimal precisionMinima;

    @NotNull
    @Column(name = "fecha_ultima_calibracion", nullable = false)
    private LocalDate fechaUltimaCalibracion;

    @NotNull
    @Column(name = "fecha_proxima_calibracion", nullable = false)
    private LocalDate fechaProximaCalibracion;

    @Size(max = 50)
    @Column(name = "departamento", length = 50)
    private String departamento;

    @Size(max = 100)
    @Column(name = "provincia", length = 100)
    private String provincia;

    @Size(max = 100)
    @Column(name = "municipio", length = 100)
    private String municipio;

    @Size(max = 250)
    @Column(name = "direccion", length = 250)
    private String direccion;

    @Column(name = "latitud", precision = 10, scale = 7)
    private BigDecimal latitud;

    @Column(name = "longitud", precision = 10, scale = 7)
    private BigDecimal longitud;

    // Auditoría
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // Relaciones
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ingenio_minero_id", nullable = false)
    private IngenioMinero ingenioMineroId;
}