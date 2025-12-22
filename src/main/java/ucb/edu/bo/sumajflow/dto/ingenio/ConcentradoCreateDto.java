package ucb.edu.bo.sumajflow.dto.ingenio;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConcentradoCreateDto {
    private String codigoConcentrado; // Código único del concentrado
    private List<LoteParaConcentradoDto> lotes; // Lotes que forman parte del concentrado
    private String mineralPrincipal; // Mineral predominante
    private String observaciones;
}