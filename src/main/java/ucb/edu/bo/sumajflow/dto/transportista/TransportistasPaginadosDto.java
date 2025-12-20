package ucb.edu.bo.sumajflow.dto.transportista;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO para respuesta paginada de transportistas
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransportistasPaginadosDto {

    private List<TransportistaResponseDto> transportistas;
    private Integer paginaActual;
    private Integer totalPaginas;
    private Long totalElementos;
    private Integer elementosPorPagina;

    // Estadísticas
    private Long totalActivos;
    private Long totalInactivos;
    private Long totalTransportando;
    private Long totalPendientes;
    private Double tonaladasTransportadasTotal;
    private Integer viajesCompletadosTotal;
    private Double kilometrosRecorridosTotal;

    public TransportistasPaginadosDto(List<TransportistaResponseDto> transportistas,
                                      Integer paginaActual,
                                      Integer totalPaginas,
                                      Long totalElementos,
                                      Integer elementosPorPagina) {
        this.transportistas = transportistas;
        this.paginaActual = paginaActual;
        this.totalPaginas = totalPaginas;
        this.totalElementos = totalElementos;
        this.elementosPorPagina = elementosPorPagina;
    }
}