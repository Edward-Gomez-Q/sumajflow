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
@Table(name = "minerales")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"plantaMineralesList", "loteMineralesList"})
public class Minerales implements Serializable {

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
    @Column(name = "nomenclatura", nullable = false, length = 50)
    private String nomenclatura;

    // Auditoría
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // Relaciones
    @OneToMany(mappedBy = "mineralesId", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    @Builder.Default
    private List<PlantaMinerales> plantaMineralesList = new ArrayList<>();

    @OneToMany(mappedBy = "mineralesId", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    @Builder.Default
    private List<LoteMinerales> loteMineralesList = new ArrayList<>();

    // Métodos helper
    public void addPlantaMineral(PlantaMinerales plantaMineral) {
        plantaMineralesList.add(plantaMineral);
        plantaMineral.setMineralesId(this);
    }

    public void removePlantaMineral(PlantaMinerales plantaMineral) {
        plantaMineralesList.remove(plantaMineral);
        plantaMineral.setMineralesId(null);
    }

    public void addLoteMineral(LoteMinerales loteMineral) {
        loteMineralesList.add(loteMineral);
        loteMineral.setMineralesId(this);
    }

    public void removeLoteMineral(LoteMinerales loteMineral) {
        loteMineralesList.remove(loteMineral);
        loteMineral.setMineralesId(null);
    }
}