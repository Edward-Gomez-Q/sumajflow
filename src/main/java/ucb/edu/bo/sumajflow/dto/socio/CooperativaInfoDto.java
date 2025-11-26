package ucb.edu.bo.sumajflow.dto.socio;

public class CooperativaInfoDto {
    private Integer id;
    private String razonSocial;
    private String nit;
    private String correoContacto;
    private String numeroTelefonoMovil;

    public CooperativaInfoDto() {
    }

    public CooperativaInfoDto(Integer id, String razonSocial, String nit,
                              String correoContacto, String numeroTelefonoMovil) {
        this.id = id;
        this.razonSocial = razonSocial;
        this.nit = nit;
        this.correoContacto = correoContacto;
        this.numeroTelefonoMovil = numeroTelefonoMovil;
    }

    // Getters y Setters
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

    public String getCorreoContacto() {
        return correoContacto;
    }

    public void setCorreoContacto(String correoContacto) {
        this.correoContacto = correoContacto;
    }

    public String getNumeroTelefonoMovil() {
        return numeroTelefonoMovil;
    }

    public void setNumeroTelefonoMovil(String numeroTelefonoMovil) {
        this.numeroTelefonoMovil = numeroTelefonoMovil;
    }
}
