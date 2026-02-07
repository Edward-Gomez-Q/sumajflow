package ucb.edu.bo.sumajflow.bl;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ucb.edu.bo.sumajflow.dto.ingenio.LiquidacionTollResponseDto;
import ucb.edu.bo.sumajflow.dto.ingenio.LoteSimpleDto;
import ucb.edu.bo.sumajflow.entity.*;
import ucb.edu.bo.sumajflow.repository.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Servicio GENERAL de Liquidación de Toll
 * Contiene SOLO lógica común compartida por todos los roles
 * Sin validaciones de permisos - solo operaciones básicas
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LiquidacionTollBl {

    private final LiquidacionRepository liquidacionRepository;
    private final LiquidacionLoteRepository liquidacionLoteRepository;
    private final PersonaRepository personaRepository;
    private final LotesRepository lotesRepository;
    private final ObjectMapper objectMapper;

    // ==================== OBTENER LIQUIDACIÓN ====================

    /**
     * Obtener liquidación por ID - METODO GENERAL
     */
    @Transactional(readOnly = true)
    public Liquidacion obtenerLiquidacion(Integer liquidacionId) {
        return liquidacionRepository.findById(liquidacionId)
                .orElseThrow(() -> new IllegalArgumentException("Liquidación no encontrada"));
    }

    // ==================== LISTAR LIQUIDACIONES ====================

    /**
     * Listar liquidaciones con filtros y paginación - METODO GENERAL
     */
    @Transactional(readOnly = true)
    public Page<LiquidacionTollResponseDto> listarLiquidaciones(
            List<Liquidacion> liquidacionesBase,
            String estado,
            LocalDateTime fechaDesde,
            LocalDateTime fechaHasta,
            int page,
            int size
    ) {
        log.debug("Aplicando filtros a {} liquidaciones", liquidacionesBase.size());

        // Aplicar filtros
        List<Liquidacion> liquidacionesFiltradas = liquidacionesBase.stream()
                .filter(l -> aplicarFiltros(l, estado, fechaDesde, fechaHasta))
                .toList();

        // Paginar
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), liquidacionesFiltradas.size());

        List<LiquidacionTollResponseDto> liquidacionesDto = liquidacionesFiltradas
                .subList(start, end)
                .stream()
                .map(this::convertirADto)
                .collect(Collectors.toList());

        return new PageImpl<>(liquidacionesDto, pageable, liquidacionesFiltradas.size());
    }

    // ==================== BÚSQUEDA ====================

    /**
     * Buscar liquidación por lote - METODO GENERAL
     */
    @Transactional(readOnly = true)
    public LiquidacionTollResponseDto buscarLiquidacionPorLote(Integer loteId) {
        log.debug("Buscando liquidación de Toll para lote ID: {}", loteId);

        Lotes lote = lotesRepository.findById(loteId)
                .orElseThrow(() -> new IllegalArgumentException("Lote no encontrado"));

        List<LiquidacionLote> liquidacionesLote = liquidacionLoteRepository.findByLotesId(lote);

        if (liquidacionesLote.isEmpty()) {
            return null;
        }

        Liquidacion liquidacion = liquidacionesLote.stream()
                .map(LiquidacionLote::getLiquidacionId)
                .filter(liq -> "toll".equals(liq.getTipoLiquidacion()))
                .findFirst()
                .orElse(null);

        if (liquidacion == null) {
            return null;
        }

        return convertirADto(liquidacion);
    }

    /**
     * Buscar liquidación por múltiples lotes - METODO GENERAL
     */
    @Transactional(readOnly = true)
    public Liquidacion buscarLiquidacionPorLotes(List<Lotes> lotes) {
        if (lotes.isEmpty()) {
            return null;
        }

        List<LiquidacionLote> liquidacionesLote = liquidacionLoteRepository.findByLotesId(lotes.getFirst());

        return liquidacionesLote.stream()
                .map(LiquidacionLote::getLiquidacionId)
                .filter(liq -> "toll".equals(liq.getTipoLiquidacion()))
                .findFirst()
                .orElse(null);
    }

    // ==================== CONVERSIÓN A DTO ====================

    /**
     * Convertir a DTO - METODO GENERAL
     */
    public LiquidacionTollResponseDto convertirADto(Liquidacion liquidacion) {
        Socio socio = liquidacion.getSocioId();
        Persona persona = personaRepository.findByUsuariosId(socio.getUsuariosId()).orElse(null);

        List<LoteSimpleDto> lotesDto = liquidacion.getLiquidacionLoteList().stream()
                .map(ll -> LoteSimpleDto.builder()
                        .id(ll.getLotesId().getId())
                        .minaNombre(ll.getLotesId().getMinasId().getNombre())
                        .tipoMineral(ll.getLotesId().getTipoMineral())
                        .pesoTotalReal(ll.getPesoEntrada())
                        .estado(ll.getLotesId().getEstado())
                        .build())
                .collect(Collectors.toList());

        Map<String, Object> serviciosAdicionales = null;
        try {
            if (liquidacion.getServiciosAdicionales() != null) {
                serviciosAdicionales = objectMapper.readValue(
                        liquidacion.getServiciosAdicionales(),
                        Map.class
                );
            }
        } catch (Exception e) {
            log.warn("Error al parsear servicios adicionales", e);
        }

        int totalCamiones = 0;
        if (serviciosAdicionales != null && serviciosAdicionales.containsKey("uso_balanza")) {
            Map<String, Object> usoBalanza = (Map<String, Object>) serviciosAdicionales.get("uso_balanza");
            totalCamiones = (Integer) usoBalanza.get("cantidad_camiones");
        }

        return LiquidacionTollResponseDto.builder()
                .id(liquidacion.getId())
                .tipoLiquidacion(liquidacion.getTipoLiquidacion())
                .estado(liquidacion.getEstado())
                .socioId(socio.getId())
                .socioNombres(persona != null ? persona.getNombres() : null)
                .socioApellidos(persona != null ?
                        persona.getPrimerApellido() +
                                (persona.getSegundoApellido() != null ? " " + persona.getSegundoApellido() : "") : null)
                .socioCi(persona != null ? persona.getCi() : null)
                .pesoTotalEntradaKg(liquidacion.getPesoTotalEntrada())
                .pesoTotalToneladas(liquidacion.getPesoTmh())
                .costoPorTonelada(liquidacion.getCostoPorTonelada())
                .costoProcesamientoTotal(liquidacion.getCostoProcesamientoTotal())
                .serviciosAdicionales(serviciosAdicionales)
                .totalServiciosAdicionales(liquidacion.getTotalServiciosAdicionales())
                .valorBrutoUsd(liquidacion.getValorBrutoUsd())
                .valorNetoUsd(liquidacion.getValorNetoUsd())
                .tipoCambio(liquidacion.getTipoCambio())
                .valorNetoBob(liquidacion.getValorNetoBob())
                .moneda(liquidacion.getMoneda())
                .lotes(lotesDto)
                .totalLotes(lotesDto.size())
                .totalCamiones(totalCamiones)
                .fechaAprobacion(liquidacion.getFechaAprobacion())
                .fechaPago(liquidacion.getFechaPago())
                .metodoPago(liquidacion.getMetodoPago())
                .numeroComprobante(liquidacion.getNumeroComprobante())
                .urlComprobante(liquidacion.getUrlComprobante())
                .observaciones(liquidacion.getObservaciones())
                .createdAt(liquidacion.getCreatedAt())
                .updatedAt(liquidacion.getUpdatedAt())
                .build();
    }

    // ==================== MÉTODOS AUXILIARES PRIVADOS ====================

    private boolean aplicarFiltros(
            Liquidacion l,
            String estado,
            LocalDateTime fechaDesde,
            LocalDateTime fechaHasta
    ) {
        if (estado != null && !estado.isEmpty() && !estado.equals(l.getEstado())) {
            return false;
        }
        if (fechaDesde != null && l.getCreatedAt().isBefore(fechaDesde)) {
            return false;
        }
        return fechaHasta == null || !l.getCreatedAt().isAfter(fechaHasta);
    }
}