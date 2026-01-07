package ucb.edu.bo.sumajflow.dto.comercializadora;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LotesComercializadoraPaginadosDto {
    private List<LoteComercializadoraResponseDto> lotes;
    private long totalElements;
    private int totalPages;
    private int currentPage;
    private int pageSize;
    private boolean hasNext;
    private boolean hasPrevious;
}