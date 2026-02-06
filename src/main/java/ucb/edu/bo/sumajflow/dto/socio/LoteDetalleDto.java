// src/main/java/ucb/edu/bo/sumajflow/dto/socio/LoteDetalleDto.java
package ucb.edu.bo.sumajflow.dto.socio;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import ucb.edu.bo.sumajflow.dto.ingenio.LiquidacionTollResponseDto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoteDetalleDto {
    private Integer id;
    private String estado;
    private Integer camionlesSolicitados;
    private String tipoOperacion;
    private String tipoMineral;

    private BigDecimal cooperativaBalanzaLatitud;
    private BigDecimal cooperativaBalanzaLongitud;

    private Integer minaId;
    private String minaNombre;
    private String minaFotoUrl;
    private Double minaLatitud;
    private Double minaLongitud;
    private String sectorNombre;

    private List<MineralInfoDto> minerales;

    private Integer destinoId;
    private String destinoNombre;
    private String destinoTipo;
    private String destinoNIT;
    private String destinoDireccion;
    private String destinoDepartamento;
    private String destinoMunicipio;
    private String destinoTelefono;
    private BigDecimal destinoBalanzaLatitud;
    private BigDecimal destinoBalanzaLongitud;
    private BigDecimal destinoAlmacenLatitud;
    private BigDecimal destinoAlmacenLongitud;

    private LocalDateTime fechaCreacion;
    private LocalDateTime fechaAprobacionCooperativa;
    private LocalDateTime fechaAprobacionDestino;
    private LocalDateTime fechaInicioTransporte;
    private LocalDateTime fechaFinTransporte;

    private BigDecimal pesoTotalEstimado;
    private BigDecimal pesoTotalReal;

    private String observaciones;

    private Integer socioId;
    private String socioNombres;
    private String socioApellidos;

    private Integer camioneAsignados;
    private List<AsignacionCamionSimpleDto> asignaciones;

    private List<AuditoriaLoteDto> historialCambios;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private LiquidacionTollResponseDto liquidacionToll;
}