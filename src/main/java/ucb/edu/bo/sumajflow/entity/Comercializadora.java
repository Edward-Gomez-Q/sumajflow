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
@Table(name = "comercializadora")
@XmlRootElement
@NamedQueries({
    @NamedQuery(name = "Comercializadora.findAll", query = "SELECT c FROM Comercializadora c"),
    @NamedQuery(name = "Comercializadora.findById", query = "SELECT c FROM Comercializadora c WHERE c.id = :id"),
    @NamedQuery(name = "Comercializadora.findByRazonSocial", query = "SELECT c FROM Comercializadora c WHERE c.razonSocial = :razonSocial"),
    @NamedQuery(name = "Comercializadora.findByNit", query = "SELECT c FROM Comercializadora c WHERE c.nit = :nit"),
    @NamedQuery(name = "Comercializadora.findByNim", query = "SELECT c FROM Comercializadora c WHERE c.nim = :nim"),
    @NamedQuery(name = "Comercializadora.findByCorreoContacto", query = "SELECT c FROM Comercializadora c WHERE c.correoContacto = :correoContacto"),
    @NamedQuery(name = "Comercializadora.findByNumeroTelefonoFijo", query = "SELECT c FROM Comercializadora c WHERE c.numeroTelefonoFijo = :numeroTelefonoFijo"),
    @NamedQuery(name = "Comercializadora.findByNumeroTelefonoMovil", query = "SELECT c FROM Comercializadora c WHERE c.numeroTelefonoMovil = :numeroTelefonoMovil"),
    @NamedQuery(name = "Comercializadora.findByDepartamento", query = "SELECT c FROM Comercializadora c WHERE c.departamento = :departamento"),
    @NamedQuery(name = "Comercializadora.findByProvincia", query = "SELECT c FROM Comercializadora c WHERE c.provincia = :provincia"),
    @NamedQuery(name = "Comercializadora.findByMunicipio", query = "SELECT c FROM Comercializadora c WHERE c.municipio = :municipio"),
    @NamedQuery(name = "Comercializadora.findByDireccion", query = "SELECT c FROM Comercializadora c WHERE c.direccion = :direccion"),
    @NamedQuery(name = "Comercializadora.findByLatitud", query = "SELECT c FROM Comercializadora c WHERE c.latitud = :latitud"),
    @NamedQuery(name = "Comercializadora.findByLongitud", query = "SELECT c FROM Comercializadora c WHERE c.longitud = :longitud")})
public class Comercializadora implements Serializable {

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
    @JoinColumn(name = "usuarios_id", referencedColumnName = "id")
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private Usuarios usuariosId;
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "comercializadoraId", fetch = FetchType.LAZY)
    private List<AlmacenComercializadora> almacenComercializadoraList;
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "comercializadoraId", fetch = FetchType.LAZY)
    private List<BalanzaComercializadora> balanzaComercializadoraList;
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "comercializadoraId", fetch = FetchType.LAZY)
    private List<LoteComercializadora> loteComercializadoraList;

    public Comercializadora() {
    }

    public Comercializadora(Integer id) {
        this.id = id;
    }

    public Comercializadora(Integer id, String razonSocial, String nit, int nim, String correoContacto, String departamento, String provincia, String municipio, String direccion) {
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

    public Usuarios getUsuariosId() {
        return usuariosId;
    }

    public void setUsuariosId(Usuarios usuariosId) {
        this.usuariosId = usuariosId;
    }

    @XmlTransient
    public List<AlmacenComercializadora> getAlmacenComercializadoraList() {
        return almacenComercializadoraList;
    }

    public void setAlmacenComercializadoraList(List<AlmacenComercializadora> almacenComercializadoraList) {
        this.almacenComercializadoraList = almacenComercializadoraList;
    }

    @XmlTransient
    public List<BalanzaComercializadora> getBalanzaComercializadoraList() {
        return balanzaComercializadoraList;
    }

    public void setBalanzaComercializadoraList(List<BalanzaComercializadora> balanzaComercializadoraList) {
        this.balanzaComercializadoraList = balanzaComercializadoraList;
    }

    @XmlTransient
    public List<LoteComercializadora> getLoteComercializadoraList() {
        return loteComercializadoraList;
    }

    public void setLoteComercializadoraList(List<LoteComercializadora> loteComercializadoraList) {
        this.loteComercializadoraList = loteComercializadoraList;
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
        if (!(object instanceof Comercializadora)) {
            return false;
        }
        Comercializadora other = (Comercializadora) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "ucb.edu.bo.sumajflow.entity.Comercializadora[ id=" + id + " ]";
    }
    
}
