// IniciarViajeRequestDto.java
package ucb.edu.bo.sumajflow.dto.transporte;

import lombok.Data;

@Data
public class IniciarViajeRequestDto {
    private Double lat;
    private Double lng;
    private String observaciones;
}