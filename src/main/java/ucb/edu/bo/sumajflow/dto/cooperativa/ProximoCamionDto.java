package ucb.edu.bo.sumajflow.dto.cooperativa;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public  class ProximoCamionDto {
    private String placaVehiculo;
    private LocalDateTime eta;
    private Integer loteId;
}
