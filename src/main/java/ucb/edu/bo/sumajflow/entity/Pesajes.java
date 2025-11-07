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
@Table(name = "pesajes")
@XmlRootElement
@NamedQueries({
    @NamedQuery(name = "Pesajes.findAll", query = "SELECT p FROM Pesajes p"),
    @NamedQuery(name = "Pesajes.findById", query = "SELECT p FROM Pesajes p WHERE p.id = :id"),
    @NamedQuery(name = "Pesajes.findByTipoPesaje", query = "SELECT p FROM Pesajes p WHERE p.tipoPesaje = :tipoPesaje"),
    @NamedQuery(name = "Pesajes.findByPesoBruto", query = "SELECT p FROM Pesajes p WHERE p.pesoBruto = :pesoBruto"),
    @NamedQuery(name = "Pesajes.findByPesoTara", query = "SELECT p FROM Pesajes p WHERE p.pesoTara = :pesoTara"),
    @NamedQuery(name = "Pesajes.findByPesoNeto", query = "SELECT p FROM Pesajes p WHERE p.pesoNeto = :pesoNeto"),
    @NamedQuery(name = "Pesajes.findByFechaPesaje", query = "SELECT p FROM Pesajes p WHERE p.fechaPesaje = :fechaPesaje"),
    @NamedQuery(name = "Pesajes.findByObservaciones", query = "SELECT p FROM Pesajes p WHERE p.observaciones = :observaciones")})
public class Pesajes implements Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic(optional = false)
    @Column(name = "id")
    private Integer id;
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 50)
    @Column(name = "tipo_pesaje")
    private String tipoPesaje;
    // @Max(value=?)  @Min(value=?)//if you know range of your decimal fields consider using these annotations to enforce field validation
    @Basic(optional = false)
    @NotNull
    @Column(name = "peso_bruto")
    private BigDecimal pesoBruto;
    @Basic(optional = false)
    @NotNull
    @Column(name = "peso_tara")
    private BigDecimal pesoTara;
    @Column(name = "peso_neto")
    private BigDecimal pesoNeto;
    @Column(name = "fecha_pesaje")
    @Temporal(TemporalType.TIMESTAMP)
    private Date fechaPesaje;
    @Size(max = 255)
    @Column(name = "observaciones")
    private String observaciones;
    @JoinColumn(name = "asignacion_camion_id", referencedColumnName = "id")
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private AsignacionCamion asignacionCamionId;

    public Pesajes() {
    }

    public Pesajes(Integer id) {
        this.id = id;
    }

    public Pesajes(Integer id, String tipoPesaje, BigDecimal pesoBruto, BigDecimal pesoTara) {
        this.id = id;
        this.tipoPesaje = tipoPesaje;
        this.pesoBruto = pesoBruto;
        this.pesoTara = pesoTara;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getTipoPesaje() {
        return tipoPesaje;
    }

    public void setTipoPesaje(String tipoPesaje) {
        this.tipoPesaje = tipoPesaje;
    }

    public BigDecimal getPesoBruto() {
        return pesoBruto;
    }

    public void setPesoBruto(BigDecimal pesoBruto) {
        this.pesoBruto = pesoBruto;
    }

    public BigDecimal getPesoTara() {
        return pesoTara;
    }

    public void setPesoTara(BigDecimal pesoTara) {
        this.pesoTara = pesoTara;
    }

    public BigDecimal getPesoNeto() {
        return pesoNeto;
    }

    public void setPesoNeto(BigDecimal pesoNeto) {
        this.pesoNeto = pesoNeto;
    }

    public Date getFechaPesaje() {
        return fechaPesaje;
    }

    public void setFechaPesaje(Date fechaPesaje) {
        this.fechaPesaje = fechaPesaje;
    }

    public String getObservaciones() {
        return observaciones;
    }

    public void setObservaciones(String observaciones) {
        this.observaciones = observaciones;
    }

    public AsignacionCamion getAsignacionCamionId() {
        return asignacionCamionId;
    }

    public void setAsignacionCamionId(AsignacionCamion asignacionCamionId) {
        this.asignacionCamionId = asignacionCamionId;
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
        if (!(object instanceof Pesajes)) {
            return false;
        }
        Pesajes other = (Pesajes) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "ucb.edu.bo.sumajflow.entity.Pesajes[ id=" + id + " ]";
    }
    
}
