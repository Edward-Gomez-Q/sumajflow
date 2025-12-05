package ucb.edu.bo.sumajflow.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.xml.bind.annotation.XmlRootElement;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Date;

/**
 * Entidad de auditoría mejorada para cumplir con ISO/IEC 27001
 * y garantizar trazabilidad completa en el sistema minero
 *
 * @author osval
 */
@Entity
@Table(name = "auditoria", indexes = {
        @Index(name = "idx_auditoria_usuario", columnList = "usuarios_id"),
        @Index(name = "idx_auditoria_tabla", columnList = "tabla_afectada"),
        @Index(name = "idx_auditoria_fecha", columnList = "fecha_operacion"),
        @Index(name = "idx_auditoria_accion", columnList = "accion"),
        @Index(name = "idx_auditoria_criticidad", columnList = "nivel_criticidad"),
        @Index(name = "idx_auditoria_registro", columnList = "tabla_afectada, registro_id")
})
@XmlRootElement
public class Auditoria implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic(optional = false)
    @Column(name = "id")
    private Integer id;

    // === Identificación del usuario y contexto ===
    @JoinColumn(name = "usuarios_id", referencedColumnName = "id")
    @ManyToOne(fetch = FetchType.LAZY)
    private Usuarios usuariosId;

    @Size(max = 50)
    @Column(name = "tipo_usuario", length = 50)
    private String tipoUsuario;

    // === Detalles de la operación ===
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 100)
    @Column(name = "tabla_afectada")
    private String tablaAfectada;

    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 20)
    @Column(name = "accion")
    private String accion;

    @Column(name = "registro_id")
    private Integer registroId;

    // === Información detallada del cambio ===
    @Column(name = "datos_anteriores", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private String datosAnteriores;

    @Column(name = "datos_nuevos", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private String datosNuevos;

    @Column(name = "campos_modificados", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private String camposModificados;

    // === Contexto técnico y seguridad ===
    @Size(max = 45)
    @Column(name = "ip_origen", length = 45)
    private String ipOrigen;

    @Column(name = "user_agent", columnDefinition = "text")
    private String userAgent;

    @Size(max = 10)
    @Column(name = "metodo_http", length = 10)
    private String metodoHttp;

    @Size(max = 255)
    @Column(name = "endpoint", length = 255)
    private String endpoint;

    // === Trazabilidad temporal ===
    @Basic(optional = false)
    @NotNull
    @Column(name = "fecha_operacion")
    @Temporal(TemporalType.TIMESTAMP)
    private Date fechaOperacion;

    // === Información adicional ===
    @Column(name = "descripcion", columnDefinition = "text")
    private String descripcion;

    @Size(max = 20)
    @Column(name = "nivel_criticidad", length = 20)
    private String nivelCriticidad;

    @Size(max = 50)
    @Column(name = "modulo", length = 50)
    private String modulo;

    // === Indicadores de seguridad ===
    @Column(name = "operacion_exitosa")
    private Boolean operacionExitosa;

    @Column(name = "mensaje_error", columnDefinition = "text")
    private String mensajeError;

    // === Constructores ===
    public Auditoria() {
        this.fechaOperacion = new Date();
        this.operacionExitosa = true;
        this.nivelCriticidad = "MEDIO";
    }

    public Auditoria(Integer id) {
        this();
        this.id = id;
    }

    // === Getters y Setters ===
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Usuarios getUsuariosId() {
        return usuariosId;
    }

    public void setUsuariosId(Usuarios usuariosId) {
        this.usuariosId = usuariosId;
    }

    public String getTipoUsuario() {
        return tipoUsuario;
    }

    public void setTipoUsuario(String tipoUsuario) {
        this.tipoUsuario = tipoUsuario;
    }

    public String getTablaAfectada() {
        return tablaAfectada;
    }

    public void setTablaAfectada(String tablaAfectada) {
        this.tablaAfectada = tablaAfectada;
    }

    public String getAccion() {
        return accion;
    }

    public void setAccion(String accion) {
        this.accion = accion;
    }

    public Integer getRegistroId() {
        return registroId;
    }

    public void setRegistroId(Integer registroId) {
        this.registroId = registroId;
    }

    public String getDatosAnteriores() {
        return datosAnteriores;
    }

    public void setDatosAnteriores(String datosAnteriores) {
        this.datosAnteriores = datosAnteriores;
    }

    public String getDatosNuevos() {
        return datosNuevos;
    }

    public void setDatosNuevos(String datosNuevos) {
        this.datosNuevos = datosNuevos;
    }

    public String getCamposModificados() {
        return camposModificados;
    }

    public void setCamposModificados(String camposModificados) {
        this.camposModificados = camposModificados;
    }

    public String getIpOrigen() {
        return ipOrigen;
    }

    public void setIpOrigen(String ipOrigen) {
        this.ipOrigen = ipOrigen;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public String getMetodoHttp() {
        return metodoHttp;
    }

    public void setMetodoHttp(String metodoHttp) {
        this.metodoHttp = metodoHttp;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public Date getFechaOperacion() {
        return fechaOperacion;
    }

    public void setFechaOperacion(Date fechaOperacion) {
        this.fechaOperacion = fechaOperacion;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getNivelCriticidad() {
        return nivelCriticidad;
    }

    public void setNivelCriticidad(String nivelCriticidad) {
        this.nivelCriticidad = nivelCriticidad;
    }

    public String getModulo() {
        return modulo;
    }

    public void setModulo(String modulo) {
        this.modulo = modulo;
    }

    public Boolean getOperacionExitosa() {
        return operacionExitosa;
    }

    public void setOperacionExitosa(Boolean operacionExitosa) {
        this.operacionExitosa = operacionExitosa;
    }

    public String getMensajeError() {
        return mensajeError;
    }

    public void setMensajeError(String mensajeError) {
        this.mensajeError = mensajeError;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (id != null ? id.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        if (!(object instanceof Auditoria)) {
            return false;
        }
        Auditoria other = (Auditoria) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "ucb.edu.bo.sumajflow.entity.Auditoria[ id=" + id + " ]";
    }
}