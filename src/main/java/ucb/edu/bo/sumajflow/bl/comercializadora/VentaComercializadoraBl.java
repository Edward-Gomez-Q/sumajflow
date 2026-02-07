package ucb.edu.bo.sumajflow.bl.comercializadora;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ucb.edu.bo.sumajflow.bl.LiquidacionVentaBl;
import ucb.edu.bo.sumajflow.bl.NotificacionBl;
import ucb.edu.bo.sumajflow.dto.venta.*;
import ucb.edu.bo.sumajflow.entity.*;
import ucb.edu.bo.sumajflow.repository.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Servicio de Venta para el ROL COMERCIALIZADORA
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VentaComercializadoraBl {

    private final LiquidacionVentaBl liquidacionVentaBl;
    private final LiquidacionRepository liquidacionRepository;
    private final LiquidacionConcentradoRepository liquidacionConcentradoRepository;
    private final LiquidacionLoteRepository liquidacionLoteRepository;
    private final ReporteQuimicoRepository reporteQuimicoRepository;
    private final ConcentradoRepository concentradoRepository;
    private final LotesRepository lotesRepository;
    private final ComercializadoraRepository comercializadoraRepository;
    private final UsuariosRepository usuariosRepository;
    private final NotificacionBl notificacionBl;

    // ==================== APROBAR / RECHAZAR ====================

    @Transactional
    public VentaLiquidacionResponseDto aprobarVenta(Integer liquidacionId, Integer usuarioId) {
        log.info("Comercializadora aprobando venta - ID: {}", liquidacionId);
        Comercializadora com = obtenerComercializadoraDelUsuario(usuarioId);
        Liquidacion liq = obtenerLiquidacionConPermisos(liquidacionId, com);

        if (!"pendiente_aprobacion".equals(liq.getEstado())) {
            throw new IllegalArgumentException("Debe estar en estado 'pendiente_aprobacion'. Estado: " + liq.getEstado());
        }

        liq.setEstado("aprobado");
        liq.setFechaAprobacion(LocalDateTime.now());
        appendObs(liq, "APROBADO por comercializadora: " + LocalDateTime.now());
        liquidacionRepository.save(liq);

        notificarSocio(liq, "Venta aprobada",
                "La comercializadora " + com.getRazonSocial() + " ha aprobado tu solicitud. Ambos deben subir el reporte químico.",
                "success");

        return liquidacionVentaBl.convertirADto(liq);
    }

    @Transactional
    public VentaLiquidacionResponseDto rechazarVenta(Integer liquidacionId, String motivo, Integer usuarioId) {
        log.info("Comercializadora rechazando venta - ID: {}", liquidacionId);
        Comercializadora com = obtenerComercializadoraDelUsuario(usuarioId);
        Liquidacion liq = obtenerLiquidacionConPermisos(liquidacionId, com);

        if (!"pendiente_aprobacion".equals(liq.getEstado())) {
            throw new IllegalArgumentException("Solo se pueden rechazar ventas pendientes de aprobación");
        }

        liq.setEstado("rechazado");
        appendObs(liq, "RECHAZADO: " + (motivo != null ? motivo : "Sin motivo") + " | " + LocalDateTime.now());
        liquidacionRepository.save(liq);
        revertirEstadoItems(liq);

        notificarSocio(liq, "Venta rechazada",
                "La comercializadora " + com.getRazonSocial() + " ha rechazado. Motivo: " + (motivo != null ? motivo : "No especificado"),
                "error");

        return liquidacionVentaBl.convertirADto(liq);
    }

    /**
     * Comercializadora sube su reporte químico.
     * Si el socio ya subió, se procede al promedio automático.
     */
    @Transactional
    public VentaLiquidacionResponseDto subirReporteQuimico(
            ReporteQuimicoUploadDto uploadDto,
            Integer usuarioId
    ) {
        log.info("Comercializadora subiendo reporte químico - Liquidación ID: {}", uploadDto.getLiquidacionId());

        Comercializadora com = obtenerComercializadoraDelUsuario(usuarioId);
        Liquidacion liq = obtenerLiquidacionConPermisos(uploadDto.getLiquidacionId(), com);

        // Validar estado
        if (!"aprobado".equals(liq.getEstado()) && !"esperando_reportes".equals(liq.getEstado())) {
            throw new IllegalArgumentException(
                    "La liquidación debe estar en estado 'aprobado' o 'esperando_reportes'. Estado actual: " + liq.getEstado());
        }

        // Verificar que no haya subido ya
        if (yaSubioReporteComercializadora(liq)) {
            throw new IllegalArgumentException("Ya has subido un reporte químico para esta liquidación");
        }

        // Asignar tipo de venta y validar
        uploadDto.setTipoVenta(liq.getTipoLiquidacion());

        try {
            uploadDto.validarSegunTipoVenta();
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Error en validación: " + e.getMessage());
        }

        // Crear reporte
        String numeroReporte = String.format("RQ-COM-%d-%d",
                liq.getId(),
                System.currentTimeMillis() % 10000);

        ReporteQuimico reporte = ReporteQuimico.builder()
                .numeroReporte(numeroReporte)
                .tipoReporte("comercializadora")
                .tipoVenta(liq.getTipoLiquidacion())
                .laboratorio(uploadDto.getLaboratorio())

                // Fechas
                .fechaEmpaquetado(uploadDto.getFechaEmpaquetado())
                .fechaRecepcionLaboratorio(uploadDto.getFechaRecepcionLaboratorio())
                .fechaSalidaLaboratorio(uploadDto.getFechaSalidaLaboratorio())
                .fechaAnalisis(uploadDto.getFechaAnalisis())

                // Leyes
                .leyMineralPrincipal(uploadDto.getLeyMineralPrincipal()) // Solo venta_concentrado
                .leyAgGmt(uploadDto.getLeyAgGmt()) // Solo venta_concentrado
                .leyAgDm(uploadDto.getLeyAgDm()) // Solo venta_lote_complejo
                .leyPb(uploadDto.getLeyPb()) // Solo lote_complejo
                .leyZn(uploadDto.getLeyZn()) // Solo lote_complejo
                .porcentajeH2o(uploadDto.getPorcentajeH2o()) // Solo venta_concentrado

                // Empaquetado
                .numeroSacos(uploadDto.getNumeroSacos())
                .pesoPorSaco(uploadDto.getPesoPorSaco())
                .tipoEmpaque(uploadDto.getTipoEmpaque())

                // Documentación
                .urlPdf(uploadDto.getUrlPdf())
                .observacionesLaboratorio(uploadDto.getObservacionesLaboratorio())

                // Validación
                .estado("validado")
                .subidoPorComercializadora(true)
                .fechaSubidaComercializadora(LocalDateTime.now())
                .build();

        reporte.validarCamposSegunTipoVenta();
        reporte = reporteQuimicoRepository.save(reporte);

        asociarReporteALiquidacion(liq, reporte, uploadDto.getReferenciaId());

        if ("aprobado".equals(liq.getEstado())) {
            liq.setEstado("esperando_reportes");
            liquidacionRepository.save(liq);
        }

        // Si el socio ya subió, procesar acuerdo
        if (yaSubioReporteSocio(liq)) {
            procesarAcuerdoQuimico(liq);
        } else {
            // Notificar al socio
            notificarSocio(liq,
                    "Reporte de comercializadora subido",
                    "La comercializadora ha subido su reporte químico. Por favor sube el tuyo para continuar.",
                    "info");
        }

        log.info("✅ Reporte químico de comercializadora subido - Liquidación ID: {}", uploadDto.getLiquidacionId());
        return liquidacionVentaBl.convertirADto(liq);
    }
    private boolean yaSubioReporteSocio(Liquidacion liq) {
        if (LiquidacionVentaBl.TIPO_VENTA_CONCENTRADO.equals(liq.getTipoLiquidacion())) {
            return liq.getLiquidacionConcentradoList().stream()
                    .anyMatch(lc -> {
                        ReporteQuimico rq = lc.getReporteQuimicoId();
                        return rq != null && Boolean.TRUE.equals(rq.getSubidoPorSocio());
                    });
        } else {
            return liq.getLiquidacionLoteList().stream()
                    .anyMatch(ll -> {
                        ReporteQuimico rq = ll.getReporteQuimicoId();
                        return rq != null && Boolean.TRUE.equals(rq.getSubidoPorSocio());
                    });
        }
    }

    private boolean yaSubioReporteComercializadora(Liquidacion liq) {
        if (LiquidacionVentaBl.TIPO_VENTA_CONCENTRADO.equals(liq.getTipoLiquidacion())) {
            return liq.getLiquidacionConcentradoList().stream()
                    .anyMatch(lc -> {
                        ReporteQuimico rq = lc.getReporteQuimicoId();
                        return rq != null && Boolean.TRUE.equals(rq.getSubidoPorComercializadora());
                    });
        } else {
            return liq.getLiquidacionLoteList().stream()
                    .anyMatch(ll -> {
                        ReporteQuimico rq = ll.getReporteQuimicoId();
                        return rq != null && Boolean.TRUE.equals(rq.getSubidoPorComercializadora());
                    });
        }
    }

    // ==================== CONFIRMAR PAGO ====================

    @Transactional
    public VentaLiquidacionResponseDto confirmarPago(Integer liquidacionId, VentaPagoDto pagoDto, Integer usuarioId) {
        log.info("Comercializadora confirmando pago - ID: {}", liquidacionId);
        Comercializadora com = obtenerComercializadoraDelUsuario(usuarioId);
        Liquidacion liq = obtenerLiquidacionConPermisos(liquidacionId, com);

        if (!"cerrado".equals(liq.getEstado())) {
            throw new IllegalArgumentException("Debe estar en estado 'cerrado'. Estado: " + liq.getEstado());
        }

        liq.setEstado("pagado");
        liq.setFechaPago(LocalDateTime.now());
        liq.setMetodoPago(pagoDto.getMetodoPago());
        liq.setNumeroComprobante(pagoDto.getNumeroComprobante());
        liq.setUrlComprobante(pagoDto.getUrlComprobante());
        if (pagoDto.getObservaciones() != null && !pagoDto.getObservaciones().isBlank()) {
            appendObs(liq, "PAGO: " + pagoDto.getObservaciones());
        }
        liquidacionRepository.save(liq);
        marcarItemsVendidos(liq);

        notificarSocio(liq, "Pago recibido",
                String.format("La comercializadora %s ha confirmado el pago de %.2f BOB", com.getRazonSocial(), liq.getValorNetoBob()),
                "success");

        return liquidacionVentaBl.convertirADto(liq);
    }

    // ==================== LISTAR Y CONSULTAR ====================

    @Transactional(readOnly = true)
    public Page<VentaLiquidacionResponseDto> listarVentas(
            Integer usuarioId, String estado, String tipoLiquidacion,
            LocalDateTime fechaDesde, LocalDateTime fechaHasta, int page, int size) {
        Comercializadora com = obtenerComercializadoraDelUsuario(usuarioId);
        List<Liquidacion> ventas = liquidacionRepository
                .findByComercializadoraIdAndTipoLiquidacionInOrderByCreatedAtDesc(com, LiquidacionVentaBl.TIPOS_VENTA);
        return liquidacionVentaBl.listarLiquidaciones(ventas, estado, tipoLiquidacion, fechaDesde, fechaHasta, page, size);
    }

    @Transactional(readOnly = true)
    public VentaLiquidacionResponseDto obtenerDetalleVenta(Integer liquidacionId, Integer usuarioId) {
        Comercializadora com = obtenerComercializadoraDelUsuario(usuarioId);
        return liquidacionVentaBl.convertirADto(obtenerLiquidacionConPermisos(liquidacionId, com));
    }

    // ==================== ESTADÍSTICAS ====================

    @Transactional(readOnly = true)
    public Map<String, Object> obtenerEstadisticas(Integer usuarioId) {
        Comercializadora com = obtenerComercializadoraDelUsuario(usuarioId);
        List<Liquidacion> todas = liquidacionRepository
                .findByComercializadoraIdAndTipoLiquidacionInOrderByCreatedAtDesc(com, LiquidacionVentaBl.TIPOS_VENTA);

        Map<String, Object> stats = new HashMap<>();
        stats.put("total", todas.size());
        stats.put("pendienteAprobacion", contarPorEstado(todas, "pendiente_aprobacion"));
        stats.put("aprobadas", contarPorEstado(todas, "aprobado"));
        stats.put("esperandoCierre", contarPorEstado(todas, "esperando_cierre_venta"));
        stats.put("cerradas", contarPorEstado(todas, "cerrado"));
        stats.put("pagadas", contarPorEstado(todas, "pagado"));
        stats.put("rechazadas", contarPorEstado(todas, "rechazado"));
        stats.put("totalPagadoBob", sumarPorEstado(todas, "pagado"));
        stats.put("totalPendientePagoBob", sumarPorEstado(todas, "cerrado"));
        return stats;
    }

    // ==================== MÉTODOS PRIVADOS ====================

    private Comercializadora obtenerComercializadoraDelUsuario(Integer usuarioId) {
        Usuarios usuario = usuariosRepository.findById(usuarioId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));
        return comercializadoraRepository.findByUsuariosId(usuario)
                .orElseThrow(() -> new IllegalArgumentException("Comercializadora no encontrada"));
    }

    private Liquidacion obtenerLiquidacionConPermisos(Integer liquidacionId, Comercializadora com) {
        Liquidacion liq = liquidacionVentaBl.obtenerLiquidacion(liquidacionId);
        if (!LiquidacionVentaBl.TIPOS_VENTA.contains(liq.getTipoLiquidacion())) {
            throw new IllegalArgumentException("Esta no es una liquidación de venta");
        }
        if (liq.getComercializadoraId() == null || !liq.getComercializadoraId().getId().equals(com.getId())) {
            throw new IllegalArgumentException("No tienes permiso para acceder a esta liquidación");
        }
        return liq;
    }


    private void asociarReporteALiquidacion(Liquidacion liq, ReporteQuimico reporte, Integer referenciaId) {
        if (LiquidacionVentaBl.TIPO_VENTA_CONCENTRADO.equals(liq.getTipoLiquidacion())) {
            LiquidacionConcentrado lc = liq.getLiquidacionConcentradoList().stream()
                    .filter(r -> r.getConcentradoId().getId().equals(referenciaId)).findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Concentrado ID " + referenciaId + " no está en esta liquidación"));

            if (lc.getReporteQuimicoId() != null) {
                LiquidacionConcentrado lcNuevo = LiquidacionConcentrado.builder()
                        .liquidacionId(liq).concentradoId(lc.getConcentradoId())
                        .pesoLiquidado(lc.getPesoLiquidado()).reporteQuimicoId(reporte).build();
                liq.addLiquidacionConcentrado(lcNuevo);
                liquidacionConcentradoRepository.save(lcNuevo);
            } else {
                lc.setReporteQuimicoId(reporte);
                liquidacionConcentradoRepository.save(lc);
            }
        } else {
            LiquidacionLote ll = liq.getLiquidacionLoteList().stream()
                    .filter(r -> r.getLotesId().getId().equals(referenciaId)).findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Lote ID " + referenciaId + " no está en esta liquidación"));

            if (ll.getReporteQuimicoId() != null) {
                LiquidacionLote llNuevo = LiquidacionLote.builder()
                        .liquidacionId(liq).lotesId(ll.getLotesId())
                        .pesoEntrada(ll.getPesoEntrada()).reporteQuimicoId(reporte).build();
                liq.addLiquidacionLote(llNuevo);
                liquidacionLoteRepository.save(llNuevo);
            } else {
                ll.setReporteQuimicoId(reporte);
                liquidacionLoteRepository.save(ll);
            }
        }
    }

    /**
     * Cuando ambos reportes están subidos, calcular promedio y avanzar estado.
     * El promedio se guarda en serviciosAdicionales como JSON.
     */
    private void procesarAcuerdoQuimico(Liquidacion liquidacion) {
        log.info("Ambos reportes subidos - Procesando acuerdo químico - Liquidación ID: {}", liquidacion.getId());

        ReporteQuimico reporteSocio = null;
        ReporteQuimico reporteComercializadora = null;

        // Obtener ambos reportes
        if (LiquidacionVentaBl.TIPO_VENTA_CONCENTRADO.equals(liquidacion.getTipoLiquidacion())) {
            for (LiquidacionConcentrado lc : liquidacion.getLiquidacionConcentradoList()) {
                ReporteQuimico rq = lc.getReporteQuimicoId();
                if (rq != null) {
                    if (Boolean.TRUE.equals(rq.getSubidoPorSocio())) reporteSocio = rq;
                    if (Boolean.TRUE.equals(rq.getSubidoPorComercializadora())) reporteComercializadora = rq;
                }
            }
        } else {
            for (LiquidacionLote ll : liquidacion.getLiquidacionLoteList()) {
                ReporteQuimico rq = ll.getReporteQuimicoId();
                if (rq != null) {
                    if (Boolean.TRUE.equals(rq.getSubidoPorSocio())) reporteSocio = rq;
                    if (Boolean.TRUE.equals(rq.getSubidoPorComercializadora())) reporteComercializadora = rq;
                }
            }
        }

        if (reporteSocio == null || reporteComercializadora == null) {
            log.warn("No se encontraron ambos reportes para procesar acuerdo");
            return;
        }

        // Promediar reportes según tipo de venta
        Map<String, Object> reporteAcordado = new HashMap<>();
        boolean requiereRevision = false;
        BigDecimal diferencia = BigDecimal.ZERO;

        if (LiquidacionVentaBl.TIPO_VENTA_CONCENTRADO.equals(liquidacion.getTipoLiquidacion())) {

            BigDecimal leyMineralPromedio = promedio(reporteSocio.getLeyMineralPrincipal(), reporteComercializadora.getLeyMineralPrincipal());
            BigDecimal leyAgGmtPromedio = promedio(reporteSocio.getLeyAgGmt(), reporteComercializadora.getLeyAgGmt());
            BigDecimal leyPbPromedio = promedio(reporteSocio.getLeyPb(), reporteComercializadora.getLeyPb());
            BigDecimal h2oPromedio = promedio(reporteSocio.getPorcentajeH2o(), reporteComercializadora.getPorcentajeH2o());

            // Calcular diferencia en ley mineral principal
            diferencia = reporteSocio.getLeyMineralPrincipal()
                    .subtract(reporteComercializadora.getLeyMineralPrincipal())
                    .abs();
            requiereRevision = diferencia.compareTo(new BigDecimal("5")) > 0;

            reporteAcordado.put("ley_mineral_principal", leyMineralPromedio);
            reporteAcordado.put("ley_ag_gmt", leyAgGmtPromedio);
            reporteAcordado.put("ley_pb", leyPbPromedio);
            reporteAcordado.put("porcentaje_h2o", h2oPromedio);

        } else {
            // Para lote complejo: promediar ley_ag_dm, ley_pb, ley_zn (NO hay ley_mineral_principal)

            BigDecimal leyAgDmPromedio = promedio(reporteSocio.getLeyAgDm(), reporteComercializadora.getLeyAgDm());
            BigDecimal leyPbPromedio = promedio(reporteSocio.getLeyPb(), reporteComercializadora.getLeyPb());
            BigDecimal leyZnPromedio = promedio(reporteSocio.getLeyZn(), reporteComercializadora.getLeyZn());

            // Calcular diferencia en Pb (principal metal en lote complejo)
            diferencia = reporteSocio.getLeyPb()
                    .subtract(reporteComercializadora.getLeyPb())
                    .abs();
            requiereRevision = diferencia.compareTo(new BigDecimal("3")) > 0; // 3% para lote complejo

            reporteAcordado.put("ley_ag_dm", leyAgDmPromedio);
            reporteAcordado.put("ley_pb", leyPbPromedio);
            reporteAcordado.put("ley_zn", leyZnPromedio);
        }

        reporteAcordado.put("requiere_revision", requiereRevision);
        reporteAcordado.put("diferencia", diferencia);

        // Guardar en serviciosAdicionales
        Map<String, Object> extras = liquidacionVentaBl.parsearJson(liquidacion.getServiciosAdicionales());
        extras.put("reporte_acordado", reporteAcordado);
        liquidacion.setServiciosAdicionales(liquidacionVentaBl.convertirAJson(extras));

        // Cambiar estado
        liquidacion.setEstado("esperando_cierre_venta");
        liquidacionRepository.save(liquidacion);

        // Notificar al socio
        Integer socioUsuarioId = liquidacion.getSocioId().getUsuariosId().getId();
        Map<String, Object> metadataNotif = new HashMap<>();
        metadataNotif.put("liquidacionId", liquidacion.getId());
        metadataNotif.put("requiereRevision", requiereRevision);

        notificacionBl.crearNotificacion(
                socioUsuarioId,
                requiereRevision ? "warning" : "info",
                "Reportes químicos procesados",
                requiereRevision
                        ? String.format("Diferencia de %.2f%% detectada. Revisa antes de cerrar la venta.", diferencia)
                        : "Ambos reportes procesados correctamente. Cierra la venta cuando la cotización te convenga.",
                metadataNotif
        );

        log.info("✅ Acuerdo químico procesado - Liquidación ID: {}, Requiere revisión: {}",
                liquidacion.getId(), requiereRevision);
    }

    private BigDecimal promedio(BigDecimal a, BigDecimal b) {
        if (a == null && b == null) return null;
        if (a == null) return b;
        if (b == null) return a;
        return a.add(b).divide(BigDecimal.valueOf(2), 4, RoundingMode.HALF_UP);
    }

    private void revertirEstadoItems(Liquidacion liq) {
        if (LiquidacionVentaBl.TIPO_VENTA_CONCENTRADO.equals(liq.getTipoLiquidacion())) {
            liq.getLiquidacionConcentradoList().forEach(lc -> {
                Concentrado c = lc.getConcentradoId();
                if ("en_venta".equals(c.getEstado())) { c.setEstado("listo_para_venta"); concentradoRepository.save(c); }
            });
        } else {
            liq.getLiquidacionLoteList().forEach(ll -> {
                Lotes l = ll.getLotesId();
                if ("En venta".equals(l.getEstado())) { l.setEstado("Transporte completo"); lotesRepository.save(l); }
            });
        }
    }

    private void marcarItemsVendidos(Liquidacion liq) {
        if (LiquidacionVentaBl.TIPO_VENTA_CONCENTRADO.equals(liq.getTipoLiquidacion())) {
            liq.getLiquidacionConcentradoList().forEach(lc -> {
                Concentrado c = lc.getConcentradoId();
                c.setEstado("vendido");
                concentradoRepository.save(c);
                log.info("✅ Concentrado ID: {} marcado como vendido", c.getId());
            });
        } else {
            liq.getLiquidacionLoteList().forEach(ll -> {
                Lotes l = ll.getLotesId();
                l.setEstado("Vendido");
                lotesRepository.save(l);
                log.info("✅ Lote ID: {} marcado como vendido", l.getId());
            });
        }
    }

    private void notificarSocio(Liquidacion liq, String titulo, String mensaje, String tipo) {
        Map<String, Object> meta = new HashMap<>();
        meta.put("liquidacionId", liq.getId());
        meta.put("tipoLiquidacion", liq.getTipoLiquidacion());
        notificacionBl.crearNotificacion(liq.getSocioId().getUsuariosId().getId(), tipo, titulo, mensaje, meta);
    }

    private void appendObs(Liquidacion liq, String texto) {
        String obs = liq.getObservaciones() != null ? liq.getObservaciones() : "";
        liq.setObservaciones(obs + " | " + texto);
    }

    private long contarPorEstado(List<Liquidacion> list, String estado) {
        return list.stream().filter(l -> estado.equals(l.getEstado())).count();
    }

    private BigDecimal sumarPorEstado(List<Liquidacion> list, String estado) {
        return list.stream().filter(l -> estado.equals(l.getEstado()))
                .map(Liquidacion::getValorNetoBob).filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}