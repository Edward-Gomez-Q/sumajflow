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
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "planta")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"ingenioMineroId", "plantaMineralesList", "procesosPlantaList"})
public class Planta implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @NotNull
    @Column(name = "cupo_minimo", nullable = false, precision = 12, scale = 6)
    private BigDecimal cupoMinimo;

    @NotNull
    @Column(name = "capacidad_procesamiento", nullable = false, precision = 12, scale = 6)
    private BigDecimal capacidadProcesamiento;

    @NotNull
    @Column(name = "costo_procesamiento", nullable = false, precision = 12, scale = 6)
    private BigDecimal costoProcesamiento;

    @Size(max = 200)
    @Column(name = "licencia_ambiental_url", length = 200)
    private String licenciaAmbientalUrl;

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

    @OneToMany(mappedBy = "plantaId", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    @Builder.Default
    private List<PlantaMinerales> plantaMineralesList = new ArrayList<>();

    @OneToMany(mappedBy = "plantaId", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    @Builder.Default
    private List<ProcesosPlanta> procesosPlantaList = new ArrayList<>();

    // Métodos helper
    public void addPlantaMineral(PlantaMinerales plantaMineral) {
        plantaMineralesList.add(plantaMineral);
        plantaMineral.setPlantaId(this);
    }

    public void removePlantaMineral(PlantaMinerales plantaMineral) {
        plantaMineralesList.remove(plantaMineral);
        plantaMineral.setPlantaId(null);
    }

    public void addProcesoPlanta(ProcesosPlanta procesoPlanta) {
        procesosPlantaList.add(procesoPlanta);
        procesoPlanta.setPlantaId(this);
    }

    public void removeProcesoPlanta(ProcesosPlanta procesoPlanta) {
        procesosPlantaList.remove(procesoPlanta);
        procesoPlanta.setPlantaId(null);
    }
}