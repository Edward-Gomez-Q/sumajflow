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
@Table(name = "procesos_planta")
@XmlRootElement
@NamedQueries({
    @NamedQuery(name = "ProcesosPlanta.findAll", query = "SELECT p FROM ProcesosPlanta p"),
    @NamedQuery(name = "ProcesosPlanta.findById", query = "SELECT p FROM ProcesosPlanta p WHERE p.id = :id")})
public class ProcesosPlanta implements Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic(optional = false)
    @Column(name = "id")
    private Integer id;
    @JoinColumn(name = "planta_id", referencedColumnName = "id")
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private Planta plantaId;
    @JoinColumn(name = "procesos_id", referencedColumnName = "id")
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private Procesos procesosId;

    public ProcesosPlanta() {
    }

    public ProcesosPlanta(Integer id) {
        this.id = id;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Planta getPlantaId() {
        return plantaId;
    }

    public void setPlantaId(Planta plantaId) {
        this.plantaId = plantaId;
    }

    public Procesos getProcesosId() {
        return procesosId;
    }

    public void setProcesosId(Procesos procesosId) {
        this.procesosId = procesosId;
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
        if (!(object instanceof ProcesosPlanta)) {
            return false;
        }
        ProcesosPlanta other = (ProcesosPlanta) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "ucb.edu.bo.sumajflow.entity.ProcesosPlanta[ id=" + id + " ]";
    }
    
}
