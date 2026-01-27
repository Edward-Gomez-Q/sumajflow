package ucb.edu.bo.sumajflow.dto.ingenio;

import lombok.*;
import ucb.edu.bo.sumajflow.dto.socio.MineralInfoDto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConcentradoResponseDto {

    private Integer id;
    private String codigoConcentrado;
    private String estado;
    private BigDecimal pesoInicial;
    private BigDecimal pesoFinal;
    private BigDecimal merma;
    private String mineralPrincipal;
    private Integer numeroSacos;
    private Integer ingenioId;
    private String ingenioNombre;
    private Integer socioId;
    private String socioNombres;
    private String socioApellidos;
    private String socioCi;
    private List<LoteSimpleDto> lotes;
    private List<MineralInfoDto> minerales;
    private LocalDateTime fechaInicio;
    private LocalDateTime fechaFin;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Map<String, Object> observaciones;
}