package ucb.edu.bo.sumajflow.bl.cooperativa;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import ucb.edu.bo.sumajflow.bl.AuditoriaBl;
import ucb.edu.bo.sumajflow.bl.NotificacionBl;
import ucb.edu.bo.sumajflow.bl.QRCodeService;
import ucb.edu.bo.sumajflow.bl.WhatsAppService;
import ucb.edu.bo.sumajflow.dto.transportista.*;
import ucb.edu.bo.sumajflow.entity.*;
import ucb.edu.bo.sumajflow.repository.*;
import ucb.edu.bo.sumajflow.utils.JwtUtil;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransportistaBl {

    private final TransportistaRepository transportistaRepository;
    private final InvitacionTransportistaRepository invitacionRepository;
    private final InvitacionCooperativaRepository invitacionCooperativaRepository; // ✅ NUEVO
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
    private static final int QR_VALIDEZ_DIAS = 7;

    /**
     * ✅ ACTUALIZADO: Crear invitación y generar QR
     * Ahora crea también el registro en invitacion_cooperativa
     */
    @Transactional
    public Map<String, Object> crearInvitacionConQR(
            Integer usuarioId,
            TransportistaInvitacionDto dto,
            String ipOrigen,
            String metodoHttp,
            String endpoint
    ) {
        log.info("Creando invitación con QR para: {} {}", dto.getPrimerNombre(), dto.getPrimerApellido());

        Cooperativa cooperativa = obtenerCooperativaDelUsuario(usuarioId);
        Usuarios usuario = obtenerUsuario(usuarioId);

        validarInvitacionDuplicada(cooperativa.getId(), dto.getNumeroCelular());

        String token = generarTokenInvitacion();
        String qrData = qrCodeService.generateInvitacionQRData(cooperativa.getId(), token);
        String qrCodeBase64 = qrCodeService.generateQRCodeBase64(qrData);

        // ✅ PASO 1: Crear la invitación (SIN cooperativa_id)
        InvitacionTransportista invitacion = crearYGuardarInvitacion(
                dto, token, qrData, qrCodeBase64
        );

        // ✅ PASO 2: Crear la relación en invitacion_cooperativa
        InvitacionCooperativa invitacionCooperativa = InvitacionCooperativa.builder()
                .cooperativa(cooperativa)
                .invitacionTransportista(invitacion)
                .build();
        invitacionCooperativaRepository.save(invitacionCooperativa);

        log.info("Relación cooperativa-invitación creada: cooperativa={}, invitacion={}", 
                cooperativa.getId(), invitacion.getId());

        registrarAuditoriaCreacion(usuario, invitacion, cooperativa, ipOrigen, metodoHttp, endpoint);
        enviarNotificacionCreacion(usuarioId, invitacion.getNombreCompleto());

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
     * Listar invitaciones con filtros (sin cambios en firma)
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

        // El repository ya está actualizado para usar invitacion_cooperativa
        Page<InvitacionTransportista> pageInvitaciones = invitacionRepository
                .findByCooperativaWithFilters(cooperativa.getId(), estado, busqueda, pageable);

        return pageInvitaciones.map(this::convertirInvitacionADto);
    }

    /**
     * ✅ ACTUALIZADO: Iniciar onboarding desde app móvil
     * Ahora obtiene la cooperativa desde invitacion_cooperativa
     */
    @Transactional
    public Map<String, Object> iniciarOnboarding(
            IniciarOnboardingDto dto,
            String ipOrigen
    ) {
        log.info("Iniciando onboarding con token: {}", dto.getToken());

        InvitacionTransportista invitacion = invitacionRepository
                .findByTokenInvitacion(dto.getToken())
                .orElseThrow(() -> new IllegalArgumentException("Token de invitación inválido"));

        if (!"pendiente_qr".equals(invitacion.getEstado())) {
            throw new IllegalArgumentException("Esta invitación ya fue procesada o está en otro estado");
        }

        if (invitacion.isExpirado()) {
            invitacion.setEstado("expirado");
            invitacionRepository.save(invitacion);
            throw new IllegalArgumentException("Esta invitación ha expirado");
        }

        String codigo = generarCodigoVerificacion();

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

        invitacion.setCodigoVerificacion(codigo);
        invitacion.setFechaEnvioCodigo(LocalDateTime.now());
        invitacion.setEstado("codigo_enviado");
        invitacion.setIntentosVerificacion(0);
        invitacionRepository.save(invitacion);

        // ✅ Obtener cooperativa desde la tabla intermedia
        Cooperativa cooperativa = invitacion.getPrimeraCooperativa();
        String cooperativaNombre = (cooperativa != null) ? cooperativa.getRazonSocial() : "Cooperativa";

        Map<String, Object> respuesta = new HashMap<>();
        respuesta.put("invitacionId", invitacion.getId());
        respuesta.put("primerNombre", invitacion.getPrimerNombre());
        respuesta.put("segundoNombre", invitacion.getSegundoNombre());
        respuesta.put("primerApellido", invitacion.getPrimerApellido());
        respuesta.put("segundoApellido", invitacion.getSegundoApellido());
        respuesta.put("numeroCelular", invitacion.getNumeroCelular());
        respuesta.put("cooperativaNombre", cooperativaNombre);
        respuesta.put("codigoEnviado", codigoEnviado);
        respuesta.put("mensaje", codigoEnviado ? "Código enviado" : mensajeError);

        return respuesta;
    }

    /**
     * Verificar código (sin cambios)
     */
    @Transactional
    public Map<String, Object> verificarCodigo(
            VerificarCodigoDto dto,
            String ipOrigen
    ) {
        log.info("Verificando código para token: {}", dto.getToken());

        InvitacionTransportista invitacion = invitacionRepository
                .findByTokenInvitacion(dto.getToken())
                .orElseThrow(() -> new IllegalArgumentException("Token inválido"));

        if (!"codigo_enviado".equals(invitacion.getEstado())) {
            throw new IllegalArgumentException("No hay código pendiente de verificación");
        }

        if (invitacion.getIntentosVerificacion() >= MAX_INTENTOS_VERIFICACION) {
            throw new IllegalArgumentException("Has excedido el número máximo de intentos");
        }

        if (!invitacion.isCodigoValido()) {
            throw new IllegalArgumentException("El código ha expirado");
        }

        boolean codigoCorrecto = invitacion.getCodigoVerificacion().equals(dto.getCodigo());

        if (!codigoCorrecto) {
            invitacion.setIntentosVerificacion(invitacion.getIntentosVerificacion() + 1);
            invitacionRepository.save(invitacion);
            int intentosRestantes = MAX_INTENTOS_VERIFICACION - invitacion.getIntentosVerificacion();
            throw new IllegalArgumentException("Código incorrecto. Te quedan " + intentosRestantes + " intentos.");
        }

        invitacion.setCodigoVerificado(true);
        invitacion.setEstado("verificado");
        invitacionRepository.save(invitacion);

        Map<String, Object> respuesta = new HashMap<>();
        respuesta.put("verificado", true);
        respuesta.put("mensaje", "Número verificado exitosamente");
        respuesta.put("siguientePaso", "completar_datos_vehiculo");

        return respuesta;
    }

    /**
     * Reenviar código (sin cambios)
     */
    @Transactional
    public Map<String, Object> reenviarCodigo(
            ReenviarCodigoDto dto,
            String ipOrigen
    ) {
        log.info("Reenviando código para token: {}", dto.getToken());

        InvitacionTransportista invitacion = invitacionRepository
                .findByTokenInvitacion(dto.getToken())
                .orElseThrow(() -> new IllegalArgumentException("Token inválido"));

        if (!"codigo_enviado".equals(invitacion.getEstado())) {
            throw new IllegalArgumentException("No hay código pendiente de verificación");
        }

        if (invitacion.getFechaEnvioCodigo() != null) {
            LocalDateTime limite = invitacion.getFechaEnvioCodigo().plusMinutes(1);
            if (LocalDateTime.now().isBefore(limite)) {
                throw new IllegalArgumentException("Debes esperar 1 minuto antes de solicitar un nuevo código");
            }
        }

        String nuevoCodigo = generarCodigoVerificacion();

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

        invitacion.setCodigoVerificacion(nuevoCodigo);
        invitacion.setFechaEnvioCodigo(LocalDateTime.now());
        invitacion.setIntentosVerificacion(0);
        invitacionRepository.save(invitacion);

        Map<String, Object> respuesta = new HashMap<>();
        respuesta.put("codigoEnviado", codigoEnviado);
        respuesta.put("mensaje", codigoEnviado ? "Nuevo código enviado" : "Error al enviar código");

        return respuesta;
    }

    /**
     * ✅ ACTUALIZADO: Completar onboarding
     * Sin cambios lógicos, pero ahora invitacionTransportista es NOT NULL
     */
    @Transactional
    public Map<String, Object> completarOnboarding(
            CompletarOnboardingDto dto,
            String ipOrigen
    ) {
        log.info("Completando onboarding para token: {}", dto.getToken());

        InvitacionTransportista invitacion = invitacionRepository
                .findByTokenInvitacion(dto.getToken())
                .orElseThrow(() -> new IllegalArgumentException("Token inválido"));

        if (!"verificado".equals(invitacion.getEstado())) {
            throw new IllegalArgumentException("Debes verificar tu número antes de completar el registro");
        }

        if (usuariosRepository.findByCorreo(dto.getCorreo()).isPresent()) {
            throw new IllegalArgumentException("El correo ya está registrado");
        }

        if (transportistaRepository.existsByCi(dto.getCi())) {
            throw new IllegalArgumentException("El CI ya está registrado");
        }

        if (transportistaRepository.existsByPlacaVehiculo(dto.getPlacaVehiculo())) {
            throw new IllegalArgumentException("La placa ya está registrada");
        }

        TipoUsuario tipoTransportista = tipoUsuarioRepository
                .findByTipoUsuario("transportista")
                .orElseThrow(() -> new IllegalArgumentException("Tipo de usuario no encontrado"));

        Usuarios usuario = Usuarios.builder()
                .correo(dto.getCorreo())
                .contrasena(passwordEncoder.encode(dto.getContrasena()))
                .tipoUsuarioId(tipoTransportista)
                .build();
        usuario = usuariosRepository.save(usuario);

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

        // ✅ Ahora el estado default es "aprobado" según @PrePersist
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
                .fechaAprobacion(LocalDateTime.now())
                .viajesCompletados(0)
                .calificacionPromedio(BigDecimal.ZERO)
                .invitacionTransportista(invitacion) // ✅ NOT NULL
                .build();
        transportista = transportistaRepository.save(transportista);

        invitacion.setEstado("completado");
        invitacion.setFechaAceptacion(LocalDateTime.now());
        invitacionRepository.save(invitacion);

        registrarAuditoriaOnboardingCompleto(usuario, transportista, invitacion, ipOrigen);
        
        // ✅ Notificar a TODAS las cooperativas que hicieron la invitación
        notificarCooperativasOnboardingCompleto(invitacion, usuario.getId());
        
        notificarTransportistaRegistroCompleto(usuario, invitacion.getNombreCompleto());

        Map<String, Object> respuesta = new HashMap<>();
        respuesta.put("transportistaId", transportista.getId());
        respuesta.put("usuarioId", usuario.getId());
        respuesta.put("mensaje", "Registro completado exitosamente");
        respuesta.put("correo", usuario.getCorreo());
        respuesta.put("token", jwtUtil.generateAccessToken(usuario.getId(), usuario.getCorreo(), "transportista", true));

        return respuesta;
    }

    /**
     * ✅ ACTUALIZADO: Obtener datos de invitación
     */
    @Transactional(readOnly = true)
    public Map<String, Object> obtenerDatosInvitacion(String token) {
        log.info("Obteniendo datos de invitación con token: {}", token);

        InvitacionTransportista invitacion = invitacionRepository
                .findByTokenInvitacion(token)
                .orElseThrow(() -> new IllegalArgumentException("Token inválido"));

        if (!"verificado".equals(invitacion.getEstado())) {
            throw new IllegalArgumentException("La invitación debe estar verificada");
        }

        // ✅ Obtener cooperativa desde tabla intermedia
        Cooperativa cooperativa = invitacion.getPrimeraCooperativa();
        String cooperativaNombre = (cooperativa != null) ? cooperativa.getRazonSocial() : "Cooperativa";

        Map<String, Object> respuesta = new HashMap<>();
        respuesta.put("primerNombre", invitacion.getPrimerNombre());
        respuesta.put("segundoNombre", invitacion.getSegundoNombre());
        respuesta.put("primerApellido", invitacion.getPrimerApellido());
        respuesta.put("segundoApellido", invitacion.getSegundoApellido());
        respuesta.put("numeroCelular", invitacion.getNumeroCelular());
        respuesta.put("cooperativaNombre", cooperativaNombre);

        return respuesta;
    }

    // ==================== ENDPOINTS DE TRANSPORTISTAS ====================

    /**
     * Listar transportistas con filtros y paginación (sin cambios en firma)
     * El repository ya está actualizado para usar invitacion_cooperativa
     */
    @Transactional(readOnly = true)
    public Map<String, Object> listarTransportistas(
            Integer usuarioId,
            String estado,
            String busqueda,
            Integer pagina,
            Integer tamanoPagina,
            String ordenarPor,
            String direccion
    ) {
        log.debug("Listando transportistas - Usuario: {}", usuarioId);

        Cooperativa cooperativa = obtenerCooperativaDelUsuario(usuarioId);

        String estadoParam = (estado == null || estado.trim().isEmpty()) ? "" : estado.trim();
        String busquedaParam = (busqueda == null || busqueda.trim().isEmpty()) ? "" : busqueda.trim();
        String busquedaPattern = busquedaParam.isEmpty() ? "" : "%" + busquedaParam + "%";

        String ordenarPorBD = mapearColumna(ordenarPor);
        String direccionParam = "asc".equalsIgnoreCase(direccion) ? "asc" : "desc";

        int offset = pagina * tamanoPagina;
        int limit = tamanoPagina;

        Long totalElementos = transportistaRepository.countByCooperativaWithFilters(
                cooperativa.getId(),
                estadoParam,
                busquedaParam,
                busquedaPattern
        );

        List<Transportista> transportistas = transportistaRepository.findByCooperativaWithFiltersNative(
                cooperativa.getId(),
                estadoParam,
                busquedaParam,
                busquedaPattern,
                ordenarPorBD,
                direccionParam,
                offset,
                limit
        );

        List<Map<String, Object>> transportistasDto = transportistas
                .stream()
                .map(this::convertirTransportistaADto)
                .collect(Collectors.toList());

        int totalPaginas = (int) Math.ceil((double) totalElementos / tamanoPagina);

        Map<String, Object> respuesta = new HashMap<>();
        respuesta.put("transportistas", transportistasDto);
        respuesta.put("paginaActual", pagina);
        respuesta.put("totalPaginas", totalPaginas);
        respuesta.put("totalElementos", totalElementos);
        respuesta.put("elementosPorPagina", tamanoPagina);

        return respuesta;
    }

    /**
     * ✅ ACTUALIZADO: Obtener detalle de transportista
     * Ahora valida usando invitacion_cooperativa
     */
    @Transactional(readOnly = true)
    public Map<String, Object> obtenerDetalleTransportista(
            Integer usuarioId,
            Integer transportistaId
    ) {
        log.debug("Obteniendo detalle de transportista: {}", transportistaId);

        Cooperativa cooperativa = obtenerCooperativaDelUsuario(usuarioId);

        Transportista transportista = transportistaRepository
                .findById(transportistaId)
                .orElseThrow(() -> new IllegalArgumentException("Transportista no encontrado"));

        // ✅ Validar que la cooperativa tenga relación con este transportista
        if (!transportista.trabajaConCooperativa(cooperativa.getId())) {
            throw new IllegalArgumentException("No tienes permiso para ver este transportista");
        }

        return convertirTransportistaADtoDetallado(transportista);
    }

    /**
     * ✅ ACTUALIZADO: Cambiar estado de transportista
     * Ahora valida usando invitacion_cooperativa
     */
    @Transactional
    public void cambiarEstadoTransportista(
            Integer usuarioId,
            Integer transportistaId,
            String nuevoEstado,
            String motivo,
            String ipOrigen,
            String metodoHttp,
            String endpoint
    ) {
        log.info("Cambiando estado de transportista {} a: {}", transportistaId, nuevoEstado);

        if (!List.of("aprobado", "inactivo", "en_ruta").contains(nuevoEstado)) {
            throw new IllegalArgumentException("Estado inválido: " + nuevoEstado);
        }

        Cooperativa cooperativa = obtenerCooperativaDelUsuario(usuarioId);
        Usuarios usuario = obtenerUsuario(usuarioId);

        Transportista transportista = transportistaRepository
                .findById(transportistaId)
                .orElseThrow(() -> new IllegalArgumentException("Transportista no encontrado"));

        // ✅ Validar que la cooperativa tenga relación con este transportista
        if (!transportista.trabajaConCooperativa(cooperativa.getId())) {
            throw new IllegalArgumentException("No tienes permiso para modificar este transportista");
        }

        String estadoAnterior = transportista.getEstado();

        if ("inactivo".equals(nuevoEstado) && "en_ruta".equals(estadoAnterior)) {
            throw new IllegalArgumentException("No se puede desactivar un transportista que está en ruta");
        }

        transportista.setEstado(nuevoEstado);

        if ("aprobado".equals(nuevoEstado) && transportista.getFechaAprobacion() == null) {
            transportista.setFechaAprobacion(LocalDateTime.now());
        }

        transportistaRepository.save(transportista);

        registrarAuditoriaCambioEstado(usuario, transportista, estadoAnterior, nuevoEstado, motivo, ipOrigen, metodoHttp, endpoint);
        notificarCambioEstado(transportista, nuevoEstado, motivo);
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

    /**
     * ✅ ACTUALIZADO: Validar invitación duplicada
     * Ahora recibe cooperativaId en lugar de objeto Cooperativa
     */
    private void validarInvitacionDuplicada(Integer cooperativaId, String celular) {
        Optional<InvitacionTransportista> invitacionActiva = invitacionRepository
                .findInvitacionActivaPorCelularYCooperativa(
                        cooperativaId, 
                        celular, 
                        LocalDateTime.now()
                );

        if (invitacionActiva.isPresent()) {
            throw new IllegalArgumentException("Ya existe una invitación activa para este número");
        }

        boolean tieneInvitacionReciente = invitacionRepository.existeInvitacionReciente(
                cooperativaId,
                celular,
                LocalDateTime.now().minusHours(24)
        );

        if (tieneInvitacionReciente) {
            throw new IllegalArgumentException("Ya se creó una invitación para este número en las últimas 24 horas");
        }
    }

    private String generarTokenInvitacion() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    private String generarCodigoVerificacion() {
        SecureRandom random = new SecureRandom();
        int codigo = 100000 + random.nextInt(900000);
        return String.valueOf(codigo);
    }

    /**
     * ✅ ACTUALIZADO: Crear invitación SIN cooperativaId
     */
    private InvitacionTransportista crearYGuardarInvitacion(
            TransportistaInvitacionDto dto,
            String token,
            String qrData,
            String qrCodeBase64
    ) {
        InvitacionTransportista invitacion = InvitacionTransportista.builder()
                // ❌ ELIMINADO: .cooperativaId(cooperativa)
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

    private String mapearColumna(String columna) {
        if (columna == null || columna.isEmpty()) {
            return "created_at";
        }

        switch (columna) {
            case "createdAt": return "created_at";
            case "updatedAt": return "updated_at";
            case "placaVehiculo": return "placa_vehiculo";
            case "marcaVehiculo": return "marca_vehiculo";
            case "modeloVehiculo": return "modelo_vehiculo";
            case "colorVehiculo": return "color_vehiculo";
            case "viajesCompletados": return "viajes_completados";
            case "calificacionPromedio": return "calificacion_promedio";
            case "estado": return "estado";
            case "ci": return "ci";
            default: return "created_at";
        }
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

    private Map<String, Object> convertirTransportistaADto(Transportista t) {
        Map<String, Object> dto = new HashMap<>();
        dto.put("id", t.getId());
        dto.put("ci", t.getCi());
        dto.put("estadoCuenta", t.getEstado());
        dto.put("estadoTrazabilidad", determinarEstadoTrazabilidad(t));

        if (t.getUsuariosId() != null) {
            dto.put("correo", t.getUsuariosId().getCorreo());
            personaRepository.findByUsuariosId(t.getUsuariosId()).ifPresent(persona -> {
                String nombreCompleto = construirNombreCompleto(
                        persona.getNombres(),
                        persona.getPrimerApellido(),
                        persona.getSegundoApellido()
                );
                dto.put("nombreCompleto", nombreCompleto);
                dto.put("numeroCelular", persona.getNumeroCelular());
            });
        }

        dto.put("placaVehiculo", t.getPlacaVehiculo());
        dto.put("marcaVehiculo", t.getMarcaVehiculo());
        dto.put("modeloVehiculo", t.getModeloVehiculo());
        dto.put("colorVehiculo", t.getColorVehiculo());
        dto.put("capacidadCarga", t.getCapacidadCarga());
        dto.put("viajesCompletados", t.getViajesCompletados() != null ? t.getViajesCompletados() : 0);
        dto.put("calificacionPromedio", t.getCalificacionPromedio() != null ? t.getCalificacionPromedio() : BigDecimal.ZERO);
        dto.put("fechaAprobacion", t.getFechaAprobacion());
        dto.put("createdAt", t.getCreatedAt());

        return dto;
    }

    private Map<String, Object> convertirTransportistaADtoDetallado(Transportista t) {
        Map<String, Object> dto = convertirTransportistaADto(t);
        dto.put("licenciaConducir", t.getLicenciaConducir());
        dto.put("categoriaLicencia", t.getCategoriaLicencia());
        dto.put("fechaVencimientoLicencia", t.getFechaVencimientoLicencia());
        dto.put("pesoTara", t.getPesoTara());

        if (t.getInvitacionTransportista() != null) {
            Map<String, Object> invitacion = new HashMap<>();
            invitacion.put("fechaInvitacion", t.getInvitacionTransportista().getFechaEnvio());
            invitacion.put("fechaAceptacion", t.getInvitacionTransportista().getFechaAceptacion());
            dto.put("invitacion", invitacion);
        }

        return dto;
    }

    private String determinarEstadoTrazabilidad(Transportista t) {
        if ("inactivo".equals(t.getEstado())) return "habilitado";
        if ("en_ruta".equals(t.getEstado())) return "en_ruta";
        return "habilitado";
    }

    private String construirNombreCompleto(String nombres, String primerApellido, String segundoApellido) {
        StringBuilder nombreCompleto = new StringBuilder();
        if (nombres != null && !nombres.isEmpty()) {
            nombreCompleto.append(nombres);
        }
        if (primerApellido != null && !primerApellido.isEmpty()) {
            if (nombreCompleto.length() > 0) nombreCompleto.append(" ");
            nombreCompleto.append(primerApellido);
        }
        if (segundoApellido != null && !segundoApellido.isEmpty()) {
            if (nombreCompleto.length() > 0) nombreCompleto.append(" ");
            nombreCompleto.append(segundoApellido);
        }
        return nombreCompleto.toString();
    }

    private String construirNombres(InvitacionTransportista invitacion) {
        StringBuilder nombres = new StringBuilder();
        if (invitacion.getPrimerNombre() != null) {
            nombres.append(invitacion.getPrimerNombre());
        }
        if (invitacion.getSegundoNombre() != null && !invitacion.getSegundoNombre().isEmpty()) {
            nombres.append(" ").append(invitacion.getSegundoNombre());
        }
        return nombres.toString().trim();
    }

    /**
     * ✅ ACTUALIZADO: Registrar auditoría de creación
     * Ahora incluye cooperativa como parámetro
     */
    private void registrarAuditoriaCreacion(
            Usuarios usuario,
            InvitacionTransportista invitacion,
            Cooperativa cooperativa,
            String ipOrigen,
            String metodoHttp,
            String endpoint
    ) {
        Map<String, Object> datos = Map.of(
                "invitacionId", invitacion.getId(),
                "nombreCompleto", invitacion.getNombreCompleto(),
                "numeroCelular", invitacion.getNumeroCelular(),
                "cooperativaId", cooperativa.getId(),
                "cooperativaNombre", cooperativa.getRazonSocial()
        );

        auditoriaBl.registrar(
                usuario,
                "invitacion_transportista",
                "CREATE",
                "Invitación con QR creada: " + invitacion.getNombreCompleto(),
                invitacion.getId(),
                null,
                datos,
                List.of("nombreCompleto", "numeroCelular", "cooperativaId"),
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

    /**
     * ✅ NUEVO: Notificar a TODAS las cooperativas que hicieron la invitación
     */
    private void notificarCooperativasOnboardingCompleto(
            InvitacionTransportista invitacion,
            Integer transportistaUsuarioId
    ) {
        // Obtener todas las cooperativas relacionadas con esta invitación
        List<InvitacionCooperativa> invitacionesCooperativa = 
                invitacionCooperativaRepository.findByInvitacionTransportista(invitacion);

        for (InvitacionCooperativa ic : invitacionesCooperativa) {
            Cooperativa cooperativa = ic.getCooperativa();
            Usuarios usuarioCooperativa = cooperativa.getUsuariosId();

            Map<String, Object> metadata = Map.of(
                    "tipo", "nuevo_transportista",
                    "nombreTransportista", invitacion.getNombreCompleto(),
                    "transportistaUsuarioId", transportistaUsuarioId,
                    "cooperativaId", cooperativa.getId()
            );

            notificacionBl.crearNotificacion(
                    usuarioCooperativa.getId(),
                    "success",
                    "Nuevo transportista registrado",
                    invitacion.getNombreCompleto() + " completó su registro como transportista",
                    metadata
            );
        }
    }

    private void notificarTransportistaRegistroCompleto(Usuarios usuario, String nombreCompleto) {
        Map<String, Object> metadata = Map.of(
                "tipo", "registro_completado",
                "usuarioId", usuario.getId()
        );

        notificacionBl.crearNotificacion(
                usuario.getId(),
                "success",
                "¡Bienvenido a SumajFlow!",
                "Tu registro como transportista se completó exitosamente. Ya puedes iniciar sesión y comenzar a trabajar.",
                metadata
        );
    }

    private void registrarAuditoriaCambioEstado(
            Usuarios usuario,
            Transportista transportista,
            String estadoAnterior,
            String estadoNuevo,
            String motivo,
            String ipOrigen,
            String metodoHttp,
            String endpoint
    ) {
        Map<String, Object> datosAnteriores = Map.of("estado", estadoAnterior);
        Map<String, Object> datosNuevos = Map.of("estado", estadoNuevo, "motivo", motivo);

        auditoriaBl.registrar(
                usuario,
                "transportista",
                "UPDATE",
                "Cambio de estado: " + estadoAnterior + " → " + estadoNuevo,
                transportista.getId(),
                datosAnteriores,
                datosNuevos,
                List.of("estado"),
                ipOrigen,
                null,
                metodoHttp,
                endpoint,
                "RF_TRANSP_ESTADO",
                "MEDIO"
        );
    }

    private void notificarCambioEstado(Transportista transportista, String nuevoEstado, String motivo) {
        Map<String, Object> metadata = Map.of(
                "tipo", "cambio_estado",
                "estadoNuevo", nuevoEstado,
                "motivo", motivo
        );

        String mensaje = switch (nuevoEstado) {
            case "aprobado" -> "Tu cuenta ha sido activada. Ya puedes recibir asignaciones.";
            case "inactivo" -> "Tu cuenta ha sido desactivada. " + (!motivo.isEmpty() ? "Motivo: " + motivo : "");
            case "en_ruta" -> "Estás en ruta. Completa tu viaje actual.";
            default -> "Tu estado ha cambiado a: " + nuevoEstado;
        };

        notificacionBl.crearNotificacion(
                transportista.getUsuariosId().getId(),
                nuevoEstado.equals("inactivo") ? "warning" : "info",
                "Cambio de estado",
                mensaje,
                metadata
        );
    }
}