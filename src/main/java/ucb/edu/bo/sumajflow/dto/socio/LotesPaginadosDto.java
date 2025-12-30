// src/main/java/ucb/edu/bo/sumajflow/dto/socio/LotesPaginadosDto.java
package ucb.edu.bo.sumajflow.dto.socio;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LotesPaginadosDto {
    private List<LoteResponseDto> lotes;
    private long totalElementos;
    private int totalPaginas;
    private int paginaActual;
    private int elementosPorPagina;
    private boolean tieneSiguiente;
    private boolean tieneAnterior;
}