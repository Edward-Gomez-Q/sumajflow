package ucb.edu.bo.sumajflow.dto.socio;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CamionEnRutaDto {
    private Integer loteId;
    private String lotecodigo; // Formato: LT-2026-001
    private Integer asignacionId;
    private Integer numeroCamion;
    private String placaVehiculo;
    private String conductorNombre;
    private String estadoViaje;
    private Integer progreso; // 0-100
    private String ultimaUbicacionTexto; // Ej: "Cerca de Balanza Cooperativa"
    private Integer minutosTranscurridos;
}
