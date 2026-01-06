package ucb.edu.bo.sumajflow.dto.cooperativa;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import ucb.edu.bo.sumajflow.dto.socio.AsignacionCamionSimpleDto;
import ucb.edu.bo.sumajflow.dto.socio.AuditoriaLoteDto;
import ucb.edu.bo.sumajflow.dto.socio.MineralInfoDto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoteDetalleCooperativaDto {
    // Información del lote
    private Integer id;
    private String estado;
    private LocalDateTime fechaCreacion;
    private LocalDateTime fechaAprobacionCooperativa;
    private LocalDateTime fechaAprobacionDestino;
    private LocalDateTime fechaInicioTransporte;
    private LocalDateTime fechaFinTransporte;
    private BigDecimal pesoTotalEstimado;
    private BigDecimal pesoTotalReal;
    private String observaciones;

    // Información de la mina
    private Integer minaId;
    private String minaNombre;
    private String minaFotoUrl;
    private BigDecimal minaLatitud;
    private BigDecimal minaLongitud;

    // Información del sector
    private Integer sectorId;
    private String sectorNombre;
    private String sectorColor;

    // Información del socio (propietario de la mina)
    private Integer socioId;
    private String socioNombres;
    private String socioApellidos;
    private String socioCi;
    private String socioTelefono;
    private String socioEstado;

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
    private String destinoNIT;
    private String destinoContacto;
    private String destinoDireccion;
    private String destinoDepartamento;
    private String destinoMunicipio;
    private String destinoTelefono;
    private BigDecimal destinoAlmacenLatitud;
    private BigDecimal destinoAlmacenLongitud;
    private BigDecimal destinoBalanzaLatitud;
    private BigDecimal destinoBalanzaLongitud;

    // Información de la cooperativa (balanza)
    private BigDecimal cooperativaBalanzaLatitud;
    private BigDecimal cooperativaBalanzaLongitud;

    // Información de transporte
    private Integer camioneAsignados;
    private List<AsignacionCamionSimpleDto> asignaciones;

    // Historial de cambios
    private List<AuditoriaLoteDto> historialCambios;

    // Metadata
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}