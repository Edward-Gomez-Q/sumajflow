package ucb.edu.bo.sumajflow.dto.ingenio;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoteFiltrosIngenioDto {
    private String estado;
    private String tipoMineral;
    private String cooperativaNombre;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime fechaDesde;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime fechaHasta;

    private Integer page = 0;
    private Integer size = 10;
    private String sortBy = "fechaCreacion";
    private String sortDir = "desc";
}