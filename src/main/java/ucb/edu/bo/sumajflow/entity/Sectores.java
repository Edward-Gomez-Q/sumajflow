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
@Table(name = "sectores")
@XmlRootElement
@NamedQueries({
    @NamedQuery(name = "Sectores.findAll", query = "SELECT s FROM Sectores s"),
    @NamedQuery(name = "Sectores.findById", query = "SELECT s FROM Sectores s WHERE s.id = :id"),
    @NamedQuery(name = "Sectores.findByNombre", query = "SELECT s FROM Sectores s WHERE s.nombre = :nombre"),
    @NamedQuery(name = "Sectores.findByColor", query = "SELECT s FROM Sectores s WHERE s.color = :color")})
public class Sectores implements Serializable {

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
    @Size(max = 10)
    @Column(name = "color")
    private String color;
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 50)
    @Column(name = "estado")
    private String estado;
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "sectoresId", fetch = FetchType.LAZY)
    private List<SectoresCoordenadas> sectoresCoordenadasList;
    @JoinColumn(name = "cooperativa_id", referencedColumnName = "id")
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private Cooperativa cooperativaId;
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "sectoresId", fetch = FetchType.LAZY)
    private List<Minas> minasList;

    public Sectores() {
    }

    public Sectores(Integer id) {
        this.id = id;
    }

    public Sectores(Integer id, String nombre) {
        this.id = id;
        this.nombre = nombre;
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

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }
    public String getEstado() {
        return estado;
    }
    public void setEstado(String estado) {
        this.estado = estado;
    }

    @XmlTransient
    public List<SectoresCoordenadas> getSectoresCoordenadasList() {
        return sectoresCoordenadasList;
    }

    public void setSectoresCoordenadasList(List<SectoresCoordenadas> sectoresCoordenadasList) {
        this.sectoresCoordenadasList = sectoresCoordenadasList;
    }

    public Cooperativa getCooperativaId() {
        return cooperativaId;
    }

    public void setCooperativaId(Cooperativa cooperativaId) {
        this.cooperativaId = cooperativaId;
    }

    @XmlTransient
    public List<Minas> getMinasList() {
        return minasList;
    }

    public void setMinasList(List<Minas> minasList) {
        this.minasList = minasList;
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
        if (!(object instanceof Sectores)) {
            return false;
        }
        Sectores other = (Sectores) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "ucb.edu.bo.sumajflow.entity.Sectores[ id=" + id + " ]";
    }
    
}
