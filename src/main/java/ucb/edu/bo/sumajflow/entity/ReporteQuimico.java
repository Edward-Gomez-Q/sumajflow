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
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 *
 * @author osval
 */
@Entity
@Table(name = "reporte_quimico")
@XmlRootElement
@NamedQueries({
    @NamedQuery(name = "ReporteQuimico.findAll", query = "SELECT r FROM ReporteQuimico r"),
    @NamedQuery(name = "ReporteQuimico.findById", query = "SELECT r FROM ReporteQuimico r WHERE r.id = :id"),
    @NamedQuery(name = "ReporteQuimico.findByNumeroReporte", query = "SELECT r FROM ReporteQuimico r WHERE r.numeroReporte = :numeroReporte"),
    @NamedQuery(name = "ReporteQuimico.findByLaboratorio", query = "SELECT r FROM ReporteQuimico r WHERE r.laboratorio = :laboratorio"),
    @NamedQuery(name = "ReporteQuimico.findByFechaAnalisis", query = "SELECT r FROM ReporteQuimico r WHERE r.fechaAnalisis = :fechaAnalisis"),
    @NamedQuery(name = "ReporteQuimico.findByLeyAg", query = "SELECT r FROM ReporteQuimico r WHERE r.leyAg = :leyAg"),
    @NamedQuery(name = "ReporteQuimico.findByLeyPb", query = "SELECT r FROM ReporteQuimico r WHERE r.leyPb = :leyPb"),
    @NamedQuery(name = "ReporteQuimico.findByLeyZn", query = "SELECT r FROM ReporteQuimico r WHERE r.leyZn = :leyZn"),
    @NamedQuery(name = "ReporteQuimico.findByUrlPdf", query = "SELECT r FROM ReporteQuimico r WHERE r.urlPdf = :urlPdf")})
public class ReporteQuimico implements Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic(optional = false)
    @Column(name = "id")
    private Integer id;
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 100)
    @Column(name = "numero_reporte")
    private String numeroReporte;
    @Size(max = 100)
    @Column(name = "laboratorio")
    private String laboratorio;
    @Basic(optional = false)
    @NotNull
    @Column(name = "fecha_analisis")
    @Temporal(TemporalType.DATE)
    private Date fechaAnalisis;
    // @Max(value=?)  @Min(value=?)//if you know range of your decimal fields consider using these annotations to enforce field validation
    @Column(name = "ley_ag")
    private BigDecimal leyAg;
    @Column(name = "ley_pb")
    private BigDecimal leyPb;
    @Column(name = "ley_zn")
    private BigDecimal leyZn;
    @Size(max = 200)
    @Column(name = "url_pdf")
    private String urlPdf;
    @JoinColumn(name = "concentrado_id", referencedColumnName = "id")
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private Concentrado concentradoId;

    public ReporteQuimico() {
    }

    public ReporteQuimico(Integer id) {
        this.id = id;
    }

    public ReporteQuimico(Integer id, String numeroReporte, Date fechaAnalisis) {
        this.id = id;
        this.numeroReporte = numeroReporte;
        this.fechaAnalisis = fechaAnalisis;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getNumeroReporte() {
        return numeroReporte;
    }

    public void setNumeroReporte(String numeroReporte) {
        this.numeroReporte = numeroReporte;
    }

    public String getLaboratorio() {
        return laboratorio;
    }

    public void setLaboratorio(String laboratorio) {
        this.laboratorio = laboratorio;
    }

    public Date getFechaAnalisis() {
        return fechaAnalisis;
    }

    public void setFechaAnalisis(Date fechaAnalisis) {
        this.fechaAnalisis = fechaAnalisis;
    }

    public BigDecimal getLeyAg() {
        return leyAg;
    }

    public void setLeyAg(BigDecimal leyAg) {
        this.leyAg = leyAg;
    }

    public BigDecimal getLeyPb() {
        return leyPb;
    }

    public void setLeyPb(BigDecimal leyPb) {
        this.leyPb = leyPb;
    }

    public BigDecimal getLeyZn() {
        return leyZn;
    }

    public void setLeyZn(BigDecimal leyZn) {
        this.leyZn = leyZn;
    }

    public String getUrlPdf() {
        return urlPdf;
    }

    public void setUrlPdf(String urlPdf) {
        this.urlPdf = urlPdf;
    }

    public Concentrado getConcentradoId() {
        return concentradoId;
    }

    public void setConcentradoId(Concentrado concentradoId) {
        this.concentradoId = concentradoId;
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
        if (!(object instanceof ReporteQuimico)) {
            return false;
        }
        ReporteQuimico other = (ReporteQuimico) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "ucb.edu.bo.sumajflow.entity.ReporteQuimico[ id=" + id + " ]";
    }
    
}
