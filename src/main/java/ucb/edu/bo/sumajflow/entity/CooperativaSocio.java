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
import java.util.Date;

/**
 *
 * @author osval
 */
@Entity
@Table(name = "cooperativa_socio")
@XmlRootElement
@NamedQueries({
    @NamedQuery(name = "CooperativaSocio.findAll", query = "SELECT c FROM CooperativaSocio c"),
    @NamedQuery(name = "CooperativaSocio.findById", query = "SELECT c FROM CooperativaSocio c WHERE c.id = :id"),
    @NamedQuery(name = "CooperativaSocio.findByFechaAfiliacion", query = "SELECT c FROM CooperativaSocio c WHERE c.fechaAfiliacion = :fechaAfiliacion"),
    @NamedQuery(name = "CooperativaSocio.findByEstado", query = "SELECT c FROM CooperativaSocio c WHERE c.estado = :estado"),
    @NamedQuery(name = "CooperativaSocio.findByObservaciones", query = "SELECT c FROM CooperativaSocio c WHERE c.observaciones = :observaciones")})
public class CooperativaSocio implements Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic(optional = false)
    @Column(name = "id")
    private Integer id;
    @Basic(optional = false)
    @NotNull
    @Column(name = "fecha_afiliacion")
    @Temporal(TemporalType.DATE)
    private Date fechaAfiliacion;
    @Size(max = 50)
    @Column(name = "estado")
    private String estado;
    @Size(max = 255)
    @Column(name = "observaciones")
    private String observaciones;
    @JoinColumn(name = "cooperativa_id", referencedColumnName = "id")
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private Cooperativa cooperativaId;
    @JoinColumn(name = "socio_id", referencedColumnName = "id")
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private Socio socioId;

    public CooperativaSocio() {
    }

    public CooperativaSocio(Integer id) {
        this.id = id;
    }

    public CooperativaSocio(Integer id, Date fechaAfiliacion) {
        this.id = id;
        this.fechaAfiliacion = fechaAfiliacion;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Date getFechaAfiliacion() {
        return fechaAfiliacion;
    }

    public void setFechaAfiliacion(Date fechaAfiliacion) {
        this.fechaAfiliacion = fechaAfiliacion;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public String getObservaciones() {
        return observaciones;
    }

    public void setObservaciones(String observaciones) {
        this.observaciones = observaciones;
    }

    public Cooperativa getCooperativaId() {
        return cooperativaId;
    }

    public void setCooperativaId(Cooperativa cooperativaId) {
        this.cooperativaId = cooperativaId;
    }

    public Socio getSocioId() {
        return socioId;
    }

    public void setSocioId(Socio socioId) {
        this.socioId = socioId;
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
        if (!(object instanceof CooperativaSocio)) {
            return false;
        }
        CooperativaSocio other = (CooperativaSocio) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "ucb.edu.bo.sumajflow.entity.CooperativaSocio[ id=" + id + " ]";
    }
    
}
