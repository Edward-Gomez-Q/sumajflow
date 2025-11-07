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
import java.util.Date;
import java.util.List;

/**
 *
 * @author osval
 */
@Entity
@Table(name = "socio")
@XmlRootElement
@NamedQueries({
    @NamedQuery(name = "Socio.findAll", query = "SELECT s FROM Socio s"),
    @NamedQuery(name = "Socio.findById", query = "SELECT s FROM Socio s WHERE s.id = :id"),
    @NamedQuery(name = "Socio.findByFechaEnvio", query = "SELECT s FROM Socio s WHERE s.fechaEnvio = :fechaEnvio"),
    @NamedQuery(name = "Socio.findByEstado", query = "SELECT s FROM Socio s WHERE s.estado = :estado"),
    @NamedQuery(name = "Socio.findByCarnetAfiliacionUrl", query = "SELECT s FROM Socio s WHERE s.carnetAfiliacionUrl = :carnetAfiliacionUrl"),
    @NamedQuery(name = "Socio.findByCarnetIdentidadUrl", query = "SELECT s FROM Socio s WHERE s.carnetIdentidadUrl = :carnetIdentidadUrl")})
public class Socio implements Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic(optional = false)
    @Column(name = "id")
    private Integer id;
    @Basic(optional = false)
    @NotNull
    @Column(name = "fecha_envio")
    @Temporal(TemporalType.TIMESTAMP)
    private Date fechaEnvio;
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 100)
    @Column(name = "estado")
    private String estado;
    @Size(max = 200)
    @Column(name = "carnet_afiliacion_url")
    private String carnetAfiliacionUrl;
    @Size(max = 200)
    @Column(name = "carnet_identidad_url")
    private String carnetIdentidadUrl;
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "socioId", fetch = FetchType.LAZY)
    private List<CooperativaSocio> cooperativaSocioList;
    @JoinColumn(name = "usuarios_id", referencedColumnName = "id")
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private Usuarios usuariosId;
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "socioId", fetch = FetchType.LAZY)
    private List<Minas> minasList;
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "socioId", fetch = FetchType.LAZY)
    private List<Liquidacion> liquidacionList;

    public Socio() {
    }

    public Socio(Integer id) {
        this.id = id;
    }

    public Socio(Integer id, Date fechaEnvio, String estado) {
        this.id = id;
        this.fechaEnvio = fechaEnvio;
        this.estado = estado;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Date getFechaEnvio() {
        return fechaEnvio;
    }

    public void setFechaEnvio(Date fechaEnvio) {
        this.fechaEnvio = fechaEnvio;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
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

    @XmlTransient
    public List<CooperativaSocio> getCooperativaSocioList() {
        return cooperativaSocioList;
    }

    public void setCooperativaSocioList(List<CooperativaSocio> cooperativaSocioList) {
        this.cooperativaSocioList = cooperativaSocioList;
    }

    public Usuarios getUsuariosId() {
        return usuariosId;
    }

    public void setUsuariosId(Usuarios usuariosId) {
        this.usuariosId = usuariosId;
    }

    @XmlTransient
    public List<Minas> getMinasList() {
        return minasList;
    }

    public void setMinasList(List<Minas> minasList) {
        this.minasList = minasList;
    }

    @XmlTransient
    public List<Liquidacion> getLiquidacionList() {
        return liquidacionList;
    }

    public void setLiquidacionList(List<Liquidacion> liquidacionList) {
        this.liquidacionList = liquidacionList;
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
        if (!(object instanceof Socio)) {
            return false;
        }
        Socio other = (Socio) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "ucb.edu.bo.sumajflow.entity.Socio[ id=" + id + " ]";
    }
    
}
