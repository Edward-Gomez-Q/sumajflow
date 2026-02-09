package ucb.edu.bo.sumajflow.dto.cooperativa;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConcentradoVendidoDto {
    private Integer concentradoId;
    private String codigoConcentrado;
    private String mineralPrincipal;
    private String estado;
    private String mensajeEstado; // Para mostrar "Aún no vendido" o "Vendido"
    private List<DeduccionVentaDto> deducciones;
}