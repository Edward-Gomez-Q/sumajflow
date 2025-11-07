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
import jakarta.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;

/**
 *
 * @author osval
 */
@Entity
@Table(name = "lote_ingenio")
@XmlRootElement
@NamedQueries({
    @NamedQuery(name = "LoteIngenio.findAll", query = "SELECT l FROM LoteIngenio l"),
    @NamedQuery(name = "LoteIngenio.findById", query = "SELECT l FROM LoteIngenio l WHERE l.id = :id")})
public class LoteIngenio implements Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic(optional = false)
    @Column(name = "id")
    private Integer id;
    @JoinColumn(name = "ingenio_minero_id", referencedColumnName = "id")
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private IngenioMinero ingenioMineroId;
    @JoinColumn(name = "lotes_id", referencedColumnName = "id")
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private Lotes lotesId;

    public LoteIngenio() {
    }

    public LoteIngenio(Integer id) {
        this.id = id;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public IngenioMinero getIngenioMineroId() {
        return ingenioMineroId;
    }

    public void setIngenioMineroId(IngenioMinero ingenioMineroId) {
        this.ingenioMineroId = ingenioMineroId;
    }

    public Lotes getLotesId() {
        return lotesId;
    }

    public void setLotesId(Lotes lotesId) {
        this.lotesId = lotesId;
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
        if (!(object instanceof LoteIngenio)) {
            return false;
        }
        LoteIngenio other = (LoteIngenio) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "ucb.edu.bo.sumajflow.entity.LoteIngenio[ id=" + id + " ]";
    }
    
}
