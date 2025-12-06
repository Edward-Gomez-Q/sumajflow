package ucb.edu.bo.sumajflow.dto.socio;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO de respuesta para minas
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MinaResponseDto {

    private Integer id;
    private String nombre;
    private String fotoUrl;
    private BigDecimal latitud;
    private BigDecimal longitud;
    private String estado; // "activo" o "inactivo"

    // Información del sector
    private Integer sectorId;
    private String sectorNombre;
    private String sectorColor;

    // Información del socio propietario
    private Integer socioId;
    private String socioNombre;

    // Metadatos
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

}