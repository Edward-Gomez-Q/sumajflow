package ucb.edu.bo.sumajflow.dto.socio;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO para creación y actualización de minas
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MinaCreateDto {

    private String nombre;
    private String fotoUrl;
    private BigDecimal latitud;
    private BigDecimal longitud;
    private Integer sectorId;

}