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
public class ConcentradoDetalleDto {

    // Información básica
    private Integer id;
    private String codigoConcentrado;
    private String estado;
    private BigDecimal pesoInicial;
    private BigDecimal pesoFinal;
    private BigDecimal merma;
    private String mineralPrincipal;
    private Integer numeroSacos;

    // Ingenio
    private Integer ingenioId;
    private String ingenioNombre;
    private String ingenioNIT;
    private String ingenioContacto;
    private String ingenioDireccion;
    private String ingenioDepartamento;
    private String ingenioMunicipio;
    private String almacenNombre;
    private BigDecimal almacenLatitud;
    private BigDecimal almacenLongitud;
    private Integer socioId;
    private String socioNombres;
    private String socioApellidos;
    private String socioCi;
    private String socioTelefono;
    private List<LoteDetalleConcentradoDto> lotes;
    private List<MineralInfoDto> minerales;
    private List<ProcesoPlantaDto> procesos;
    private ReporteQuimicoSimpleDto reporteQuimico;
    private LocalDateTime fechaInicio;
    private LocalDateTime fechaFin;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<Map<String, Object>> historialCambios;
}