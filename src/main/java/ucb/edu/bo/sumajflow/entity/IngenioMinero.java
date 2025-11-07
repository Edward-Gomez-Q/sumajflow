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
@Table(name = "ingenio_minero")
@XmlRootElement
@NamedQueries({
    @NamedQuery(name = "IngenioMinero.findAll", query = "SELECT i FROM IngenioMinero i"),
    @NamedQuery(name = "IngenioMinero.findById", query = "SELECT i FROM IngenioMinero i WHERE i.id = :id"),
    @NamedQuery(name = "IngenioMinero.findByRazonSocial", query = "SELECT i FROM IngenioMinero i WHERE i.razonSocial = :razonSocial"),
    @NamedQuery(name = "IngenioMinero.findByNit", query = "SELECT i FROM IngenioMinero i WHERE i.nit = :nit"),
    @NamedQuery(name = "IngenioMinero.findByNim", query = "SELECT i FROM IngenioMinero i WHERE i.nim = :nim"),
    @NamedQuery(name = "IngenioMinero.findByCorreoContacto", query = "SELECT i FROM IngenioMinero i WHERE i.correoContacto = :correoContacto"),
    @NamedQuery(name = "IngenioMinero.findByNumeroTelefonoFijo", query = "SELECT i FROM IngenioMinero i WHERE i.numeroTelefonoFijo = :numeroTelefonoFijo"),
    @NamedQuery(name = "IngenioMinero.findByNumeroTelefonoMovil", query = "SELECT i FROM IngenioMinero i WHERE i.numeroTelefonoMovil = :numeroTelefonoMovil"),
    @NamedQuery(name = "IngenioMinero.findByDepartamento", query = "SELECT i FROM IngenioMinero i WHERE i.departamento = :departamento"),
    @NamedQuery(name = "IngenioMinero.findByProvincia", query = "SELECT i FROM IngenioMinero i WHERE i.provincia = :provincia"),
    @NamedQuery(name = "IngenioMinero.findByMunicipio", query = "SELECT i FROM IngenioMinero i WHERE i.municipio = :municipio"),
    @NamedQuery(name = "IngenioMinero.findByDireccion", query = "SELECT i FROM IngenioMinero i WHERE i.direccion = :direccion"),
    @NamedQuery(name = "IngenioMinero.findByLatitud", query = "SELECT i FROM IngenioMinero i WHERE i.latitud = :latitud"),
    @NamedQuery(name = "IngenioMinero.findByLongitud", query = "SELECT i FROM IngenioMinero i WHERE i.longitud = :longitud")})
