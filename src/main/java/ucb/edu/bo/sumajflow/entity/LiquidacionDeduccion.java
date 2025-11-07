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
@Table(name = "liquidacion_deduccion")
@XmlRootElement
@NamedQueries({
    @NamedQuery(name = "LiquidacionDeduccion.findAll", query = "SELECT l FROM LiquidacionDeduccion l"),
    @NamedQuery(name = "LiquidacionDeduccion.findById", query = "SELECT l FROM LiquidacionDeduccion l WHERE l.id = :id"),
    @NamedQuery(name = "LiquidacionDeduccion.findByConcepto", query = "SELECT l FROM LiquidacionDeduccion l WHERE l.concepto = :concepto"),
    @NamedQuery(name = "LiquidacionDeduccion.findByMonto", query = "SELECT l FROM LiquidacionDeduccion l WHERE l.monto = :monto"),
    @NamedQuery(name = "LiquidacionDeduccion.findByPorcentaje", query = "SELECT l FROM LiquidacionDeduccion l WHERE l.porcentaje = :porcentaje")})
public class LiquidacionDeduccion implements Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic(optional = false)
    @Column(name = "id")
    private Integer id;
    @Size(max = 100)
    @Column(name = "concepto")
    private String concepto;
    // @Max(value=?)  @Min(value=?)//if you know range of your decimal fields consider using these annotations to enforce field validation
    @Column(name = "monto")
    private BigDecimal monto;
    @Column(name = "porcentaje")
    private BigDecimal porcentaje;
    @JoinColumn(name = "liquidacion_id", referencedColumnName = "id")
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private Liquidacion liquidacionId;

    public LiquidacionDeduccion() {
    }

    public LiquidacionDeduccion(Integer id) {
        this.id = id;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getConcepto() {
        return concepto;
    }

    public void setConcepto(String concepto) {
        this.concepto = concepto;
    }

    public BigDecimal getMonto() {
        return monto;
    }

    public void setMonto(BigDecimal monto) {
        this.monto = monto;
    }

    public BigDecimal getPorcentaje() {
        return porcentaje;
    }

    public void setPorcentaje(BigDecimal porcentaje) {
        this.porcentaje = porcentaje;
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
        if (!(object instanceof LiquidacionDeduccion)) {
            return false;
        }
        LiquidacionDeduccion other = (LiquidacionDeduccion) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "ucb.edu.bo.sumajflow.entity.LiquidacionDeduccion[ id=" + id + " ]";
    }
    
}
