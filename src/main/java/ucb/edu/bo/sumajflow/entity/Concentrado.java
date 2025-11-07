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
@Table(name = "concentrado")
@XmlRootElement
@NamedQueries({
    @NamedQuery(name = "Concentrado.findAll", query = "SELECT c FROM Concentrado c"),
    @NamedQuery(name = "Concentrado.findById", query = "SELECT c FROM Concentrado c WHERE c.id = :id"),
    @NamedQuery(name = "Concentrado.findByCodigoConcentrado", query = "SELECT c FROM Concentrado c WHERE c.codigoConcentrado = :codigoConcentrado"),
    @NamedQuery(name = "Concentrado.findByPesoInicial", query = "SELECT c FROM Concentrado c WHERE c.pesoInicial = :pesoInicial"),
    @NamedQuery(name = "Concentrado.findByPesoFinal", query = "SELECT c FROM Concentrado c WHERE c.pesoFinal = :pesoFinal"),
    @NamedQuery(name = "Concentrado.findByMerma", query = "SELECT c FROM Concentrado c WHERE c.merma = :merma"),
    @NamedQuery(name = "Concentrado.findByMineralPrincipal", query = "SELECT c FROM Concentrado c WHERE c.mineralPrincipal = :mineralPrincipal"),
    @NamedQuery(name = "Concentrado.findByEstado", query = "SELECT c FROM Concentrado c WHERE c.estado = :estado"),
    @NamedQuery(name = "Concentrado.findByFechaInicio", query = "SELECT c FROM Concentrado c WHERE c.fechaInicio = :fechaInicio"),
    @NamedQuery(name = "Concentrado.findByFechaFin", query = "SELECT c FROM Concentrado c WHERE c.fechaFin = :fechaFin")})
public class Concentrado implements Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic(optional = false)
    @Column(name = "id")
    private Integer id;
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 50)
    @Column(name = "codigo_concentrado")
    private String codigoConcentrado;
    // @Max(value=?)  @Min(value=?)//if you know range of your decimal fields consider using these annotations to enforce field validation
    @Basic(optional = false)
    @NotNull
    @Column(name = "peso_inicial")
    private BigDecimal pesoInicial;
    @Column(name = "peso_final")
    private BigDecimal pesoFinal;
    @Column(name = "merma")
    private BigDecimal merma;
    @Size(max = 50)
    @Column(name = "mineral_principal")
    private String mineralPrincipal;
    @Size(max = 50)
    @Column(name = "estado")
    private String estado;
    @Column(name = "fecha_inicio")
    @Temporal(TemporalType.TIMESTAMP)
    private Date fechaInicio;
    @Column(name = "fecha_fin")
    @Temporal(TemporalType.TIMESTAMP)
    private Date fechaFin;
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "concentradoId", fetch = FetchType.LAZY)
    private List<ReporteQuimico> reporteQuimicoList;
    @JoinColumn(name = "ingenio_minero_id", referencedColumnName = "id")
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private IngenioMinero ingenioMineroId;
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "concentradoId", fetch = FetchType.LAZY)
    private List<Liquidacion> liquidacionList;

    public Concentrado() {
    }

    public Concentrado(Integer id) {
        this.id = id;
    }

    public Concentrado(Integer id, String codigoConcentrado, BigDecimal pesoInicial) {
        this.id = id;
        this.codigoConcentrado = codigoConcentrado;
        this.pesoInicial = pesoInicial;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getCodigoConcentrado() {
        return codigoConcentrado;
    }

    public void setCodigoConcentrado(String codigoConcentrado) {
        this.codigoConcentrado = codigoConcentrado;
    }

    public BigDecimal getPesoInicial() {
        return pesoInicial;
    }

    public void setPesoInicial(BigDecimal pesoInicial) {
        this.pesoInicial = pesoInicial;
    }

    public BigDecimal getPesoFinal() {
        return pesoFinal;
    }

    public void setPesoFinal(BigDecimal pesoFinal) {
        this.pesoFinal = pesoFinal;
    }

    public BigDecimal getMerma() {
        return merma;
    }

    public void setMerma(BigDecimal merma) {
        this.merma = merma;
    }

    public String getMineralPrincipal() {
        return mineralPrincipal;
    }

    public void setMineralPrincipal(String mineralPrincipal) {
        this.mineralPrincipal = mineralPrincipal;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public Date getFechaInicio() {
        return fechaInicio;
    }

    public void setFechaInicio(Date fechaInicio) {
        this.fechaInicio = fechaInicio;
    }

    public Date getFechaFin() {
        return fechaFin;
    }

    public void setFechaFin(Date fechaFin) {
        this.fechaFin = fechaFin;
    }

    @XmlTransient
    public List<ReporteQuimico> getReporteQuimicoList() {
        return reporteQuimicoList;
    }

    public void setReporteQuimicoList(List<ReporteQuimico> reporteQuimicoList) {
        this.reporteQuimicoList = reporteQuimicoList;
    }

    public IngenioMinero getIngenioMineroId() {
        return ingenioMineroId;
    }

    public void setIngenioMineroId(IngenioMinero ingenioMineroId) {
        this.ingenioMineroId = ingenioMineroId;
    }

    @XmlTransient
    public List<Liquidacion> getLiquidacionList() {
        return liquidacionList;
    }

    public void setLiquidacionList(List<Liquidacion> liquidacionList) {
        this.liquidacionList = liquidacionList;
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
        if (!(object instanceof Concentrado)) {
            return false;
        }
        Concentrado other = (Concentrado) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "ucb.edu.bo.sumajflow.entity.Concentrado[ id=" + id + " ]";
    }
    
}
