package ucb.edu.bo.sumajflow.dto.ingenio;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class KanbanColumnaDto {
    private String id;
    private String titulo;
    private String estado;
    private String color;
    private List<ConcentradoKanbanDto> concentrados;
}
