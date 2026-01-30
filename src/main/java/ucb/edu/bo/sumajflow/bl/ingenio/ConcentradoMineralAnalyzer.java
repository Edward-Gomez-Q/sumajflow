package ucb.edu.bo.sumajflow.bl.ingenio;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ucb.edu.bo.sumajflow.entity.LoteMinerales;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Component
public class ConcentradoMineralAnalyzer {

    // Constantes de minerales
    private static final String ZINC = "Zn";
    private static final String PLOMO = "Pb";
    private static final String PLATA = "Ag";

    /**
     * Analiza los minerales de un lote y determina qué concentrados crear
     */
    public ConcentradosPlanificados determinarConcentradosDesdeSet(Set<String> mineralesPresentes) {
        log.debug("Minerales batch detectados: {}", mineralesPresentes);

        boolean tieneZn = mineralesPresentes.contains(ZINC);
        boolean tienePb = mineralesPresentes.contains(PLOMO);
        boolean tieneAg = mineralesPresentes.contains(PLATA);

        List<ConcentradoPlanificado> concentrados = new ArrayList<>();

        if (tieneZn && tienePb) {
            log.info("Caso batch: Zn + Pb => Crear 2 concentrados (Zn y Pb). Secundarios: solo Ag si existe.");

            ConcentradoPlanificado concZn = ConcentradoPlanificado.builder()
                    .mineralPrincipal(ZINC)
                    .mineralesSecundarios(null)               // <- NO cruzar Pb
                    .mineralesTraza(tieneAg ? PLATA : null)   // <- solo Ag
                    .porcentajePeso(50)
                    .build();

            ConcentradoPlanificado concPb = ConcentradoPlanificado.builder()
                    .mineralPrincipal(PLOMO)
                    .mineralesSecundarios(null)               // <- NO cruzar Zn
                    .mineralesTraza(tieneAg ? PLATA : null)
                    .porcentajePeso(50)
                    .build();

            concentrados.add(concZn);
            concentrados.add(concPb);
        } else if (tieneZn && !tienePb) {
            // Zn (con o sin Ag)
            ConcentradoPlanificado concZn = ConcentradoPlanificado.builder()
                    .mineralPrincipal(ZINC)
                    .mineralesTraza(tieneAg ? PLATA : null)
                    .porcentajePeso(100)
                    .build();
            concentrados.add(concZn);
        } else if (tienePb && !tieneZn) {
            // Pb (con o sin Ag)
            ConcentradoPlanificado concPb = ConcentradoPlanificado.builder()
                    .mineralPrincipal(PLOMO)
                    .mineralesTraza(tieneAg ? PLATA : null)
                    .porcentajePeso(100)
                    .build();
            concentrados.add(concPb);
        } else if (tieneAg) {
            // Solo Ag
            ConcentradoPlanificado concAg = ConcentradoPlanificado.builder()
                    .mineralPrincipal(PLATA)
                    .porcentajePeso(100)
                    .requiereRevision(true)
                    .observacionRevision("Concentrado solo de Plata - caso inusual")
                    .build();
            concentrados.add(concAg);
        } else {
            throw new IllegalArgumentException("No se pudieron determinar minerales válidos. Minerales: " + mineralesPresentes);
        }

        return ConcentradosPlanificados.builder()
                .concentrados(concentrados)
                .esMultiple(concentrados.size() > 1)
                .build();
    }
    /**
     * Construye la cadena de minerales secundarios para guardar en BD
     */
    public String construirMineralesSecundarios(ConcentradoPlanificado planificado) {
        List<String> secundarios = new ArrayList<>();

        // Regla: en BD guardamos solo traza (Ag) y/o un secundario real si existiera en otros escenarios.
        if (planificado.getMineralesSecundarios() != null) secundarios.add(planificado.getMineralesSecundarios());
        if (planificado.getMineralesTraza() != null) secundarios.add(planificado.getMineralesTraza());

        return secundarios.isEmpty() ? null : String.join(", ", secundarios);
    }

    /**
     * Clase interna para representar un concentrado planificado
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ConcentradoPlanificado {
        private String mineralPrincipal;
        private String mineralesSecundarios;
        private String mineralesTraza;
        private Integer porcentajePeso;

        @Builder.Default
        private Boolean requiereRevision = false;
        private String observacionRevision;
    }

    /**
     * Clase interna para agrupar todos los concentrados planificados
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ConcentradosPlanificados {
        private List<ConcentradoPlanificado> concentrados;
        private Boolean esMultiple; // true si se crean 2+ concentrados del mismo lote
    }
}