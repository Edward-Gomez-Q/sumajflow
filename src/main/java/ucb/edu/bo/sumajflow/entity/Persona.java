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
@Table(name = "persona")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = "usuariosId")
public class Persona implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @NotNull
    @Size(min = 1, max = 100)
    @Column(name = "nombres", nullable = false, length = 100)
    private String nombres;

    @NotNull
    @Size(min = 1, max = 50)
    @Column(name = "primer_apellido", nullable = false, length = 50)
    private String primerApellido;

    @Size(max = 50)
    @Column(name = "segundo_apellido", length = 50)
    private String segundoApellido;

    @NotNull
    @Size(min = 1, max = 50)
    @Column(name = "ci", nullable = false, length = 50)
    private String ci;

    @NotNull
    @Column(name = "fecha_nacimiento", nullable = false)
    private LocalDate fechaNacimiento;

    @Size(max = 50)
    @Column(name = "numero_celular", length = 50)
    private String numeroCelular;

    @Size(max = 20)
    @Column(name = "genero", length = 20)
    private String genero;

    @Size(max = 50)
    @Column(name = "nacionalidad", length = 50)
    private String nacionalidad;

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
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuarios_id", nullable = false, unique = true)
    private Usuarios usuariosId;
}