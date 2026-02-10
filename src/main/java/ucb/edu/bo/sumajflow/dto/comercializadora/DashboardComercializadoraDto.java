// DashboardComercializadoraDto.java
package ucb.edu.bo.sumajflow.dto.comercializadora;

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
public class DashboardComercializadoraDto {
    private ComprasDataDto comprasData;
    private FinancieroDataDto financieroData;
    private ConcentradosDataDto concentradosData;
    private CotizacionesActualesDto cotizacionesActuales;
    private List<PipelineEtapaDto> pipelineEtapas;
    private List<CotizacionDto> cotizaciones;
    private List<HistoricoCotizacionDto> historicoCotizaciones;
    private List<AlertaCotizacionDto> alertasCotizacion;
    private List<CarteraConcentradoDto> carteraConcentrados;
    private ResumenCarteraDto resumenCartera;
    private List<DistribucionCarteraDto> distribucionCartera;
    private List<CompraPorMesDto> comprasPorMes;
    private List<CompraPorSocioDto> comprasPorSocio;
}

// === DATOS GENERALES ===

// === PIPELINE DE COMPRAS ===

// === COTIZACIONES ===

// === CARTERA DE CONCENTRADOS ===

// === ANÁLISIS DE COMPRAS ===

