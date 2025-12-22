package ucb.edu.bo.sumajflow.dto.ingenio;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConcentradoDetalleDto {
    private Integer id;
    private String codigoConcentrado;
    private Integer ingenioMineroId;
    private String ingenioNombre;
    private Integer socioPropietarioId;
    private String socioPropietarioNombre;
    private String socioCi;
    private String socioTelefono;
    private BigDecimal pesoInicial;
    private BigDecimal pesoFinal;
    private BigDecimal merma;
    private BigDecimal porcentajeMerma;
    private String mineralPrincipal;
    private String estado;
    private LocalDateTime fechaInicio;
    private LocalDateTime fechaFin;
    private LocalDateTime createdAt;

    // Lotes relacionados
    private List<LoteConcentradoRelacionDto> lotesRelacionados;

    // Procesos de planta
    private List<ProcesoPlantaDto> procesos;
}