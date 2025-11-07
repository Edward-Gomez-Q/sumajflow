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
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 *
 * @author osval
 */
@Entity
@Table(name = "almacen_ingenio")
@XmlRootElement
@NamedQueries({
    @NamedQuery(name = "AlmacenIngenio.findAll", query = "SELECT a FROM AlmacenIngenio a"),
    @NamedQuery(name = "AlmacenIngenio.findById", query = "SELECT a FROM AlmacenIngenio a WHERE a.id = :id"),
    @NamedQuery(name = "AlmacenIngenio.findByNombre", query = "SELECT a FROM AlmacenIngenio a WHERE a.nombre = :nombre"),
    @NamedQuery(name = "AlmacenIngenio.findByCapacidadMaxima", query = "SELECT a FROM AlmacenIngenio a WHERE a.capacidadMaxima = :capacidadMaxima"),
    @NamedQuery(name = "AlmacenIngenio.findByArea", query = "SELECT a FROM AlmacenIngenio a WHERE a.area = :area"),
    @NamedQuery(name = "AlmacenIngenio.findByDepartamento", query = "SELECT a FROM AlmacenIngenio a WHERE a.departamento = :departamento"),
    @NamedQuery(name = "AlmacenIngenio.findByProvincia", query = "SELECT a FROM AlmacenIngenio a WHERE a.provincia = :provincia"),
    @NamedQuery(name = "AlmacenIngenio.findByMunicipio", query = "SELECT a FROM AlmacenIngenio a WHERE a.municipio = :municipio"),
    @NamedQuery(name = "AlmacenIngenio.findByDireccion", query = "SELECT a FROM AlmacenIngenio a WHERE a.direccion = :direccion"),
    @NamedQuery(name = "AlmacenIngenio.findByLatitud", query = "SELECT a FROM AlmacenIngenio a WHERE a.latitud = :latitud"),
    @NamedQuery(name = "AlmacenIngenio.findByLongitud", query = "SELECT a FROM AlmacenIngenio a WHERE a.longitud = :longitud")})
public class AlmacenIngenio implements Serializable {

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
    // @Max(value=?)  @Min(value=?)//if you know range of your decimal fields consider using these annotations to enforce field validation
    @Basic(optional = false)
    @NotNull
    @Column(name = "capacidad_maxima")
    private BigDecimal capacidadMaxima;
    @Column(name = "area")
    private BigDecimal area;
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
    @JoinColumn(name = "ingenio_minero_id", referencedColumnName = "id")
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private IngenioMinero ingenioMineroId;

    public AlmacenIngenio() {
    }

    public AlmacenIngenio(Integer id) {
        this.id = id;
    }

    public AlmacenIngenio(Integer id, String nombre, BigDecimal capacidadMaxima) {
        this.id = id;
        this.nombre = nombre;
        this.capacidadMaxima = capacidadMaxima;
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

    public BigDecimal getCapacidadMaxima() {
        return capacidadMaxima;
    }

    public void setCapacidadMaxima(BigDecimal capacidadMaxima) {
        this.capacidadMaxima = capacidadMaxima;
    }

    public BigDecimal getArea() {
        return area;
    }

    public void setArea(BigDecimal area) {
        this.area = area;
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

    public IngenioMinero getIngenioMineroId() {
        return ingenioMineroId;
    }

    public void setIngenioMineroId(IngenioMinero ingenioMineroId) {
        this.ingenioMineroId = ingenioMineroId;
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
        if (!(object instanceof AlmacenIngenio)) {
            return false;
        }
        AlmacenIngenio other = (AlmacenIngenio) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "ucb.edu.bo.sumajflow.entity.AlmacenIngenio[ id=" + id + " ]";
    }
    
}
