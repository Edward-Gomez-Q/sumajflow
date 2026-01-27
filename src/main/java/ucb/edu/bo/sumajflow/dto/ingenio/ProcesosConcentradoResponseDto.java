package ucb.edu.bo.sumajflow.dto.ingenio;

import lombok.*;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProcesosConcentradoResponseDto {

    private Integer concentradoId;
    private String codigoConcentrado;
    private String estadoConcentrado;
    private Integer totalProcesos;
    private Integer procesosCompletados;
    private Integer procesosPendientes;
    private ProcesoPlantaDto procesoActual;
    private List<ProcesoPlantaDto> todosProcesos;
}