package ucb.edu.bo.sumajflow.bl.liquidacion;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ucb.edu.bo.sumajflow.bl.NotificacionBl;
import ucb.edu.bo.sumajflow.bl.cooperativa.AuditoriaLotesBl;
import ucb.edu.bo.sumajflow.dto.liquidacion.*;
import ucb.edu.bo.sumajflow.entity.*;
import ucb.edu.bo.sumajflow.repository.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class LiquidacionBl {

    // Repositorios
    private final LiquidacionRepository liquidacionRepository;
    private final LiquidacionLoteRepository liquidacionLoteRepository;
    private final LiquidacionConcentradoRepository liquidacionConcentradoRepository;
    private final LiquidacionCotizacionRepository liquidacionCotizacionRepository;
    private final LiquidacionDeduccionRepository liquidacionDeduccionRepository;
    private final LotesRepository lotesRepository;
    private final ConcentradoRepository concentradoRepository;
    private final ReporteQuimicoRepository reporteQuimicoRepository;
    private final SocioRepository socioRepository;
    private final PersonaRepository personaRepository;
    private final LoteMineralesRepository loteMineralesRepository;
    private final AuditoriaLotesBl auditoriaLotesBl;
    private final NotificacionBl notificacionBl;

    /**
     * Crear una nueva liquidación
     */
    @Transactional
    public LiquidacionResponseDto crearLiquidacion(
            LiquidacionCreateDto dto,
            Integer usuarioId,
            String ipOrigen
    ) {
        log.info("Creando liquidación - Tipo: {}, Socio ID: {}", dto.getTipoLiquidacion(), dto.getSocioId());

        // 1. Validaciones
        validarDatosLiquidacion(dto);

        // 2. Obtener socio
        Socio socio = socioRepository.findById(dto.getSocioId())
                .orElseThrow(() -> new IllegalArgumentException("Socio no encontrado"));

        // 3. Validar relaciones según tipo de liquidación
        validarRelacionesPorTipo(dto);

        // 4. Crear liquidación con estado "borrador"
        Liquidacion liquidacion = crearYGuardarLiquidacion(dto, socio);

        // 5. Crear relación con lote o concentrado
        crearRelacion(liquidacion, dto);

        // 6. Crear cotizaciones
        if (dto.getCotizaciones() != null && !dto.getCotizaciones().isEmpty()) {
            crearCotizaciones(liquidacion, dto.getCotizaciones());
        }

        // 7. Crear deducciones
        if (dto.getDeducciones() != null && !dto.getDeducciones().isEmpty()) {
            crearDeducciones(liquidacion, dto.getDeducciones());
        }

        // 8. Calcular valores
        calcularValores(liquidacion);

        // 9. Registrar en auditoría
        registrarAuditoriaCreacion(liquidacion, ipOrigen);

        // 10. Notificar
        notificarCreacion(liquidacion, socio);

        log.info("Liquidación creada exitosamente - ID: {}", liquidacion.getId());

        return convertToResponseDto(liquidacion);
    }

    /**
     * Obtener liquidaciones
     */
    @Transactional(readOnly = true)
    public List<LiquidacionResponseDto> getLiquidaciones(
            Integer socioId,
            String tipo,
            String estado,
            LocalDate fechaInicio,
            LocalDate fechaFin
    ) {
        log.debug("Obteniendo liquidaciones - Socio: {}, Tipo: {}, Estado: {}", socioId, tipo, estado);

        List<Liquidacion> liquidaciones;

        if (fechaInicio != null && fechaFin != null) {
            liquidaciones = liquidacionRepository.findByRangoFechas(fechaInicio, fechaFin);
        } else if (socioId != null && estado != null) {
            Socio socio = socioRepository.findById(socioId)
                    .orElseThrow(() -> new IllegalArgumentException("Socio no encontrado"));
            liquidaciones = liquidacionRepository.findBySocioAndEstado(socio, estado);
        } else if (socioId != null) {
            Socio socio = socioRepository.findById(socioId)
                    .orElseThrow(() -> new IllegalArgumentException("Socio no encontrado"));
            liquidaciones = liquidacionRepository.findBySocioIdOrderByFechaLiquidacionDesc(socio);
        } else if (tipo != null) {
            liquidaciones = liquidacionRepository.findByTipo(tipo);
        } else if (estado != null) {
            liquidaciones = liquidacionRepository.findByEstado(estado);
        } else {
            liquidaciones = liquidacionRepository.findAll();
        }

        return liquidaciones.stream()
                .map(this::convertToResponseDto)
                .collect(Collectors.toList());
    }

    /**
     * Obtener detalle completo de una liquidación
     */
    @Transactional(readOnly = true)
    public LiquidacionDetalleDto getDetalleLiquidacion(Integer liquidacionId) {
        log.debug("Obteniendo detalle de liquidación ID: {}", liquidacionId);

        Liquidacion liquidacion = liquidacionRepository.findById(liquidacionId)
                .orElseThrow(() -> new IllegalArgumentException("Liquidación no encontrada"));

        return convertToDetalleDto(liquidacion);
    }

    /**
     * Actualizar liquidación (solo si está en borrador)
     */
    @Transactional
    public LiquidacionResponseDto actualizarLiquidacion(
            Integer liquidacionId,
            LiquidacionCreateDto dto,
            Integer usuarioId,
            String ipOrigen
    ) {
        log.info("Actualizando liquidación ID: {}", liquidacionId);

        Liquidacion liquidacion = liquidacionRepository.findById(liquidacionId)
                .orElseThrow(() -> new IllegalArgumentException("Liquidación no encontrada"));

        // Solo se puede actualizar si está en borrador
        if (!"borrador".equals(liquidacion.getEstado())) {
            throw new IllegalArgumentException(
                    "Solo se pueden actualizar liquidaciones en estado borrador"
            );
        }

        // Validar datos
        validarDatosLiquidacion(dto);

        // Actualizar datos básicos
        actualizarDatosBasicos(liquidacion, dto);

        // Actualizar cotizaciones
        liquidacionCotizacionRepository.deleteByLiquidacionId(liquidacion);
        if (dto.getCotizaciones() != null && !dto.getCotizaciones().isEmpty()) {
            crearCotizaciones(liquidacion, dto.getCotizaciones());
        }

        // Actualizar deducciones
        liquidacionDeduccionRepository.deleteByLiquidacionId(liquidacion);
        if (dto.getDeducciones() != null && !dto.getDeducciones().isEmpty()) {
            crearDeducciones(liquidacion, dto.getDeducciones());
        }

        // Recalcular valores
        calcularValores(liquidacion);

        // Registrar en auditoría
        registrarAuditoriaActualizacion(liquidacion, ipOrigen);

        log.info("Liquidación actualizada - ID: {}", liquidacionId);

        return convertToResponseDto(liquidacion);
    }

    /**
     * Finalizar liquidación (cambiar de borrador a pendiente de pago)
     */
    @Transactional
    public LiquidacionResponseDto finalizarLiquidacion(
            Integer liquidacionId,
            Integer usuarioId,
            String ipOrigen
    ) {
        log.info("Finalizando liquidación ID: {}", liquidacionId);

        Liquidacion liquidacion = liquidacionRepository.findById(liquidacionId)
                .orElseThrow(() -> new IllegalArgumentException("Liquidación no encontrada"));

        if (!"borrador".equals(liquidacion.getEstado())) {
            throw new IllegalArgumentException(
                    "Solo se pueden finalizar liquidaciones en estado borrador"
            );
        }

        // Validar que tenga cotizaciones
        List<LiquidacionCotizacion> cotizaciones =
                liquidacionCotizacionRepository.findByLiquidacionId(liquidacion);
        if (cotizaciones.isEmpty()) {
            throw new IllegalArgumentException(
                    "La liquidación debe tener al menos una cotización antes de finalizar"
            );
        }

        // Cambiar estado
        liquidacion.setEstado("pendiente_pago");
        liquidacionRepository.save(liquidacion);

        // Registrar en auditoría
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("liquidacion_id", liquidacion.getId());

        auditoriaLotesBl.registrarAuditoria(
                null,
                "sistema",
                "borrador",
                "pendiente_pago",
                "FINALIZAR_LIQUIDACION",
                "Liquidación finalizada - ID: " + liquidacion.getId(),
                null,
                metadata,
                ipOrigen
        );

        // Notificar
        notificarFinalizacion(liquidacion);

        log.info("Liquidación finalizada - ID: {}", liquidacionId);

        return convertToResponseDto(liquidacion);
    }

    /**
     * Marcar liquidación como pagada
     */
    @Transactional
    public LiquidacionResponseDto marcarComoPagada(
            Integer liquidacionId,
            Integer usuarioId,
            String ipOrigen
    ) {
        log.info("Marcando liquidación como pagada - ID: {}", liquidacionId);

        Liquidacion liquidacion = liquidacionRepository.findById(liquidacionId)
                .orElseThrow(() -> new IllegalArgumentException("Liquidación no encontrada"));

        if (!"pendiente_pago".equals(liquidacion.getEstado())) {
            throw new IllegalArgumentException(
                    "Solo se pueden marcar como pagadas liquidaciones en estado pendiente_pago"
            );
        }

        // Cambiar estado
        liquidacion.setEstado("pagado");
        liquidacionRepository.save(liquidacion);

        // Registrar en auditoría
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("liquidacion_id", liquidacion.getId());
        metadata.put("valor_neto", liquidacion.getValorNeto());

        auditoriaLotesBl.registrarAuditoria(
                null,
                "sistema",
                "pendiente_pago",
                "pagado",
                "PAGAR_LIQUIDACION",
                "Liquidación pagada - ID: " + liquidacion.getId() +
                        ", Valor: " + liquidacion.getValorNeto() + " " + liquidacion.getMoneda(),
                null,
                metadata,
                ipOrigen
        );

        // Notificar
        notificarPago(liquidacion);

        log.info("Liquidación marcada como pagada - ID: {}", liquidacionId);

        return convertToResponseDto(liquidacion);
    }

    /**
     * Obtener estadísticas de liquidaciones
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getEstadisticas(Integer socioId) {
        log.debug("Obteniendo estadísticas de liquidaciones");

        Map<String, Object> stats = new HashMap<>();

        List<Liquidacion> liquidaciones = socioId != null
                ? liquidacionRepository.findBySocioIdOrderByFechaLiquidacionDesc(
                socioRepository.findById(socioId)
                        .orElseThrow(() -> new IllegalArgumentException("Socio no encontrado"))
        )
                : liquidacionRepository.findAll();

        stats.put("totalLiquidaciones", liquidaciones.size());

        // Por estado
        Map<String, Long> porEstado = liquidaciones.stream()
                .collect(Collectors.groupingBy(
                        Liquidacion::getEstado,
                        Collectors.counting()
                ));
        stats.put("liquidacionesPorEstado", porEstado);

        // Por tipo
        Map<String, Long> porTipo = liquidaciones.stream()
                .collect(Collectors.groupingBy(
                        Liquidacion::getTipoLiquidacion,
                        Collectors.counting()
                ));
        stats.put("liquidacionesPorTipo", porTipo);

        // Total pagado
        BigDecimal totalPagado = liquidaciones.stream()
                .filter(l -> "pagado".equals(l.getEstado()))
                .map(Liquidacion::getValorNeto)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        stats.put("totalPagado", totalPagado);

        // Pendiente de pago
        BigDecimal pendientePago = liquidaciones.stream()
                .filter(l -> "pendiente_pago".equals(l.getEstado()))
                .map(Liquidacion::getValorNeto)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        stats.put("pendientePago", pendientePago);

        return stats;
    }

    // ==================== MÉTODOS DE VALIDACIÓN ====================

    private void validarDatosLiquidacion(LiquidacionCreateDto dto) {
        if (dto.getSocioId() == null) {
            throw new IllegalArgumentException("El socio es requerido");
        }

        if (dto.getTipoLiquidacion() == null || dto.getTipoLiquidacion().trim().isEmpty()) {
            throw new IllegalArgumentException("El tipo de liquidación es requerido");
        }

        if (!List.of("venta_directa", "venta_concentrado", "cobro_ingenio")
                .contains(dto.getTipoLiquidacion())) {
            throw new IllegalArgumentException(
                    "Tipo de liquidación inválido. Debe ser: venta_directa, venta_concentrado o cobro_ingenio"
            );
        }

        if (dto.getFechaLiquidacion() == null) {
            throw new IllegalArgumentException("La fecha de liquidación es requerida");
        }

        if (dto.getFechaLiquidacion().isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("La fecha de liquidación no puede ser futura");
        }

        if (dto.getPesoLiquidado() == null || dto.getPesoLiquidado().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("El peso liquidado debe ser mayor a 0");
        }
    }

    private void validarRelacionesPorTipo(LiquidacionCreateDto dto) {
        switch (dto.getTipoLiquidacion()) {
            case "venta_directa":
                if (dto.getLoteId() == null) {
                    throw new IllegalArgumentException(
                            "Para venta directa se requiere un lote"
                    );
                }
                // Validar que el lote exista y esté completado
                Lotes lote = lotesRepository.findById(dto.getLoteId())
                        .orElseThrow(() -> new IllegalArgumentException("Lote no encontrado"));

                if (!"Completado".equals(lote.getEstado())) {
                    throw new IllegalArgumentException(
                            "El lote debe estar en estado Completado"
                    );
                }

                // Validar que no tenga ya una liquidación
                if (liquidacionLoteRepository.loteYaTieneLiquidacion(lote)) {
                    throw new IllegalArgumentException(
                            "El lote ya tiene una liquidación asociada"
                    );
                }
                break;

            case "venta_concentrado":
                if (dto.getConcentradoId() == null) {
                    throw new IllegalArgumentException(
                            "Para venta de concentrado se requiere un concentrado"
                    );
                }
                // Validar que el concentrado exista y esté procesado
                Concentrado concentradoVenta = concentradoRepository.findById(dto.getConcentradoId())
                        .orElseThrow(() -> new IllegalArgumentException("Concentrado no encontrado"));

                if (!"procesado".equals(concentradoVenta.getEstado())) {
                    throw new IllegalArgumentException(
                            "El concentrado debe estar en estado procesado"
                    );
                }

                // Validar que no tenga ya una liquidación de venta
                if (liquidacionConcentradoRepository.concentradoYaTieneLiquidacionVenta(concentradoVenta)) {
                    throw new IllegalArgumentException(
                            "El concentrado ya tiene una liquidación de venta"
                    );
                }
                break;

            case "cobro_ingenio":
                if (dto.getConcentradoId() == null) {
                    throw new IllegalArgumentException(
                            "Para cobro de ingenio se requiere un concentrado"
                    );
                }
                // Validar que el concentrado exista y esté procesado
                Concentrado concentradoCobro = concentradoRepository.findById(dto.getConcentradoId())
                        .orElseThrow(() -> new IllegalArgumentException("Concentrado no encontrado"));

                if (!"procesado".equals(concentradoCobro.getEstado())) {
                    throw new IllegalArgumentException(
                            "El concentrado debe estar en estado procesado"
                    );
                }

                // Validar que no tenga ya una liquidación de cobro
                if (liquidacionConcentradoRepository.concentradoYaTieneLiquidacionIngenio(concentradoCobro)) {
                    throw new IllegalArgumentException(
                            "El concentrado ya tiene una liquidación de cobro de ingenio"
                    );
                }
                break;
        }
    }

    // ==================== MÉTODOS DE CREACIÓN ====================

    private Liquidacion crearYGuardarLiquidacion(LiquidacionCreateDto dto, Socio socio) {
        Liquidacion liquidacion = Liquidacion.builder()
                .socioId(socio)
                .tipoLiquidacion(dto.getTipoLiquidacion())
                .fechaLiquidacion(dto.getFechaLiquidacion())
                .moneda(dto.getMoneda() != null ? dto.getMoneda() : "BOB")
                .pesoLiquidado(dto.getPesoLiquidado())
                .estado("borrador")
                .build();

        return liquidacionRepository.save(liquidacion);
    }

    private void crearRelacion(Liquidacion liquidacion, LiquidacionCreateDto dto) {
        if (dto.getLoteId() != null) {
            Lotes lote = lotesRepository.findById(dto.getLoteId())
                    .orElseThrow(() -> new IllegalArgumentException("Lote no encontrado"));

            LiquidacionLote liquidacionLote = LiquidacionLote.builder()
                    .liquidacionId(liquidacion)
                    .lotesId(lote)
                    .reporteQuimicoId(dto.getReporteQuimicoId() != null
                            ? reporteQuimicoRepository.findById(dto.getReporteQuimicoId()).orElse(null)
                            : null)
                    .build();

            liquidacionLoteRepository.save(liquidacionLote);
        }

        if (dto.getConcentradoId() != null) {
            Concentrado concentrado = concentradoRepository.findById(dto.getConcentradoId())
                    .orElseThrow(() -> new IllegalArgumentException("Concentrado no encontrado"));

            LiquidacionConcentrado liquidacionConcentrado = LiquidacionConcentrado.builder()
                    .liquidacionId(liquidacion)
                    .concentradoId(concentrado)
                    .reporteQuimicoId(dto.getReporteQuimicoId() != null
                            ? reporteQuimicoRepository.findById(dto.getReporteQuimicoId()).orElse(null)
                            : null)
                    .build();

            liquidacionConcentradoRepository.save(liquidacionConcentrado);
        }
    }

    private void crearCotizaciones(Liquidacion liquidacion, List<CotizacionDto> cotizacionesDto) {
        for (CotizacionDto cotizacionDto : cotizacionesDto) {
            LiquidacionCotizacion cotizacion = LiquidacionCotizacion.builder()
                    .liquidacionId(liquidacion)
                    .mineral(cotizacionDto.getMineral())
                    .cotizacionUsd(cotizacionDto.getCotizacionUsd())
                    .unidad(cotizacionDto.getUnidad())
                    .build();

            liquidacionCotizacionRepository.save(cotizacion);
        }
    }

    private void crearDeducciones(Liquidacion liquidacion, List<DeduccionDto> deduccionesDto) {
        for (DeduccionDto deduccionDto : deduccionesDto) {
            LiquidacionDeduccion deduccion = LiquidacionDeduccion.builder()
                    .liquidacionId(liquidacion)
                    .concepto(deduccionDto.getConcepto())
                    .monto(deduccionDto.getMonto())
                    .porcentaje(deduccionDto.getPorcentaje())
                    .tipoDeduccion(deduccionDto.getTipoDeduccion())
                    .build();

            liquidacionDeduccionRepository.save(deduccion);
        }
    }

    private void actualizarDatosBasicos(Liquidacion liquidacion, LiquidacionCreateDto dto) {
        liquidacion.setFechaLiquidacion(dto.getFechaLiquidacion());
        liquidacion.setMoneda(dto.getMoneda() != null ? dto.getMoneda() : "BOB");
        liquidacion.setPesoLiquidado(dto.getPesoLiquidado());

        liquidacionRepository.save(liquidacion);
    }

    /**
     * Calcular valores bruto y neto de la liquidación
     */
    private void calcularValores(Liquidacion liquidacion) {
        // Obtener cotizaciones
        List<LiquidacionCotizacion> cotizaciones =
                liquidacionCotizacionRepository.findByLiquidacionId(liquidacion);

        // Calcular valor bruto (suma de todas las cotizaciones)
        BigDecimal valorBruto = cotizaciones.stream()
                .map(c -> c.getCotizacionUsd().multiply(liquidacion.getPesoLiquidado()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        liquidacion.setValorBruto(valorBruto);

        // Obtener deducciones
        List<LiquidacionDeduccion> deducciones =
                liquidacionDeduccionRepository.findByLiquidacionId(liquidacion);

        // Calcular total de deducciones
        BigDecimal totalDeducciones = BigDecimal.ZERO;

        for (LiquidacionDeduccion deduccion : deducciones) {
            BigDecimal montoDeduccion;

            if ("porcentaje".equals(deduccion.getTipoDeduccion())) {
                // Calcular porcentaje sobre valor bruto
                montoDeduccion = valorBruto
                        .multiply(deduccion.getPorcentaje())
                        .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);

                // Actualizar monto calculado
                deduccion.setMonto(montoDeduccion);
                liquidacionDeduccionRepository.save(deduccion);
            } else {
                // Monto fijo
                montoDeduccion = deduccion.getMonto();
            }

            totalDeducciones = totalDeducciones.add(montoDeduccion);
        }

        // Calcular valor neto
        BigDecimal valorNeto = valorBruto.subtract(totalDeducciones);
        liquidacion.setValorNeto(valorNeto);

        liquidacionRepository.save(liquidacion);

        log.debug("Valores calculados - Bruto: {}, Deducciones: {}, Neto: {}",
                valorBruto, totalDeducciones, valorNeto);
    }

    // ==================== MÉTODOS DE AUDITORÍA ====================

    private void registrarAuditoriaCreacion(Liquidacion liquidacion, String ipOrigen) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("liquidacion_id", liquidacion.getId());
        metadata.put("tipo_liquidacion", liquidacion.getTipoLiquidacion());
        metadata.put("socio_id", liquidacion.getSocioId().getId());
        metadata.put("peso_liquidado", liquidacion.getPesoLiquidado());

        auditoriaLotesBl.registrarAuditoria(
                null,
                "sistema",
                null,
                "borrador",
                "CREAR_LIQUIDACION",
                "Liquidación creada - Tipo: " + liquidacion.getTipoLiquidacion(),
                null,
                metadata,
                ipOrigen
        );
    }

    private void registrarAuditoriaActualizacion(Liquidacion liquidacion, String ipOrigen) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("liquidacion_id", liquidacion.getId());

        auditoriaLotesBl.registrarAuditoria(
                null,
                "sistema",
                null,
                null,
                "ACTUALIZAR_LIQUIDACION",
                "Liquidación actualizada - ID: " + liquidacion.getId(),
                null,
                metadata,
                ipOrigen
        );
    }

    // ==================== MÉTODOS DE NOTIFICACIÓN ====================

    private void notificarCreacion(Liquidacion liquidacion, Socio socio) {
        Integer socioUsuarioId = socio.getUsuariosId().getId();

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("liquidacionId", liquidacion.getId());
        metadata.put("tipo", liquidacion.getTipoLiquidacion());

        notificacionBl.crearNotificacion(
                socioUsuarioId,
                "info",
                "Liquidación creada",
                "Se ha creado una liquidación tipo " + liquidacion.getTipoLiquidacion(),
                metadata
        );
    }

    private void notificarFinalizacion(Liquidacion liquidacion) {
        Integer socioUsuarioId = liquidacion.getSocioId().getUsuariosId().getId();

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("liquidacionId", liquidacion.getId());
        metadata.put("valorNeto", liquidacion.getValorNeto());

        notificacionBl.crearNotificacion(
                socioUsuarioId,
                "success",
                "Liquidación finalizada",
                "Tu liquidación ha sido finalizada. Valor neto: " +
                        liquidacion.getValorNeto() + " " + liquidacion.getMoneda(),
                metadata
        );
    }

    private void notificarPago(Liquidacion liquidacion) {
        Integer socioUsuarioId = liquidacion.getSocioId().getUsuariosId().getId();

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("liquidacionId", liquidacion.getId());
        metadata.put("valorNeto", liquidacion.getValorNeto());

        notificacionBl.crearNotificacion(
                socioUsuarioId,
                "success",
                "Pago realizado",
                "Se ha registrado el pago de tu liquidación. Monto: " +
                        liquidacion.getValorNeto() + " " + liquidacion.getMoneda(),
                metadata
        );
    }

    // ==================== MÉTODOS DE CONVERSIÓN ====================

    private LiquidacionResponseDto convertToResponseDto(Liquidacion liquidacion) {
        LiquidacionResponseDto dto = new LiquidacionResponseDto();

        dto.setId(liquidacion.getId());
        dto.setSocioId(liquidacion.getSocioId().getId());

        Persona persona = personaRepository.findByUsuariosId(
                liquidacion.getSocioId().getUsuariosId()
        ).orElse(null);
        if (persona != null) {
            dto.setSocioNombre(persona.getNombres() + " " + persona.getPrimerApellido());
        }

        dto.setTipoLiquidacion(liquidacion.getTipoLiquidacion());
        dto.setFechaLiquidacion(liquidacion.getFechaLiquidacion());
        dto.setMoneda(liquidacion.getMoneda());
        dto.setPesoLiquidado(liquidacion.getPesoLiquidado());
        dto.setValorBruto(liquidacion.getValorBruto());
        dto.setValorNeto(liquidacion.getValorNeto());
        dto.setEstado(liquidacion.getEstado());
        dto.setCreatedAt(liquidacion.getCreatedAt());
        dto.setUpdatedAt(liquidacion.getUpdatedAt());

        // Información relacionada
        // TODO: Completar con información de lote/concentrado

        return dto;
    }

    private LiquidacionDetalleDto convertToDetalleDto(Liquidacion liquidacion) {
        LiquidacionDetalleDto dto = new LiquidacionDetalleDto();

        dto.setId(liquidacion.getId());
        dto.setSocioId(liquidacion.getSocioId().getId());

        Persona persona = personaRepository.findByUsuariosId(
                liquidacion.getSocioId().getUsuariosId()
        ).orElse(null);
        if (persona != null) {
            dto.setSocioNombre(persona.getNombres() + " " + persona.getPrimerApellido());
            dto.setSocioCi(persona.getCi());
            dto.setSocioTelefono(persona.getNumeroCelular());
        }

        dto.setTipoLiquidacion(liquidacion.getTipoLiquidacion());
        dto.setFechaLiquidacion(liquidacion.getFechaLiquidacion());
        dto.setMoneda(liquidacion.getMoneda());
        dto.setPesoLiquidado(liquidacion.getPesoLiquidado());
        dto.setValorBruto(liquidacion.getValorBruto());
        dto.setValorNeto(liquidacion.getValorNeto());
        dto.setEstado(liquidacion.getEstado());
        dto.setCreatedAt(liquidacion.getCreatedAt());
        dto.setUpdatedAt(liquidacion.getUpdatedAt());

        // Cotizaciones
        List<LiquidacionCotizacion> cotizaciones =
                liquidacionCotizacionRepository.findByLiquidacionId(liquidacion);
        dto.setCotizaciones(cotizaciones.stream()
                .map(this::convertCotizacionToDto)
                .collect(Collectors.toList())
        );

        // Deducciones
        List<LiquidacionDeduccion> deducciones =
                liquidacionDeduccionRepository.findByLiquidacionId(liquidacion);
        dto.setDeducciones(deducciones.stream()
                .map(this::convertDeduccionToDto)
                .collect(Collectors.toList())
        );

        // Cálculo
        dto.setCalculo(calcularDetalleValores(liquidacion, cotizaciones, deducciones));

        return dto;
    }

    private CotizacionDto convertCotizacionToDto(LiquidacionCotizacion cotizacion) {
        return new CotizacionDto(
                cotizacion.getId(),
                cotizacion.getMineral(),
                cotizacion.getCotizacionUsd(),
                cotizacion.getUnidad()
        );
    }

    private DeduccionDto convertDeduccionToDto(LiquidacionDeduccion deduccion) {
        return new DeduccionDto(
                deduccion.getId(),
                deduccion.getConcepto(),
                deduccion.getMonto(),
                deduccion.getPorcentaje(),
                deduccion.getTipoDeduccion()
        );
    }

    private LiquidacionCalculoDto calcularDetalleValores(
            Liquidacion liquidacion,
            List<LiquidacionCotizacion> cotizaciones,
            List<LiquidacionDeduccion> deducciones
    ) {
        LiquidacionCalculoDto calculo = new LiquidacionCalculoDto();

        calculo.setValorBruto(liquidacion.getValorBruto());
        calculo.setValorNeto(liquidacion.getValorNeto());

        // Valor por mineral
        Map<String, BigDecimal> valorPorMineral = new HashMap<>();
        for (LiquidacionCotizacion cotizacion : cotizaciones) {
            BigDecimal valor = cotizacion.getCotizacionUsd()
                    .multiply(liquidacion.getPesoLiquidado());
            valorPorMineral.put(cotizacion.getMineral(), valor);
        }
        calculo.setValorPorMineral(valorPorMineral);

        // Deducciones por concepto
        Map<String, BigDecimal> deduccionesPorConcepto = new HashMap<>();
        BigDecimal totalDeducciones = BigDecimal.ZERO;

        for (LiquidacionDeduccion deduccion : deducciones) {
            deduccionesPorConcepto.put(deduccion.getConcepto(), deduccion.getMonto());
            totalDeducciones = totalDeducciones.add(deduccion.getMonto());
        }
        calculo.setDeduccionesPorConcepto(deduccionesPorConcepto);
        calculo.setTotalDeducciones(totalDeducciones);

        return calculo;
    }
}