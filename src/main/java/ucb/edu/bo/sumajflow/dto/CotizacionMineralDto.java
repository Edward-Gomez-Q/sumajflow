package ucb.edu.bo.sumajflow.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CotizacionMineralDto {
    private String nomenclatura;
    private String nombre;
    private BigDecimal cotizacionUsdTon;
    private BigDecimal cotizacionUsdOz;
    private String unidad;
    private BigDecimal dolarOficial;
    private LocalDate fecha;
    private String fuente;
}