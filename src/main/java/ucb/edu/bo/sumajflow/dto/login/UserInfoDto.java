package ucb.edu.bo.sumajflow.dto.login;

public class UserInfoDto {

    private Integer id;
    private String correo;
    private String rol;
    private String nombres;
    private String primerApellido;
    private String segundoApellido;

    // Constructors
    public UserInfoDto() {
    }

    public UserInfoDto(Integer id, String correo, String rol, String nombres,
                       String primerApellido, String segundoApellido) {
        this.id = id;
        this.correo = correo;
        this.rol = rol;
        this.nombres = nombres;
        this.primerApellido = primerApellido;
        this.segundoApellido = segundoApellido;
    }

    // Getters and Setters
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

    public String getRol() {
        return rol;
    }

    public void setRol(String rol) {
        this.rol = rol;
    }

    public String getNombres() {
        return nombres;
    }

    public void setNombres(String nombres) {
        this.nombres = nombres;
    }

    public String getPrimerApellido() {
        return primerApellido;
    }

    public void setPrimerApellido(String primerApellido) {
        this.primerApellido = primerApellido;
    }

    public String getSegundoApellido() {
        return segundoApellido;
    }

    public void setSegundoApellido(String segundoApellido) {
        this.segundoApellido = segundoApellido;
    }

    /**
     * Retorna el nombre completo del usuario
     */
    public String getNombreCompleto() {
        StringBuilder nombreCompleto = new StringBuilder(nombres);
        if (primerApellido != null && !primerApellido.isEmpty()) {
            nombreCompleto.append(" ").append(primerApellido);
        }
        if (segundoApellido != null && !segundoApellido.isEmpty()) {
            nombreCompleto.append(" ").append(segundoApellido);
        }
        return nombreCompleto.toString();
    }
}
