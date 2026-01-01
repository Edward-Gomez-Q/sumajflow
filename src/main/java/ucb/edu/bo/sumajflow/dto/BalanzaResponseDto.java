package ucb.edu.bo.sumajflow.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BalanzaResponseDto {
    private Integer id;
    private String nombre;
    private String marca;
    private String modelo;
    private String numeroSerie;
    private BigDecimal capacidadMaxima;
    private BigDecimal precisionMinima;
    private LocalDate fechaUltimaCalibracion;
    private LocalDate fechaProximaCalibracion;
    private String departamento;
    private String provincia;
    private String municipio;
    private String direccion;
    private BigDecimal latitud;
    private BigDecimal longitud;
    private String tipoEntidad; // "cooperativa", "ingenio", "comercializadora"
    private Integer entidadId;

    // Campos calculados para estadísticas
    private Integer diasParaCalibracion;
    private String estadoCalibracion; // "vigente", "proximo_vencimiento", "vencido"
    private Integer totalDivisiones;
}