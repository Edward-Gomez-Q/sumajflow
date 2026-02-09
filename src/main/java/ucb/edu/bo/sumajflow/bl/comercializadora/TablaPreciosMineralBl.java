// src/main/java/ucb/edu/bo/sumajflow/bl/comercializadora/TablaPreciosMineralBl.java
package ucb.edu.bo.sumajflow.bl.comercializadora;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ucb.edu.bo.sumajflow.dto.comercializadora.TablaPreciosMineralDto;
import ucb.edu.bo.sumajflow.dto.comercializadora.ValidacionPreciosResponseDto;
import ucb.edu.bo.sumajflow.entity.Comercializadora;
import ucb.edu.bo.sumajflow.entity.TablaPreciosMineral;
import ucb.edu.bo.sumajflow.entity.Usuarios;
import ucb.edu.bo.sumajflow.repository.ComercializadoraRepository;
import ucb.edu.bo.sumajflow.repository.TablaPreciosMineralRepository;
import ucb.edu.bo.sumajflow.repository.UsuariosRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TablaPreciosMineralBl {

    private final TablaPreciosMineralRepository tablaPreciosRepository;
    private final ComercializadoraRepository comercializadoraRepository;
    private final UsuariosRepository usuariosRepository;

    // ==================== CRUD ====================

    @Transactional
    public TablaPreciosMineralDto crear(TablaPreciosMineralDto dto, Integer usuarioId) {
        log.info("Creando nuevo rango de precios - Usuario ID: {}", usuarioId);

        dto.validar();
        Comercializadora comercializadora = obtenerComercializadora(usuarioId);

        // Validar que no haya solapamiento de rangos
        validarSolapamiento(dto, comercializadora, null);

        TablaPreciosMineral entity = TablaPreciosMineral.builder()
                .comercializadoraId(comercializadora)
                .mineral(dto.getMineral())
                .unidadMedida(dto.getUnidadMedida())
                .rangoMinimo(dto.getRangoMinimo())
                .rangoMaximo(dto.getRangoMaximo())
                .precioUsd(dto.getPrecioUsd())
                .fechaInicio(dto.getFechaInicio())
                .fechaFin(dto.getFechaFin())
                .activo(dto.getActivo() != null ? dto.getActivo() : true)
                .observaciones(dto.getObservaciones())
                .build();

        entity = tablaPreciosRepository.save(entity);
        log.info("✅ Rango de precios creado - ID: {}", entity.getId());

        return convertirADto(entity);
    }

    @Transactional
    public TablaPreciosMineralDto actualizar(Integer id, TablaPreciosMineralDto dto, Integer usuarioId) {
        log.info("Actualizando rango de precios ID: {} - Usuario ID: {}", id, usuarioId);

        dto.validar();
        Comercializadora comercializadora = obtenerComercializadora(usuarioId);
        TablaPreciosMineral entity = obtenerConPermisos(id, comercializadora);

        // Validar solapamiento excluyendo el actual
        validarSolapamiento(dto, comercializadora, id);

        entity.setMineral(dto.getMineral());
        entity.setUnidadMedida(dto.getUnidadMedida());
        entity.setRangoMinimo(dto.getRangoMinimo());
        entity.setRangoMaximo(dto.getRangoMaximo());
        entity.setPrecioUsd(dto.getPrecioUsd());
        entity.setFechaInicio(dto.getFechaInicio());
        entity.setFechaFin(dto.getFechaFin());
        entity.setActivo(dto.getActivo() != null ? dto.getActivo() : entity.getActivo());
        entity.setObservaciones(dto.getObservaciones());

        entity = tablaPreciosRepository.save(entity);
        log.info("✅ Rango de precios actualizado - ID: {}", id);

        return convertirADto(entity);
    }

    @Transactional
    public void eliminar(Integer id, Integer usuarioId) {
        log.info("Eliminando rango de precios ID: {} - Usuario ID: {}", id, usuarioId);

        Comercializadora comercializadora = obtenerComercializadora(usuarioId);
        TablaPreciosMineral entity = obtenerConPermisos(id, comercializadora);

        tablaPreciosRepository.delete(entity);
        log.info("✅ Rango de precios eliminado - ID: {}", id);
    }

    @Transactional
    public TablaPreciosMineralDto desactivar(Integer id, Integer usuarioId) {
        log.info("Desactivando rango de precios ID: {} - Usuario ID: {}", id, usuarioId);

        Comercializadora comercializadora = obtenerComercializadora(usuarioId);
        TablaPreciosMineral entity = obtenerConPermisos(id, comercializadora);

        entity.setActivo(false);
        entity = tablaPreciosRepository.save(entity);

        log.info("✅ Rango de precios desactivado - ID: {}", id);
        return convertirADto(entity);
    }

    // ==================== CONSULTAS ====================

    @Transactional(readOnly = true)
    public List<TablaPreciosMineralDto> listar(Integer usuarioId, String mineral, Boolean activo) {
        Comercializadora comercializadora = obtenerComercializadora(usuarioId);

        List<TablaPreciosMineral> precios = comercializadora.getTablaPreciosMineralList();

        return precios.stream()
                .filter(p -> mineral == null || p.getMineral().equals(mineral))
                .filter(p -> activo == null || p.getActivo().equals(activo))
                .sorted(Comparator.comparing(TablaPreciosMineral::getMineral)
                        .thenComparing(TablaPreciosMineral::getRangoMinimo))
                .map(this::convertirADto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Map<String, List<TablaPreciosMineralDto>> listarAgrupadosPorMineral(Integer usuarioId) {
        List<TablaPreciosMineralDto> todos = listar(usuarioId, null, true);

        return todos.stream()
                .collect(Collectors.groupingBy(
                        TablaPreciosMineralDto::getMineral,
                        TreeMap::new,
                        Collectors.toList()
                ));
    }

    // ==================== VALIDACIÓN ====================

    @Transactional(readOnly = true)
    public ValidacionPreciosResponseDto validarConfiguracion(Integer comercializadoraId) {
        log.info("Validando configuración de precios - Comercializadora ID: {}", comercializadoraId);

        Comercializadora comercializadora = comercializadoraRepository.findById(comercializadoraId)
                .orElseThrow(() -> new IllegalArgumentException("Comercializadora no encontrada"));

        LocalDate hoy = LocalDate.now();
        List<TablaPreciosMineral> preciosVigentes = tablaPreciosRepository
                .findPreciosVigentes(comercializadoraId, hoy);

        ValidacionPreciosResponseDto validacion = ValidacionPreciosResponseDto.builder()
                .configuracionCompleta(true)
                .mineralesFaltantes(new ArrayList<>())
                .advertencias(new ArrayList<>())
                .errores(new ArrayList<>())
                .build();

        // Agrupar por mineral
        Map<String, List<TablaPreciosMineral>> porMineral = preciosVigentes.stream()
                .collect(Collectors.groupingBy(TablaPreciosMineral::getMineral));

        // Validar cada mineral
        validarMineral(porMineral, "Pb", validacion);
        validarMineral(porMineral, "Zn", validacion);
        validarMineral(porMineral, "Ag", validacion);

        validacion.setTotalRangosPb(porMineral.getOrDefault("Pb", Collections.emptyList()).size());
        validacion.setTotalRangosZn(porMineral.getOrDefault("Zn", Collections.emptyList()).size());
        validacion.setTotalRangosAg(porMineral.getOrDefault("Ag", Collections.emptyList()).size());

        log.info("✅ Validación completada - Configuración completa: {}", validacion.getConfiguracionCompleta());
        return validacion;
    }

    private void validarMineral(
            Map<String, List<TablaPreciosMineral>> porMineral,
            String mineral,
            ValidacionPreciosResponseDto validacion
    ) {
        List<TablaPreciosMineral> rangos = porMineral.getOrDefault(mineral, new ArrayList<>());

        if (rangos.isEmpty()) {
            validacion.getMineralesFaltantes().add(mineral);
            validacion.agregarError(String.format("No hay rangos configurados para %s", mineral));
            return;
        }

        // Ordenar por rango mínimo
        rangos.sort(Comparator.comparing(TablaPreciosMineral::getRangoMinimo));

        // Validar que no haya gaps (espacios vacíos)
        for (int i = 0; i < rangos.size() - 1; i++) {
            TablaPreciosMineral actual = rangos.get(i);
            TablaPreciosMineral siguiente = rangos.get(i + 1);

            if (actual.getRangoMaximo().compareTo(siguiente.getRangoMinimo()) < 0) {
                validacion.agregarAdvertencia(String.format(
                        "%s: Gap detectado entre %.4f-%.4f y %.4f-%.4f",
                        mineral,
                        actual.getRangoMinimo(),
                        actual.getRangoMaximo(),
                        siguiente.getRangoMinimo(),
                        siguiente.getRangoMaximo()
                ));
            }
        }

        // Validar que el último rango llegue a un tope alto
        TablaPreciosMineral ultimo = rangos.getLast();
        if (ultimo.getRangoMaximo().compareTo(new BigDecimal("100")) < 0) {
            validacion.agregarAdvertencia(String.format(
                    "%s: El último rango termina en %.4f. Considera agregar un rango superior.",
                    mineral, ultimo.getRangoMaximo()
            ));
        }
    }

    // ==================== HELPERS ====================

    private void validarSolapamiento(
            TablaPreciosMineralDto dto,
            Comercializadora comercializadora,
            Integer idExcluir
    ) {
        LocalDate fechaInicio = dto.getFechaInicio();
        LocalDate fechaFin = dto.getFechaFin() != null ? dto.getFechaFin() : LocalDate.of(9999, 12, 31);

        List<TablaPreciosMineral> existentes = comercializadora.getTablaPreciosMineralList().stream()
                .filter(TablaPreciosMineral::getActivo)
                .filter(p -> p.getMineral().equals(dto.getMineral()))
                .filter(p -> !p.getId().equals(idExcluir))
                .toList();

        for (TablaPreciosMineral existente : existentes) {
            LocalDate existenteFin = existente.getFechaFin() != null
                    ? existente.getFechaFin()
                    : LocalDate.of(9999, 12, 31);

            // Verificar solapamiento de fechas
            boolean solapamientoFechas =
                    !(fechaFin.isBefore(existente.getFechaInicio()) || fechaInicio.isAfter(existenteFin));

            if (solapamientoFechas) {
                // Verificar solapamiento de rangos
                boolean solapamientoRangos =
                        !(dto.getRangoMaximo().compareTo(existente.getRangoMinimo()) < 0
                                || dto.getRangoMinimo().compareTo(existente.getRangoMaximo()) > 0);

                if (solapamientoRangos) {
                    throw new IllegalArgumentException(String.format(
                            "Ya existe un rango de %s que se solapa: %.4f-%.4f (vigente desde %s hasta %s)",
                            dto.getMineral(),
                            existente.getRangoMinimo(),
                            existente.getRangoMaximo(),
                            existente.getFechaInicio(),
                            existente.getFechaFin() != null ? existente.getFechaFin() : "indefinido"
                    ));
                }
            }
        }
    }

    private Comercializadora obtenerComercializadora(Integer usuarioId) {
        Usuarios usuario = usuariosRepository.findById(usuarioId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));
        return comercializadoraRepository.findByUsuariosId(usuario)
                .orElseThrow(() -> new IllegalArgumentException("Comercializadora no encontrada"));
    }

    private TablaPreciosMineral obtenerConPermisos(Integer id, Comercializadora comercializadora) {
        TablaPreciosMineral entity = tablaPreciosRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Rango de precios no encontrado"));

        if (!entity.getComercializadoraId().getId().equals(comercializadora.getId())) {
            throw new IllegalArgumentException("No tienes permiso para modificar este rango");
        }

        return entity;
    }

    private TablaPreciosMineralDto convertirADto(TablaPreciosMineral entity) {
        return TablaPreciosMineralDto.builder()
                .id(entity.getId())
                .mineral(entity.getMineral())
                .unidadMedida(entity.getUnidadMedida())
                .rangoMinimo(entity.getRangoMinimo())
                .rangoMaximo(entity.getRangoMaximo())
                .precioUsd(entity.getPrecioUsd())
                .fechaInicio(entity.getFechaInicio())
                .fechaFin(entity.getFechaFin())
                .activo(entity.getActivo())
                .observaciones(entity.getObservaciones())
                .build();
    }
}