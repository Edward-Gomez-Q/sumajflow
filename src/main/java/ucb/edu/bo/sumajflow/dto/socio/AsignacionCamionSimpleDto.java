// src/main/java/ucb/edu/bo/sumajflow/dto/socio/AsignacionCamionSimpleDto.java
package ucb.edu.bo.sumajflow.dto.socio;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AsignacionCamionSimpleDto {
    private Integer id;
    private Integer numeroCamion;
    private String estado;
    private LocalDateTime fechaAsignacion;

    // Info del transportista
    private Integer transportistaId;
    private String transportistaNombre;
    private String transportistaPlaca;
    private String transportistaTelefono;

    // Pesaje Origen (Cooperativa)
    private BigDecimal pesajeOrigenTaraKg;
    private BigDecimal pesajeOrigenBrutoKg;
    private BigDecimal pesajeOrigenNetoKg;
    private LocalDateTime pesajeOrigenFecha;

    // Pesaje Destino (Ingenio/Comercializadora)
    private BigDecimal pesajeDestinoTaraKg;
    private BigDecimal pesajeDestinoBrutoKg;
    private BigDecimal pesajeDestinoNetoKg;
    private LocalDateTime pesajeDestinoFecha;

    // Constructor simplificado
    public AsignacionCamionSimpleDto(Integer id, @NotNull Integer numeroCamion, @Size(max = 50) String estado,
                                     LocalDateTime fechaAsignacion, Integer transportistaId, String transportistaNombre,
                                     @NotNull @Size(min = 1, max = 20) String placaVehiculo, @Size(max = 50) String numeroCelular) {
        this.id = id;
        this.numeroCamion = numeroCamion;
        this.estado = estado;
        this.fechaAsignacion = fechaAsignacion;
        this.transportistaId = transportistaId;
        this.transportistaNombre = transportistaNombre;
        this.transportistaPlaca = placaVehiculo;
        this.transportistaTelefono = numeroCelular;
    }
}