// src/main/java/ucb/edu/bo/sumajflow/dto/cooperativa/SectorResponseDto.java
package ucb.edu.bo.sumajflow.dto.cooperativa;

import java.util.List;

public class SectorResponseDto {
    private Integer id;
    private String nombre;
    private String color;
    private List<CoordenadaResponseDto> coordenadas;
    private Double area; // en hectáreas
    private String estado; // activo, inactivo

    // Constructors
    public SectorResponseDto() {}

    public SectorResponseDto(Integer id, String nombre, String color, List<CoordenadaResponseDto> coordenadas, Double area, String estado) {
        this.id = id;
        this.nombre = nombre;
        this.color = color;
        this.coordenadas = coordenadas;
        this.area = area;
        this.estado = estado;
    }

    // Getters and Setters
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }

    public List<CoordenadaResponseDto> getCoordenadas() { return coordenadas; }
    public void setCoordenadas(List<CoordenadaResponseDto> coordenadas) { this.coordenadas = coordenadas; }

    public Double getArea() { return area; }
    public void setArea(Double area) { this.area = area; }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
}