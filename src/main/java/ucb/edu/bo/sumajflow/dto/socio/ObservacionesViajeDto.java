// src/main/java/ucb/edu/bo/sumajflow/dto/socio/ObservacionesViajeDto.java
package ucb.edu.bo.sumajflow.dto.socio;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class ObservacionesViajeDto {
    @JsonProperty("pesaje_origen")
    private PesajeDto pesajeOrigen;

    @JsonProperty("pesaje_destino")
    private PesajeDto pesajeDestino;
}