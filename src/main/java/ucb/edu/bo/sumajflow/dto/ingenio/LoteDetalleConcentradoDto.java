package ucb.edu.bo.sumajflow.dto.ingenio;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoteDetalleConcentradoDto {

    private Integer id;
    private String minaNombre;
    private String sectorNombre;
    private String cooperativaNombre;
    private String tipoMineral;
    private BigDecimal pesoTotalReal;
    private BigDecimal pesoEntrada;
    private BigDecimal pesoSalida;
    private BigDecimal porcentajeRecuperacion;
    private String estado;
    private LocalDateTime fechaCreacion;
}