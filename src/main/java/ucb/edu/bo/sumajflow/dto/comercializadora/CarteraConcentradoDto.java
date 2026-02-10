package ucb.edu.bo.sumajflow.dto.comercializadora;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CarteraConcentradoDto {
    private Integer id;
    private String codigo;
    private String mineralPrincipal;
    private BigDecimal pesoFinal; // kg
    private BigDecimal valorCompra; // USD
    private LocalDate fechaCompra;
    private Long diasEnCartera;
    private BigDecimal valorizacionActual; // USD
    private BigDecimal ganancia; // USD
    private BigDecimal rentabilidad; // porcentaje
    private String socioNombre;
    private String ingenioNombre;
}
