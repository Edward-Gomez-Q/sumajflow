package ucb.edu.bo.sumajflow.dto.socio;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoteCreateDto {
    private Integer minaId;
    private List<Integer> mineralesIds; // Lista de IDs de minerales
    private Integer camionlesSolicitados;
    private String tipoOperacion; // "procesamiento_planta" o "venta_directa"
    private Integer destinoId; // ID del ingenio o comercializadora
    private String tipoMineral; // "complejo" o "concentrado"
    private BigDecimal pesoTotalEstimado; // Opcional
    private String observaciones; // Opcional
}