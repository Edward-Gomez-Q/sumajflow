package ucb.edu.bo.sumajflow.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.util.List;

public class CooperativaDto {

    @JsonProperty("razon_social")
    private String razonSocial;

    @JsonProperty("nit")
    private String nit;

    @JsonProperty("nim")
    private Integer nim;

    @JsonProperty("correo_contacto")
    private String correoContacto;

    @JsonProperty("numero_telefono_fijo")
    private String numeroTelefonoFijo;

    @JsonProperty("numero_telefono_movil")
    private String numeroTelefonoMovil;

    private String departamento;
    private String provincia;
    private String municipio;
    private String direccion;
    private BigDecimal latitud;
    private BigDecimal longitud;

    private List<SectorDto> sectores;
    private BalanzaDto balanza;

    // Constructors
    public CooperativaDto() {
    }

    // Getters and Setters
    public String getRazonSocial() {
        return razonSocial;
    }

    public void setRazonSocial(String razonSocial) {
        this.razonSocial = razonSocial;
    }

    public String getNit() {
        return nit;
    }

    public void setNit(String nit) {
        this.nit = nit;
    }

    public Integer getNim() {
        return nim;
    }

    public void setNim(Integer nim) {
        this.nim = nim;
    }

    public String getCorreoContacto() {
        return correoContacto;
    }

    public void setCorreoContacto(String correoContacto) {
        this.correoContacto = correoContacto;
    }

    public String getNumeroTelefonoFijo() {
        return numeroTelefonoFijo;
    }

    public void setNumeroTelefonoFijo(String numeroTelefonoFijo) {
        this.numeroTelefonoFijo = numeroTelefonoFijo;
    }

    public String getNumeroTelefonoMovil() {
        return numeroTelefonoMovil;
    }

    public void setNumeroTelefonoMovil(String numeroTelefonoMovil) {
        this.numeroTelefonoMovil = numeroTelefonoMovil;
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

    public List<SectorDto> getSectores() {
        return sectores;
    }

    public void setSectores(List<SectorDto> sectores) {
        this.sectores = sectores;
    }

    public BalanzaDto getBalanza() {
        return balanza;
    }

    public void setBalanza(BalanzaDto balanza) {
        this.balanza = balanza;
    }
}