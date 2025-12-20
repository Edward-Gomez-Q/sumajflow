package ucb.edu.bo.sumajflow.bl.cooperativa;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ucb.edu.bo.sumajflow.bl.AuditoriaBl;
import ucb.edu.bo.sumajflow.bl.NotificacionBl;
import ucb.edu.bo.sumajflow.bl.QRCodeService;
import ucb.edu.bo.sumajflow.bl.WhatsAppService;
import ucb.edu.bo.sumajflow.dto.transportista.*;
import ucb.edu.bo.sumajflow.entity.*;
import ucb.edu.bo.sumajflow.repository.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import ucb.edu.bo.sumajflow.utils.JwtUtil;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Servicio para gestión de onboarding de transportistas con QR
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TransportistaBl {

    private final TransportistaRepository transportistaRepository;
    private final InvitacionTransportistaRepository invitacionRepository;
    private final CooperativaRepository cooperativaRepository;
    private final UsuariosRepository usuariosRepository;
    private final QRCodeService qrCodeService;
    private final BCryptPasswordEncoder passwordEncoder;
    private final WhatsAppService whatsAppService;
    private final AuditoriaBl auditoriaBl;
    private final NotificacionBl notificacionBl;
    private final TipoUsuarioRepository tipoUsuarioRepository;
    private final PersonaRepository personaRepository;
    private final JwtUtil jwtUtil;

    private static final int MAX_INTENTOS_VERIFICACION = 3;
    private static final int CODIGO_EXPIRACION_MINUTOS = 10;
    private static final int QR_VALIDEZ_DIAS = 7;

    /**
     * Crear invitación y generar QR
     */
    @Transactional
    public Map<String, Object> crearInvitacionConQR(
            Integer usuarioId,
            TransportistaInvitacionDto dto,
            String ipOrigen,
            String metodoHttp,
            String endpoint
    ) {
        log.info("Creando invitación con QR para: {} {}",
                dto.getPrimerNombre(), dto.getPrimerApellido());

        // 1. Obtener cooperativa
        Cooperativa cooperativa = obtenerCooperativaDelUsuario(usuarioId);
        Usuarios usuario = obtenerUsuario(usuarioId);

        // 2. Validar que no exista invitación activa reciente
        validarInvitacionDuplicada(cooperativa, dto.getNumeroCelular());

        // 3. Generar token único
        String token = generarTokenInvitacion();

        // 4. Generar datos del QR
        String qrData = qrCodeService.generateInvitacionQRData(cooperativa.getId(), token);

        // 5. Generar imagen QR en Base64
        String qrCodeBase64 = qrCodeService.generateQRCodeBase64(qrData);

        // 6. Crear invitación en BD
        InvitacionTransportista invitacion = crearYGuardarInvitacion(
                cooperativa, dto, token, qrData, qrCodeBase64
        );

        // 7. Registrar auditoría
        registrarAuditoriaCreacion(
                usuario, invitacion, ipOrigen, metodoHttp, endpoint
        );

        // 8. Crear notificación
        enviarNotificacionCreacion(usuarioId, invitacion.getNombreCompleto());

        // 9. Construir respuesta
        Map<String, Object> respuesta = new HashMap<>();
        respuesta.put("invitacionId", invitacion.getId());
        respuesta.put("token", token);
        respuesta.put("qrCodeData", qrData);
        respuesta.put("qrCodeBase64", qrCodeBase64);
        respuesta.put("nombreCompleto", invitacion.getNombreCompleto());
        respuesta.put("numeroCelular", invitacion.getNumeroCelular());
        respuesta.put("fechaExpiracion", invitacion.getFechaExpiracion());
        respuesta.put("estado", invitacion.getEstado());

        return respuesta;
    }

    /**
     * Iniciar onboarding desde app móvil (después de escanear QR)
     */
    @Transactional
    public Map<String, Object> iniciarOnboarding(
            IniciarOnboardingDto dto,
            String ipOrigen
    ) {
        log.info("Iniciando onboarding con token: {}", dto.getToken());

        // 1. Buscar invitación
        InvitacionTransportista invitacion = invitacionRepository
                .findByTokenInvitacion(dto.getToken())
                .orElseThrow(() -> new IllegalArgumentException("Token de invitación inválido"));

        // 2. Validar estado
        if (!"pendiente_qr".equals(invitacion.getEstado())) {
            throw new IllegalArgumentException(
                    "Esta invitación ya fue procesada o está en otro estado");
        }

        // 3. Validar que no esté expirada
        if (invitacion.isExpirado()) {
            invitacion.setEstado("expirado");
            invitacionRepository.save(invitacion);
            throw new IllegalArgumentException("Esta invitación ha expirado");
        }

        // 4. Generar código de verificación
        String codigo = generarCodigoVerificacion();

        // 5. Enviar código por WhatsApp
        boolean codigoEnviado = false;
        String mensajeError = null;
        try {
            whatsAppService.enviarCodigoVerificacion(
                    invitacion.getNumeroCelular(),
                    invitacion.getNombreCompleto(),
                    codigo
            );
            codigoEnviado = true;
        } catch (Exception e) {
            log.error("Error al enviar código: {}", e.getMessage());
            mensajeError = "No se pudo enviar el código de verificación. Código: " + codigo;
        }

        // 6. Actualizar invitación
        invitacion.setCodigoVerificacion(codigo);
        invitacion.setFechaEnvioCodigo(LocalDateTime.now());
        invitacion.setEstado("codigo_enviado");
        invitacion.setIntentosVerificacion(0);
        invitacionRepository.save(invitacion);

        // 7. Construir respuesta
        Map<String, Object> respuesta = new HashMap<>();
        respuesta.put("invitacionId", invitacion.getId());
        respuesta.put("primerNombre", invitacion.getPrimerNombre());
        respuesta.put("segundoNombre", invitacion.getSegundoNombre());
        respuesta.put("primerApellido", invitacion.getPrimerApellido());
        respuesta.put("segundoApellido", invitacion.getSegundoApellido());
        respuesta.put("numeroCelular", invitacion.getNumeroCelular());
        respuesta.put("cooperativaNombre", invitacion.getCooperativaId().getRazonSocial());
        respuesta.put("codigoEnviado", codigoEnviado);

        if (!codigoEnviado) {
            respuesta.put("mensaje", mensajeError);
        } else {
            respuesta.put("mensaje", "Código de verificación enviado a tu WhatsApp");
        }

        return respuesta;
    }

    /**
     * Verificar código enviado por WhatsApp
     */
    @Transactional
    public Map<String, Object> verificarCodigo(
            VerificarCodigoDto dto,
            String ipOrigen
    ) {
        log.info("Verificando código para token: {}", dto.getToken());

        // 1. Buscar invitación
        InvitacionTransportista invitacion = invitacionRepository
                .findByTokenInvitacion(dto.getToken())
                .orElseThrow(() -> new IllegalArgumentException("Token inválido"));

        // 2. Validar estado
        if (!"codigo_enviado".equals(invitacion.getEstado())) {
            throw new IllegalArgumentException("No hay código pendiente de verificación");
        }

        // 3. Validar intentos
        if (invitacion.getIntentosVerificacion() >= MAX_INTENTOS_VERIFICACION) {
            throw new IllegalArgumentException(
                    "Has excedido el número máximo de intentos. Solicita un nuevo código.");
        }

        // 4. Validar que el código no haya expirado
        if (!invitacion.isCodigoValido()) {
            throw new IllegalArgumentException(
                    "El código ha expirado. Solicita un nuevo código.");
        }

        // 5. Verificar código
        boolean codigoCorrecto = invitacion.getCodigoVerificacion().equals(dto.getCodigo());

        if (!codigoCorrecto) {
            // Incrementar intentos
            invitacion.setIntentosVerificacion(invitacion.getIntentosVerificacion() + 1);
            invitacionRepository.save(invitacion);

            int intentosRestantes = MAX_INTENTOS_VERIFICACION - invitacion.getIntentosVerificacion();
            throw new IllegalArgumentException(
                    "Código incorrecto. Te quedan " + intentosRestantes + " intentos.");
        }

        // 6. Código correcto - actualizar estado
        invitacion.setCodigoVerificado(true);
        invitacion.setEstado("verificado");
        invitacionRepository.save(invitacion);

        // 7. Respuesta exitosa
        Map<String, Object> respuesta = new HashMap<>();
        respuesta.put("verificado", true);
        respuesta.put("mensaje", "Número verificado exitosamente");
        respuesta.put("siguientePaso", "completar_datos_vehiculo");

        return respuesta;
    }

    /**
     * Reenviar código de verificación
     */
    @Transactional
    public Map<String, Object> reenviarCodigo(
            ReenviarCodigoDto dto,
            String ipOrigen
    ) {
        log.info("Reenviando código para token: {}", dto.getToken());

        // 1. Buscar invitación
        InvitacionTransportista invitacion = invitacionRepository
                .findByTokenInvitacion(dto.getToken())
                .orElseThrow(() -> new IllegalArgumentException("Token inválido"));

        // 2. Validar estado
        if (!"codigo_enviado".equals(invitacion.getEstado())) {
            throw new IllegalArgumentException("No hay código pendiente de verificación");
        }

        // 3. Validar rate limiting (no más de 1 por minuto)
        if (invitacion.getFechaEnvioCodigo() != null) {
            LocalDateTime limite = invitacion.getFechaEnvioCodigo().plusMinutes(1);
            if (LocalDateTime.now().isBefore(limite)) {
                throw new IllegalArgumentException(
                        "Debes esperar 1 minuto antes de solicitar un nuevo código");
            }
        }

        // 4. Generar nuevo código
        String nuevoCodigo = generarCodigoVerificacion();

        // 5. Enviar por WhatsApp
        boolean codigoEnviado = false;
        try {
            whatsAppService.reenviarCodigoVerificacion(
                    invitacion.getNumeroCelular(),
                    invitacion.getNombreCompleto(),
                    nuevoCodigo
            );
            codigoEnviado = true;
        } catch (Exception e) {
            log.error("Error al reenviar código: {}", e.getMessage());
        }

        // 6. Actualizar invitación
        invitacion.setCodigoVerificacion(nuevoCodigo);
        invitacion.setFechaEnvioCodigo(LocalDateTime.now());
        invitacion.setIntentosVerificacion(0); // Resetear intentos
        invitacionRepository.save(invitacion);

        // 7. Respuesta
        Map<String, Object> respuesta = new HashMap<>();
        respuesta.put("codigoEnviado", codigoEnviado);
        respuesta.put("mensaje", codigoEnviado ?
                "Nuevo código enviado" : "Error al enviar código");

        return respuesta;
    }

    /**
     * Listar invitaciones con filtros
     */
    @Transactional(readOnly = true)
    public Page<Map<String, Object>> listarInvitaciones(
            Integer usuarioId,
            String estado,
            String busqueda,
            Integer pagina,
            Integer tamanoPagina
    ) {
        log.debug("Listando invitaciones - Usuario: {}", usuarioId);

        Cooperativa cooperativa = obtenerCooperativaDelUsuario(usuarioId);
        Pageable pageable = PageRequest.of(pagina, tamanoPagina);

        Page<InvitacionTransportista> pageInvitaciones = invitacionRepository
                .findByCooperativaWithFilters(cooperativa.getId(), estado, busqueda, pageable);

        return pageInvitaciones.map(this::convertirInvitacionADto);
    }
    @Transactional
    public Map<String, Object> completarOnboarding(
            CompletarOnboardingDto dto,
            String ipOrigen
    ) {
        log.info("🚀 Completando onboarding para token: {}", dto.getToken());

        // 1. Buscar invitación
        InvitacionTransportista invitacion = invitacionRepository
                .findByTokenInvitacion(dto.getToken())
                .orElseThrow(() -> new IllegalArgumentException("Token inválido"));

        // 2. Validar que esté verificada
        if (!"verificado".equals(invitacion.getEstado())) {
            throw new IllegalArgumentException(
                    "Debes verificar tu número de teléfono antes de completar el registro");
        }

        // 3. Validar que el correo no esté registrado
        if (usuariosRepository.findByCorreo(dto.getCorreo()).isPresent()) {
            throw new IllegalArgumentException("El correo ya está registrado en el sistema");
        }

        // 4. Validar que el CI no esté registrado
        if (transportistaRepository.existsByCi(dto.getCi())) {
            throw new IllegalArgumentException("El CI ya está registrado en el sistema");
        }

        // 5. Validar que la placa no esté registrada
        if (transportistaRepository.existsByPlacaVehiculo(dto.getPlacaVehiculo())) {
            throw new IllegalArgumentException("La placa del vehículo ya está registrada");
        }

        try {
            // 6. Buscar tipo de usuario transportista
            TipoUsuario tipoTransportista = tipoUsuarioRepository
                    .findByTipoUsuario("transportista")
                    .orElseThrow(() -> new IllegalArgumentException("Tipo de usuario no encontrado"));

            // 7. Crear usuario
            Usuarios usuario = Usuarios.builder()
                    .correo(dto.getCorreo())
                    .contrasena(passwordEncoder.encode(dto.getContrasena()))
                    .tipoUsuarioId(tipoTransportista)
                    .build();

            usuario = usuariosRepository.save(usuario);
            log.info("✅ Usuario creado con ID: {}", usuario.getId());

            // 8. Crear registro en tabla persona
            Persona persona = Persona.builder()
                    .nombres(construirNombres(invitacion))
                    .primerApellido(invitacion.getPrimerApellido())
                    .segundoApellido(invitacion.getSegundoApellido())
                    .ci(dto.getCi())
                    .numeroCelular(invitacion.getNumeroCelular())
                    .fechaNacimiento(dto.getFechaNacimiento())
                    .usuariosId(usuario)
                    .build();

            persona = personaRepository.save(persona);
            log.info("✅ Persona creada con ID: {}", persona.getId());

            // 9. Crear transportista
            Transportista transportista = Transportista.builder()
                    .usuariosId(usuario)
                    .ci(dto.getCi())
                    .licenciaConducir(dto.getLicenciaConducirUrl())
                    .categoriaLicencia(dto.getCategoriaLicencia())
                    .fechaVencimientoLicencia(dto.getFechaVencimientoLicencia())
                    .placaVehiculo(dto.getPlacaVehiculo())
                    .marcaVehiculo(dto.getMarcaVehiculo())
                    .modeloVehiculo(dto.getModeloVehiculo())
                    .colorVehiculo(dto.getColorVehiculo())
                    .pesoTara(BigDecimal.valueOf(dto.getPesoTara()))
                    .capacidadCarga(BigDecimal.valueOf(dto.getCapacidadCarga()))
                    .estado("activo")
                    .fechaAprobacion(LocalDateTime.now())
                    .viajesCompletados(0)
                    .calificacionPromedio(BigDecimal.ZERO)
                    .invitacionTransportista(invitacion)
                    .build();

            transportista = transportistaRepository.save(transportista);
            log.info("✅ Transportista creado con ID: {}", transportista.getId());

            // 10. Actualizar estado de invitación
            invitacion.setEstado("completado");
            invitacion.setFechaAceptacion(LocalDateTime.now());
            invitacionRepository.save(invitacion);

            // 11. Registrar auditoría
            registrarAuditoriaOnboardingCompleto(
                    usuario, transportista, invitacion, ipOrigen
            );

            // 12. Notificar a la cooperativa
            notificarCooperativaOnboardingCompleto(
                    invitacion.getCooperativaId(),
                    invitacion.getNombreCompleto(),
                    usuario.getId()
            );

            // 13. Notificar al transportista
            notificarTransportistaRegistroCompleto(usuario, invitacion.getNombreCompleto());

            // 14. Construir respuesta
            Map<String, Object> respuesta = new HashMap<>();
            respuesta.put("transportistaId", transportista.getId());
            respuesta.put("usuarioId", usuario.getId());
            respuesta.put("mensaje", "Registro completado exitosamente");
            respuesta.put("correo", usuario.getCorreo());
            respuesta.put("token", jwtUtil.generateAccessToken(usuario.getId(), usuario.getCorreo(), "transportista", true));


            log.info("✅ Onboarding completado exitosamente para: {}", invitacion.getNombreCompleto());

            return respuesta;

        } catch (Exception e) {
            log.error("❌ Error al completar onboarding: {}", e.getMessage(), e);
            throw new RuntimeException("Error al completar el registro: " + e.getMessage());
        }
    }

// ==================== MÉTODOS AUXILIARES ====================

    private String construirNombres(InvitacionTransportista invitacion) {
        StringBuilder nombres = new StringBuilder();
        if (invitacion.getPrimerNombre() != null) {
            nombres.append(invitacion.getPrimerNombre());
        }
        if (invitacion.getSegundoNombre() != null &&
                !invitacion.getSegundoNombre().isEmpty()) {
            nombres.append(" ").append(invitacion.getSegundoNombre());
        }
        return nombres.toString().trim();
    }

    private void registrarAuditoriaOnboardingCompleto(
            Usuarios usuario,
            Transportista transportista,
            InvitacionTransportista invitacion,
            String ipOrigen
    ) {
        Map<String, Object> datos = Map.of(
                "transportistaId", transportista.getId(),
                "usuarioId", usuario.getId(),
                "invitacionId", invitacion.getId(),
                "nombreCompleto", invitacion.getNombreCompleto(),
                "ci", transportista.getCi(),
                "placa", transportista.getPlacaVehiculo(),
                "correo", usuario.getCorreo()
        );

        auditoriaBl.registrar(
                usuario,
                "transportista",
                "CREATE",
                "Onboarding completado: " + invitacion.getNombreCompleto(),
                transportista.getId(),
                null,
                datos,
                List.of("ci", "placaVehiculo", "correo", "estado"),
                ipOrigen,
                null,
                "POST",
                "/public/onboarding/completar",
                "RF_TRANSP_ONBOARD",
                "ALTO"
        );
    }

    private void notificarCooperativaOnboardingCompleto(
            Cooperativa cooperativa,
            String nombreTransportista,
            Integer transportistaUsuarioId
    ) {
        Usuarios usuarioCooperativa = cooperativa.getUsuariosId();

        Map<String, Object> metadata = Map.of(
                "tipo", "nuevo_transportista",
                "nombreTransportista", nombreTransportista,
                "transportistaUsuarioId", transportistaUsuarioId,
                "cooperativaId", cooperativa.getId()
        );

        notificacionBl.crearNotificacion(
                usuarioCooperativa.getId(),
                "success",
                "Nuevo transportista registrado",
                nombreTransportista + " completó su registro como transportista",
                metadata
        );
    }

    private void notificarTransportistaRegistroCompleto(
            Usuarios usuario,
            String nombreCompleto
    ) {
        Map<String, Object> metadata = Map.of(
                "tipo", "registro_completado",
                "usuarioId", usuario.getId()
        );

        notificacionBl.crearNotificacion(
                usuario.getId(),
                "success",
                "¡Bienvenido a SumajFlow!",
                "Tu registro como transportista se completó exitosamente. " +
                        "Ya puedes iniciar sesión y comenzar a trabajar.",
                metadata
        );
    }

    // ==================== MÉTODOS AUXILIARES ====================

    private Cooperativa obtenerCooperativaDelUsuario(Integer usuarioId) {
        Usuarios usuario = obtenerUsuario(usuarioId);
        return cooperativaRepository.findByUsuariosId(usuario)
                .orElseThrow(() -> new IllegalArgumentException("Cooperativa no encontrada"));
    }

    private Usuarios obtenerUsuario(Integer usuarioId) {
        return usuariosRepository.findById(usuarioId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));
    }

    private void validarInvitacionDuplicada(Cooperativa cooperativa, String celular) {
        // Verificar invitación activa
        Optional<InvitacionTransportista> invitacionActiva =
                invitacionRepository.findInvitacionActivaPorCelular(
                        cooperativa, celular, LocalDateTime.now()
                );

        if (invitacionActiva.isPresent()) {
            throw new IllegalArgumentException(
                    "Ya existe una invitación activa para este número de celular");
        }

        // Verificar invitación reciente (últimas 24 horas)
        boolean tieneInvitacionReciente = invitacionRepository.existeInvitacionReciente(
                cooperativa, celular, LocalDateTime.now().minusHours(24)
        );

        if (tieneInvitacionReciente) {
            throw new IllegalArgumentException(
                    "Ya se creó una invitación para este número en las últimas 24 horas. " +
                            "Por favor espera antes de crear otra.");
        }
    }

    private String generarTokenInvitacion() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    private String generarCodigoVerificacion() {
        SecureRandom random = new SecureRandom();
        int codigo = 100000 + random.nextInt(900000); // 6 dígitos
        return String.valueOf(codigo);
    }

    private InvitacionTransportista crearYGuardarInvitacion(
            Cooperativa cooperativa,
            TransportistaInvitacionDto dto,
            String token,
            String qrData,
            String qrCodeBase64
    ) {
        InvitacionTransportista invitacion = InvitacionTransportista.builder()
                .cooperativaId(cooperativa)
                .primerNombre(dto.getPrimerNombre())
                .segundoNombre(dto.getSegundoNombre())
                .primerApellido(dto.getPrimerApellido())
                .segundoApellido(dto.getSegundoApellido())
                .numeroCelular(dto.getNumeroCelular())
                .tokenInvitacion(token)
                .qrCodeData(qrData)
                .qrCodeUrl(qrCodeBase64)
                .estado("pendiente_qr")
                .fechaEnvio(LocalDateTime.now())
                .fechaExpiracion(LocalDateTime.now().plusDays(QR_VALIDEZ_DIAS))
                .codigoVerificado(false)
                .intentosVerificacion(0)
                .build();

        return invitacionRepository.save(invitacion);
    }

    private Map<String, Object> convertirInvitacionADto(InvitacionTransportista inv) {
        Map<String, Object> dto = new HashMap<>();
        dto.put("id", inv.getId());
        dto.put("nombreCompleto", inv.getNombreCompleto());
        dto.put("numeroCelular", inv.getNumeroCelular());
        dto.put("estado", inv.getEstado());
        dto.put("fechaCreacion", inv.getCreatedAt());
        dto.put("fechaExpiracion", inv.getFechaExpiracion());
        dto.put("expirado", inv.isExpirado());
        dto.put("qrCodeBase64", inv.getQrCodeUrl());
        return dto;
    }

    private void registrarAuditoriaCreacion(
            Usuarios usuario,
            InvitacionTransportista invitacion,
            String ipOrigen,
            String metodoHttp,
            String endpoint
    ) {
        Map<String, Object> datos = Map.of(
                "invitacionId", invitacion.getId(),
                "nombreCompleto", invitacion.getNombreCompleto(),
                "numeroCelular", invitacion.getNumeroCelular()
        );

        auditoriaBl.registrar(
                usuario,
                "invitacion_transportista",
                "CREATE",
                "Invitación con QR creada: " + invitacion.getNombreCompleto(),
                invitacion.getId(),
                null,
                datos,
                List.of("nombreCompleto", "numeroCelular"),
                ipOrigen,
                null,
                metodoHttp,
                endpoint,
                "RF_TRANSP_QR_01",
                "MEDIO"
        );
    }

    private void enviarNotificacionCreacion(Integer usuarioId, String nombreCompleto) {
        Map<String, Object> metadata = Map.of(
                "tipo", "invitacion_transportista_qr",
                "nombreCompleto", nombreCompleto
        );

        notificacionBl.crearNotificacion(
                usuarioId,
                "success",
                "Invitación creada",
                "El código QR para " + nombreCompleto + " fue generado exitosamente",
                metadata
        );
    }
    /**
     * Obtener datos de la invitación para pre-llenar el formulario
     */
    @Transactional(readOnly = true)
    public Map<String, Object> obtenerDatosInvitacion(String token) {
        log.info("Obteniendo datos de invitación con token: {}", token);

        // 1. Buscar invitación
        InvitacionTransportista invitacion = invitacionRepository
                .findByTokenInvitacion(token)
                .orElseThrow(() -> new IllegalArgumentException("Token de invitación inválido"));

        // 2. Validar que esté verificada
        if (!"verificado".equals(invitacion.getEstado())) {
            throw new IllegalArgumentException(
                    "La invitación debe estar verificada para acceder a estos datos");
        }

        // 3. Construir respuesta con los datos
        Map<String, Object> respuesta = new HashMap<>();
        respuesta.put("primerNombre", invitacion.getPrimerNombre());
        respuesta.put("segundoNombre", invitacion.getSegundoNombre());
        respuesta.put("primerApellido", invitacion.getPrimerApellido());
        respuesta.put("segundoApellido", invitacion.getSegundoApellido());
        respuesta.put("numeroCelular", invitacion.getNumeroCelular());
        respuesta.put("cooperativaNombre", invitacion.getCooperativaId().getRazonSocial());

        return respuesta;
    }


}