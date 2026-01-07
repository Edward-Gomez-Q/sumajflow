package ucb.edu.bo.sumajflow.dto.comercializadora;

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
public class LoteDetalleComercializadoraDto {
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

    // Información de la cooperativa
    private Integer cooperativaId;
    private String cooperativaNombre;
    private BigDecimal cooperativaBalanzaLatitud;
    private BigDecimal cooperativaBalanzaLongitud;

    // Información del socio
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

    // Información de la comercializadora (destino)
    private Integer comercializadoraId;
    private String comercializadoraNombre;
    private String comercializadoraNIT;
    private String comercializadoraContacto;
    private String comercializadoraDireccion;
    private String comercializadoraDepartamento;
    private String comercializadoraMunicipio;
    private String comercializadoraTelefono;
    private BigDecimal comercializadoraAlmacenLatitud;
    private BigDecimal comercializadoraAlmacenLongitud;
    private BigDecimal comercializadoraBalanzaLatitud;
    private BigDecimal comercializadoraBalanzaLongitud;

    // Información de transporte
    private Integer camioneAsignados;
    private List<AsignacionCamionSimpleDto> asignaciones;

    // Historial de cambios
    private List<AuditoriaLoteDto> historialCambios;

    // Metadata
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}