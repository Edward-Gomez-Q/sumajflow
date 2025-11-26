package ucb.edu.bo.sumajflow.dto;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.util.Date;
import java.util.Map;

public class NotificacionDto {
    private Integer id;
    private String tipo;
    private String titulo;
    private String mensaje;
    private Boolean leido;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss", timezone = "UTC")
    private Date fechaCreacion;

    private String time; // "Hace 5 minutos"
    private Map<String, Object> metadata;

    // Constructores
    public NotificacionDto() {
    }

    public NotificacionDto(Integer id, String tipo, String titulo, String mensaje,
                           Boolean leido, Date fechaCreacion, String time) {
        this.id = id;
        this.tipo = tipo;
        this.titulo = titulo;
        this.mensaje = mensaje;
        this.leido = leido;
        this.fechaCreacion = fechaCreacion;
        this.time = time;
    }

    // Getters y Setters
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public String getTitulo() {
        return titulo;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    public String getMensaje() {
        return mensaje;
    }

    public void setMensaje(String mensaje) {
        this.mensaje = mensaje;
    }

    public Boolean getLeido() {
        return leido;
    }

    public void setLeido(Boolean leido) {
        this.leido = leido;
    }

    public Date getFechaCreacion() {
        return fechaCreacion;
    }

    public void setFechaCreacion(Date fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }
}