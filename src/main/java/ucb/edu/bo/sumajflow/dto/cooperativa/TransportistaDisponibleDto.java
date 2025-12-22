package ucb.edu.bo.sumajflow.dto.cooperativa;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TransportistaDisponibleDto {
    private Integer id;
    private String nombreCompleto;
    private String ci;
    private String placaVehiculo;
    private String marcaVehiculo;
    private String modeloVehiculo;
    private BigDecimal capacidadCarga;
    private BigDecimal pesoTara;
    private Integer viajesCompletados;
    private BigDecimal calificacionPromedio;
    private String estado;
}