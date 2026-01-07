package ucb.edu.bo.sumajflow.dto.ingenio;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import ucb.edu.bo.sumajflow.dto.socio.MineralInfoDto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoteIngenioResponseDto {
    // Información básica del lote
    private Integer id;
    private String estado;
    private String tipoMineral;
    private Integer camionlesSolicitados;
    private BigDecimal pesoTotalEstimado;
    private BigDecimal pesoTotalReal;

    // Información de la mina
    private Integer minaId;
    private String minaNombre;

    // Información del sector
    private Integer sectorId;
    private String sectorNombre;

    // Información de la cooperativa
    private Integer cooperativaId;
    private String cooperativaNombre;

    // Información del socio
    private Integer socioId;
    private String socioNombres;
    private String socioApellidos;
    private String socioCi;
    private String socioTelefono;

    // Información de minerales
    private List<MineralInfoDto> minerales;

    // Fechas importantes
    private LocalDateTime fechaCreacion;
    private LocalDateTime fechaAprobacionCooperativa;
    private LocalDateTime fechaAprobacionDestino;

    // Información de transporte
    private Integer camioneAsignados;

    // Observaciones
    private String observaciones;

    // Metadata
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}