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
import java.util.List;

/**
 *
 * @author osval
 */
@Entity
@Table(name = "lotes")
@XmlRootElement
@NamedQueries({
    @NamedQuery(name = "Lotes.findAll", query = "SELECT l FROM Lotes l"),
    @NamedQuery(name = "Lotes.findById", query = "SELECT l FROM Lotes l WHERE l.id = :id"),
    @NamedQuery(name = "Lotes.findByCamionesSolicitados", query = "SELECT l FROM Lotes l WHERE l.camionesSolicitados = :camionesSolicitados"),
    @NamedQuery(name = "Lotes.findByTipoOperacion", query = "SELECT l FROM Lotes l WHERE l.tipoOperacion = :tipoOperacion"),
    @NamedQuery(name = "Lotes.findByEstado", query = "SELECT l FROM Lotes l WHERE l.estado = :estado")})
public class Lotes implements Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic(optional = false)
    @Column(name = "id")
    private Integer id;
    @Basic(optional = false)
    @NotNull
    @Column(name = "camiones_solicitados")
    private int camionesSolicitados;
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 50)
    @Column(name = "tipo_operacion")
    private String tipoOperacion;
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 50)
    @Column(name = "estado")
    private String estado;
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "lotesId", fetch = FetchType.LAZY)
    private List<AsignacionCamion> asignacionCamionList;
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "lotesId", fetch = FetchType.LAZY)
    private List<LoteIngenio> loteIngenioList;
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "lotesId", fetch = FetchType.LAZY)
    private List<LoteMinerales> loteMineralesList;
    @JoinColumn(name = "minas_id", referencedColumnName = "id")
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private Minas minasId;
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "lotesId", fetch = FetchType.LAZY)
    private List<LoteComercializadora> loteComercializadoraList;

    public Lotes() {
    }

    public Lotes(Integer id) {
        this.id = id;
    }

    public Lotes(Integer id, int camionesSolicitados, String tipoOperacion, String estado) {
        this.id = id;
        this.camionesSolicitados = camionesSolicitados;
        this.tipoOperacion = tipoOperacion;
        this.estado = estado;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public int getCamionesSolicitados() {
        return camionesSolicitados;
    }

    public void setCamionesSolicitados(int camionesSolicitados) {
        this.camionesSolicitados = camionesSolicitados;
    }

    public String getTipoOperacion() {
        return tipoOperacion;
    }

    public void setTipoOperacion(String tipoOperacion) {
        this.tipoOperacion = tipoOperacion;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    @XmlTransient
    public List<AsignacionCamion> getAsignacionCamionList() {
        return asignacionCamionList;
    }

    public void setAsignacionCamionList(List<AsignacionCamion> asignacionCamionList) {
        this.asignacionCamionList = asignacionCamionList;
    }

    @XmlTransient
    public List<LoteIngenio> getLoteIngenioList() {
        return loteIngenioList;
    }

    public void setLoteIngenioList(List<LoteIngenio> loteIngenioList) {
        this.loteIngenioList = loteIngenioList;
    }

    @XmlTransient
    public List<LoteMinerales> getLoteMineralesList() {
        return loteMineralesList;
    }

    public void setLoteMineralesList(List<LoteMinerales> loteMineralesList) {
        this.loteMineralesList = loteMineralesList;
    }

    public Minas getMinasId() {
        return minasId;
    }

    public void setMinasId(Minas minasId) {
        this.minasId = minasId;
    }

    @XmlTransient
    public List<LoteComercializadora> getLoteComercializadoraList() {
        return loteComercializadoraList;
    }

    public void setLoteComercializadoraList(List<LoteComercializadora> loteComercializadoraList) {
        this.loteComercializadoraList = loteComercializadoraList;
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
        if (!(object instanceof Lotes)) {
            return false;
        }
        Lotes other = (Lotes) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "ucb.edu.bo.sumajflow.entity.Lotes[ id=" + id + " ]";
    }
    
}
