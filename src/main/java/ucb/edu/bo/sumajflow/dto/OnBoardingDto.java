package ucb.edu.bo.sumajflow.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class OnBoardingDto {

    private PersonaDto persona;
    private UsuarioDto usuario;

    @JsonProperty("cooperativa")
    private CooperativaDto cooperativa;

    @JsonProperty("socio")
    private SocioDto socio;

    @JsonProperty("ingenio")
    private IngenioDto ingenio;

    @JsonProperty("comercializadora")
    private ComercializadoraDto comercializadora;

    // Constructors
    public OnBoardingDto() {
    }

    // Getters and Setters
    public PersonaDto getPersona() {
        return persona;
    }

    public void setPersona(PersonaDto persona) {
        this.persona = persona;
    }

    public UsuarioDto getUsuario() {
        return usuario;
    }

    public void setUsuario(UsuarioDto usuario) {
        this.usuario = usuario;
    }

    public CooperativaDto getCooperativa() {
        return cooperativa;
    }

    public void setCooperativa(CooperativaDto cooperativa) {
        this.cooperativa = cooperativa;
    }

    public SocioDto getSocio() {
        return socio;
    }

    public void setSocio(SocioDto socio) {
        this.socio = socio;
    }

    public IngenioDto getIngenio() {
        return ingenio;
    }

    public void setIngenio(IngenioDto ingenio) {
        this.ingenio = ingenio;
    }

    public ComercializadoraDto getComercializadora() {
        return comercializadora;
    }

    public void setComercializadora(ComercializadoraDto comercializadora) {
        this.comercializadora = comercializadora;
    }

    /**
     * Determina el tipo de usuario basado en qué DTO específico está presente
     */
    public String getTipoUsuario() {
        if (cooperativa != null) return "cooperativa";
        if (socio != null) return "socio";
        if (ingenio != null) return "ingenio";
        if (comercializadora != null) return "comercializadora";
        return null;
    }
}