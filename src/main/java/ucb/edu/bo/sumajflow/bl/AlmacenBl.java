package ucb.edu.bo.sumajflow.bl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ucb.edu.bo.sumajflow.dto.AlmacenResponseDto;
import ucb.edu.bo.sumajflow.dto.AlmacenUpdateDto;
import ucb.edu.bo.sumajflow.entity.*;
import ucb.edu.bo.sumajflow.repository.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AlmacenBl {

    private final AlmacenIngenioRepository almacenIngenioRepository;
    private final AlmacenComercializadoraRepository almacenComercializadoraRepository;
    private final IngenioMineroRepository ingenioMineroRepository;
    private final ComercializadoraRepository comercializadoraRepository;
    private final UsuariosRepository usuariosRepository;
    private final LotesRepository lotesRepository;
    private final AuditoriaBl auditoriaBl;

    /**
     * Obtiene el almacén de un ingenio con KPIs calculados
     */
    @Transactional(readOnly = true)
    public AlmacenResponseDto obtenerAlmacenIngenio(Integer usuarioId) {
        log.info("Obteniendo almacén de ingenio - Usuario ID: {}", usuarioId);

        Usuarios usuario = usuariosRepository.findById(usuarioId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        IngenioMinero ingenio = ingenioMineroRepository.findByUsuariosId(usuario)
                .orElseThrow(() -> new IllegalArgumentException("Ingenio no encontrado"));

        AlmacenIngenio almacen = almacenIngenioRepository.findByIngenioMineroId(ingenio)
                .orElseThrow(() -> new IllegalArgumentException("Almacén no encontrado"));

        return convertirADtoIngenio(almacen, ingenio.getId());
    }

    /**
     * Obtiene el almacén de una comercializadora con KPIs calculados
     */
    @Transactional(readOnly = true)
    public AlmacenResponseDto obtenerAlmacenComercializadora(Integer usuarioId) {
        log.info("Obteniendo almacén de comercializadora - Usuario ID: {}", usuarioId);

        Usuarios usuario = usuariosRepository.findById(usuarioId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        Comercializadora comercializadora = comercializadoraRepository.findByUsuariosId(usuario)
                .orElseThrow(() -> new IllegalArgumentException("Comercializadora no encontrada"));

        AlmacenComercializadora almacen = almacenComercializadoraRepository.findByComercializadoraId(comercializadora)
                .orElseThrow(() -> new IllegalArgumentException("Almacén no encontrado"));

        return convertirADtoComercializadora(almacen, comercializadora.getId());
    }

    /**
     * Actualiza el almacén de un ingenio
     * VALIDACIÓN: Verifica que no existan lotes en proceso antes de permitir la actualización
     */
    @Transactional
    public AlmacenResponseDto actualizarAlmacenIngenio(
            Integer usuarioId,
            AlmacenUpdateDto updateDto,
            String ipOrigen,
            String metodoHttp,
            String endpoint
    ) {
        log.info("Actualizando almacén de ingenio - Usuario ID: {}", usuarioId);

        Usuarios usuario = usuariosRepository.findById(usuarioId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        IngenioMinero ingenio = ingenioMineroRepository.findByUsuariosId(usuario)
                .orElseThrow(() -> new IllegalArgumentException("Ingenio no encontrado"));

        // **VALIDACIÓN CRÍTICA: Verificar que no existan lotes en proceso**
        boolean existenLotesEnProceso = lotesRepository.existenLotesEnProcesoParaIngenio(ingenio.getId());
        if (existenLotesEnProceso) {
            List<Lotes> lotesEnProceso = lotesRepository.obtenerLotesEnProcesoParaIngenio(ingenio.getId());
            log.warn("Intento de actualización de almacén con lotes en proceso - Ingenio ID: {}, Lotes en proceso: {}",
                    ingenio.getId(), lotesEnProceso.size());

            throw new IllegalStateException(
                    "No se puede actualizar el almacén mientras existan lotes en proceso. " +
                            "Actualmente tienes " + lotesEnProceso.size() + " lote(s) que no están completados o rechazados. " +
                            "Por favor, finaliza todos los lotes antes de actualizar la configuración del almacén."
            );
        }

        AlmacenIngenio almacen = almacenIngenioRepository.findByIngenioMineroId(ingenio)
                .orElseThrow(() -> new IllegalArgumentException("Almacén no encontrado"));

        // Guardar datos anteriores para auditoría
        AlmacenIngenio almacenAnterior = AlmacenIngenio.builder()
                .nombre(almacen.getNombre())
                .capacidadMaxima(almacen.getCapacidadMaxima())
                .area(almacen.getArea())
                .departamento(almacen.getDepartamento())
                .provincia(almacen.getProvincia())
                .municipio(almacen.getMunicipio())
                .direccion(almacen.getDireccion())
                .latitud(almacen.getLatitud())
                .longitud(almacen.getLongitud())
                .build();

        // Actualizar campos
        actualizarCamposAlmacenIngenio(almacen, updateDto);

        almacenIngenioRepository.save(almacen);

        // Registrar en auditoría
        auditoriaBl.registrar(
                usuario,
                "almacen_ingenio",
                "UPDATE",
                "Actualización de almacén de ingenio",
                almacen.getId(),
                almacenAnterior,
                almacen,
                List.of("nombre", "capacidadMaxima", "area", "departamento", "provincia",
                        "municipio", "direccion", "latitud", "longitud"),
                ipOrigen,
                null,
                metodoHttp,
                endpoint,
                "RF02",
                "MEDIO"
        );

        log.info("Almacén de ingenio actualizado - ID: {}", almacen.getId());

        return convertirADtoIngenio(almacen, ingenio.getId());
    }

    /**
     * Actualiza el almacén de una comercializadora
     * VALIDACIÓN: Verifica que no existan lotes en proceso antes de permitir la actualización
     */
    @Transactional
    public AlmacenResponseDto actualizarAlmacenComercializadora(
            Integer usuarioId,
            AlmacenUpdateDto updateDto,
            String ipOrigen,
            String metodoHttp,
            String endpoint
    ) {
        log.info("Actualizando almacén de comercializadora - Usuario ID: {}", usuarioId);

        Usuarios usuario = usuariosRepository.findById(usuarioId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        Comercializadora comercializadora = comercializadoraRepository.findByUsuariosId(usuario)
                .orElseThrow(() -> new IllegalArgumentException("Comercializadora no encontrada"));

        // **VALIDACIÓN CRÍTICA: Verificar que no existan lotes en proceso**
        boolean existenLotesEnProceso = lotesRepository.existenLotesEnProcesoParaComercializadora(comercializadora.getId());
        if (existenLotesEnProceso) {
            List<Lotes> lotesEnProceso = lotesRepository.obtenerLotesEnProcesoParaComercializadora(comercializadora.getId());
            log.warn("Intento de actualización de almacén con lotes en proceso - Comercializadora ID: {}, Lotes en proceso: {}",
                    comercializadora.getId(), lotesEnProceso.size());

            throw new IllegalStateException(
                    "No se puede actualizar el almacén mientras existan lotes en proceso. " +
                            "Actualmente tienes " + lotesEnProceso.size() + " lote(s) que no están completados o rechazados. " +
                            "Por favor, finaliza todos los lotes antes de actualizar la configuración del almacén."
            );
        }

        AlmacenComercializadora almacen = almacenComercializadoraRepository.findByComercializadoraId(comercializadora)
                .orElseThrow(() -> new IllegalArgumentException("Almacén no encontrado"));

        // Guardar datos anteriores para auditoría
        AlmacenComercializadora almacenAnterior = AlmacenComercializadora.builder()
                .nombre(almacen.getNombre())
                .capacidadMaxima(almacen.getCapacidadMaxima())
                .area(almacen.getArea())
                .departamento(almacen.getDepartamento())
                .provincia(almacen.getProvincia())
                .municipio(almacen.getMunicipio())
                .direccion(almacen.getDireccion())
                .latitud(almacen.getLatitud())
                .longitud(almacen.getLongitud())
                .build();

        // Actualizar campos
        actualizarCamposAlmacenComercializadora(almacen, updateDto);

        almacenComercializadoraRepository.save(almacen);

        // Registrar en auditoría
        auditoriaBl.registrar(
                usuario,
                "almacen_comercializadora",
                "UPDATE",
                "Actualización de almacén de comercializadora",
                almacen.getId(),
                almacenAnterior,
                almacen,
                List.of("nombre", "capacidadMaxima", "area", "departamento", "provincia",
                        "municipio", "direccion", "latitud", "longitud"),
                ipOrigen,
                null,
                metodoHttp,
                endpoint,
                "RF02",
                "MEDIO"
        );

        log.info("Almacén de comercializadora actualizado - ID: {}", almacen.getId());

        return convertirADtoComercializadora(almacen, comercializadora.getId());
    }

    // ==================== MÉTODOS AUXILIARES ====================

    private void actualizarCamposAlmacenIngenio(AlmacenIngenio almacen, AlmacenUpdateDto updateDto) {
        almacen.setNombre(updateDto.getNombre());
        almacen.setCapacidadMaxima(updateDto.getCapacidadMaxima());
        almacen.setArea(updateDto.getArea());
        almacen.setDepartamento(updateDto.getDepartamento());
        almacen.setProvincia(updateDto.getProvincia());
        almacen.setMunicipio(updateDto.getMunicipio());
        almacen.setDireccion(updateDto.getDireccion());
        almacen.setLatitud(updateDto.getLatitud());
        almacen.setLongitud(updateDto.getLongitud());
    }

    private void actualizarCamposAlmacenComercializadora(AlmacenComercializadora almacen, AlmacenUpdateDto updateDto) {
        almacen.setNombre(updateDto.getNombre());
        almacen.setCapacidadMaxima(updateDto.getCapacidadMaxima());
        almacen.setArea(updateDto.getArea());
        almacen.setDepartamento(updateDto.getDepartamento());
        almacen.setProvincia(updateDto.getProvincia());
        almacen.setMunicipio(updateDto.getMunicipio());
        almacen.setDireccion(updateDto.getDireccion());
        almacen.setLatitud(updateDto.getLatitud());
        almacen.setLongitud(updateDto.getLongitud());
    }

    /**
     * Convierte almacén de ingenio a DTO con KPIs calculados
     */
    private AlmacenResponseDto convertirADtoIngenio(AlmacenIngenio almacen, Integer ingenioId) {
        // Calcular KPIs
        Double ocupacionActualDouble = lotesRepository.calcularOcupacionAlmacenIngenio(ingenioId);
        BigDecimal ocupacionActual = BigDecimal.valueOf(ocupacionActualDouble != null ? ocupacionActualDouble / 1000.0 : 0.0); // Convertir kg a toneladas

        Integer totalLotes = lotesRepository.contarLotesAlmacenadosIngenio(ingenioId);

        BigDecimal capacidadDisponible = almacen.getCapacidadMaxima().subtract(ocupacionActual);

        BigDecimal porcentajeOcupacion = BigDecimal.ZERO;
        if (almacen.getCapacidadMaxima().compareTo(BigDecimal.ZERO) > 0) {
            porcentajeOcupacion = ocupacionActual
                    .divide(almacen.getCapacidadMaxima(), 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
        }

        String estadoCapacidad = determinarEstadoCapacidad(porcentajeOcupacion);

        return AlmacenResponseDto.builder()
                .id(almacen.getId())
                .nombre(almacen.getNombre())
                .capacidadMaxima(almacen.getCapacidadMaxima())
                .area(almacen.getArea())
                .departamento(almacen.getDepartamento())
                .provincia(almacen.getProvincia())
                .municipio(almacen.getMunicipio())
                .direccion(almacen.getDireccion())
                .latitud(almacen.getLatitud())
                .longitud(almacen.getLongitud())
                .tipoEntidad("ingenio")
                .entidadId(ingenioId)
                .ocupacionActual(ocupacionActual)
                .capacidadDisponible(capacidadDisponible)
                .porcentajeOcupacion(porcentajeOcupacion)
                .estadoCapacidad(estadoCapacidad)
                .totalLotesAlmacenados(totalLotes)
                .build();
    }

    /**
     * Convierte almacén de comercializadora a DTO con KPIs calculados
     */
    private AlmacenResponseDto convertirADtoComercializadora(AlmacenComercializadora almacen, Integer comercializadoraId) {
        // Calcular KPIs
        Double ocupacionActualDouble = lotesRepository.calcularOcupacionAlmacenComercializadora(comercializadoraId);
        BigDecimal ocupacionActual = BigDecimal.valueOf(ocupacionActualDouble != null ? ocupacionActualDouble / 1000.0 : 0.0); // Convertir kg a toneladas

        Integer totalLotes = lotesRepository.contarLotesAlmacenadosComercializadora(comercializadoraId);

        BigDecimal capacidadDisponible = almacen.getCapacidadMaxima().subtract(ocupacionActual);

        BigDecimal porcentajeOcupacion = BigDecimal.ZERO;
        if (almacen.getCapacidadMaxima().compareTo(BigDecimal.ZERO) > 0) {
            porcentajeOcupacion = ocupacionActual
                    .divide(almacen.getCapacidadMaxima(), 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
        }

        String estadoCapacidad = determinarEstadoCapacidad(porcentajeOcupacion);

        return AlmacenResponseDto.builder()
                .id(almacen.getId())
                .nombre(almacen.getNombre())
                .capacidadMaxima(almacen.getCapacidadMaxima())
                .area(almacen.getArea())
                .departamento(almacen.getDepartamento())
                .provincia(almacen.getProvincia())
                .municipio(almacen.getMunicipio())
                .direccion(almacen.getDireccion())
                .latitud(almacen.getLatitud())
                .longitud(almacen.getLongitud())
                .tipoEntidad("comercializadora")
                .entidadId(comercializadoraId)
                .ocupacionActual(ocupacionActual)
                .capacidadDisponible(capacidadDisponible)
                .porcentajeOcupacion(porcentajeOcupacion)
                .estadoCapacidad(estadoCapacidad)
                .totalLotesAlmacenados(totalLotes)
                .build();
    }

    /**
     * Determina el estado de capacidad basado en el porcentaje de ocupación
     */
    private String determinarEstadoCapacidad(BigDecimal porcentajeOcupacion) {
        if (porcentajeOcupacion.compareTo(BigDecimal.valueOf(90)) >= 0) {
            return "critico"; // >= 90%
        } else if (porcentajeOcupacion.compareTo(BigDecimal.valueOf(70)) >= 0) {
            return "lleno"; // >= 70%
        } else if (porcentajeOcupacion.compareTo(BigDecimal.valueOf(40)) >= 0) {
            return "medio"; // >= 40%
        } else {
            return "disponible"; // < 40%
        }
    }
}