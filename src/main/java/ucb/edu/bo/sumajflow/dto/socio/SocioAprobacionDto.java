package ucb.edu.bo.sumajflow.dto.socio;

/**
 * DTO para aprobar o rechazar una solicitud de socio
 */
public class SocioAprobacionDto {

    private Integer cooperativaSocioId;
    private String estado; // "aprobado" o "rechazado"
    private String observaciones;

    public SocioAprobacionDto() {
    }

    public SocioAprobacionDto(Integer cooperativaSocioId, String estado, String observaciones) {
        this.cooperativaSocioId = cooperativaSocioId;
        this.estado = estado;
        this.observaciones = observaciones;
    }

    // Getters y Setters
    public Integer getCooperativaSocioId() {
        return cooperativaSocioId;
    }

    public void setCooperativaSocioId(Integer cooperativaSocioId) {
        this.cooperativaSocioId = cooperativaSocioId;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public String getObservaciones() {
        return observaciones;
    }

    public void setObservaciones(String observaciones) {
        this.observaciones = observaciones;
    }
}
