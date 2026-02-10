// DashboardIngenioDto.java
package ucb.edu.bo.sumajflow.dto.ingenio;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DashboardIngenioDto {
    private OperacionesDataDto operacionesData;
    private KanbanDataDto kanbanData;
    private FinancieroDataDto financieroData;
    private LotesPendientesDataDto lotesPendientesData;
    private List<KanbanColumnaDto> kanbanColumnas;
    private List<ProcesoPlantaDashboardDto> procesosPlanta;
    private CapacidadPlantaDto capacidadPlanta;
    private TurnoActualDto turnoActual;
    private List<AlertaOperacionalDto> alertasOperacionales;
    private LiquidacionesTollDto liquidacionesToll;
    private List<ProduccionDiariaDto> produccionDiaria;
    private List<ProduccionPorMineralDto> produccionPorMineral;
    private List<LoteDisponibleDto> lotesDisponibles;
}

// === DATOS GENERALES ===

// === KANBAN ===

// === PLANTA ===

// === LIQUIDACIONES TOLL ===

// === PRODUCCIÓN ===

// === COLA DE ENTRADA ===

