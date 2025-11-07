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
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlTransient;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

/**
 *
 * @author osval
 */
@Entity
@Table(name = "asignacion_camion")
@XmlRootElement
@NamedQueries({
    @NamedQuery(name = "AsignacionCamion.findAll", query = "SELECT a FROM AsignacionCamion a"),
    @NamedQuery(name = "AsignacionCamion.findById", query = "SELECT a FROM AsignacionCamion a WHERE a.id = :id"),
    @NamedQuery(name = "AsignacionCamion.findByNumeroCamion", query = "SELECT a FROM AsignacionCamion a WHERE a.numeroCamion = :numeroCamion"),
    @NamedQuery(name = "AsignacionCamion.findByEstado", query = "SELECT a FROM AsignacionCamion a WHERE a.estado = :estado"),
    @NamedQuery(name = "AsignacionCamion.findByFechaAsignacion", query = "SELECT a FROM AsignacionCamion a WHERE a.fechaAsignacion = :fechaAsignacion"),
    @NamedQuery(name = "AsignacionCamion.findByFechaInicio", query = "SELECT a FROM AsignacionCamion a WHERE a.fechaInicio = :fechaInicio"),
    @NamedQuery(name = "AsignacionCamion.findByFechaFin", query = "SELECT a FROM AsignacionCamion a WHERE a.fechaFin = :fechaFin"),
    @NamedQuery(name = "AsignacionCamion.findByPesoBruto", query = "SELECT a FROM AsignacionCamion a WHERE a.pesoBruto = :pesoBruto"),
    @NamedQuery(name = "AsignacionCamion.findByPesoNeto", query = "SELECT a FROM AsignacionCamion a WHERE a.pesoNeto = :pesoNeto"),
    @NamedQuery(name = "AsignacionCamion.findByObservaciones", query = "SELECT a FROM AsignacionCamion a WHERE a.observaciones = :observaciones")})
public class AsignacionCamion implements Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic(optional = false)
    @Column(name = "id")
    private Integer id;
    @Basic(optional = false)
    @NotNull
    @Column(name = "numero_camion")
    private int numeroCamion;
    @Size(max = 50)
    @Column(name = "estado")
    private String estado;
    @Column(name = "fecha_asignacion")
    @Temporal(TemporalType.TIMESTAMP)
    private Date fechaAsignacion;
    @Column(name = "fecha_inicio")
    @Temporal(TemporalType.TIMESTAMP)
    private Date fechaInicio;
    @Column(name = "fecha_fin")
    @Temporal(TemporalType.TIMESTAMP)
    private Date fechaFin;
    // @Max(value=?)  @Min(value=?)//if you know range of your decimal fields consider using these annotations to enforce field validation
    @Column(name = "peso_bruto")
    private BigDecimal pesoBruto;
    @Column(name = "peso_neto")
    private BigDecimal pesoNeto;
    @Size(max = 255)
    @Column(name = "observaciones")
    private String observaciones;
    @JoinColumn(name = "lotes_id", referencedColumnName = "id")
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private Lotes lotesId;
    @JoinColumn(name = "transportista_id", referencedColumnName = "id")
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private Transportista transportistaId;
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "asignacionCamionId", fetch = FetchType.LAZY)
    private List<Pesajes> pesajesList;

    public AsignacionCamion() {
    }

    public AsignacionCamion(Integer id) {
        this.id = id;
    }

    public AsignacionCamion(Integer id, int numeroCamion) {
        this.id = id;
        this.numeroCamion = numeroCamion;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public int getNumeroCamion() {
        return numeroCamion;
    }

    public void setNumeroCamion(int numeroCamion) {
        this.numeroCamion = numeroCamion;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public Date getFechaAsignacion() {
        return fechaAsignacion;
    }

    public void setFechaAsignacion(Date fechaAsignacion) {
        this.fechaAsignacion = fechaAsignacion;
    }

    public Date getFechaInicio() {
        return fechaInicio;
    }

    public void setFechaInicio(Date fechaInicio) {
        this.fechaInicio = fechaInicio;
    }

    public Date getFechaFin() {
        return fechaFin;
    }

    public void setFechaFin(Date fechaFin) {
        this.fechaFin = fechaFin;
    }

    public BigDecimal getPesoBruto() {
        return pesoBruto;
    }

    public void setPesoBruto(BigDecimal pesoBruto) {
        this.pesoBruto = pesoBruto;
    }

    public BigDecimal getPesoNeto() {
        return pesoNeto;
    }

    public void setPesoNeto(BigDecimal pesoNeto) {
        this.pesoNeto = pesoNeto;
    }

    public String getObservaciones() {
        return observaciones;
    }

    public void setObservaciones(String observaciones) {
        this.observaciones = observaciones;
    }

    public Lotes getLotesId() {
        return lotesId;
    }

    public void setLotesId(Lotes lotesId) {
        this.lotesId = lotesId;
    }

    public Transportista getTransportistaId() {
        return transportistaId;
    }

    public void setTransportistaId(Transportista transportistaId) {
        this.transportistaId = transportistaId;
    }

    @XmlTransient
    public List<Pesajes> getPesajesList() {
        return pesajesList;
    }

    public void setPesajesList(List<Pesajes> pesajesList) {
        this.pesajesList = pesajesList;
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
        if (!(object instanceof AsignacionCamion)) {
            return false;
        }
        AsignacionCamion other = (AsignacionCamion) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "ucb.edu.bo.sumajflow.entity.AsignacionCamion[ id=" + id + " ]";
    }
    
}
