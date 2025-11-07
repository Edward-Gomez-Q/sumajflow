/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package ucb.edu.bo.sumajflow.entity;

import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 *
 * @author osval
 */
@Entity
@Table(name = "balanza_cooperativa")
@XmlRootElement
@NamedQueries({
    @NamedQuery(name = "BalanzaCooperativa.findAll", query = "SELECT b FROM BalanzaCooperativa b"),
    @NamedQuery(name = "BalanzaCooperativa.findById", query = "SELECT b FROM BalanzaCooperativa b WHERE b.id = :id"),
    @NamedQuery(name = "BalanzaCooperativa.findByNombre", query = "SELECT b FROM BalanzaCooperativa b WHERE b.nombre = :nombre"),
    @NamedQuery(name = "BalanzaCooperativa.findByMarca", query = "SELECT b FROM BalanzaCooperativa b WHERE b.marca = :marca"),
    @NamedQuery(name = "BalanzaCooperativa.findByModelo", query = "SELECT b FROM BalanzaCooperativa b WHERE b.modelo = :modelo"),
    @NamedQuery(name = "BalanzaCooperativa.findByNumeroSerie", query = "SELECT b FROM BalanzaCooperativa b WHERE b.numeroSerie = :numeroSerie"),
    @NamedQuery(name = "BalanzaCooperativa.findByCapacidadMaxima", query = "SELECT b FROM BalanzaCooperativa b WHERE b.capacidadMaxima = :capacidadMaxima"),
    @NamedQuery(name = "BalanzaCooperativa.findByPrecisionMinima", query = "SELECT b FROM BalanzaCooperativa b WHERE b.precisionMinima = :precisionMinima"),
    @NamedQuery(name = "BalanzaCooperativa.findByFechaUltimaCalibracion", query = "SELECT b FROM BalanzaCooperativa b WHERE b.fechaUltimaCalibracion = :fechaUltimaCalibracion"),
    @NamedQuery(name = "BalanzaCooperativa.findByFechaProximaCalibracion", query = "SELECT b FROM BalanzaCooperativa b WHERE b.fechaProximaCalibracion = :fechaProximaCalibracion"),
    @NamedQuery(name = "BalanzaCooperativa.findByDepartamento", query = "SELECT b FROM BalanzaCooperativa b WHERE b.departamento = :departamento"),
    @NamedQuery(name = "BalanzaCooperativa.findByProvincia", query = "SELECT b FROM BalanzaCooperativa b WHERE b.provincia = :provincia"),
    @NamedQuery(name = "BalanzaCooperativa.findByMunicipio", query = "SELECT b FROM BalanzaCooperativa b WHERE b.municipio = :municipio"),
    @NamedQuery(name = "BalanzaCooperativa.findByDireccion", query = "SELECT b FROM BalanzaCooperativa b WHERE b.direccion = :direccion"),
    @NamedQuery(name = "BalanzaCooperativa.findByLatitud", query = "SELECT b FROM BalanzaCooperativa b WHERE b.latitud = :latitud"),
    @NamedQuery(name = "BalanzaCooperativa.findByLongitud", query = "SELECT b FROM BalanzaCooperativa b WHERE b.longitud = :longitud")})
public class BalanzaCooperativa implements Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic(optional = false)
    @Column(name = "id")
    private Integer id;
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 100)
    @Column(name = "nombre")
    private String nombre;
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 50)
    @Column(name = "marca")
    private String marca;
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 50)
    @Column(name = "modelo")
    private String modelo;
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 100)
    @Column(name = "numero_serie")
    private String numeroSerie;
    // @Max(value=?)  @Min(value=?)//if you know range of your decimal fields consider using these annotations to enforce field validation
    @Basic(optional = false)
    @NotNull
    @Column(name = "capacidad_maxima")
    private BigDecimal capacidadMaxima;
    @Basic(optional = false)
    @NotNull
    @Column(name = "precision_minima")
    private BigDecimal precisionMinima;
    @Basic(optional = false)
    @NotNull
    @Column(name = "fecha_ultima_calibracion")
    @Temporal(TemporalType.DATE)
    private Date fechaUltimaCalibracion;
    @Basic(optional = false)
    @NotNull
    @Column(name = "fecha_proxima_calibracion")
    @Temporal(TemporalType.DATE)
    private Date fechaProximaCalibracion;
    @Size(max = 50)
    @Column(name = "departamento")
    private String departamento;
    @Size(max = 100)
    @Column(name = "provincia")
    private String provincia;
    @Size(max = 100)
    @Column(name = "municipio")
    private String municipio;
    @Size(max = 250)
    @Column(name = "direccion")
    private String direccion;
    @Column(name = "latitud")
    private BigDecimal latitud;
    @Column(name = "longitud")
    private BigDecimal longitud;
    @JoinColumn(name = "cooperativa_id", referencedColumnName = "id")
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private Cooperativa cooperativaId;

    public BalanzaCooperativa() {
    }

    public BalanzaCooperativa(Integer id) {
        this.id = id;
    }

    public BalanzaCooperativa(Integer id, String nombre, String marca, String modelo, String numeroSerie, BigDecimal capacidadMaxima, BigDecimal precisionMinima, Date fechaUltimaCalibracion, Date fechaProximaCalibracion) {
        this.id = id;
        this.nombre = nombre;
        this.marca = marca;
        this.modelo = modelo;
        this.numeroSerie = numeroSerie;
        this.capacidadMaxima = capacidadMaxima;
        this.precisionMinima = precisionMinima;
        this.fechaUltimaCalibracion = fechaUltimaCalibracion;
        this.fechaProximaCalibracion = fechaProximaCalibracion;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getMarca() {
        return marca;
    }

    public void setMarca(String marca) {
        this.marca = marca;
    }

    public String getModelo() {
        return modelo;
    }

    public void setModelo(String modelo) {
        this.modelo = modelo;
    }

    public String getNumeroSerie() {
        return numeroSerie;
    }

    public void setNumeroSerie(String numeroSerie) {
        this.numeroSerie = numeroSerie;
    }

    public BigDecimal getCapacidadMaxima() {
        return capacidadMaxima;
    }

    public void setCapacidadMaxima(BigDecimal capacidadMaxima) {
        this.capacidadMaxima = capacidadMaxima;
    }

    public BigDecimal getPrecisionMinima() {
        return precisionMinima;
    }

    public void setPrecisionMinima(BigDecimal precisionMinima) {
        this.precisionMinima = precisionMinima;
    }

    public Date getFechaUltimaCalibracion() {
        return fechaUltimaCalibracion;
    }

    public void setFechaUltimaCalibracion(Date fechaUltimaCalibracion) {
        this.fechaUltimaCalibracion = fechaUltimaCalibracion;
    }

    public Date getFechaProximaCalibracion() {
        return fechaProximaCalibracion;
    }

    public void setFechaProximaCalibracion(Date fechaProximaCalibracion) {
        this.fechaProximaCalibracion = fechaProximaCalibracion;
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

    public Cooperativa getCooperativaId() {
        return cooperativaId;
    }

    public void setCooperativaId(Cooperativa cooperativaId) {
        this.cooperativaId = cooperativaId;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (id != null ? id.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof BalanzaCooperativa)) {
            return false;
        }
        BalanzaCooperativa other = (BalanzaCooperativa) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "ucb.edu.bo.sumajflow.entity.BalanzaCooperativa[ id=" + id + " ]";
    }
    
}
