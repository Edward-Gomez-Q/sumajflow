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
import jakarta.validation.constraints.Size;
import jakarta.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 *
 * @author osval
 */
@Entity
@Table(name = "liquidacion_cotizacion")
@XmlRootElement
@NamedQueries({
    @NamedQuery(name = "LiquidacionCotizacion.findAll", query = "SELECT l FROM LiquidacionCotizacion l"),
    @NamedQuery(name = "LiquidacionCotizacion.findById", query = "SELECT l FROM LiquidacionCotizacion l WHERE l.id = :id"),
    @NamedQuery(name = "LiquidacionCotizacion.findByMineral", query = "SELECT l FROM LiquidacionCotizacion l WHERE l.mineral = :mineral"),
    @NamedQuery(name = "LiquidacionCotizacion.findByCotizacionUsd", query = "SELECT l FROM LiquidacionCotizacion l WHERE l.cotizacionUsd = :cotizacionUsd"),
    @NamedQuery(name = "LiquidacionCotizacion.findByUnidad", query = "SELECT l FROM LiquidacionCotizacion l WHERE l.unidad = :unidad")})
public class LiquidacionCotizacion implements Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic(optional = false)
    @Column(name = "id")
    private Integer id;
    @Size(max = 50)
    @Column(name = "mineral")
    private String mineral;
    // @Max(value=?)  @Min(value=?)//if you know range of your decimal fields consider using these annotations to enforce field validation
    @Column(name = "cotizacion_usd")
    private BigDecimal cotizacionUsd;
    @Size(max = 50)
    @Column(name = "unidad")
    private String unidad;
    @JoinColumn(name = "liquidacion_id", referencedColumnName = "id")
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private Liquidacion liquidacionId;

    public LiquidacionCotizacion() {
    }

    public LiquidacionCotizacion(Integer id) {
        this.id = id;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getMineral() {
        return mineral;
    }

    public void setMineral(String mineral) {
        this.mineral = mineral;
    }

    public BigDecimal getCotizacionUsd() {
        return cotizacionUsd;
    }

    public void setCotizacionUsd(BigDecimal cotizacionUsd) {
        this.cotizacionUsd = cotizacionUsd;
    }

    public String getUnidad() {
        return unidad;
    }

    public void setUnidad(String unidad) {
        this.unidad = unidad;
    }

    public Liquidacion getLiquidacionId() {
        return liquidacionId;
    }

    public void setLiquidacionId(Liquidacion liquidacionId) {
        this.liquidacionId = liquidacionId;
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
        if (!(object instanceof LiquidacionCotizacion)) {
            return false;
        }
        LiquidacionCotizacion other = (LiquidacionCotizacion) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "ucb.edu.bo.sumajflow.entity.LiquidacionCotizacion[ id=" + id + " ]";
    }
    
}
