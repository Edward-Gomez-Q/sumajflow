package ucb.edu.bo.sumajflow.dto.socio;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoteResponseDto {
    private Integer id;
    private Integer minaId;
    private String minaNombre;
    private List<MineralInfoDto> minerales;
    private Integer camionlesSolicitados;
    private String tipoOperacion;
    private String tipoMineral;
    private String estado;
    private LocalDateTime fechaCreacion;
    private BigDecimal pesoTotalEstimado;
    private String observaciones;

    // Información del destino
    private Integer destinoId;
    private String destinoNombre;
    private String destinoTipo; // "ingenio" o "comercializadora"

    // Metadatos
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}