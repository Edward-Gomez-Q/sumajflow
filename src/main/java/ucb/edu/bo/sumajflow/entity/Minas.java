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
import java.math.BigDecimal;
import java.util.List;

/**
 *
 * @author osval
 */
@Entity
@Table(name = "minas")
@XmlRootElement
@NamedQueries({
    @NamedQuery(name = "Minas.findAll", query = "SELECT m FROM Minas m"),
    @NamedQuery(name = "Minas.findById", query = "SELECT m FROM Minas m WHERE m.id = :id"),
    @NamedQuery(name = "Minas.findByNombre", query = "SELECT m FROM Minas m WHERE m.nombre = :nombre"),
    @NamedQuery(name = "Minas.findByFotoUrl", query = "SELECT m FROM Minas m WHERE m.fotoUrl = :fotoUrl"),
    @NamedQuery(name = "Minas.findByLatitud", query = "SELECT m FROM Minas m WHERE m.latitud = :latitud"),
    @NamedQuery(name = "Minas.findByLongitud", query = "SELECT m FROM Minas m WHERE m.longitud = :longitud")})
public class Minas implements Serializable {

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
    @Size(max = 200)
    @Column(name = "foto_url")
    private String fotoUrl;
    // @Max(value=?)  @Min(value=?)//if you know range of your decimal fields consider using these annotations to enforce field validation
    @Basic(optional = false)
    @NotNull
    @Column(name = "latitud")
    private BigDecimal latitud;
    @Basic(optional = false)
    @NotNull
    @Column(name = "longitud")
    private BigDecimal longitud;
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "minasId", fetch = FetchType.LAZY)
    private List<Lotes> lotesList;
    @JoinColumn(name = "sectores_id", referencedColumnName = "id")
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private Sectores sectoresId;
    @JoinColumn(name = "socio_id", referencedColumnName = "id")
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private Socio socioId;

    public Minas() {
    }

    public Minas(Integer id) {
        this.id = id;
    }

    public Minas(Integer id, String nombre, BigDecimal latitud, BigDecimal longitud) {
        this.id = id;
        this.nombre = nombre;
        this.latitud = latitud;
        this.longitud = longitud;
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

    public String getFotoUrl() {
        return fotoUrl;
    }

    public void setFotoUrl(String fotoUrl) {
        this.fotoUrl = fotoUrl;
    }

    public BigDecimal getLatitud() {
        return latitud;
    }

    public void setLatitud(BigDecimal latitud) {
        this.latitud = latitud;
    }

    public BigDecimal getLongitud() {
        return longitud;
    }

    public void setLongitud(BigDecimal longitud) {
        this.longitud = longitud;
    }

    @XmlTransient
    public List<Lotes> getLotesList() {
        return lotesList;
    }

    public void setLotesList(List<Lotes> lotesList) {
        this.lotesList = lotesList;
    }

    public Sectores getSectoresId() {
        return sectoresId;
    }

    public void setSectoresId(Sectores sectoresId) {
        this.sectoresId = sectoresId;
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
        if (!(object instanceof Minas)) {
            return false;
        }
        Minas other = (Minas) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "ucb.edu.bo.sumajflow.entity.Minas[ id=" + id + " ]";
    }
    
}
