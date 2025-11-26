package ucb.edu.bo.sumajflow.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDate;

/**
 * DTO para Socio con todos los campos necesarios para el onboarding
 */
public class SocioDto {

    @JsonProperty("cooperativa_id")
    private Integer cooperativaId;

    @JsonProperty("carnet_afiliacion_url")
    private String carnetAfiliacionUrl;

    @JsonProperty("fecha_afiliacion")
    private LocalDate fechaAfiliacion;

    @JsonProperty("carnet_identidad_url")
    private String carnetIdentidadUrl;

    // Constructors
    public SocioDto() {
    }

    // Getters and Setters
    public Integer getCooperativaId() {
        return cooperativaId;
    }


    public void setCooperativaId(Integer cooperativaId) {
        this.cooperativaId = cooperativaId;
    }
    public LocalDate getFechaAfiliacion() {
        return fechaAfiliacion;
    }
    public void setFechaAfiliacion(LocalDate fechaAfiliacion) {
        this.fechaAfiliacion = fechaAfiliacion;
    }

    public String getCarnetAfiliacionUrl() {
        return carnetAfiliacionUrl;
    }

    public void setCarnetAfiliacionUrl(String carnetAfiliacionUrl) {
        this.carnetAfiliacionUrl = carnetAfiliacionUrl;
    }

    public String getCarnetIdentidadUrl() {
        return carnetIdentidadUrl;
    }

    public void setCarnetIdentidadUrl(String carnetIdentidadUrl) {
        this.carnetIdentidadUrl = carnetIdentidadUrl;
    }
}