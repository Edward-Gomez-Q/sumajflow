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
@Table(name = "procesos")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"procesosPlantaList", "loteProcesoPlantaList"})
public class Procesos implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @NotNull
    @Size(min = 1, max = 50)
    @Column(name = "nombre", nullable = false, length = 50)
    private String nombre;

    // Auditoría
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // Relaciones
    @OneToMany(mappedBy = "procesosId", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    @Builder.Default
    private List<ProcesosPlanta> procesosPlantaList = new ArrayList<>();

    @OneToMany(mappedBy = "procesoId", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    @Builder.Default
    private List<LoteProcesoPlanta> loteProcesoPlantaList = new ArrayList<>();

    // Métodos helper
    public void addProcesoPlanta(ProcesosPlanta procesoPlanta) {
        procesosPlantaList.add(procesoPlanta);
        procesoPlanta.setProcesosId(this);
    }

    public void removeProcesoPlanta(ProcesosPlanta procesoPlanta) {
        procesosPlantaList.remove(procesoPlanta);
        procesoPlanta.setProcesosId(null);
    }

    public void addLoteProceso(LoteProcesoPlanta loteProceso) {
        loteProcesoPlantaList.add(loteProceso);
        loteProceso.setProcesoId(this);
    }

    public void removeLoteProceso(LoteProcesoPlanta loteProceso) {
        loteProcesoPlantaList.remove(loteProceso);
        loteProceso.setProcesoId(null);
    }
}