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
@Table(name = "transportista")
@XmlRootElement
@NamedQueries({
    @NamedQuery(name = "Transportista.findAll", query = "SELECT t FROM Transportista t"),
    @NamedQuery(name = "Transportista.findById", query = "SELECT t FROM Transportista t WHERE t.id = :id"),
    @NamedQuery(name = "Transportista.findByCi", query = "SELECT t FROM Transportista t WHERE t.ci = :ci"),
    @NamedQuery(name = "Transportista.findByLicenciaConducir", query = "SELECT t FROM Transportista t WHERE t.licenciaConducir = :licenciaConducir"),
    @NamedQuery(name = "Transportista.findByCategoriaLicencia", query = "SELECT t FROM Transportista t WHERE t.categoriaLicencia = :categoriaLicencia"),
    @NamedQuery(name = "Transportista.findByFechaVencimientoLicencia", query = "SELECT t FROM Transportista t WHERE t.fechaVencimientoLicencia = :fechaVencimientoLicencia"),
    @NamedQuery(name = "Transportista.findByPlacaVehiculo", query = "SELECT t FROM Transportista t WHERE t.placaVehiculo = :placaVehiculo"),
    @NamedQuery(name = "Transportista.findByMarcaVehiculo", query = "SELECT t FROM Transportista t WHERE t.marcaVehiculo = :marcaVehiculo"),
    @NamedQuery(name = "Transportista.findByModeloVehiculo", query = "SELECT t FROM Transportista t WHERE t.modeloVehiculo = :modeloVehiculo"),
    @NamedQuery(name = "Transportista.findByColorVehiculo", query = "SELECT t FROM Transportista t WHERE t.colorVehiculo = :colorVehiculo"),
    @NamedQuery(name = "Transportista.findByPesoTara", query = "SELECT t FROM Transportista t WHERE t.pesoTara = :pesoTara"),
    @NamedQuery(name = "Transportista.findByCapacidadCarga", query = "SELECT t FROM Transportista t WHERE t.capacidadCarga = :capacidadCarga"),
    @NamedQuery(name = "Transportista.findByEstado", query = "SELECT t FROM Transportista t WHERE t.estado = :estado"),
    @NamedQuery(name = "Transportista.findByFechaAprobacion", query = "SELECT t FROM Transportista t WHERE t.fechaAprobacion = :fechaAprobacion"),
    @NamedQuery(name = "Transportista.findByViajesCompletados", query = "SELECT t FROM Transportista t WHERE t.viajesCompletados = :viajesCompletados"),
    @NamedQuery(name = "Transportista.findByCalificacionPromedio", query = "SELECT t FROM Transportista t WHERE t.calificacionPromedio = :calificacionPromedio")})
