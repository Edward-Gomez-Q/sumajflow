/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package ucb.edu.bo.sumajflow.entity;

import jakarta.persistence.Basic;
import jakarta.persistence.CascadeType;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlTransient;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

/**
 *
 * @author osval
 */
@Entity
@Table(name = "planta")
@XmlRootElement
@NamedQueries({
    @NamedQuery(name = "Planta.findAll", query = "SELECT p FROM Planta p"),
    @NamedQuery(name = "Planta.findById", query = "SELECT p FROM Planta p WHERE p.id = :id"),
    @NamedQuery(name = "Planta.findByCupoMinimo", query = "SELECT p FROM Planta p WHERE p.cupoMinimo = :cupoMinimo"),
    @NamedQuery(name = "Planta.findByCapacidadProcesamiento", query = "SELECT p FROM Planta p WHERE p.capacidadProcesamiento = :capacidadProcesamiento"),
    @NamedQuery(name = "Planta.findByCostoProcesamiento", query = "SELECT p FROM Planta p WHERE p.costoProcesamiento = :costoProcesamiento"),
    @NamedQuery(name = "Planta.findByLicenciaAmbientalUrl", query = "SELECT p FROM Planta p WHERE p.licenciaAmbientalUrl = :licenciaAmbientalUrl"),
    @NamedQuery(name = "Planta.findByDepartamento", query = "SELECT p FROM Planta p WHERE p.departamento = :departamento"),
    @NamedQuery(name = "Planta.findByProvincia", query = "SELECT p FROM Planta p WHERE p.provincia = :provincia"),
    @NamedQuery(name = "Planta.findByMunicipio", query = "SELECT p FROM Planta p WHERE p.municipio = :municipio"),
    @NamedQuery(name = "Planta.findByDireccion", query = "SELECT p FROM Planta p WHERE p.direccion = :direccion"),
    @NamedQuery(name = "Planta.findByLatitud", query = "SELECT p FROM Planta p WHERE p.latitud = :latitud"),
    @NamedQuery(name = "Planta.findByLongitud", query = "SELECT p FROM Planta p WHERE p.longitud = :longitud")})
public class Planta implements Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic(optional = false)
    @Column(name = "id")
    private Integer id;
    // @Max(value=?)  @Min(value=?)//if you know range of your decimal fields consider using these annotations to enforce field validation
    @Basic(optional = false)
    @NotNull
    @Column(name = "cupo_minimo")
    private BigDecimal cupoMinimo;
    @Basic(optional = false)
    @NotNull
    @Column(name = "capacidad_procesamiento")
    private BigDecimal capacidadProcesamiento;
    @Basic(optional = false)
    @NotNull
    @Column(name = "costo_procesamiento")
    private BigDecimal costoProcesamiento;
    @Size(max = 200)
    @Column(name = "licencia_ambiental_url")
    private String licenciaAmbientalUrl;
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
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "plantaId", fetch = FetchType.LAZY)
    private List<ProcesosPlanta> procesosPlantaList;
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "plantaId", fetch = FetchType.LAZY)
    private List<PlantaMinerales> plantaMineralesList;

    public Planta() {
    }

    public Planta(Integer id) {
        this.id = id;
    }

    public Planta(Integer id, BigDecimal cupoMinimo, BigDecimal capacidadProcesamiento, BigDecimal costoProcesamiento) {
        this.id = id;
        this.cupoMinimo = cupoMinimo;
        this.capacidadProcesamiento = capacidadProcesamiento;
        this.costoProcesamiento = costoProcesamiento;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
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

    public IngenioMinero getIngenioMineroId() {
        return ingenioMineroId;
    }

    public void setIngenioMineroId(IngenioMinero ingenioMineroId) {
        this.ingenioMineroId = ingenioMineroId;
    }

    @XmlTransient
    public List<ProcesosPlanta> getProcesosPlantaList() {
        return procesosPlantaList;
    }

    public void setProcesosPlantaList(List<ProcesosPlanta> procesosPlantaList) {
        this.procesosPlantaList = procesosPlantaList;
    }

    @XmlTransient
    public List<PlantaMinerales> getPlantaMineralesList() {
        return plantaMineralesList;
    }

    public void setPlantaMineralesList(List<PlantaMinerales> plantaMineralesList) {
        this.plantaMineralesList = plantaMineralesList;
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
        if (!(object instanceof Planta)) {
            return false;
        }
        Planta other = (Planta) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "ucb.edu.bo.sumajflow.entity.Planta[ id=" + id + " ]";
    }
    
}
