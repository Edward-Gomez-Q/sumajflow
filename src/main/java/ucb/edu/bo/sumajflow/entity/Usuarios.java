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
import java.util.List;

/**
 *
 * @author osval
 */
@Entity
@Table(name = "usuarios")
@XmlRootElement
@NamedQueries({
    @NamedQuery(name = "Usuarios.findAll", query = "SELECT u FROM Usuarios u"),
    @NamedQuery(name = "Usuarios.findById", query = "SELECT u FROM Usuarios u WHERE u.id = :id"),
    @NamedQuery(name = "Usuarios.findByCorreo", query = "SELECT u FROM Usuarios u WHERE u.correo = :correo"),
    @NamedQuery(name = "Usuarios.findByContrasena", query = "SELECT u FROM Usuarios u WHERE u.contrasena = :contrasena")})
public class Usuarios implements Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic(optional = false)
    @Column(name = "id")
    private Integer id;
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 50)
    @Column(name = "correo")
    private String correo;
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 250)
    @Column(name = "contrasena")
    private String contrasena;
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "usuariosId", fetch = FetchType.LAZY)
    private List<Comercializadora> comercializadoraList;
    @JoinColumn(name = "tipo_usuario_id", referencedColumnName = "id")
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private TipoUsuario tipoUsuarioId;
    @OneToMany(mappedBy = "usuariosId", fetch = FetchType.LAZY)
    private List<Auditoria> auditoriaList;
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "usuariosId", fetch = FetchType.LAZY)
    private List<Transportista> transportistaList;
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "usuariosId", fetch = FetchType.LAZY)
    private List<Cooperativa> cooperativaList;
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "usuariosId", fetch = FetchType.LAZY)
    private List<Persona> personaList;
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "usuariosId", fetch = FetchType.LAZY)
    private List<Socio> socioList;
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "usuariosId", fetch = FetchType.LAZY)
    private List<IngenioMinero> ingenioMineroList;

    public Usuarios() {
    }

    public Usuarios(Integer id) {
        this.id = id;
    }

    public Usuarios(Integer id, String correo, String contrasena) {
        this.id = id;
        this.correo = correo;
        this.contrasena = contrasena;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getCorreo() {
        return correo;
    }

    public void setCorreo(String correo) {
        this.correo = correo;
    }

    public String getContrasena() {
        return contrasena;
    }

    public void setContrasena(String contrasena) {
        this.contrasena = contrasena;
    }

    @XmlTransient
    public List<Comercializadora> getComercializadoraList() {
        return comercializadoraList;
    }

    public void setComercializadoraList(List<Comercializadora> comercializadoraList) {
        this.comercializadoraList = comercializadoraList;
    }

    public TipoUsuario getTipoUsuarioId() {
        return tipoUsuarioId;
    }

    public void setTipoUsuarioId(TipoUsuario tipoUsuarioId) {
        this.tipoUsuarioId = tipoUsuarioId;
    }

    @XmlTransient
    public List<Auditoria> getAuditoriaList() {
        return auditoriaList;
    }

    public void setAuditoriaList(List<Auditoria> auditoriaList) {
        this.auditoriaList = auditoriaList;
    }

    @XmlTransient
    public List<Transportista> getTransportistaList() {
        return transportistaList;
    }

    public void setTransportistaList(List<Transportista> transportistaList) {
        this.transportistaList = transportistaList;
    }

    @XmlTransient
    public List<Cooperativa> getCooperativaList() {
        return cooperativaList;
    }

    public void setCooperativaList(List<Cooperativa> cooperativaList) {
        this.cooperativaList = cooperativaList;
    }

    @XmlTransient
    public List<Persona> getPersonaList() {
        return personaList;
    }

    public void setPersonaList(List<Persona> personaList) {
        this.personaList = personaList;
    }

    @XmlTransient
    public List<Socio> getSocioList() {
        return socioList;
    }

    public void setSocioList(List<Socio> socioList) {
        this.socioList = socioList;
    }

    @XmlTransient
    public List<IngenioMinero> getIngenioMineroList() {
        return ingenioMineroList;
    }

    public void setIngenioMineroList(List<IngenioMinero> ingenioMineroList) {
        this.ingenioMineroList = ingenioMineroList;
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
        if (!(object instanceof Usuarios)) {
            return false;
        }
        Usuarios other = (Usuarios) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "ucb.edu.bo.sumajflow.entity.Usuarios[ id=" + id + " ]";
    }
    
}
