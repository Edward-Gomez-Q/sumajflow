package ucb.edu.bo.sumajflow.dto.socio;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * DTO para la respuesta de información de un socio
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SocioResponseDto {

    private Integer id;
    private Integer usuarioId;

    // Información personal
    private String nombres;
    private String primerApellido;
    private String segundoApellido;
    private String nombreCompleto;
    private String ci;
    private LocalDate fechaNacimiento;
    private String numeroCelular;
    private String genero;

    // Información de socio
    private String estado;
    private LocalDateTime fechaEnvio;
    private String carnetAfiliacionUrl;
    private String carnetIdentidadUrl;

    // Información de cooperativa-socio
    private Integer cooperativaSocioId;
    private LocalDate fechaAfiliacion;
    private String observaciones;

    // Información adicional
    private String correo;
}