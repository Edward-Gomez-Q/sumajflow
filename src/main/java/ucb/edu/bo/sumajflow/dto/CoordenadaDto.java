package ucb.edu.bo.sumajflow.dto;

import java.math.BigDecimal;

public class CoordenadaDto {

    private Integer orden;
    private BigDecimal latitud;
    private BigDecimal longitud;

    // Constructors
    public CoordenadaDto() {
    }

    // Getters and Setters
    public Integer getOrden() {
        return orden;
    }

    public void setOrden(Integer orden) {
        this.orden = orden;
    }

    public BigDecimal getLatitud() {
        return latitud;
    }

    public void setLatitud(BigDecimal latitud) {
        this.latitud = latitud;
    }

    public BigDecimal getLongitud() {
        return longitud;
    }

    public void setLongitud(BigDecimal longitud) {
        this.longitud = longitud;
    }
}