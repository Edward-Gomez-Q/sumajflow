// DashboardCooperativaDto.java
package ucb.edu.bo.sumajflow.dto.cooperativa;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DashboardCooperativaDto {
    private SociosDataDto sociosData;
    private LotesDataDto lotesData;
    private TransportistasDataDto transportistasData;
    private VolumetriaDataDto volumetriaData;
    private List<LotePendienteDashboardDto> lotesPendientes;
    private List<TransportistaEnRutaDto> transportistasEnRuta;
    private List<BalanzaMonitorDto> balanzasMonitor;
    private List<AprobacionPorDiaDto> aprobacionesPorDia;
    private List<MinasPorSectorDto> minasPorSector;
}

