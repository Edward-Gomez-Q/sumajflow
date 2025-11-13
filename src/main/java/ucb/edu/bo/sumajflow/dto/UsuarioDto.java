package ucb.edu.bo.sumajflow.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class UsuarioDto {

    private String correo;
    private String contrasena;

    @JsonProperty("tipo_usuario")
    private String tipoUsuario;

    // Constructors
    public UsuarioDto() {
    }

    // Getters and Setters
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

    public String getTipoUsuario() {
        return tipoUsuario;
    }

    public void setTipoUsuario(String tipoUsuario) {
        this.tipoUsuario = tipoUsuario;
    }
}