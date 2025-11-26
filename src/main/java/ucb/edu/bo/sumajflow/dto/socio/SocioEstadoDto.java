package ucb.edu.bo.sumajflow.dto.socio;
import java.util.Date;

public class SocioEstadoDto {
    private Integer socioId;
    private String estado;
    private Date fechaEnvio;
    private Date fechaAfiliacion;
    private String solicitudId;
    private CooperativaInfoDto cooperativa;
    private String mensaje;

    // Constructor vacío
    public SocioEstadoDto() {
    }

    // Constructor completo
    public SocioEstadoDto(Integer socioId, String estado, Date fechaEnvio,
                          Date fechaAfiliacion, String solicitudId,
                          CooperativaInfoDto cooperativa, String mensaje) {
        this.socioId = socioId;
        this.estado = estado;
        this.fechaEnvio = fechaEnvio;
        this.fechaAfiliacion = fechaAfiliacion;
        this.solicitudId = solicitudId;
        this.cooperativa = cooperativa;
        this.mensaje = mensaje;
    }

    // Getters y Setters
    public Integer getSocioId() {
        return socioId;
    }

    public void setSocioId(Integer socioId) {
        this.socioId = socioId;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public Date getFechaEnvio() {
        return fechaEnvio;
    }

    public void setFechaEnvio(Date fechaEnvio) {
        this.fechaEnvio = fechaEnvio;
    }

    public Date getFechaAfiliacion() {
        return fechaAfiliacion;
    }

    public void setFechaAfiliacion(Date fechaAfiliacion) {
        this.fechaAfiliacion = fechaAfiliacion;
    }

    public String getSolicitudId() {
        return solicitudId;
    }

    public void setSolicitudId(String solicitudId) {
        this.solicitudId = solicitudId;
    }

    public CooperativaInfoDto getCooperativa() {
        return cooperativa;
    }

    public void setCooperativa(CooperativaInfoDto cooperativa) {
        this.cooperativa = cooperativa;
    }

    public String getMensaje() {
        return mensaje;
    }

    public void setMensaje(String mensaje) {
        this.mensaje = mensaje;
    }
}
