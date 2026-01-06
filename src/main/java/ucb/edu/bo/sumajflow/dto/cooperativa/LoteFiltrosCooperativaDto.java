package ucb.edu.bo.sumajflow.dto.cooperativa;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoteFiltrosCooperativaDto {
    // Filtros básicos
    private String estado;
    private String tipoOperacion;
    private String tipoMineral;

    // Filtros por fechas
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime fechaDesde;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime fechaHasta;

    // Filtros por entidades relacionadas
    private Integer socioId;
    private Integer minaId;
    private Integer sectorId;

    // Paginación y ordenamiento
    private Integer page = 0;
    private Integer size = 10;
    private String sortBy = "fechaCreacion";
    private String sortDir = "desc";
}