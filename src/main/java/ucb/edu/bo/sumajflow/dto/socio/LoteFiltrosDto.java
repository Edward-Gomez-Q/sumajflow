// src/main/java/ucb/edu/bo/sumajflow/dto/socio/LoteFiltrosDto.java
package ucb.edu.bo.sumajflow.dto.socio;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class LoteFiltrosDto {
    private String estado;              // Filtro por estado específico
    private String tipoOperacion;       // procesamiento_planta o venta_directa
    private String tipoMineral;         // complejo o concentrado
    private LocalDateTime fechaDesde;   // Fecha desde
    private LocalDateTime fechaHasta;   // Fecha hasta
    private Integer minaId;             // Filtro por mina específica
    private Integer destinoId;          // Filtro por destino (ingenio o comercializadora)

    // Paginación
    private Integer page = 0;           // Página actual (default 0)
    private Integer size = 10;          // Tamaño de página (default 10)

    // Ordenamiento
    private String sortBy = "fechaCreacion";  // Campo por el que ordenar
    private String sortDir = "desc";          // asc o desc
}