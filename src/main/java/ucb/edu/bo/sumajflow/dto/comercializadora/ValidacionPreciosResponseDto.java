// src/main/java/ucb/edu/bo/sumajflow/dto/comercializadora/ValidacionPreciosResponseDto.java
package ucb.edu.bo.sumajflow.dto.comercializadora;

import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ValidacionPreciosResponseDto {
    private Boolean configuracionCompleta;
    private List<String> mineralesFaltantes;
    private List<String> advertencias;
    private Integer totalRangosPb;
    private Integer totalRangosZn;
    private Integer totalRangosAg;

    @Builder.Default
    private List<String> errores = new ArrayList<>();

    public void agregarError(String error) {
        if (errores == null) errores = new ArrayList<>();
        errores.add(error);
        configuracionCompleta = false;
    }

    public void agregarAdvertencia(String advertencia) {
        if (advertencias == null) advertencias = new ArrayList<>();
        advertencias.add(advertencia);
    }
}