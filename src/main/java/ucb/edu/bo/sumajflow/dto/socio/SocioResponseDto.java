package ucb.edu.bo.sumajflow.dto.socio;

import java.time.LocalDate;
import java.util.Date;

/**
 * DTO para la respuesta de información de un socio
 */
public class SocioResponseDto {

    private Integer id;
    private Integer usuarioId;

    // Información personal
    private String nombres;
    private String primerApellido;
    private String segundoApellido;
    private String nombreCompleto;
    private String ci;
    private Date fechaNacimiento;
    private String numeroCelular;
    private String genero;

    // Información de socio
    private String estado;
    private Date fechaEnvio;
    private String carnetAfiliacionUrl;
    private String carnetIdentidadUrl;

    // Información de cooperativa-socio
    private Integer cooperativaSocioId;
    private Date fechaAfiliacion;
    private String observaciones;

    // Información adicional
    private String correo;

    // Constructores
    public SocioResponseDto() {
    }

    // Getters y Setters
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getUsuarioId() {
        return usuarioId;
    }

    public void setUsuarioId(Integer usuarioId) {
        this.usuarioId = usuarioId;
    }

    public String getNombres() {
        return nombres;
    }

    public void setNombres(String nombres) {
        this.nombres = nombres;
    }

    public String getPrimerApellido() {
        return primerApellido;
    }

    public void setPrimerApellido(String primerApellido) {
        this.primerApellido = primerApellido;
    }

    public String getSegundoApellido() {
        return segundoApellido;
    }

    public void setSegundoApellido(String segundoApellido) {
        this.segundoApellido = segundoApellido;
    }

    public String getNombreCompleto() {
        return nombreCompleto;
    }

    public void setNombreCompleto(String nombreCompleto) {
        this.nombreCompleto = nombreCompleto;
    }

    public String getCi() {
        return ci;
    }

    public void setCi(String ci) {
        this.ci = ci;
    }

    public Date getFechaNacimiento() {
        return fechaNacimiento;
    }

    public void setFechaNacimiento(Date fechaNacimiento) {
        this.fechaNacimiento = fechaNacimiento;
    }

    public String getNumeroCelular() {
        return numeroCelular;
    }

    public void setNumeroCelular(String numeroCelular) {
        this.numeroCelular = numeroCelular;
    }

    public String getGenero() {
        return genero;
    }

    public void setGenero(String genero) {
        this.genero = genero;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public Date getFechaEnvio() {
        return fechaEnvio;
    }

    public void setFechaEnvio(Date fechaEnvio) {
        this.fechaEnvio = fechaEnvio;
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

    public Integer getCooperativaSocioId() {
        return cooperativaSocioId;
    }

    public void setCooperativaSocioId(Integer cooperativaSocioId) {
        this.cooperativaSocioId = cooperativaSocioId;
    }

    public Date getFechaAfiliacion() {
        return fechaAfiliacion;
    }

    public void setFechaAfiliacion(Date fechaAfiliacion) {
        this.fechaAfiliacion = fechaAfiliacion;
    }

    public String getObservaciones() {
        return observaciones;
    }

    public void setObservaciones(String observaciones) {
        this.observaciones = observaciones;
    }

    public String getCorreo() {
        return correo;
    }

    public void setCorreo(String correo) {
        this.correo = correo;
    }
}