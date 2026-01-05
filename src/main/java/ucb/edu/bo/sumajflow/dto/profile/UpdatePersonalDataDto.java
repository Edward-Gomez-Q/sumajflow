package ucb.edu.bo.sumajflow.dto.profile;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * DTO para actualizar datos personales
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdatePersonalDataDto {

    @NotBlank(message = "Los nombres son requeridos")
    private String nombres;

    @NotBlank(message = "El primer apellido es requerido")
    private String primerApellido;

    private String segundoApellido;

    @NotBlank(message = "El CI es requerido")
    private String ci;

    private LocalDate fechaNacimiento;

    @Pattern(regexp = "^\\+?[0-9\\s-]{8,20}$", message = "Formato de número de celular inválido")
    private String numeroCelular;

    private String genero;
    private String nacionalidad;
    private String departamento;
    private String provincia;
    private String municipio;
    private String direccion;
}