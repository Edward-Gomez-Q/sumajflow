// src/main/java/ucb/edu/bo/sumajflow/dto/socio/PesajeDto.java
package ucb.edu.bo.sumajflow.dto.socio;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class PesajeDto {
    @JsonProperty("peso_tara_kg")
    private BigDecimal pesoTaraKg;

    @JsonProperty("peso_bruto_kg")
    private BigDecimal pesoBrutoKg;

    @JsonProperty("peso_neto_kg")
    private BigDecimal pesoNetoKg;

    @JsonProperty("timestamp")
    private LocalDateTime timestamp;

    @JsonProperty("observaciones")
    private String observaciones;
}