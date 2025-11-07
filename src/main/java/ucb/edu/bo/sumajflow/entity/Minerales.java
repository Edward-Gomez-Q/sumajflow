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
@Table(name = "minerales")
@XmlRootElement
@NamedQueries({
    @NamedQuery(name = "Minerales.findAll", query = "SELECT m FROM Minerales m"),
    @NamedQuery(name = "Minerales.findById", query = "SELECT m FROM Minerales m WHERE m.id = :id"),
    @NamedQuery(name = "Minerales.findByNombre", query = "SELECT m FROM Minerales m WHERE m.nombre = :nombre"),
    @NamedQuery(name = "Minerales.findByNomenclatura", query = "SELECT m FROM Minerales m WHERE m.nomenclatura = :nomenclatura")})
public class Minerales implements Serializable {

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
    @Column(name = "nomenclatura")
    private String nomenclatura;
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "mineralesId", fetch = FetchType.LAZY)
    private List<LoteMinerales> loteMineralesList;
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "mineralesId", fetch = FetchType.LAZY)
    private List<PlantaMinerales> plantaMineralesList;

    public Minerales() {
    }

    public Minerales(Integer id) {
        this.id = id;
    }

    public Minerales(Integer id, String nombre, String nomenclatura) {
        this.id = id;
        this.nombre = nombre;
        this.nomenclatura = nomenclatura;
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

    public String getNomenclatura() {
        return nomenclatura;
    }

    public void setNomenclatura(String nomenclatura) {
        this.nomenclatura = nomenclatura;
    }

    @XmlTransient
    public List<LoteMinerales> getLoteMineralesList() {
        return loteMineralesList;
    }

    public void setLoteMineralesList(List<LoteMinerales> loteMineralesList) {
        this.loteMineralesList = loteMineralesList;
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
        if (!(object instanceof Minerales)) {
            return false;
        }
        Minerales other = (Minerales) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "ucb.edu.bo.sumajflow.entity.Minerales[ id=" + id + " ]";
    }
    
}
