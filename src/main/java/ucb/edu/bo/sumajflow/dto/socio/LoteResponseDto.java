package ucb.edu.bo.sumajflow.dto.socio;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class LoteResponseDto {
    private Integer id;

    // Información de la mina
    private Integer minaId;
    private String minaNombre;

    // Minerales
    private List<MineralInfoDto> minerales;

    // Configuración del lote
    private Integer camionlesSolicitados;
    private String tipoOperacion;
    private String tipoMineral;

    // Destino
    private Integer destinoId;
    private String destinoNombre;
    private String destinoTipo; // "ingenio" o "comercializadora"

    // Estado y fechas
    private String estado;
    private LocalDateTime fechaCreacion;
    private LocalDateTime fechaAprobacionCooperativa;
    private LocalDateTime fechaAprobacionDestino;
    private LocalDateTime fechaInicioTransporte;
    private LocalDateTime fechaFinTransporte;

    // Pesos
    private BigDecimal pesoTotalEstimado;
    private BigDecimal pesoTotalReal;

    // Información adicional
    private String observaciones;

    // Metadatos
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}