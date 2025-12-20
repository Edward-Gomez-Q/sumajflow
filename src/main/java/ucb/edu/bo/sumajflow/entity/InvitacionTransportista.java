package ucb.edu.bo.sumajflow.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Entidad para invitaciones de transportistas mediante QR
 */
@Entity
@Table(name = "invitacion_transportista")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvitacionTransportista {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cooperativa_id", nullable = false)
    private Cooperativa cooperativaId;

    @Column(name = "primer_nombre", nullable = false, length = 100)
    private String primerNombre;

    @Column(name = "segundo_nombre", length = 100)
    private String segundoNombre;

    @Column(name = "primer_apellido", nullable = false, length = 100)
    private String primerApellido;

    @Column(name = "segundo_apellido", length = 100)
    private String segundoApellido;

    @Column(name = "numero_celular", nullable = false, length = 50)
    private String numeroCelular;

    @Column(name = "token_invitacion", nullable = false, unique = true, length = 100)
    private String tokenInvitacion;

    @Column(name = "qr_code_data", nullable = false, columnDefinition = "TEXT")
    private String qrCodeData;

    @Column(name = "qr_code_url", columnDefinition = "TEXT")
    private String qrCodeUrl;

    @Column(name = "codigo_verificacion", length = 6)
    private String codigoVerificacion;

    @Column(name = "fecha_envio_codigo")
    private LocalDateTime fechaEnvioCodigo;

    @Column(name = "codigo_verificado", nullable = false)
    @Builder.Default
    private Boolean codigoVerificado = false;

    @Column(name = "intentos_verificacion", nullable = false)
    @Builder.Default
    private Integer intentosVerificacion = 0;

    /**
     * Estados posibles:
     * - pendiente_qr: QR generado, esperando escaneo
     * - codigo_enviado: App escaneó QR, código de verificación enviado
     * - verificado: Código verificado exitosamente
     * - completado: Onboarding completado
     * - expirado: Invitación venció
     */
    @Column(name = "estado", nullable = false, length = 50)
    @Builder.Default
    private String estado = "pendiente_qr";

    @Column(name = "fecha_envio", nullable = false)
    @Builder.Default
    private LocalDateTime fechaEnvio = LocalDateTime.now();

    @Column(name = "fecha_expiracion", nullable = false)
    private LocalDateTime fechaExpiracion;

    @Column(name = "fecha_aceptacion")
    private LocalDateTime fechaAceptacion;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "invitacionTransportista", fetch = FetchType.LAZY)
    private List<Transportista> transportistaList;

    // Métodos de conveniencia
    public String getNombreCompleto() {
        StringBuilder nombre = new StringBuilder();
        nombre.append(primerNombre);
        if (segundoNombre != null && !segundoNombre.isEmpty()) {
            nombre.append(" ").append(segundoNombre);
        }
        nombre.append(" ").append(primerApellido);
        if (segundoApellido != null && !segundoApellido.isEmpty()) {
            nombre.append(" ").append(segundoApellido);
        }
        return nombre.toString();
    }

    public boolean isExpirado() {
        return LocalDateTime.now().isAfter(fechaExpiracion);
    }

    public boolean isCodigoValido() {
        if (fechaEnvioCodigo == null) return false;
        // Código válido por 10 minutos
        return LocalDateTime.now().isBefore(fechaEnvioCodigo.plusMinutes(10));
    }
}