// dto/transportista/CompletarOnboardingDto.java
package ucb.edu.bo.sumajflow.dto.transportista;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDate;

@Data
public class CompletarOnboardingDto {

    @NotBlank(message = "El token es requerido")
    private String token;

    @NotBlank(message = "El CI es requerido")
    @Pattern(regexp = "^[0-9]{5,15}$", message = "CI inválido")
    private String ci;

    @NotNull(message = "La fecha de nacimiento es requerida")
    private LocalDate fechaNacimiento;

    @NotBlank(message = "El correo es requerido")
    @Email(message = "Correo electrónico inválido")
    private String correo;

    @NotBlank(message = "La contraseña es requerida")
    @Size(min = 8, message = "La contraseña debe tener al menos 8 caracteres")
    private String contrasena;

    @NotBlank(message = "La placa del vehículo es requerida")
    private String placaVehiculo;

    @NotBlank(message = "La marca del vehículo es requerida")
    private String marcaVehiculo;

    @NotBlank(message = "El modelo del vehículo es requerido")
    private String modeloVehiculo;

    @NotBlank(message = "El color del vehículo es requerido")
    private String colorVehiculo;

    @NotNull(message = "El peso tara es requerido")
    @Positive(message = "El peso tara debe ser positivo")
    private Double pesoTara;

    @NotNull(message = "La capacidad de carga es requerida")
    @Positive(message = "La capacidad de carga debe ser positiva")
    private Double capacidadCarga;

    @NotBlank(message = "La foto de la licencia es requerida")
    private String licenciaConducirUrl;

    @NotBlank(message = "La categoría de licencia es requerida")
    @Pattern(regexp = "^(A|B|C|D|E)$", message = "Categoría de licencia inválida")
    private String categoriaLicencia;

    @NotNull(message = "La fecha de vencimiento de licencia es requerida")
    private LocalDate fechaVencimientoLicencia;
}