package ucb.edu.bo.sumajflow.dto.ingenio;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
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

    // Pesos
    private BigDecimal pesoInicial;
    private BigDecimal pesoFinal;
    private BigDecimal merma;

    // Minerales - ACTUALIZADO
    private String mineralPrincipal;
    private String mineralesSecundarios; // NUEVO: minerales secundarios/traza separados por comas
    private Boolean loteOrigenMultiple; // NUEVO: indica si proviene de separación múltiple

    // Sacos (opcional al inicio)
    private Integer numeroSacos;

    // Ingenio
    private Integer ingenioId;
    private String ingenioNombre;

    // Socio propietario
    private Integer socioId;
    private String socioNombres;
    private String socioApellidos;
    private String socioCi;

    // Lotes asociados
    private List<LoteSimpleDto> lotes;

    // Minerales presentes
    private List<MineralInfoDto> minerales;

    // Fechas
    private LocalDateTime fechaInicio;
    private LocalDateTime fechaFin;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Observaciones (último registro del historial)
    private Map<String, Object> observaciones;
}