package ucb.edu.bo.sumajflow.dto.socio;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DashboardSocioDto {
    private FinancialDataDto financialData;
    private OperationsDataDto operationsData;
    private List<CamionEnRutaDto> camionesEnRuta;
    private List<AlertaDto> alertas;
    private List<ConcentradoResumenDto> concentrados;
    private LiquidacionesPendientesDto liquidacionesPendientes;
    private List<IngresoMensualDto> ingresosMensuales;
    private List<IngresoPorMineralDto> ingresosPorMineral;
}

