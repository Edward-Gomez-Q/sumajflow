package ucb.edu.bo.sumajflow.dto.ingenio;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProcesoPlantaDashboardDto {
    private String nombre;
    private BigDecimal capacidadMaxima; // ton/h
    private BigDecimal utilizado; // ton/h
    private BigDecimal eficiencia; // porcentaje
    private Integer concentradosEnEtapa;
    private BigDecimal tiempoPromedioEtapa; // horas
    private LocalDate ultimoMantenimiento;
    private LocalDate proximoMantenimiento;
}
