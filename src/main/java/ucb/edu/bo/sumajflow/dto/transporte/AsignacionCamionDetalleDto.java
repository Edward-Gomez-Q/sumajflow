package ucb.edu.bo.sumajflow.dto.transporte;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AsignacionCamionDetalleDto {
    private Integer id;
    private Integer loteId;
    private Integer numeroCamion;
    private String estado;
    private LocalDateTime fechaAsignacion;
    private LocalDateTime fechaInicio;
    private LocalDateTime fechaFin;
    private String observaciones;

    // Información del transportista
    private Integer transportistaId;
    private String transportistaNombre;
    private String transportistaCi;
    private String transportistaTelefono;
    private String placaVehiculo;
    private String marcaVehiculo;
    private String modeloVehiculo;

    // Información del lote
    private String minaNombre;
    private String destinoNombre;
    private String tipoOperacion;

    // Pesajes realizados
    private List<PesajeResponseDto> pesajes;
}