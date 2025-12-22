package ucb.edu.bo.sumajflow.dto.socio;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class IngenioSimpleDto {
    private Integer id;
    private String razonSocial;
    private String nit;
    private String correoContacto;
    private String numeroTelefonoMovil;
    private String departamento;
    private String municipio;
    private String direccion;
}