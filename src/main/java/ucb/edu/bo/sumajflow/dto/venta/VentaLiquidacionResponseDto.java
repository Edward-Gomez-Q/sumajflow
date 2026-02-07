package ucb.edu.bo.sumajflow.dto.venta;

import lombok.*;
import ucb.edu.bo.sumajflow.dto.ingenio.LoteSimpleDto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO de respuesta completo para liquidación de venta
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VentaLiquidacionResponseDto {

    private Integer id;
    private String tipoLiquidacion; // venta_concentrado | venta_lote_complejo
    private String estado;

    // Socio
    private Integer socioId;
    private String socioNombres;
    private String socioApellidos;
    private String socioCi;

    // Comercializadora
    private Integer comercializadoraId;
    private String comercializadoraNombre;
    private String comercializadoraNit;

    // Concentrados (para venta_concentrado)
    private List<ConcentradoVentaDto> concentrados;

    // Lotes (para venta_lote_complejo)
    private List<LoteSimpleDto> lotes;

    // Pesos del concentrado/lote
    private BigDecimal pesoTmh;
    private BigDecimal pesoTms;
    private BigDecimal pesoFinalTms;

    // Mineral
    private String mineralPrincipal;
    private BigDecimal leyMineralPrincipalPromedio; // promedio de reportes

    // Reportes químicos
    private ReporteQuimicoResumenDto reporteSocio;
    private ReporteQuimicoResumenDto reporteComercializadora;
    private ReporteQuimicoResumenDto reporteAcordado; // promedio

    // Cálculos de venta
    private BigDecimal cotizacionInternacionalUsd; // precio internacional por tonelada
    private BigDecimal precioAjustadoUsd;           // cotización * (ley_mineral/100)
    private BigDecimal valorBrutoUsd;               // precioAjustado * pesoFinalTms
    private BigDecimal totalDeduccionesUsd;          // suma de todas las deducciones
    private BigDecimal valorNetoUsd;                 // valorBruto - totalDeducciones
    private BigDecimal tipoCambio;
    private BigDecimal valorNetoBob;
    private String moneda;

    // Deducciones detalladas
    private List<DeduccionDetalleDto> deducciones;

    // Pago
    private LocalDateTime fechaAprobacion;
    private LocalDateTime fechaCierre;
    private LocalDateTime fechaPago;
    private String metodoPago;
    private String numeroComprobante;
    private String urlComprobante;
    private String observaciones;

    // Auditoría
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ConcentradoVentaDto {
        private Integer id;
        private String codigoConcentrado;
        private String mineralPrincipal;
        private BigDecimal pesoInicial;
        private BigDecimal pesoFinal;
        private BigDecimal pesoTmh;
        private BigDecimal pesoTms;
        private String estado;
        private Integer numeroSacos;
    }

    /**
     * Resumen de reporte químico
     * Campos opcionales según tipo de venta:
     * - venta_concentrado: leyMineralPrincipal, leyAgGmt, porcentajeH2o, leyPb
     * - venta_lote_complejo: leyAgDm, leyPb, leyZn
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ReporteQuimicoResumenDto {
        private Integer id;
        private String origen; // socio | comercializadora | acordado

        // SOLO venta_concentrado
        private BigDecimal leyMineralPrincipal;
        private BigDecimal leyAgGmt; // Plata en g/MT
        private BigDecimal porcentajeH2o; // Humedad

        // SOLO venta_lote_complejo
        private BigDecimal leyAgDm; // Plata en decimarcos

        // AMBOS tipos
        private BigDecimal leyPb; // Plomo
        private BigDecimal leyZn; // Zinc (principalmente en lote_complejo)

        private String laboratorio;
        private String urlPdf;
        private String estado;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class DeduccionDetalleDto {
        private String nombre;
        private BigDecimal porcentaje;
        private BigDecimal montoUsd;
        private BigDecimal montoBob;
        private String descripcion;
    }
}