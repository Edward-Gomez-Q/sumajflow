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
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlTransient;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

/**
 *
 * @author osval
 */
@Entity
@Table(name = "liquidacion")
@XmlRootElement
@NamedQueries({
    @NamedQuery(name = "Liquidacion.findAll", query = "SELECT l FROM Liquidacion l"),
    @NamedQuery(name = "Liquidacion.findById", query = "SELECT l FROM Liquidacion l WHERE l.id = :id"),
    @NamedQuery(name = "Liquidacion.findByCodigoLiquidacion", query = "SELECT l FROM Liquidacion l WHERE l.codigoLiquidacion = :codigoLiquidacion"),
    @NamedQuery(name = "Liquidacion.findByTipoLiquidacion", query = "SELECT l FROM Liquidacion l WHERE l.tipoLiquidacion = :tipoLiquidacion"),
    @NamedQuery(name = "Liquidacion.findByFechaLiquidacion", query = "SELECT l FROM Liquidacion l WHERE l.fechaLiquidacion = :fechaLiquidacion"),
    @NamedQuery(name = "Liquidacion.findByMoneda", query = "SELECT l FROM Liquidacion l WHERE l.moneda = :moneda"),
    @NamedQuery(name = "Liquidacion.findByPesoLiquidado", query = "SELECT l FROM Liquidacion l WHERE l.pesoLiquidado = :pesoLiquidado"),
    @NamedQuery(name = "Liquidacion.findByValorBruto", query = "SELECT l FROM Liquidacion l WHERE l.valorBruto = :valorBruto"),
    @NamedQuery(name = "Liquidacion.findByDeducciones", query = "SELECT l FROM Liquidacion l WHERE l.deducciones = :deducciones"),
    @NamedQuery(name = "Liquidacion.findByValorNeto", query = "SELECT l FROM Liquidacion l WHERE l.valorNeto = :valorNeto"),
    @NamedQuery(name = "Liquidacion.findByEstado", query = "SELECT l FROM Liquidacion l WHERE l.estado = :estado")})
public class Liquidacion implements Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic(optional = false)
    @Column(name = "id")
    private Integer id;
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 50)
    @Column(name = "codigo_liquidacion")
    private String codigoLiquidacion;
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 50)
    @Column(name = "tipo_liquidacion")
    private String tipoLiquidacion;
    @Basic(optional = false)
    @NotNull
    @Column(name = "fecha_liquidacion")
    @Temporal(TemporalType.DATE)
    private Date fechaLiquidacion;
    @Size(max = 10)
    @Column(name = "moneda")
    private String moneda;
    // @Max(value=?)  @Min(value=?)//if you know range of your decimal fields consider using these annotations to enforce field validation
    @Column(name = "peso_liquidado")
    private BigDecimal pesoLiquidado;
    @Column(name = "valor_bruto")
    private BigDecimal valorBruto;
    @Column(name = "deducciones")
    private BigDecimal deducciones;
    @Column(name = "valor_neto")
    private BigDecimal valorNeto;
    @Size(max = 50)
    @Column(name = "estado")
    private String estado;
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "liquidacionId", fetch = FetchType.LAZY)
    private List<LiquidacionDeduccion> liquidacionDeduccionList;
    @JoinColumn(name = "concentrado_id", referencedColumnName = "id")
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private Concentrado concentradoId;
    @JoinColumn(name = "socio_id", referencedColumnName = "id")
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private Socio socioId;
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "liquidacionId", fetch = FetchType.LAZY)
    private List<LiquidacionCotizacion> liquidacionCotizacionList;

    public Liquidacion() {
    }

    public Liquidacion(Integer id) {
        this.id = id;
    }

    public Liquidacion(Integer id, String codigoLiquidacion, String tipoLiquidacion, Date fechaLiquidacion) {
        this.id = id;
        this.codigoLiquidacion = codigoLiquidacion;
        this.tipoLiquidacion = tipoLiquidacion;
        this.fechaLiquidacion = fechaLiquidacion;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getCodigoLiquidacion() {
        return codigoLiquidacion;
    }

    public void setCodigoLiquidacion(String codigoLiquidacion) {
        this.codigoLiquidacion = codigoLiquidacion;
    }

    public String getTipoLiquidacion() {
        return tipoLiquidacion;
    }

    public void setTipoLiquidacion(String tipoLiquidacion) {
        this.tipoLiquidacion = tipoLiquidacion;
    }

    public Date getFechaLiquidacion() {
        return fechaLiquidacion;
    }

    public void setFechaLiquidacion(Date fechaLiquidacion) {
        this.fechaLiquidacion = fechaLiquidacion;
    }

    public String getMoneda() {
        return moneda;
    }

    public void setMoneda(String moneda) {
        this.moneda = moneda;
    }

    public BigDecimal getPesoLiquidado() {
        return pesoLiquidado;
    }

    public void setPesoLiquidado(BigDecimal pesoLiquidado) {
        this.pesoLiquidado = pesoLiquidado;
    }

    public BigDecimal getValorBruto() {
        return valorBruto;
    }

    public void setValorBruto(BigDecimal valorBruto) {
        this.valorBruto = valorBruto;
    }

    public BigDecimal getDeducciones() {
        return deducciones;
    }

    public void setDeducciones(BigDecimal deducciones) {
        this.deducciones = deducciones;
    }

    public BigDecimal getValorNeto() {
        return valorNeto;
    }

    public void setValorNeto(BigDecimal valorNeto) {
        this.valorNeto = valorNeto;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    @XmlTransient
    public List<LiquidacionDeduccion> getLiquidacionDeduccionList() {
        return liquidacionDeduccionList;
    }

    public void setLiquidacionDeduccionList(List<LiquidacionDeduccion> liquidacionDeduccionList) {
        this.liquidacionDeduccionList = liquidacionDeduccionList;
    }

    public Concentrado getConcentradoId() {
        return concentradoId;
    }

    public void setConcentradoId(Concentrado concentradoId) {
        this.concentradoId = concentradoId;
    }

    public Socio getSocioId() {
        return socioId;
    }

    public void setSocioId(Socio socioId) {
        this.socioId = socioId;
    }

    @XmlTransient
    public List<LiquidacionCotizacion> getLiquidacionCotizacionList() {
        return liquidacionCotizacionList;
    }

    public void setLiquidacionCotizacionList(List<LiquidacionCotizacion> liquidacionCotizacionList) {
        this.liquidacionCotizacionList = liquidacionCotizacionList;
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
        if (!(object instanceof Liquidacion)) {
            return false;
        }
        Liquidacion other = (Liquidacion) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "ucb.edu.bo.sumajflow.entity.Liquidacion[ id=" + id + " ]";
    }
    
}
