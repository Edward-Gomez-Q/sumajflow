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
@Table(name = "lote_minerales")
@XmlRootElement
@NamedQueries({
    @NamedQuery(name = "LoteMinerales.findAll", query = "SELECT l FROM LoteMinerales l"),
    @NamedQuery(name = "LoteMinerales.findById", query = "SELECT l FROM LoteMinerales l WHERE l.id = :id")})
public class LoteMinerales implements Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic(optional = false)
    @Column(name = "id")
    private Integer id;
    @JoinColumn(name = "lotes_id", referencedColumnName = "id")
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private Lotes lotesId;
    @JoinColumn(name = "minerales_id", referencedColumnName = "id")
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private Minerales mineralesId;

    public LoteMinerales() {
    }

    public LoteMinerales(Integer id) {
        this.id = id;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Lotes getLotesId() {
        return lotesId;
    }

    public void setLotesId(Lotes lotesId) {
        this.lotesId = lotesId;
    }

    public Minerales getMineralesId() {
        return mineralesId;
    }

    public void setMineralesId(Minerales mineralesId) {
        this.mineralesId = mineralesId;
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
        if (!(object instanceof LoteMinerales)) {
            return false;
        }
        LoteMinerales other = (LoteMinerales) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "ucb.edu.bo.sumajflow.entity.LoteMinerales[ id=" + id + " ]";
    }
    
}
