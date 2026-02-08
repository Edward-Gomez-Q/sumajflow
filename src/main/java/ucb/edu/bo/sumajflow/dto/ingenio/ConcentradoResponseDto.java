package ucb.edu.bo.sumajflow.dto.ingenio;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ucb.edu.bo.sumajflow.dto.socio.MineralInfoDto;
import ucb.edu.bo.sumajflow.dto.venta.VentaLiquidacionResponseDto;

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

    // Minerales
    private String mineralPrincipal;
    private String mineralesSecundarios;
    private Boolean loteOrigenMultiple;

    // Sacos
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

    // Observaciones
    private List<Map<String, Object>> observaciones;

    // Liquidaciones
    private LiquidacionTollResponseDto liquidacionToll; // Para ingenio y socio
    private List<VentaLiquidacionResponseDto> liquidacionesVenta; // Para comercializadora (✅ NUEVO)
}