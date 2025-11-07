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
import jakarta.validation.constraints.NotNull;
import jakarta.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 *
 * @author osval
 */
@Entity
@Table(name = "sectores_coordenadas")
@XmlRootElement
@NamedQueries({
    @NamedQuery(name = "SectoresCoordenadas.findAll", query = "SELECT s FROM SectoresCoordenadas s"),
    @NamedQuery(name = "SectoresCoordenadas.findById", query = "SELECT s FROM SectoresCoordenadas s WHERE s.id = :id"),
    @NamedQuery(name = "SectoresCoordenadas.findByOrden", query = "SELECT s FROM SectoresCoordenadas s WHERE s.orden = :orden"),
    @NamedQuery(name = "SectoresCoordenadas.findByLatitud", query = "SELECT s FROM SectoresCoordenadas s WHERE s.latitud = :latitud"),
    @NamedQuery(name = "SectoresCoordenadas.findByLongitud", query = "SELECT s FROM SectoresCoordenadas s WHERE s.longitud = :longitud")})
public class SectoresCoordenadas implements Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic(optional = false)
    @Column(name = "id")
    private Integer id;
    @Basic(optional = false)
    @NotNull
    @Column(name = "orden")
    private int orden;
    // @Max(value=?)  @Min(value=?)//if you know range of your decimal fields consider using these annotations to enforce field validation
    @Basic(optional = false)
    @NotNull
    @Column(name = "latitud")
    private BigDecimal latitud;
    @Basic(optional = false)
    @NotNull
    @Column(name = "longitud")
    private BigDecimal longitud;
    @JoinColumn(name = "sectores_id", referencedColumnName = "id")
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private Sectores sectoresId;

    public SectoresCoordenadas() {
    }

    public SectoresCoordenadas(Integer id) {
        this.id = id;
    }

    public SectoresCoordenadas(Integer id, int orden, BigDecimal latitud, BigDecimal longitud) {
        this.id = id;
        this.orden = orden;
        this.latitud = latitud;
        this.longitud = longitud;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public int getOrden() {
        return orden;
    }

    public void setOrden(int orden) {
        this.orden = orden;
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

    public Sectores getSectoresId() {
        return sectoresId;
    }

    public void setSectoresId(Sectores sectoresId) {
        this.sectoresId = sectoresId;
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
        if (!(object instanceof SectoresCoordenadas)) {
            return false;
        }
        SectoresCoordenadas other = (SectoresCoordenadas) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "ucb.edu.bo.sumajflow.entity.SectoresCoordenadas[ id=" + id + " ]";
    }
    
}
