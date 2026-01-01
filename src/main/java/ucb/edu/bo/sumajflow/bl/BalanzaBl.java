package ucb.edu.bo.sumajflow.bl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ucb.edu.bo.sumajflow.dto.BalanzaResponseDto;
import ucb.edu.bo.sumajflow.dto.BalanzaUpdateDto;
import ucb.edu.bo.sumajflow.entity.*;
import ucb.edu.bo.sumajflow.repository.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class BalanzaBl {

    private final BalanzaCooperativaRepository balanzaCooperativaRepository;
    private final BalanzaIngenioRepository balanzaIngenioRepository;
    private final BalanzaComercializadoraRepository balanzaComercializadoraRepository;
    private final CooperativaRepository cooperativaRepository;
    private final IngenioMineroRepository ingenioMineroRepository;
    private final ComercializadoraRepository comercializadoraRepository;
    private final UsuariosRepository usuariosRepository;
    private final LotesRepository lotesRepository;
    private final AuditoriaBl auditoriaBl;

    /**
     * Obtiene la balanza de una cooperativa
     */
    @Transactional(readOnly = true)
    public BalanzaResponseDto obtenerBalanzaCooperativa(Integer usuarioId) {
        log.info("Obteniendo balanza de cooperativa - Usuario ID: {}", usuarioId);

        Usuarios usuario = usuariosRepository.findById(usuarioId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        Cooperativa cooperativa = cooperativaRepository.findByUsuariosId(usuario)
                .orElseThrow(() -> new IllegalArgumentException("Cooperativa no encontrada"));

        BalanzaCooperativa balanza = balanzaCooperativaRepository.findByCooperativaId(cooperativa)
                .orElseThrow(() -> new IllegalArgumentException("Balanza no encontrada"));

        return convertirADto(balanza, "cooperativa", cooperativa.getId());
    }

    /**
     * Obtiene la balanza de un ingenio
     */
    @Transactional(readOnly = true)
    public BalanzaResponseDto obtenerBalanzaIngenio(Integer usuarioId) {
        log.info("Obteniendo balanza de ingenio - Usuario ID: {}", usuarioId);

        Usuarios usuario = usuariosRepository.findById(usuarioId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        IngenioMinero ingenio = ingenioMineroRepository.findByUsuariosId(usuario)
                .orElseThrow(() -> new IllegalArgumentException("Ingenio no encontrado"));

        BalanzaIngenio balanza = balanzaIngenioRepository.findByIngenioMineroId(ingenio)
                .orElseThrow(() -> new IllegalArgumentException("Balanza no encontrada"));

        return convertirADto(balanza, "ingenio", ingenio.getId());
    }

    /**
     * Obtiene la balanza de una comercializadora
     */
    @Transactional(readOnly = true)
    public BalanzaResponseDto obtenerBalanzaComercializadora(Integer usuarioId) {
        log.info("Obteniendo balanza de comercializadora - Usuario ID: {}", usuarioId);

        Usuarios usuario = usuariosRepository.findById(usuarioId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        Comercializadora comercializadora = comercializadoraRepository.findByUsuariosId(usuario)
                .orElseThrow(() -> new IllegalArgumentException("Comercializadora no encontrada"));

        BalanzaComercializadora balanza = balanzaComercializadoraRepository.findByComercializadoraId(comercializadora)
                .orElseThrow(() -> new IllegalArgumentException("Balanza no encontrada"));

        return convertirADto(balanza, "comercializadora", comercializadora.getId());
    }

    /**
     * Actualiza la balanza de una cooperativa
     * Las cooperativas no tienen restricción de lotes
     */
    @Transactional
    public BalanzaResponseDto actualizarBalanzaCooperativa(
            Integer usuarioId,
            BalanzaUpdateDto updateDto,
            String ipOrigen,
            String metodoHttp,
            String endpoint
    ) {
        log.info("Actualizando balanza de cooperativa - Usuario ID: {}", usuarioId);

        Usuarios usuario = usuariosRepository.findById(usuarioId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        Cooperativa cooperativa = cooperativaRepository.findByUsuariosId(usuario)
                .orElseThrow(() -> new IllegalArgumentException("Cooperativa no encontrada"));

        BalanzaCooperativa balanza = balanzaCooperativaRepository.findByCooperativaId(cooperativa)
                .orElseThrow(() -> new IllegalArgumentException("Balanza no encontrada"));

        // Guardar datos anteriores para auditoría
        BalanzaCooperativa balanzaAnterior = BalanzaCooperativa.builder()
                .nombre(balanza.getNombre())
                .marca(balanza.getMarca())
                .modelo(balanza.getModelo())
                .numeroSerie(balanza.getNumeroSerie())
                .capacidadMaxima(balanza.getCapacidadMaxima())
                .precisionMinima(balanza.getPrecisionMinima())
                .fechaUltimaCalibracion(balanza.getFechaUltimaCalibracion())
                .fechaProximaCalibracion(balanza.getFechaProximaCalibracion())
                .build();

        // Actualizar campos
        actualizarCamposBalanzaCooperativa(balanza, updateDto);

        balanzaCooperativaRepository.save(balanza);

        // Registrar en auditoría
        auditoriaBl.registrar(
                usuario,
                "balanza_cooperativa",
                "UPDATE",
                "Actualización de balanza de cooperativa",
                balanza.getId(),
                balanzaAnterior,
                balanza,
                List.of("nombre", "marca", "modelo", "numeroSerie", "capacidadMaxima",
                        "precisionMinima", "fechaUltimaCalibracion", "fechaProximaCalibracion",
                        "departamento", "provincia", "municipio", "direccion", "latitud", "longitud"),
                ipOrigen,
                null,
                metodoHttp,
                endpoint,
                "RF01",
                "MEDIO"
        );

        log.info("Balanza de cooperativa actualizada - ID: {}", balanza.getId());

        return convertirADto(balanza, "cooperativa", cooperativa.getId());
    }

    /**
     * Actualiza la balanza de un ingenio
     * VALIDACIÓN: Verifica que no existan lotes en proceso antes de permitir la actualización
     */
    @Transactional
    public BalanzaResponseDto actualizarBalanzaIngenio(
            Integer usuarioId,
            BalanzaUpdateDto updateDto,
            String ipOrigen,
            String metodoHttp,
            String endpoint
    ) {
        log.info("Actualizando balanza de ingenio - Usuario ID: {}", usuarioId);

        Usuarios usuario = usuariosRepository.findById(usuarioId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        IngenioMinero ingenio = ingenioMineroRepository.findByUsuariosId(usuario)
                .orElseThrow(() -> new IllegalArgumentException("Ingenio no encontrado"));

        // **VALIDACIÓN CRÍTICA: Verificar que no existan lotes en proceso**
        boolean existenLotesEnProceso = lotesRepository.existenLotesEnProcesoParaIngenio(ingenio.getId());
        if (existenLotesEnProceso) {
            List<Lotes> lotesEnProceso = lotesRepository.obtenerLotesEnProcesoParaIngenio(ingenio.getId());
            log.warn("Intento de actualización de balanza con lotes en proceso - Ingenio ID: {}, Lotes en proceso: {}",
                    ingenio.getId(), lotesEnProceso.size());

            throw new IllegalStateException(
                    "No se puede actualizar la balanza mientras existan lotes en proceso. " +
                            "Actualmente tienes " + lotesEnProceso.size() + " lote(s) que no están completados o rechazados. " +
                            "Por favor, finaliza todos los lotes antes de actualizar la configuración de la balanza."
            );
        }

        BalanzaIngenio balanza = balanzaIngenioRepository.findByIngenioMineroId(ingenio)
                .orElseThrow(() -> new IllegalArgumentException("Balanza no encontrada"));

        // Guardar datos anteriores para auditoría
        BalanzaIngenio balanzaAnterior = BalanzaIngenio.builder()
                .nombre(balanza.getNombre())
                .marca(balanza.getMarca())
                .modelo(balanza.getModelo())
                .numeroSerie(balanza.getNumeroSerie())
                .capacidadMaxima(balanza.getCapacidadMaxima())
                .precisionMinima(balanza.getPrecisionMinima())
                .fechaUltimaCalibracion(balanza.getFechaUltimaCalibracion())
                .fechaProximaCalibracion(balanza.getFechaProximaCalibracion())
                .build();

        // Actualizar campos
        actualizarCamposBalanzaIngenio(balanza, updateDto);

        balanzaIngenioRepository.save(balanza);

        // Registrar en auditoría
        auditoriaBl.registrar(
                usuario,
                "balanza_ingenio",
                "UPDATE",
                "Actualización de balanza de ingenio",
                balanza.getId(),
                balanzaAnterior,
                balanza,
                List.of("nombre", "marca", "modelo", "numeroSerie", "capacidadMaxima",
                        "precisionMinima", "fechaUltimaCalibracion", "fechaProximaCalibracion",
                        "departamento", "provincia", "municipio", "direccion", "latitud", "longitud"),
                ipOrigen,
                null,
                metodoHttp,
                endpoint,
                "RF01",
                "MEDIO"
        );

        log.info("Balanza de ingenio actualizada - ID: {}", balanza.getId());

        return convertirADto(balanza, "ingenio", ingenio.getId());
    }

    /**
     * Actualiza la balanza de una comercializadora
     * VALIDACIÓN: Verifica que no existan lotes en proceso antes de permitir la actualización
     */
    @Transactional
    public BalanzaResponseDto actualizarBalanzaComercializadora(
            Integer usuarioId,
            BalanzaUpdateDto updateDto,
            String ipOrigen,
            String metodoHttp,
            String endpoint
    ) {
        log.info("Actualizando balanza de comercializadora - Usuario ID: {}", usuarioId);

        Usuarios usuario = usuariosRepository.findById(usuarioId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        Comercializadora comercializadora = comercializadoraRepository.findByUsuariosId(usuario)
                .orElseThrow(() -> new IllegalArgumentException("Comercializadora no encontrada"));

        // **VALIDACIÓN CRÍTICA: Verificar que no existan lotes en proceso**
        boolean existenLotesEnProceso = lotesRepository.existenLotesEnProcesoParaComercializadora(comercializadora.getId());
        if (existenLotesEnProceso) {
            List<Lotes> lotesEnProceso = lotesRepository.obtenerLotesEnProcesoParaComercializadora(comercializadora.getId());
            log.warn("Intento de actualización de balanza con lotes en proceso - Comercializadora ID: {}, Lotes en proceso: {}",
                    comercializadora.getId(), lotesEnProceso.size());

            throw new IllegalStateException(
                    "No se puede actualizar la balanza mientras existan lotes en proceso. " +
                            "Actualmente tienes " + lotesEnProceso.size() + " lote(s) que no están completados o rechazados. " +
                            "Por favor, finaliza todos los lotes antes de actualizar la configuración de la balanza."
            );
        }

        BalanzaComercializadora balanza = balanzaComercializadoraRepository.findByComercializadoraId(comercializadora)
                .orElseThrow(() -> new IllegalArgumentException("Balanza no encontrada"));

        // Guardar datos anteriores para auditoría
        BalanzaComercializadora balanzaAnterior = BalanzaComercializadora.builder()
                .nombre(balanza.getNombre())
                .marca(balanza.getMarca())
                .modelo(balanza.getModelo())
                .numeroSerie(balanza.getNumeroSerie())
                .capacidadMaxima(balanza.getCapacidadMaxima())
                .precisionMinima(balanza.getPrecisionMinima())
                .fechaUltimaCalibracion(balanza.getFechaUltimaCalibracion())
                .fechaProximaCalibracion(balanza.getFechaProximaCalibracion())
                .build();

        // Actualizar campos
        actualizarCamposBalanzaComercializadora(balanza, updateDto);

        balanzaComercializadoraRepository.save(balanza);

        // Registrar en auditoría
        auditoriaBl.registrar(
                usuario,
                "balanza_comercializadora",
                "UPDATE",
                "Actualización de balanza de comercializadora",
                balanza.getId(),
                balanzaAnterior,
                balanza,
                List.of("nombre", "marca", "modelo", "numeroSerie", "capacidadMaxima",
                        "precisionMinima", "fechaUltimaCalibracion", "fechaProximaCalibracion",
                        "departamento", "provincia", "municipio", "direccion", "latitud", "longitud"),
                ipOrigen,
                null,
                metodoHttp,
                endpoint,
                "RF01",
                "MEDIO"
        );

        log.info("Balanza de comercializadora actualizada - ID: {}", balanza.getId());

        return convertirADto(balanza, "comercializadora", comercializadora.getId());
    }

    // ==================== MÉTODOS AUXILIARES ====================

    private void actualizarCamposBalanzaCooperativa(BalanzaCooperativa balanza, BalanzaUpdateDto updateDto) {
        balanza.setNombre(updateDto.getNombre());
        balanza.setMarca(updateDto.getMarca());
        balanza.setModelo(updateDto.getModelo());
        balanza.setNumeroSerie(updateDto.getNumeroSerie());
        balanza.setCapacidadMaxima(updateDto.getCapacidadMaxima());
        balanza.setPrecisionMinima(updateDto.getPrecisionMinima());
        balanza.setFechaUltimaCalibracion(updateDto.getFechaUltimaCalibracion());
        balanza.setFechaProximaCalibracion(updateDto.getFechaProximaCalibracion());
        balanza.setDepartamento(updateDto.getDepartamento());
        balanza.setProvincia(updateDto.getProvincia());
        balanza.setMunicipio(updateDto.getMunicipio());
        balanza.setDireccion(updateDto.getDireccion());
        balanza.setLatitud(updateDto.getLatitud());
        balanza.setLongitud(updateDto.getLongitud());
    }

    private void actualizarCamposBalanzaIngenio(BalanzaIngenio balanza, BalanzaUpdateDto updateDto) {
        balanza.setNombre(updateDto.getNombre());
        balanza.setMarca(updateDto.getMarca());
        balanza.setModelo(updateDto.getModelo());
        balanza.setNumeroSerie(updateDto.getNumeroSerie());
        balanza.setCapacidadMaxima(updateDto.getCapacidadMaxima());
        balanza.setPrecisionMinima(updateDto.getPrecisionMinima());
        balanza.setFechaUltimaCalibracion(updateDto.getFechaUltimaCalibracion());
        balanza.setFechaProximaCalibracion(updateDto.getFechaProximaCalibracion());
        balanza.setDepartamento(updateDto.getDepartamento());
        balanza.setProvincia(updateDto.getProvincia());
        balanza.setMunicipio(updateDto.getMunicipio());
        balanza.setDireccion(updateDto.getDireccion());
        balanza.setLatitud(updateDto.getLatitud());
        balanza.setLongitud(updateDto.getLongitud());
    }

    private void actualizarCamposBalanzaComercializadora(BalanzaComercializadora balanza, BalanzaUpdateDto updateDto) {
        balanza.setNombre(updateDto.getNombre());
        balanza.setMarca(updateDto.getMarca());
        balanza.setModelo(updateDto.getModelo());
        balanza.setNumeroSerie(updateDto.getNumeroSerie());
        balanza.setCapacidadMaxima(updateDto.getCapacidadMaxima());
        balanza.setPrecisionMinima(updateDto.getPrecisionMinima());
        balanza.setFechaUltimaCalibracion(updateDto.getFechaUltimaCalibracion());
        balanza.setFechaProximaCalibracion(updateDto.getFechaProximaCalibracion());
        balanza.setDepartamento(updateDto.getDepartamento());
        balanza.setProvincia(updateDto.getProvincia());
        balanza.setMunicipio(updateDto.getMunicipio());
        balanza.setDireccion(updateDto.getDireccion());
        balanza.setLatitud(updateDto.getLatitud());
        balanza.setLongitud(updateDto.getLongitud());
    }

    /**
     * Convierte cualquier tipo de balanza a DTO
     */
    private BalanzaResponseDto convertirADto(Object balanza, String tipoEntidad, Integer entidadId) {
        Integer id = null;
        String nombre = null;
        String marca = null;
        String modelo = null;
        String numeroSerie = null;
        BigDecimal capacidadMaxima = null;
        BigDecimal precisionMinima = null;
        LocalDate fechaUltimaCalibracion = null;
        LocalDate fechaProximaCalibracion = null;
        String departamento = null;
        String provincia = null;
        String municipio = null;
        String direccion = null;
        BigDecimal latitud = null;
        BigDecimal longitud = null;

        if (balanza instanceof BalanzaCooperativa) {
            BalanzaCooperativa b = (BalanzaCooperativa) balanza;
            id = b.getId();
            nombre = b.getNombre();
            marca = b.getMarca();
            modelo = b.getModelo();
            numeroSerie = b.getNumeroSerie();
            capacidadMaxima = b.getCapacidadMaxima();
            precisionMinima = b.getPrecisionMinima();
            fechaUltimaCalibracion = b.getFechaUltimaCalibracion();
            fechaProximaCalibracion = b.getFechaProximaCalibracion();
            departamento = b.getDepartamento();
            provincia = b.getProvincia();
            municipio = b.getMunicipio();
            direccion = b.getDireccion();
            latitud = b.getLatitud();
            longitud = b.getLongitud();
        } else if (balanza instanceof BalanzaIngenio) {
            BalanzaIngenio b = (BalanzaIngenio) balanza;
            id = b.getId();
            nombre = b.getNombre();
            marca = b.getMarca();
            modelo = b.getModelo();
            numeroSerie = b.getNumeroSerie();
            capacidadMaxima = b.getCapacidadMaxima();
            precisionMinima = b.getPrecisionMinima();
            fechaUltimaCalibracion = b.getFechaUltimaCalibracion();
            fechaProximaCalibracion = b.getFechaProximaCalibracion();
            departamento = b.getDepartamento();
            provincia = b.getProvincia();
            municipio = b.getMunicipio();
            direccion = b.getDireccion();
            latitud = b.getLatitud();
            longitud = b.getLongitud();
        } else if (balanza instanceof BalanzaComercializadora) {
            BalanzaComercializadora b = (BalanzaComercializadora) balanza;
            id = b.getId();
            nombre = b.getNombre();
            marca = b.getMarca();
            modelo = b.getModelo();
            numeroSerie = b.getNumeroSerie();
            capacidadMaxima = b.getCapacidadMaxima();
            precisionMinima = b.getPrecisionMinima();
            fechaUltimaCalibracion = b.getFechaUltimaCalibracion();
            fechaProximaCalibracion = b.getFechaProximaCalibracion();
            departamento = b.getDepartamento();
            provincia = b.getProvincia();
            municipio = b.getMunicipio();
            direccion = b.getDireccion();
            latitud = b.getLatitud();
            longitud = b.getLongitud();
        }

        // Calcular campos derivados
        Integer diasParaCalibracion = calcularDiasParaCalibracion(fechaProximaCalibracion);
        String estadoCalibracion = determinarEstadoCalibracion(diasParaCalibracion);
        Integer totalDivisiones = calcularTotalDivisiones(capacidadMaxima, precisionMinima);

        return BalanzaResponseDto.builder()
                .id(id)
                .nombre(nombre)
                .marca(marca)
                .modelo(modelo)
                .numeroSerie(numeroSerie)
                .capacidadMaxima(capacidadMaxima)
                .precisionMinima(precisionMinima)
                .fechaUltimaCalibracion(fechaUltimaCalibracion)
                .fechaProximaCalibracion(fechaProximaCalibracion)
                .departamento(departamento)
                .provincia(provincia)
                .municipio(municipio)
                .direccion(direccion)
                .latitud(latitud)
                .longitud(longitud)
                .tipoEntidad(tipoEntidad)
                .entidadId(entidadId)
                .diasParaCalibracion(diasParaCalibracion)
                .estadoCalibracion(estadoCalibracion)
                .totalDivisiones(totalDivisiones)
                .build();
    }

    private Integer calcularDiasParaCalibracion(LocalDate fechaProximaCalibracion) {
        if (fechaProximaCalibracion == null) return null;
        return (int) ChronoUnit.DAYS.between(LocalDate.now(), fechaProximaCalibracion);
    }

    private String determinarEstadoCalibracion(Integer diasParaCalibracion) {
        if (diasParaCalibracion == null) return "desconocido";
        if (diasParaCalibracion < 0) return "vencido";
        if (diasParaCalibracion <= 30) return "proximo_vencimiento";
        return "vigente";
    }

    private Integer calcularTotalDivisiones(BigDecimal capacidadMaxima, BigDecimal precisionMinima) {
        if (capacidadMaxima == null || precisionMinima == null) return null;
        if (precisionMinima.compareTo(BigDecimal.ZERO) == 0) return null;
        return capacidadMaxima.divide(precisionMinima, 0, RoundingMode.DOWN).intValueExact();
    }
}