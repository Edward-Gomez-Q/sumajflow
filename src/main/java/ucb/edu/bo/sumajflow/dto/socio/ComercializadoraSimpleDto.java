package ucb.edu.bo.sumajflow.dto.socio;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ComercializadoraSimpleDto {
    private Integer id;
    private String razonSocial;
    private String nit;
    private String correoContacto;
    private String numeroTelefonoMovil;
    private String departamento;
    private String municipio;
    private String direccion;
    private BigDecimal latitudAlmacen;
    private BigDecimal longitudAlmacen;
    private BigDecimal latitudBalanza;
    private BigDecimal longitudBalanza;
}