public class IngenioMinero implements Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic(optional = false)
    @Column(name = "id")
    private Integer id;
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 100)
    @Column(name = "razon_social")
    private String razonSocial;
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 50)
    @Column(name = "nit")
    private String nit;
    @Basic(optional = false)
    @NotNull
    @Column(name = "nim")
    private int nim;
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 50)
    @Column(name = "correo_contacto")
    private String correoContacto;
    @Size(max = 50)
    @Column(name = "numero_telefono_fijo")
    private String numeroTelefonoFijo;
    @Size(max = 50)
    @Column(name = "numero_telefono_movil")
    private String numeroTelefonoMovil;
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 50)
    @Column(name = "departamento")
    private String departamento;
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 100)
    @Column(name = "provincia")
    private String provincia;
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 100)
    @Column(name = "municipio")
    private String municipio;
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 250)
    @Column(name = "direccion")
    private String direccion;
    // @Max(value=?)  @Min(value=?)//if you know range of your decimal fields consider using these annotations to enforce field validation
    @Column(name = "latitud")
    private BigDecimal latitud;
    @Column(name = "longitud")
    private BigDecimal longitud;
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "ingenioMineroId", fetch = FetchType.LAZY)
    private List<Concentrado> concentradoList;
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "ingenioMineroId", fetch = FetchType.LAZY)
    private List<BalanzaIngenio> balanzaIngenioList;
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "ingenioMineroId", fetch = FetchType.LAZY)
    private List<LoteIngenio> loteIngenioList;
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "ingenioMineroId", fetch = FetchType.LAZY)
    private List<Planta> plantaList;
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "ingenioMineroId", fetch = FetchType.LAZY)
    private List<AlmacenIngenio> almacenIngenioList;
    @JoinColumn(name = "usuarios_id", referencedColumnName = "id")
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private Usuarios usuariosId;

    public IngenioMinero() {
    }

    public IngenioMinero(Integer id) {
        this.id = id;
    }

    public IngenioMinero(Integer id, String razonSocial, String nit, int nim, String correoContacto, String departamento, String provincia, String municipio, String direccion) {
        this.id = id;
        this.razonSocial = razonSocial;
        this.nit = nit;
        this.nim = nim;
        this.correoContacto = correoContacto;
        this.departamento = departamento;
        this.provincia = provincia;
        this.municipio = municipio;
        this.direccion = direccion;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getRazonSocial() {
        return razonSocial;
    }

    public void setRazonSocial(String razonSocial) {
        this.razonSocial = razonSocial;
    }

    public String getNit() {
        return nit;
    }

    public void setNit(String nit) {
        this.nit = nit;
    }

    public int getNim() {
        return nim;
    }

    public void setNim(int nim) {
        this.nim = nim;
    }

    public String getCorreoContacto() {
        return correoContacto;
    }

    public void setCorreoContacto(String correoContacto) {
        this.correoContacto = correoContacto;
    }

    public String getNumeroTelefonoFijo() {
        return numeroTelefonoFijo;
    }

    public void setNumeroTelefonoFijo(String numeroTelefonoFijo) {
        this.numeroTelefonoFijo = numeroTelefonoFijo;
    }

    public String getNumeroTelefonoMovil() {
        return numeroTelefonoMovil;
    }

    public void setNumeroTelefonoMovil(String numeroTelefonoMovil) {
        this.numeroTelefonoMovil = numeroTelefonoMovil;
    }

    public String getDepartamento() {
        return departamento;
    }

    public void setDepartamento(String departamento) {
        this.departamento = departamento;
    }

    public String getProvincia() {
        return provincia;
    }

    public void setProvincia(String provincia) {
        this.provincia = provincia;
    }

    public String getMunicipio() {
        return municipio;
    }

    public void setMunicipio(String municipio) {
        this.municipio = municipio;
    }

    public String getDireccion() {
        return direccion;
    }

    public void setDireccion(String direccion) {
        this.direccion = direccion;
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
    public List<Concentrado> getConcentradoList() {
        return concentradoList;
    }

    public void setConcentradoList(List<Concentrado> concentradoList) {
        this.concentradoList = concentradoList;
    }

    @XmlTransient
    public List<BalanzaIngenio> getBalanzaIngenioList() {
        return balanzaIngenioList;
    }

    public void setBalanzaIngenioList(List<BalanzaIngenio> balanzaIngenioList) {
        this.balanzaIngenioList = balanzaIngenioList;
    }

    @XmlTransient
    public List<LoteIngenio> getLoteIngenioList() {
        return loteIngenioList;
    }

    public void setLoteIngenioList(List<LoteIngenio> loteIngenioList) {
        this.loteIngenioList = loteIngenioList;
    }

    @XmlTransient
    public List<Planta> getPlantaList() {
        return plantaList;
    }

    public void setPlantaList(List<Planta> plantaList) {
        this.plantaList = plantaList;
    }

    @XmlTransient
    public List<AlmacenIngenio> getAlmacenIngenioList() {
        return almacenIngenioList;
    }

    public void setAlmacenIngenioList(List<AlmacenIngenio> almacenIngenioList) {
        this.almacenIngenioList = almacenIngenioList;
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
        if (!(object instanceof IngenioMinero)) {
            return false;
        }
        IngenioMinero other = (IngenioMinero) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "ucb.edu.bo.sumajflow.entity.IngenioMinero[ id=" + id + " ]";
    }
    
}
