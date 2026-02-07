package ucb.edu.bo.sumajflow.dto.venta;

import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO para subir un reporte químico (socio o comercializadora)
 * Los campos requeridos varían según el tipo de venta
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReporteQuimicoUploadDto {

    @NotNull(message = "El ID de la liquidación es requerido")
    private Integer liquidacionId;

    /**
     * ID del concentrado o lote al que corresponde el reporte
     * Para venta_concentrado: concentradoId
     * Para venta_lote_complejo: loteId
     */
    @NotNull(message = "El ID del concentrado/lote es requerido")
    private Integer referenciaId;

    /**
     * Tipo de venta (determina validaciones)
     * Se obtiene automáticamente de la liquidación en el backend
     */
    private String tipoVenta; // venta_concentrado | venta_lote_complejo

    @NotNull(message = "El laboratorio es requerido")
    private String laboratorio;

    // ===== FECHAS =====
    private LocalDateTime fechaEmpaquetado; // Cuando se empaquetó el concentrado

    private LocalDateTime fechaRecepcionLaboratorio; // Cuando llegó al lab

    private LocalDateTime fechaSalidaLaboratorio; // Cuando salió del lab

    @NotNull(message = "La fecha de análisis es requerida")
    private LocalDateTime fechaAnalisis; // Cuando se hizo el análisis

    // ===== LEYES DE MINERALES =====

    /**
     * Ley del mineral principal (%) - SOLO venta_concentrado
     * Ej: 51.00 para 51% de Zn en concentrado de zinc
     */
    private BigDecimal leyMineralPrincipal;

    /**
     * Plata en g/MT (gramos por tonelada métrica) - SOLO venta_concentrado
     */
    private BigDecimal leyAgGmt;

    /**
     * Plata en DM (decimarcos) - SOLO venta_lote_complejo
     */
    private BigDecimal leyAgDm;

    /**
     * Plomo (%) - AMBOS tipos de venta
     */
    private BigDecimal leyPb;

    /**
     * Zinc (%) - SOLO venta_lote_complejo
     */
    private BigDecimal leyZn;

    /**
     * Humedad (%) - SOLO venta_concentrado
     */
    private BigDecimal porcentajeH2o;

    // ===== DOCUMENTACIÓN =====
    private String urlPdf;

    /**
     * Observaciones del laboratorio
     * Ej: "Bolsa cerrada con cinta adhesiva, sellada correctamente"
     */
    private String observacionesLaboratorio;

    // ===== CARACTERÍSTICAS EMPAQUETADO (solo venta_concentrado) =====
    private Integer numeroSacos;
    private BigDecimal pesoPorSaco;
    private String tipoEmpaque; // Ej: "Sacos de polipropileno"

    /**
     * Valida que los campos obligatorios estén presentes según el tipo de venta
     */
    public void validarSegunTipoVenta() {
        if ("venta_concentrado".equals(tipoVenta)) {
            validarVentaConcentrado();
        } else if ("venta_lote_complejo".equals(tipoVenta)) {
            validarVentaLoteComplejo();
        } else {
            throw new IllegalArgumentException("Tipo de venta no válido: " + tipoVenta);
        }
    }

    private void validarVentaConcentrado() {
        if (leyMineralPrincipal == null) {
            throw new IllegalArgumentException("La ley del mineral principal es requerida para venta de concentrado");
        }
        if (leyAgGmt == null) {
            throw new IllegalArgumentException("La ley de Ag (g/MT) es requerida para venta de concentrado");
        }
        if (porcentajeH2o == null) {
            throw new IllegalArgumentException("El porcentaje de humedad es requerido para venta de concentrado");
        }
    }

    private void validarVentaLoteComplejo() {
        if (leyAgDm == null) {
            throw new IllegalArgumentException("La ley de Ag (DM) es requerida para venta de lote complejo");
        }
        if (leyPb == null) {
            throw new IllegalArgumentException("La ley de Pb es requerida para venta de lote complejo");
        }
        if (leyZn == null) {
            throw new IllegalArgumentException("La ley de Zn es requerida para venta de lote complejo");
        }

        // Validar que NO se envíen campos de concentrado
        if (leyMineralPrincipal != null) {
            throw new IllegalArgumentException("La ley del mineral principal NO aplica para venta de lote complejo");
        }
        if (porcentajeH2o != null) {
            throw new IllegalArgumentException("El porcentaje de humedad NO aplica para venta de lote complejo");
        }
        if (leyAgGmt != null) {
            throw new IllegalArgumentException("La ley de Ag en g/MT NO aplica para venta de lote complejo (usar DM)");
        }
    }
}