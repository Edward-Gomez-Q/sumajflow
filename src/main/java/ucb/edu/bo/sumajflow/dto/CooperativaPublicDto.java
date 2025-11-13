package ucb.edu.bo.sumajflow.dto;

public class CooperativaPublicDto {
    private Integer id;
    private String razonSocial;

    public CooperativaPublicDto() {
    }

    public CooperativaPublicDto(Integer id, String razonSocial) {
        this.id = id;
        this.razonSocial = razonSocial;
    }

    // Getters and Setters
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
}