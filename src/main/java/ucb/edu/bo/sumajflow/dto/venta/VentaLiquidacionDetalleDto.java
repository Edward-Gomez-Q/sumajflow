package ucb.edu.bo.sumajflow.dto.venta;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * DTO DETALLADO para mostrar toda la información de una liquidación de venta.
 * Incluye desglose completo de cálculos, reportes, cotizaciones y historial.
 * Usado principalmente en vistas de detalle/validación antes de cierre o en histórico.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VentaLiquidacionDetalleDto {

    // ==================== INFORMACIÓN BÁSICA ====================
    private Integer id;
    private String tipoLiquidacion; // venta_concentrado | venta_lote_complejo
    private String estado;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // ==================== PARTES INVOLUCRADAS ====================
    private SocioInfoDto socio;
    private ComercializadoraInfoDto comercializadora;

    // ==================== ITEMS EN VENTA ====================
    private List<ConcentradoDetalleDto> concentrados; // Para venta_concentrado
    private List<LoteDetalleDto> lotes; // Para venta_lote_complejo

    // ==================== PESOS ====================
    private PesosDto pesos;

    // ==================== REPORTES QUÍMICOS ====================
    private ReportesQuimicosDto reportesQuimicos;

    // ==================== COTIZACIONES INTERNACIONALES ====================
    private List<CotizacionDetalleDto> cotizaciones;

    // ==================== CÁLCULO DE VALORACIÓN ====================
    private ValoracionDetalleDto valoracion;

    // ==================== DEDUCCIONES ====================
    private DeduccionesDetalleDto deducciones;

    // ==================== RESULTADO FINAL ====================
    private ResultadoFinalDto resultadoFinal;

    // ==================== INFORMACIÓN DE PAGO ====================
    private PagoInfoDto pago;

    // ==================== HISTORIAL DE OBSERVACIONES ====================
    private List<ObservacionDto> historialObservaciones;

    // ==================== DTOs ANIDADOS ====================

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SocioInfoDto {
        private Integer id;
        private String nombres;
        private String apellidos;
        private String ci;
        private String codigoSocio;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ComercializadoraInfoDto {
        private Integer id;
        private String razonSocial;
        private String nit;
        private String departamento;
        private String municipio;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ConcentradoDetalleDto {
        private Integer id;
        private String codigoConcentrado;
        private String mineralPrincipal;
        private Integer numeroSacos;
        private String tipoEmpaque;

        // Pesos
        private BigDecimal pesoInicial;
        private BigDecimal pesoFinal;
        private BigDecimal pesoTmh;
        private BigDecimal pesoTms;
        private BigDecimal merma;
        private BigDecimal porcentajeMerma;

        private String estado;
        private String ingenioNombre;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class LoteDetalleDto {
        private Integer id;
        private String minaNombre;
        private String tipoMineral;
        private BigDecimal pesoTotalReal;
        private String estado;
        private LocalDateTime fechaCreacion;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PesosDto {
        private BigDecimal pesoTotalEntrada; // kg
        private BigDecimal pesoTmh; // toneladas métricas húmedas
        private BigDecimal pesoTms; // toneladas métricas secas
        private BigDecimal pesoFinalTms; // peso final usado para cálculo
        private BigDecimal porcentajeHumedad; // del reporte acordado
        private String pesoUsadoEnCalculo; // descripción de cuál peso se usó
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ReportesQuimicosDto {
        private ReporteQuimicoDetalleDto reporteSocio;
        private ReporteQuimicoDetalleDto reporteComercializadora;
        private ReporteQuimicoDetalleDto reporteAcordado;
        private DiferenciasReportesDto diferencias;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ReporteQuimicoDetalleDto {
        private Integer id;
        private String numeroReporte;
        private String origen; // socio | comercializadora | acordado
        private String laboratorio;

        // Fechas
        private LocalDateTime fechaEmpaquetado;
        private LocalDateTime fechaRecepcionLaboratorio;
        private LocalDateTime fechaSalidaLaboratorio;
        private LocalDateTime fechaAnalisis;

        // Leyes (según tipo de venta)
        private BigDecimal leyMineralPrincipal; // % (venta_concentrado)
        private BigDecimal leyAgGmt; // g/MT (venta_concentrado)
        private BigDecimal leyAgDm; // DM (venta_lote_complejo)
        private BigDecimal leyPb; // % (venta_lote_complejo)
        private BigDecimal leyZn; // % (venta_lote_complejo)
        private BigDecimal porcentajeH2o; // % (venta_concentrado)

        // Empaquetado
        private Integer numeroSacos;
        private BigDecimal pesoPorSaco;
        private String tipoEmpaque;

        // Documentación
        private String urlPdf;
        private String observacionesLaboratorio;
        private String estado;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class DiferenciasReportesDto {
        private BigDecimal diferenciaLeyPrincipal; // %
        private BigDecimal diferenciaLeyAg; // según unidad
        private BigDecimal diferenciaHumedad; // %
        private Boolean requiereRevision;
        private String mensaje;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CotizacionDetalleDto {
        private String mineral; // Pb, Zn, Ag
        private BigDecimal cotizacion;
        private String unidad; // USD/ton, USD/oz, USD/DM
        private String fuente;
        private LocalDateTime fecha;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ValoracionDetalleDto {
        private String tipoVenta; // venta_concentrado | venta_lote_complejo
        private String mineralPrincipal;

        // PARA VENTA_CONCENTRADO
        private ValoracionMineralDto valoracionMineralPrincipal;
        private ValoracionPlataDto valoracionPlata;

        // PARA VENTA_LOTE_COMPLEJO
        private ValoracionMineralDto valoracionPb;
        private ValoracionMineralDto valoracionZn;
        private ValoracionPlataDto valoracionAgDm;

        // TOTALES
        private BigDecimal valorTotalUsdPorTon;
        private BigDecimal valorBrutoTotalUsd;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ValoracionMineralDto {
        private String mineral; // Pb, Zn, o nombre del mineral principal
        private BigDecimal ley; // % del reporte acordado
        private BigDecimal cotizacionInternacional; // USD/ton
        private BigDecimal valorUsdPorTon; // (cotización × ley) / 100
        private BigDecimal pesoToneladas;
        private BigDecimal valorBrutoUsd; // valorUsdPorTon × peso
        private String formulaAplicada; // descripción del cálculo
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ValoracionPlataDto {
        private BigDecimal leyAg; // g/MT o DM
        private String unidadLey;
        private BigDecimal contenidoOzPorTon; // solo para g/MT
        private BigDecimal cotizacionUsdPorOz; // solo para g/MT
        private BigDecimal cotizacionUsdPorDm; // solo para DM
        private BigDecimal valorUsdPorTon;
        private BigDecimal pesoToneladas;
        private BigDecimal valorBrutoUsd;
        private String formulaAplicada;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class DeduccionesDetalleDto {
        private List<DeduccionItemDto> deducciones;
        private BigDecimal totalDeduccionesUsd;
        private BigDecimal totalDeduccionesBob;
        private BigDecimal porcentajeTotal; // suma de todos los %
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class DeduccionItemDto {
        private Integer orden;
        private String concepto;
        private String tipoDeduccion; // regalia | aporte | otro
        private BigDecimal porcentaje;
        private String baseCalculo; // valor_bruto_principal | valor_bruto_ag | valor_bruto_total
        private BigDecimal montoBaseUsd;
        private BigDecimal montoDeducidoUsd;
        private BigDecimal montoDeducidoBob;
        private String descripcion;
        private String formulaAplicada;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ResultadoFinalDto {
        private BigDecimal valorBrutoUsd;
        private BigDecimal totalDeduccionesUsd;
        private BigDecimal valorNetoUsd;
        private BigDecimal tipoCambio;
        private BigDecimal valorNetoBob;
        private String moneda;

        // Resumen porcentual
        private BigDecimal porcentajeDeduccionTotal;
        private BigDecimal porcentajePagoSocio; // 100 - % deducciones
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PagoInfoDto {
        private LocalDateTime fechaAprobacion;
        private LocalDateTime fechaCierre;
        private LocalDateTime fechaPago;
        private String metodoPago;
        private String numeroComprobante;
        private String urlComprobante;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ObservacionDto {
        private String estado;
        private String descripcion;
        private String observaciones;
        private Integer usuarioId;
        private String usuarioNombre;
        private String tipoUsuario; // socio | comercializadora | ingenio
        private LocalDateTime timestamp;
        private String estadoAnterior; // si hubo transición
        private Map<String, Object> metadataAdicional; // cualquier dato extra
    }
}