package ucb.edu.bo.sumajflow.dto.ingenio;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import ucb.edu.bo.sumajflow.dto.cooperativa.TransportistaAsignadoDto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LotePendienteIngenioDto {
    private Integer id;
    private String minaNombre;
    private String cooperativaNombre;
    private String socioNombre;
    private String socioCi;
    private String socioTelefono;
    private List<String> minerales;
    private Integer camionlesSolicitados;
    private String tipoMineral;
    private BigDecimal pesoTotalEstimado;
    private String estado;
    private LocalDateTime fechaCreacion;
    private LocalDateTime fechaAprobacionCooperativa;
    private String observaciones;

    // Información de transporte
    private List<TransportistaAsignadoDto> transportistasAsignados;
    private LocalDateTime fechaAsignacionTransporte;
}