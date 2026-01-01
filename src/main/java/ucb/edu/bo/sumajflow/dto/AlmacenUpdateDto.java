package ucb.edu.bo.sumajflow.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlmacenUpdateDto {
    private String nombre;
    private BigDecimal capacidadMaxima; // en toneladas
    private BigDecimal area; // en m²
    private String departamento;
    private String provincia;
    private String municipio;
    private String direccion;
    private BigDecimal latitud;
    private BigDecimal longitud;
}