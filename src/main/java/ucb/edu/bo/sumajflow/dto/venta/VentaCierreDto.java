package ucb.edu.bo.sumajflow.dto.venta;

import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.util.List;

/**
 * DTO para cerrar la venta (socio cierra cuando la cotización le conviene)
 * Incluye cotización internacional y deducciones
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VentaCierreDto {

    /**
     * Precio internacional del mineral principal por tonelada en USD
     * Ej: 2500.00 USD/tonelada para Zinc
     */
    @NotNull(message = "La cotización internacional es requerida")
    private BigDecimal cotizacionInternacionalUsd;

    /**
     * Mineral del que se toma la cotización (ej: "Zn", "Pb", "Ag")
     */
    private String mineralCotizado;

    /**
     * Fuente de la cotización (ej: "LME", "Kitco")
     */
    private String fuenteCotizacion;

    /**
     * Tipo de cambio USD -> BOB
     * Fijo en el sistema, enviado desde frontend
     */
    @NotNull(message = "El tipo de cambio es requerido")
    private BigDecimal tipoCambio;

    /**
     * Lista de deducciones con concepto y porcentaje.
     * Los valores por defecto vienen del frontend pero son editables.
     * Mapea directamente a los campos de LiquidacionDeduccion entity.
     */
    @NotNull(message = "Las deducciones son requeridas")
    private List<DeduccionDto> deducciones;

    private String observaciones;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class DeduccionDto {

        /** Nombre de la deducción. Mapea a LiquidacionDeduccion.concepto */
        @NotNull
        private String concepto;

        /** Porcentaje de la deducción. Mapea a LiquidacionDeduccion.porcentaje */
        @NotNull
        private BigDecimal porcentaje;

        /** Tipo: "regalia", "aporte", "impuesto". Mapea a LiquidacionDeduccion.tipoDeduccion */
        @NotNull
        private String tipoDeduccion;

        /** Descripción opcional. Mapea a LiquidacionDeduccion.descripcion */
        private String descripcion;
    }
}