public class Transportista implements Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic(optional = false)
    @Column(name = "id")
    private Integer id;
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 20)
    @Column(name = "ci")
    private String ci;
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 50)
    @Column(name = "licencia_conducir")
    private String licenciaConducir;
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 10)
    @Column(name = "categoria_licencia")
    private String categoriaLicencia;
    @Basic(optional = false)
    @NotNull
    @Column(name = "fecha_vencimiento_licencia")
    @Temporal(TemporalType.DATE)
    private Date fechaVencimientoLicencia;
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 20)
    @Column(name = "placa_vehiculo")
    private String placaVehiculo;
    @Size(max = 50)
    @Column(name = "marca_vehiculo")
    private String marcaVehiculo;
    @Size(max = 50)
    @Column(name = "modelo_vehiculo")
    private String modeloVehiculo;
    @Size(max = 30)
    @Column(name = "color_vehiculo")
    private String colorVehiculo;
    // @Max(value=?)  @Min(value=?)//if you know range of your decimal fields consider using these annotations to enforce field validation
    @Column(name = "peso_tara")
    private BigDecimal pesoTara;
    @Column(name = "capacidad_carga")
    private BigDecimal capacidadCarga;
    @Size(max = 50)
    @Column(name = "estado")
    private String estado;
    @Column(name = "fecha_aprobacion")
    @Temporal(TemporalType.TIMESTAMP)
    private Date fechaAprobacion;
    @Column(name = "viajes_completados")
    private Integer viajesCompletados;
    @Column(name = "calificacion_promedio")
    private BigDecimal calificacionPromedio;
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "transportistaId", fetch = FetchType.LAZY)
    private List<AsignacionCamion> asignacionCamionList;
    @JoinColumn(name = "usuarios_id", referencedColumnName = "id")
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private Usuarios usuariosId;

    public Transportista() {
    }

    public Transportista(Integer id) {
        this.id = id;
    }

    public Transportista(Integer id, String ci, String licenciaConducir, String categoriaLicencia, Date fechaVencimientoLicencia, String placaVehiculo) {
        this.id = id;
        this.ci = ci;
        this.licenciaConducir = licenciaConducir;
        this.categoriaLicencia = categoriaLicencia;
        this.fechaVencimientoLicencia = fechaVencimientoLicencia;
        this.placaVehiculo = placaVehiculo;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getCi() {
        return ci;
    }

    public void setCi(String ci) {
        this.ci = ci;
    }

    public String getLicenciaConducir() {
        return licenciaConducir;
    }

    public void setLicenciaConducir(String licenciaConducir) {
        this.licenciaConducir = licenciaConducir;
    }

    public String getCategoriaLicencia() {
        return categoriaLicencia;
    }

    public void setCategoriaLicencia(String categoriaLicencia) {
        this.categoriaLicencia = categoriaLicencia;
    }

    public Date getFechaVencimientoLicencia() {
        return fechaVencimientoLicencia;
    }

    public void setFechaVencimientoLicencia(Date fechaVencimientoLicencia) {
        this.fechaVencimientoLicencia = fechaVencimientoLicencia;
    }

    public String getPlacaVehiculo() {
        return placaVehiculo;
    }

    public void setPlacaVehiculo(String placaVehiculo) {
        this.placaVehiculo = placaVehiculo;
    }

    public String getMarcaVehiculo() {
        return marcaVehiculo;
    }

    public void setMarcaVehiculo(String marcaVehiculo) {
        this.marcaVehiculo = marcaVehiculo;
    }

    public String getModeloVehiculo() {
        return modeloVehiculo;
    }

    public void setModeloVehiculo(String modeloVehiculo) {
        this.modeloVehiculo = modeloVehiculo;
    }

    public String getColorVehiculo() {
        return colorVehiculo;
    }

    public void setColorVehiculo(String colorVehiculo) {
        this.colorVehiculo = colorVehiculo;
    }

    public BigDecimal getPesoTara() {
        return pesoTara;
    }

    public void setPesoTara(BigDecimal pesoTara) {
        this.pesoTara = pesoTara;
    }

    public BigDecimal getCapacidadCarga() {
        return capacidadCarga;
    }

    public void setCapacidadCarga(BigDecimal capacidadCarga) {
        this.capacidadCarga = capacidadCarga;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public Date getFechaAprobacion() {
        return fechaAprobacion;
    }

    public void setFechaAprobacion(Date fechaAprobacion) {
        this.fechaAprobacion = fechaAprobacion;
    }

    public Integer getViajesCompletados() {
        return viajesCompletados;
    }

    public void setViajesCompletados(Integer viajesCompletados) {
        this.viajesCompletados = viajesCompletados;
    }

    public BigDecimal getCalificacionPromedio() {
        return calificacionPromedio;
    }

    public void setCalificacionPromedio(BigDecimal calificacionPromedio) {
        this.calificacionPromedio = calificacionPromedio;
    }

    @XmlTransient
    public List<AsignacionCamion> getAsignacionCamionList() {
        return asignacionCamionList;
    }

    public void setAsignacionCamionList(List<AsignacionCamion> asignacionCamionList) {
        this.asignacionCamionList = asignacionCamionList;
    }

    public Usuarios getUsuariosId() {
        return usuariosId;
    }

    public void setUsuariosId(Usuarios usuariosId) {
        this.usuariosId = usuariosId;
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
        if (!(object instanceof Transportista)) {
            return false;
        }
        Transportista other = (Transportista) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "ucb.edu.bo.sumajflow.entity.Transportista[ id=" + id + " ]";
    }
    
}
