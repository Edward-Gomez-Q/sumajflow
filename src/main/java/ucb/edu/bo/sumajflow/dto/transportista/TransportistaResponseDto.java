package ucb.edu.bo.sumajflow.dto.transportista;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO de respuesta con información completa del transportista
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransportistaResponseDto {

    private Integer id;
    private Integer usuarioId;
    private String nombreCompleto;
    private String ci;
    private String numeroCelular;
    private String correo;

    // Información de licencia
    private String licenciaConducir;
    private String categoriaLicencia;
    private String fechaVencimientoLicencia;

    // Información del vehículo
    private String placaVehiculo;
    private String marcaVehiculo;
    private String modeloVehiculo;
    private String colorVehiculo;
    private Double pesoTara;
    private Double capacidadCarga;

    // Estado y fechas
    private String estado;
    private LocalDateTime fechaCreacion;
    private LocalDateTime fechaAprobacion;

    // Estadísticas
    private Integer viajesCompletados;
    private Double calificacionPromedio;

    // Información adicional para cooperativa
    private String estadoCuenta; // "pendiente", "aprobado", "rechazado"
    private String estadoTrazabilidad; // "habilitado", "asignado", "en_ruta", "de_regreso"
}