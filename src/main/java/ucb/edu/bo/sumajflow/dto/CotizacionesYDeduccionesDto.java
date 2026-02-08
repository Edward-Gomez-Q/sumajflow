package ucb.edu.bo.sumajflow.dto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
class CotizacionesYDeduccionesDto {
    private java.util.Map<String, CotizacionMineralDto> cotizaciones;
    private BigDecimal dolarOficial;
    private List<DeduccionConfiguracionDto> deducciones;
}