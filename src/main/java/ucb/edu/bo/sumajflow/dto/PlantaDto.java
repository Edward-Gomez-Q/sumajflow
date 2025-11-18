package ucb.edu.bo.sumajflow.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.util.List;

public class PlantaDto {

    private List<Integer> minerales; // IDs de minerales

    @JsonProperty("cupo_minimo")
    private BigDecimal cupoMinimo;

    @JsonProperty("capacidad_procesamiento")
    private BigDecimal capacidadProcesamiento;

    @JsonProperty("costo_procesamiento")
    private BigDecimal costoProcesamiento;

    private List<ProcesoDto> procesos; // Lista de procesos con ID y nombre

    @JsonProperty("numero_licencia")
    private String numeroLicencia;

    @JsonProperty("licencia_ambiental_url")
    private String licenciaAmbientalUrl;

    private String departamento;
    private String provincia;
    private String municipio;
    private String direccion;
    private BigDecimal latitud;
    private BigDecimal longitud;

    // Constructors
    public PlantaDto() {
    }

    // Getters and Setters
    public List<Integer> getMinerales() {
        return minerales;
    }

    public void setMinerales(List<Integer> minerales) {
        this.minerales = minerales;
    }

    public BigDecimal getCupoMinimo() {
        return cupoMinimo;
    }

    public void setCupoMinimo(BigDecimal cupoMinimo) {
        this.cupoMinimo = cupoMinimo;
    }

    public BigDecimal getCapacidadProcesamiento() {
        return capacidadProcesamiento;
    }

    public void setCapacidadProcesamiento(BigDecimal capacidadProcesamiento) {
        this.capacidadProcesamiento = capacidadProcesamiento;
    }

    public BigDecimal getCostoProcesamiento() {
        return costoProcesamiento;
    }

    public void setCostoProcesamiento(BigDecimal costoProcesamiento) {
        this.costoProcesamiento = costoProcesamiento;
    }

    public List<ProcesoDto> getProcesos() {
        return procesos;
    }

    public void setProcesos(List<ProcesoDto> procesos) {
        this.procesos = procesos;
    }

    public String getNumeroLicencia() {
        return numeroLicencia;
    }

    public void setNumeroLicencia(String numeroLicencia) {
        this.numeroLicencia = numeroLicencia;
    }

    public String getLicenciaAmbientalUrl() {
        return licenciaAmbientalUrl;
    }

    public void setLicenciaAmbientalUrl(String licenciaAmbientalUrl) {
        this.licenciaAmbientalUrl = licenciaAmbientalUrl;
    }

    public String getDepartamento() {
        return departamento;
    }

    public void setDepartamento(String departamento) {
        this.departamento = departamento;
    }

    public String getProvincia() {
        return provincia;
    }

    public void setProvincia(String provincia) {
        this.provincia = provincia;
    }

    public String getMunicipio() {
        return municipio;
    }

    public void setMunicipio(String municipio) {
        this.municipio = municipio;
    }

    public String getDireccion() {
        return direccion;
    }

    public void setDireccion(String direccion) {
        this.direccion = direccion;
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