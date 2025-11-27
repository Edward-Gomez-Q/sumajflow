package ucb.edu.bo.sumajflow.dto.cooperativa;

import java.math.BigDecimal;

public class CoordenadaResponseDto {
    private Integer id;
    private Integer orden;
    private BigDecimal latitud;
    private BigDecimal longitud;

    // Constructors
    public CoordenadaResponseDto() {}

    public CoordenadaResponseDto(Integer id, Integer orden, BigDecimal latitud, BigDecimal longitud) {
        this.id = id;
        this.orden = orden;
        this.latitud = latitud;
        this.longitud = longitud;
    }

    // Getters and Setters
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public Integer getOrden() { return orden; }
    public void setOrden(Integer orden) { this.orden = orden; }

    public BigDecimal getLatitud() { return latitud; }
    public void setLatitud(BigDecimal latitud) { this.latitud = latitud; }

    public BigDecimal getLongitud() { return longitud; }
    public void setLongitud(BigDecimal longitud) { this.longitud = longitud; }
}