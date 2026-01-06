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
public class LoteCooperativaResponseDto {
    // Información básica del lote
    private Integer id;
    private String estado;
    private Integer camionesSolicitados;
    private String tipoOperacion;
    private String tipoMineral;

    // Información de la mina
    private Integer minaId;
    private String minaNombre;
    private String minaDireccion;
    private BigDecimal minaLatitud;
    private BigDecimal minaLongitud;

    // Información del sector
    private Integer sectorId;
    private String sectorNombre;
    private String sectorColor;

    // Información del socio (dueño de la mina)
    private Integer socioId;
    private String socioNombres;
    private String socioApellidos;
    private String socioCi;
    private String socioTelefono;
    private String socioEstado; // estado de la afiliación del socio

    // Minerales del lote
    private List<MineralInfoDto> minerales;

    // Información del destino
    private Integer destinoId;
    private String destinoNombre;
    private String destinoTipo; // "ingenio" o "comercializadora"
    private String destinoNit;

    // Fechas del lote
    private LocalDateTime fechaCreacion;
    private LocalDateTime fechaAprobacionCooperativa;
    private LocalDateTime fechaAprobacionDestino;
    private LocalDateTime fechaInicioTransporte;
    private LocalDateTime fechaFinTransporte;

    // Pesos
    private BigDecimal pesoTotalEstimado;
    private BigDecimal pesoTotalReal;

    // Información de transporte
    private Integer camioneAsignados;

    // Observaciones
    private String observaciones;

    // Metadata
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}