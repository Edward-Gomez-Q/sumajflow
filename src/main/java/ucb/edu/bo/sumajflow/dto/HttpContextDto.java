package ucb.edu.bo.sumajflow.dto;

/**
 * DTO para pasar información de contexto HTTP a la capa BL
 */
public class HttpContextDto {
    private String ipOrigen;
    private String userAgent;
    private String metodoHttp;
    private String endpoint;

    public HttpContextDto() {}

    public HttpContextDto(String ipOrigen, String userAgent, String metodoHttp, String endpoint) {
        this.ipOrigen = ipOrigen;
        this.userAgent = userAgent;
        this.metodoHttp = metodoHttp;
        this.endpoint = endpoint;
    }

    // Getters y Setters
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
}