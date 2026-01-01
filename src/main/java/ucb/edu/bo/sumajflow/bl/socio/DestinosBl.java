package ucb.edu.bo.sumajflow.bl.socio;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ucb.edu.bo.sumajflow.dto.socio.ComercializadoraSimpleDto;
import ucb.edu.bo.sumajflow.dto.socio.IngenioSimpleDto;
import ucb.edu.bo.sumajflow.entity.Comercializadora;
import ucb.edu.bo.sumajflow.entity.IngenioMinero;
import ucb.edu.bo.sumajflow.repository.ComercializadoraRepository;
import ucb.edu.bo.sumajflow.repository.IngenioMineroRepository;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DestinosBl {

    private final IngenioMineroRepository ingenioMineroRepository;
    private final ComercializadoraRepository comercializadoraRepository;

    /**
     * Obtener lista de ingenios mineros disponibles
     */
    @Transactional(readOnly = true)
    public List<IngenioSimpleDto> getIngeniosDisponibles() {
        log.debug("Obteniendo lista de ingenios mineros disponibles");

        List<IngenioMinero> ingenios = ingenioMineroRepository.findAllActive();

        log.info("Se encontraron {} ingenios disponibles", ingenios.size());

        return ingenios.stream()
                .map(this::convertIngenioToDto)
                .collect(Collectors.toList());
    }

    /**
     * Obtener lista de comercializadoras disponibles
     */
    @Transactional(readOnly = true)
    public List<ComercializadoraSimpleDto> getComercializadorasDisponibles() {
        log.debug("Obteniendo lista de comercializadoras disponibles");

        List<Comercializadora> comercializadoras = comercializadoraRepository.findAllActive();

        log.info("Se encontraron {} comercializadoras disponibles", comercializadoras.size());

        return comercializadoras.stream()
                .map(this::convertComercializadoraToDto)
                .collect(Collectors.toList());
    }

    // ==================== MÉTODOS AUXILIARES ====================

    private IngenioSimpleDto convertIngenioToDto(IngenioMinero ingenio) {
        BigDecimal lat = ingenio.getAlmacenesIngenioList().getFirst().getLatitud();
        BigDecimal lon = ingenio.getAlmacenesIngenioList().getFirst().getLongitud();
        BigDecimal precioTonelada = ingenio.getPlantaList().getFirst().getCostoProcesamiento();
        return new IngenioSimpleDto(
                ingenio.getId(),
                ingenio.getRazonSocial(),
                ingenio.getNit(),
                ingenio.getCorreoContacto(),
                ingenio.getNumeroTelefonoMovil(),
                ingenio.getDepartamento(),
                ingenio.getMunicipio(),
                ingenio.getDireccion(),
                lat,
                lon,
                precioTonelada,
                ingenio.getBalanzasIngenioList().getFirst().getLatitud(),
                ingenio.getBalanzasIngenioList().getFirst().getLongitud()
        );
    }

    private ComercializadoraSimpleDto convertComercializadoraToDto(Comercializadora comercializadora) {
        BigDecimal lat = comercializadora.getAlmacenesList().getFirst().getLatitud();
        BigDecimal lon = comercializadora.getAlmacenesList().getFirst().getLongitud();
        return new ComercializadoraSimpleDto(
                comercializadora.getId(),
                comercializadora.getRazonSocial(),
                comercializadora.getNit(),
                comercializadora.getCorreoContacto(),
                comercializadora.getNumeroTelefonoMovil(),
                comercializadora.getDepartamento(),
                comercializadora.getMunicipio(),
                comercializadora.getDireccion(),
                lat,
                lon,
                comercializadora.getBalanzasList().getFirst().getLatitud(),
                comercializadora.getBalanzasList().getFirst().getLongitud()
        );
    }
}