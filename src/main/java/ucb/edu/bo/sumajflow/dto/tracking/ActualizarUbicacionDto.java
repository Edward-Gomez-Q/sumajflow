package ucb.edu.bo.sumajflow.dto.tracking;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO para actualizar ubicación desde el móvil
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActualizarUbicacionDto {

    @NotNull(message = "La asignación de camión es requerida")
    private Integer asignacionCamionId;

    @NotNull(message = "La latitud es requerida")
    @Min(value = -90, message = "Latitud inválida")
    @Max(value = 90, message = "Latitud inválida")
    private Double lat;

    @NotNull(message = "La longitud es requerida")
    @Min(value = -180, message = "Longitud inválida")
    @Max(value = 180, message = "Longitud inválida")
    private Double lng;

    private Double precision;      // metros
    private Double velocidad;      // km/h
    private Double rumbo;          // grados 0-360
    private Double altitud;        // metros

    // Para sincronización offline
    private LocalDateTime timestampCaptura;  // Cuándo se capturó realmente
    private Boolean esOffline;               // Si viene de sincronización offline
}

/**
 * DTO para sincronización de ubicaciones offline (batch)
 */


