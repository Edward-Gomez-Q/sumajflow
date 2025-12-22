package ucb.edu.bo.sumajflow.bl.reportes;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ucb.edu.bo.sumajflow.bl.NotificacionBl;
import ucb.edu.bo.sumajflow.bl.cooperativa.AuditoriaLotesBl;
import ucb.edu.bo.sumajflow.dto.reportes.*;
import ucb.edu.bo.sumajflow.entity.*;
import ucb.edu.bo.sumajflow.repository.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReporteQuimicoBl {

    // Repositorios
    private final ReporteQuimicoRepository reporteQuimicoRepository;
    private final LotesRepository lotesRepository;
    private final ConcentradoRepository concentradoRepository;
    private final LoteMineralesRepository loteMineralesRepository;
    private final PersonaRepository personaRepository;
    private final AuditoriaLotesBl auditoriaLotesBl;
    private final NotificacionBl notificacionBl;

    /**
     * Crear un nuevo reporte químico
     */
    @Transactional
    public ReporteQuimicoResponseDto crearReporte(
            ReporteQuimicoCreateDto dto,
            Integer usuarioId,
            String ipOrigen
    ) {
        log.info("Creando reporte químico: {}", dto.getNumeroReporte());

        // 1. Validaciones
        validarDatosReporte(dto);

        // 2. Validar número único
        validarNumeroUnico(dto.getNumeroReporte());

        // 3. Validar relación (debe tener lote O concentrado, no ambos)
        validarRelacion(dto);

        // 4. Crear reporte
        ReporteQuimico reporte = crearYGuardarReporte(dto);

        // 5. Registrar en auditoría
        registrarAuditoriaCreacion(reporte, ipOrigen);

        // 6. Notificar
        notificarCreacion(reporte);

        log.info("Reporte químico creado exitosamente - ID: {}", reporte.getId());

        return convertToResponseDto(reporte);
    }

    /**
     * Obtener reportes químicos
     */
    @Transactional(readOnly = true)
    public List<ReporteQuimicoResponseDto> getReportes(
            String tipoAnalisis,
            String laboratorio,
            LocalDate fechaInicio,
            LocalDate fechaFin,
            Integer limit
    ) {
        log.debug("Obteniendo reportes - Tipo: {}, Laboratorio: {}", tipoAnalisis, laboratorio);

        List<ReporteQuimico> reportes;

        if (fechaInicio != null && fechaFin != null) {
            reportes = reporteQuimicoRepository.findByRangoFechas(fechaInicio, fechaFin);
        } else if (tipoAnalisis != null && !tipoAnalisis.trim().isEmpty()) {
            reportes = reporteQuimicoRepository.findByTipoAnalisisOrderByFechaAnalisisDesc(tipoAnalisis);
        } else if (laboratorio != null && !laboratorio.trim().isEmpty()) {
            reportes = reporteQuimicoRepository.findByLaboratorioOrderByFechaAnalisisDesc(laboratorio);
        } else if (limit != null && limit > 0) {
            reportes = reporteQuimicoRepository.findTopNRecientes(limit);
        } else {
            reportes = reporteQuimicoRepository.findAll();
        }

        return reportes.stream()
                .map(this::convertToResponseDto)
                .collect(Collectors.toList());
    }

    /**
     * Obtener detalle completo de un reporte
     */
    @Transactional(readOnly = true)
    public ReporteQuimicoDetalleDto getDetalleReporte(Integer reporteId) {
        log.debug("Obteniendo detalle de reporte ID: {}", reporteId);

        ReporteQuimico reporte = reporteQuimicoRepository.findById(reporteId)
                .orElseThrow(() -> new IllegalArgumentException("Reporte químico no encontrado"));

        return convertToDetalleDto(reporte);
    }

    /**
     * Obtener reporte por número
     */
    @Transactional(readOnly = true)
    public ReporteQuimicoResponseDto getReportePorNumero(String numeroReporte) {
        log.debug("Buscando reporte por número: {}", numeroReporte);

        ReporteQuimico reporte = reporteQuimicoRepository.findByNumeroReporte(numeroReporte)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No se encontró un reporte con el número: " + numeroReporte
                ));

        return convertToResponseDto(reporte);
    }

    /**
     * Actualizar reporte químico
     */
    @Transactional
    public ReporteQuimicoResponseDto actualizarReporte(
            Integer reporteId,
            ReporteQuimicoCreateDto dto,
            Integer usuarioId,
            String ipOrigen
    ) {
        log.info("Actualizando reporte químico ID: {}", reporteId);

        // 1. Validaciones
        validarDatosReporte(dto);

        ReporteQuimico reporte = reporteQuimicoRepository.findById(reporteId)
                .orElseThrow(() -> new IllegalArgumentException("Reporte químico no encontrado"));

        // 2. Validar número único (si se cambió)
        if (!reporte.getNumeroReporte().equals(dto.getNumeroReporte())) {
            validarNumeroUnico(dto.getNumeroReporte());
        }

        // 3. Actualizar datos
        actualizarDatosReporte(reporte, dto);

        // 4. Registrar en auditoría
        registrarAuditoriaActualizacion(reporte, ipOrigen);

        log.info("Reporte químico actualizado - ID: {}", reporteId);

        return convertToResponseDto(reporte);
    }

    /**
     * Eliminar reporte químico
     */
    @Transactional
    public void eliminarReporte(Integer reporteId, Integer usuarioId, String ipOrigen) {
        log.info("Eliminando reporte químico ID: {}", reporteId);

        ReporteQuimico reporte = reporteQuimicoRepository.findById(reporteId)
                .orElseThrow(() -> new IllegalArgumentException("Reporte químico no encontrado"));

        // Validar que no esté asociado a una liquidación
        // TODO: Implementar validación cuando se tenga tabla liquidacion_lote

        String numeroReporte = reporte.getNumeroReporte();

        // Registrar en auditoría antes de eliminar
        registrarAuditoriaEliminacion(reporte, ipOrigen);

        reporteQuimicoRepository.delete(reporte);

        log.info("Reporte químico eliminado - Número: {}", numeroReporte);
    }

    /**
     * Obtener estadísticas de reportes
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getEstadisticas() {
        log.debug("Obteniendo estadísticas de reportes químicos");

        Map<String, Object> stats = new HashMap<>();

        List<ReporteQuimico> todosReportes = reporteQuimicoRepository.findAll();

        stats.put("totalReportes", todosReportes.size());

        // Agrupar por tipo de análisis
        Map<String, Long> porTipo = todosReportes.stream()
                .collect(Collectors.groupingBy(
                        r -> r.getTipoAnalisis() != null ? r.getTipoAnalisis() : "Sin tipo",
                        Collectors.counting()
                ));
        stats.put("reportesPorTipo", porTipo);

        // Agrupar por laboratorio
        Map<String, Long> porLaboratorio = todosReportes.stream()
                .filter(r -> r.getLaboratorio() != null)
                .collect(Collectors.groupingBy(
                        ReporteQuimico::getLaboratorio,
                        Collectors.counting()
                ));
        stats.put("reportesPorLaboratorio", porLaboratorio);

        // Promedios de leyes
        OptionalDouble promedioAg = todosReportes.stream()
                .filter(r -> r.getLeyAg() != null)
                .mapToDouble(r -> r.getLeyAg().doubleValue())
                .average();

        OptionalDouble promedioPb = todosReportes.stream()
                .filter(r -> r.getLeyPb() != null)
                .mapToDouble(r -> r.getLeyPb().doubleValue())
                .average();

        OptionalDouble promedioZn = todosReportes.stream()
                .filter(r -> r.getLeyZn() != null)
                .mapToDouble(r -> r.getLeyZn().doubleValue())
                .average();

        Map<String, Double> promedioLeyes = new HashMap<>();
        promedioAg.ifPresent(v -> promedioLeyes.put("promedioLeyAg", Math.round(v * 100.0) / 100.0));
        promedioPb.ifPresent(v -> promedioLeyes.put("promedioLeyPb", Math.round(v * 100.0) / 100.0));
        promedioZn.ifPresent(v -> promedioLeyes.put("promedioLeyZn", Math.round(v * 100.0) / 100.0));

        stats.put("promedioLeyes", promedioLeyes);

        return stats;
    }

    // ==================== MÉTODOS DE VALIDACIÓN ====================

    private void validarDatosReporte(ReporteQuimicoCreateDto dto) {
        if (dto.getNumeroReporte() == null || dto.getNumeroReporte().trim().isEmpty()) {
            throw new IllegalArgumentException("El número de reporte es requerido");
        }

        if (dto.getFechaAnalisis() == null) {
            throw new IllegalArgumentException("La fecha de análisis es requerida");
        }

        if (dto.getFechaAnalisis().isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("La fecha de análisis no puede ser futura");
        }

        // Validar que al menos una ley esté presente
        if (dto.getLeyAg() == null && dto.getLeyPb() == null && dto.getLeyZn() == null) {
            throw new IllegalArgumentException(
                    "Debe proporcionar al menos una ley mineral (Ag, Pb o Zn)"
            );
        }

        // Validar rangos de leyes (deben ser positivas)
        if (dto.getLeyAg() != null && dto.getLeyAg().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("La ley de plata no puede ser negativa");
        }
        if (dto.getLeyPb() != null && dto.getLeyPb().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("La ley de plomo no puede ser negativa");
        }
        if (dto.getLeyZn() != null && dto.getLeyZn().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("La ley de zinc no puede ser negativa");
        }

        // Validar humedad (0-100%)
        if (dto.getHumedad() != null) {
            if (dto.getHumedad().compareTo(BigDecimal.ZERO) < 0 ||
                    dto.getHumedad().compareTo(new BigDecimal("100")) > 0) {
                throw new IllegalArgumentException("La humedad debe estar entre 0 y 100%");
            }
        }
    }

    private void validarNumeroUnico(String numeroReporte) {
        if (reporteQuimicoRepository.existsByNumeroReporte(numeroReporte)) {
            throw new IllegalArgumentException(
                    "Ya existe un reporte con el número: " + numeroReporte
            );
        }
    }

    private void validarRelacion(ReporteQuimicoCreateDto dto) {
        boolean tieneLote = dto.getLoteId() != null;
        boolean tieneConcentrado = dto.getConcentradoId() != null;

        if (!tieneLote && !tieneConcentrado) {
            throw new IllegalArgumentException(
                    "El reporte debe estar asociado a un lote o un concentrado"
            );
        }

        if (tieneLote && tieneConcentrado) {
            throw new IllegalArgumentException(
                    "El reporte no puede estar asociado a un lote Y un concentrado al mismo tiempo"
            );
        }

        // Validar que el lote o concentrado existan
        if (tieneLote) {
            lotesRepository.findById(dto.getLoteId())
                    .orElseThrow(() -> new IllegalArgumentException("Lote no encontrado"));
        }

        if (tieneConcentrado) {
            concentradoRepository.findById(dto.getConcentradoId())
                    .orElseThrow(() -> new IllegalArgumentException("Concentrado no encontrado"));
        }
    }

    // ==================== MÉTODOS DE CREACIÓN ====================

    private ReporteQuimico crearYGuardarReporte(ReporteQuimicoCreateDto dto) {
        ReporteQuimico reporte = ReporteQuimico.builder()
                .numeroReporte(dto.getNumeroReporte())
                .laboratorio(dto.getLaboratorio())
                .fechaAnalisis(dto.getFechaAnalisis())
                .leyAg(dto.getLeyAg())
                .leyPb(dto.getLeyPb())
                .leyZn(dto.getLeyZn())
                .humedad(dto.getHumedad())
                .tipoAnalisis(dto.getTipoAnalisis())
                .urlPdf(dto.getUrlPdf())
                .build();

        return reporteQuimicoRepository.save(reporte);
    }

    private void actualizarDatosReporte(ReporteQuimico reporte, ReporteQuimicoCreateDto dto) {
        reporte.setNumeroReporte(dto.getNumeroReporte());
        reporte.setLaboratorio(dto.getLaboratorio());
        reporte.setFechaAnalisis(dto.getFechaAnalisis());
        reporte.setLeyAg(dto.getLeyAg());
        reporte.setLeyPb(dto.getLeyPb());
        reporte.setLeyZn(dto.getLeyZn());
        reporte.setHumedad(dto.getHumedad());
        reporte.setTipoAnalisis(dto.getTipoAnalisis());
        reporte.setUrlPdf(dto.getUrlPdf());

        reporteQuimicoRepository.save(reporte);
    }

    // ==================== MÉTODOS DE AUDITORÍA ====================

    private void registrarAuditoriaCreacion(ReporteQuimico reporte, String ipOrigen) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("reporte_id", reporte.getId());
        metadata.put("numero_reporte", reporte.getNumeroReporte());
        metadata.put("laboratorio", reporte.getLaboratorio());
        metadata.put("tipo_analisis", reporte.getTipoAnalisis());

        auditoriaLotesBl.registrarAuditoria(
                null,
                "sistema",
                null,
                "creado",
                "CREAR_REPORTE_QUIMICO",
                "Reporte químico creado: " + reporte.getNumeroReporte(),
                null,
                metadata,
                ipOrigen
        );
    }

    private void registrarAuditoriaActualizacion(ReporteQuimico reporte, String ipOrigen) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("reporte_id", reporte.getId());
        metadata.put("numero_reporte", reporte.getNumeroReporte());

        auditoriaLotesBl.registrarAuditoria(
                null,
                "sistema",
                null,
                null,
                "ACTUALIZAR_REPORTE_QUIMICO",
                "Reporte químico actualizado: " + reporte.getNumeroReporte(),
                null,
                metadata,
                ipOrigen
        );
    }

    private void registrarAuditoriaEliminacion(ReporteQuimico reporte, String ipOrigen) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("numero_reporte", reporte.getNumeroReporte());

        auditoriaLotesBl.registrarAuditoria(
                null,
                "sistema",
                null,
                "eliminado",
                "ELIMINAR_REPORTE_QUIMICO",
                "Reporte químico eliminado: " + reporte.getNumeroReporte(),
                null,
                metadata,
                ipOrigen
        );
    }

    // ==================== MÉTODOS DE NOTIFICACIÓN ====================

    private void notificarCreacion(ReporteQuimico reporte) {
        // Notificar al socio propietario del lote/concentrado
        Integer socioUsuarioId = null;

        // TODO: Obtener usuario del socio según si es lote o concentrado
        // Por ahora dejamos esta funcionalidad para implementar después

        if (socioUsuarioId != null) {
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("reporteId", reporte.getId());
            metadata.put("numeroReporte", reporte.getNumeroReporte());

            notificacionBl.crearNotificacion(
                    socioUsuarioId,
                    "info",
                    "Reporte químico disponible",
                    "Se ha generado el reporte químico " + reporte.getNumeroReporte(),
                    metadata
            );
        }
    }

    // ==================== MÉTODOS DE CONVERSIÓN ====================

    private ReporteQuimicoResponseDto convertToResponseDto(ReporteQuimico reporte) {
        ReporteQuimicoResponseDto dto = new ReporteQuimicoResponseDto();

        dto.setId(reporte.getId());
        dto.setNumeroReporte(reporte.getNumeroReporte());
        dto.setLaboratorio(reporte.getLaboratorio());
        dto.setFechaAnalisis(reporte.getFechaAnalisis());
        dto.setLeyAg(reporte.getLeyAg());
        dto.setLeyPb(reporte.getLeyPb());
        dto.setLeyZn(reporte.getLeyZn());
        dto.setHumedad(reporte.getHumedad());
        dto.setTipoAnalisis(reporte.getTipoAnalisis());
        dto.setUrlPdf(reporte.getUrlPdf());
        dto.setCreatedAt(reporte.getCreatedAt());
        dto.setUpdatedAt(reporte.getUpdatedAt());

        return dto;
    }

    private ReporteQuimicoDetalleDto convertToDetalleDto(ReporteQuimico reporte) {
        ReporteQuimicoDetalleDto dto = new ReporteQuimicoDetalleDto();

        dto.setId(reporte.getId());
        dto.setNumeroReporte(reporte.getNumeroReporte());
        dto.setLaboratorio(reporte.getLaboratorio());
        dto.setFechaAnalisis(reporte.getFechaAnalisis());
        dto.setLeyAg(reporte.getLeyAg());
        dto.setLeyPb(reporte.getLeyPb());
        dto.setLeyZn(reporte.getLeyZn());
        dto.setHumedad(reporte.getHumedad());
        dto.setTipoAnalisis(reporte.getTipoAnalisis());
        dto.setUrlPdf(reporte.getUrlPdf());
        dto.setCreatedAt(reporte.getCreatedAt());
        dto.setUpdatedAt(reporte.getUpdatedAt());

        return dto;
    }
}