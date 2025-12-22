package ucb.edu.bo.sumajflow.dto.cooperativa;

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
public class LoteDetalleDto {
    // Información del lote
    private Integer id;
    private String estado;
    private LocalDateTime fechaCreacion;
    private LocalDateTime fechaAprobacionCooperativa;
    private LocalDateTime fechaAprobacionDestino;
    private BigDecimal pesoTotalEstimado;
    private String observaciones;

    // Información de la mina
    private Integer minaId;
    private String minaNombre;
    private BigDecimal minaLatitud;
    private BigDecimal minaLongitud;
    private String sectorNombre;

    // Información del socio
    private Integer socioId;
    private String socioNombre;
    private String socioCi;
    private String socioTelefono;

    // Información de minerales
    private List<MineralInfoDto> minerales;

    // Información de operación
    private Integer camionlesSolicitados;
    private String tipoOperacion;
    private String tipoMineral;

    // Información del destino
    private Integer destinoId;
    private String destinoNombre;
    private String destinoTipo;
    private String destinoNit;
    private String destinoContacto;
    private String destinoDireccion;
}