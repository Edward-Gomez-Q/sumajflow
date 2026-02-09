// src/main/java/ucb/edu/bo/sumajflow/dto/comercializadora/TablaPreciosMineralDto.java
package ucb.edu.bo.sumajflow.dto.comercializadora;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TablaPreciosMineralDto {
    private Integer id;

    @NotBlank(message = "El mineral es obligatorio")
    @Pattern(regexp = "^(Pb|Zn|Ag)$", message = "Mineral debe ser Pb, Zn o Ag")
    private String mineral;

    @NotBlank(message = "La unidad de medida es obligatoria")
    @Size(max = 20)
    private String unidadMedida; // % o DM

    @NotNull(message = "El rango mínimo es obligatorio")
    @DecimalMin(value = "0.0001", message = "El rango mínimo debe ser mayor a 0")
    @Digits(integer = 8, fraction = 4)
    private BigDecimal rangoMinimo;

    @NotNull(message = "El rango máximo es obligatorio")
    @DecimalMin(value = "0.0001", message = "El rango máximo debe ser mayor a 0")
    @Digits(integer = 8, fraction = 4)
    private BigDecimal rangoMaximo;

    @NotNull(message = "El precio USD es obligatorio")
    @DecimalMin(value = "0.0001", message = "El precio debe ser mayor a 0")
    @Digits(integer = 10, fraction = 4)
    private BigDecimal precioUsd;

    @NotNull(message = "La fecha de inicio es obligatoria")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate fechaInicio;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate fechaFin;

    private Boolean activo;

    @Size(max = 500)
    private String observaciones;

    // Validación personalizada
    public void validar() {
        if (rangoMaximo.compareTo(rangoMinimo) <= 0) {
            throw new IllegalArgumentException("El rango máximo debe ser mayor al rango mínimo");
        }

        if (fechaFin != null && fechaFin.isBefore(fechaInicio)) {
            throw new IllegalArgumentException("La fecha fin no puede ser anterior a la fecha de inicio");
        }

        // Validar unidad según mineral
        if ("Ag".equals(mineral) && !"DM".equals(unidadMedida)) {
            throw new IllegalArgumentException("La plata (Ag) debe usar unidad DM");
        }
        if (("Pb".equals(mineral) || "Zn".equals(mineral)) && !"%".equals(unidadMedida)) {
            throw new IllegalArgumentException("Pb y Zn deben usar unidad %");
        }
    }
}