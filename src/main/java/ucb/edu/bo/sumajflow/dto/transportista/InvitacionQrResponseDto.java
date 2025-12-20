package ucb.edu.bo.sumajflow.dto.transportista;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
/**
 * DTO para respuesta de invitación con QR
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvitacionQrResponseDto {
    private Integer invitacionId;
    private String token;
    private String qrCodeData;
    private String qrCodeUrl;
    private String nombreCompleto;
    private String numeroCelular;
    private String fechaExpiracion;
    private String estado;
}