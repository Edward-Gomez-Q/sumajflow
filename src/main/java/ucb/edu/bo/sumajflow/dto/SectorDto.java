package ucb.edu.bo.sumajflow.dto;

import java.util.List;

public class SectorDto {

    private String nombre;
    private String color;
    private List<CoordenadaDto> coordenadas;

    // Constructors
    public SectorDto() {
    }

    // Getters and Setters
    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public List<CoordenadaDto> getCoordenadas() {
        return coordenadas;
    }

    public void setCoordenadas(List<CoordenadaDto> coordenadas) {
        this.coordenadas = coordenadas;
    }
}