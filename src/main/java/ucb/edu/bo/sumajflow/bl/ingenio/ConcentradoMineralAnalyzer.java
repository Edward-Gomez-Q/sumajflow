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
    public ConcentradosPlanificados determinarConcentrados(List<LoteMinerales> loteMinerales) {
        // Obtener nomenclaturas de minerales presentes
        Set<String> mineralesPresentes = loteMinerales.stream()
                .map(lm -> lm.getMineralesId().getNomenclatura())
                .collect(Collectors.toSet());

        log.debug("Minerales detectados en el lote: {}", mineralesPresentes);

        boolean tieneZn = mineralesPresentes.contains(ZINC);
        boolean tienePb = mineralesPresentes.contains(PLOMO);
        boolean tieneAg = mineralesPresentes.contains(PLATA);

        List<ConcentradoPlanificado> concentrados = new ArrayList<>();

        if (tieneZn && tienePb) {
            log.info("Caso detectado: Zn + Pb => Crear 2 concentrados");

            // Concentrado de Zinc
            ConcentradoPlanificado concZn = ConcentradoPlanificado.builder()
                    .mineralPrincipal(ZINC)
                    .mineralesSecundarios(tienePb ? PLOMO : null)
                    .mineralesTraza(tieneAg ? PLATA : null)
                    .porcentajePeso(50) // Por defecto 50/50, puede ajustarse
                    .build();

            // Concentrado de Plomo
            ConcentradoPlanificado concPb = ConcentradoPlanificado.builder()
                    .mineralPrincipal(PLOMO)
                    .mineralesSecundarios(tieneZn ? ZINC : null)
                    .mineralesTraza(tieneAg ? PLATA : null)
                    .porcentajePeso(50)
                    .build();

            concentrados.add(concZn);
            concentrados.add(concPb);
        }
        else if (tieneZn && tieneAg && !tienePb) {
            log.info("Caso detectado: Zn + Ag => Crear 1 concentrado de Zn");

            ConcentradoPlanificado concZn = ConcentradoPlanificado.builder()
                    .mineralPrincipal(ZINC)
                    .mineralesTraza(PLATA)
                    .porcentajePeso(100)
                    .build();

            concentrados.add(concZn);
        }
        else if (tienePb && tieneAg && !tieneZn) {
            log.info("Caso detectado: Pb + Ag => Crear 1 concentrado de Pb");

            ConcentradoPlanificado concPb = ConcentradoPlanificado.builder()
                    .mineralPrincipal(PLOMO)
                    .mineralesTraza(PLATA)
                    .porcentajePeso(100)
                    .build();

            concentrados.add(concPb);
        }
        else if (tieneZn && !tienePb && !tieneAg) {
            log.info("Caso detectado: Solo Zn => Crear 1 concentrado de Zn");

            ConcentradoPlanificado concZn = ConcentradoPlanificado.builder()
                    .mineralPrincipal(ZINC)
                    .porcentajePeso(100)
                    .build();

            concentrados.add(concZn);
        }
        else if (tienePb && !tieneZn && !tieneAg) {
            log.info("Caso detectado: Solo Pb => Crear 1 concentrado de Pb");

            ConcentradoPlanificado concPb = ConcentradoPlanificado.builder()
                    .mineralPrincipal(PLOMO)
                    .porcentajePeso(100)
                    .build();

            concentrados.add(concPb);
        }
        else if (tieneAg && !tieneZn && !tienePb) {
            log.warn("Caso detectado: Solo Ag => Crear 1 concentrado de Ag (requiere revisión)");

            ConcentradoPlanificado concAg = ConcentradoPlanificado.builder()
                    .mineralPrincipal(PLATA)
                    .porcentajePeso(100)
                    .requiereRevision(true)
                    .observacionRevision("Concentrado solo de Plata - caso inusual")
                    .build();

            concentrados.add(concAg);
        }
        // CASO DESCONOCIDO
        else {
            log.error("No se detectaron minerales válidos en el lote");
            throw new IllegalArgumentException(
                    "No se pudieron determinar minerales válidos para crear concentrados. Minerales detectados: " +
                            mineralesPresentes
            );
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

        if (planificado.getMineralesSecundarios() != null) {
            secundarios.add(planificado.getMineralesSecundarios());
        }

        if (planificado.getMineralesTraza() != null) {
            secundarios.add(planificado.getMineralesTraza());
        }

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
        private Integer porcentajePeso; // 0-100

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