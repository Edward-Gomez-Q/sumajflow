package ucb.edu.bo.sumajflow.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlmacenResponseDto {
    private Integer id;
    private String nombre;
    private BigDecimal capacidadMaxima; // en toneladas
    private BigDecimal area; // en m²
    private String departamento;
    private String provincia;
    private String municipio;
    private String direccion;
    private BigDecimal latitud;
    private BigDecimal longitud;
    private String tipoEntidad; // "ingenio", "comercializadora"
    private Integer entidadId;

    // Campos calculados para KPIs
    private BigDecimal capacidadDisponible; // capacidadMaxima - ocupacionActual
    private BigDecimal ocupacionActual; // suma de lotes en almacén
    private BigDecimal porcentajeOcupacion; // (ocupacionActual / capacidadMaxima) * 100
    private String estadoCapacidad; // "disponible", "medio", "critico", "lleno"
    private Integer totalLotesAlmacenados; // cantidad de lotes en almacén
}