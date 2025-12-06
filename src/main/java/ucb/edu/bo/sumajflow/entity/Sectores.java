package ucb.edu.bo.sumajflow.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "sectores")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"cooperativaId", "coordenadasList", "minasList"})
public class Sectores implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @NotNull
    @Size(min = 1, max = 100)
    @Column(name = "nombre", nullable = false, length = 100)
    private String nombre;

    @Size(max = 10)
    @Column(name = "color", length = 10)
    private String color;

    @NotNull
    @Size(min = 1, max = 50)
    @Column(name = "estado", nullable = false, length = 50)
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
    @JoinColumn(name = "cooperativa_id", nullable = false)
    private Cooperativa cooperativaId;

    @OneToMany(mappedBy = "sectoresId", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    @Builder.Default
    @OrderBy("orden ASC")
    private List<SectoresCoordenadas> coordenadasList = new ArrayList<>();

    @OneToMany(mappedBy = "sectoresId", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    @Builder.Default
    private List<Minas> minasList = new ArrayList<>();

    // Métodos helper
    public void addCoordenada(SectoresCoordenadas coordenada) {
        coordenadasList.add(coordenada);
        coordenada.setSectoresId(this);
    }

    public void removeCoordenada(SectoresCoordenadas coordenada) {
        coordenadasList.remove(coordenada);
        coordenada.setSectoresId(null);
    }

    public void addMina(Minas mina) {
        minasList.add(mina);
        mina.setSectoresId(this);
    }

    public void removeMina(Minas mina) {
        minasList.remove(mina);
        mina.setSectoresId(null);
    }

    @PrePersist
    protected void onCreate() {
        if (estado == null) {
            estado = "activo";
        }
    }
}