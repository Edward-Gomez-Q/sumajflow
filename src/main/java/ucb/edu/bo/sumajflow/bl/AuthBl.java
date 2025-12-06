package ucb.edu.bo.sumajflow.bl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ucb.edu.bo.sumajflow.dto.*;
import ucb.edu.bo.sumajflow.dto.login.LoginResponseDto;
import ucb.edu.bo.sumajflow.dto.login.UserInfoDto;
import ucb.edu.bo.sumajflow.entity.*;
import ucb.edu.bo.sumajflow.repository.*;
import ucb.edu.bo.sumajflow.utils.JwtUtil;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthBl {

    // Inyección de dependencias con @RequiredArgsConstructor
    private final UsuariosRepository usuariosRepository;
    private final TipoUsuarioRepository tipoUsuarioRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final PersonaRepository personaRepository;
    private final CooperativaRepository cooperativaRepository;
    private final SectoresRepository sectoresRepository;
    private final SectoresCoordenadasRepository sectoresCoordenadasRepository;
    private final BalanzaCooperativaRepository balanzaCooperativaRepository;
    private final SocioRepository socioRepository;
    private final CooperativaSocioRepository cooperativaSocioRepository;
    private final IngenioMineroRepository ingenioMineroRepository;
    private final PlantaRepository plantaRepository;
    private final BalanzaIngenioRepository balanzaIngenioRepository;
    private final AlmacenIngenioRepository almacenIngenioRepository;
    private final MineralesRepository mineralesRepository;
    private final ProcesosRepository procesosRepository;
    private final PlantaMineralesRepository plantaMineralesRepository;
    private final ProcesosPlantaRepository procesosPlantaRepository;
    private final ComercializadoraRepository comercializadoraRepository;
    private final BalanzaComercializadoraRepository balanzaComercializadoraRepository;
    private final AlmacenComercializadoraRepository almacenComercializadoraRepository;
    private final JwtUtil jwtUtil;
    private final AuditoriaBl auditoriaBl;
    private final NotificacionBl notificacionBl;

    /**
     * Método de login con contexto HTTP
     */
    @Transactional
    public LoginResponseDto login(String email, String password, String ipOrigen, String userAgent) {
        log.info("Intento de login - Email: {}, IP: {}", email, ipOrigen);

        // 1. Buscar usuario por correo
        Usuarios usuario = usuariosRepository.findByCorreo(email)
                .orElseThrow(() -> new IllegalArgumentException("Credenciales inválidas"));

        // 2. Verificar contraseña
        if (!passwordEncoder.matches(password, usuario.getContrasena())) {
            log.warn("Intento de login fallido - Email: {}, IP: {}", email, ipOrigen);
            throw new IllegalArgumentException("Credenciales inválidas");
        }

        // 3. Obtener datos de persona
        Persona persona = personaRepository.findByUsuariosId(usuario)
                .orElseThrow(() -> new IllegalArgumentException("Datos de persona no encontrados"));

        // 4. Obtener rol y estado de aprobación
        String rol = usuario.getTipoUsuarioId().getTipoUsuario();
        Boolean aprobado = verificarAprobacion(usuario, rol);

        // 5. Generar tokens
        String accessToken = jwtUtil.generateAccessToken(usuario.getId(), usuario.getCorreo(), rol, aprobado);
        String refreshToken = jwtUtil.generateRefreshToken(usuario.getId(), usuario.getCorreo());

        // 6. Crear UserInfoDto
        UserInfoDto userInfo = crearUserInfo(usuario, persona, rol);

        // 7. Registrar en auditoría CON CONTEXTO HTTP
        auditoriaBl.registrarLogin(usuario, ipOrigen, userAgent);

        log.info("Login exitoso - Usuario ID: {}, Rol: {}", usuario.getId(), rol);

        // 8. Retornar respuesta
        return new LoginResponseDto(accessToken, refreshToken, userInfo);
    }

    /**
     * Método para refrescar el token
     */
    @Transactional
    public LoginResponseDto refreshToken(String refreshToken) {
        log.debug("Refrescando token");

        // 1. Validar refresh token
        if (!jwtUtil.validateToken(refreshToken) || !jwtUtil.isRefreshToken(refreshToken)) {
            throw new IllegalArgumentException("Refresh token inválido o expirado");
        }

        // 2. Extraer información del token
        String correo = jwtUtil.extractCorreo(refreshToken);
        Integer usuarioId = jwtUtil.extractUsuarioId(refreshToken);

        // 3. Buscar usuario
        Usuarios usuario = usuariosRepository.findById(usuarioId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        // 4. Verificar que el correo coincida
        if (!usuario.getCorreo().equals(correo)) {
            throw new IllegalArgumentException("Token inválido");
        }

        // 5. Obtener datos de persona
        Persona persona = personaRepository.findByUsuariosId(usuario)
                .orElseThrow(() -> new IllegalArgumentException("Datos de persona no encontrados"));

        // 6. Obtener rol y estado de aprobación
        String rol = usuario.getTipoUsuarioId().getTipoUsuario();
        Boolean aprobado = verificarAprobacion(usuario, rol);

        // 7. Generar nuevos tokens
        String newAccessToken = jwtUtil.generateAccessToken(usuario.getId(), usuario.getCorreo(), rol, aprobado);
        String newRefreshToken = jwtUtil.generateRefreshToken(usuario.getId(), usuario.getCorreo());

        // 8. Crear UserInfoDto
        UserInfoDto userInfo = crearUserInfo(usuario, persona, rol);

        log.info("Token refrescado - Usuario ID: {}", usuario.getId());

        // 9. Retornar respuesta
        return new LoginResponseDto(newAccessToken, newRefreshToken, userInfo);
    }

    /**
     * Método principal para procesar el onboarding según el tipo de usuario
     */
    @Transactional
    public Usuarios processOnBoarding(OnBoardingDto onBoardingDto, String ipOrigen, String userAgent) {
        String tipoUsuario = onBoardingDto.getTipoUsuario();

        log.info("Procesando onboarding - Tipo: {}, IP: {}", tipoUsuario, ipOrigen);

        if (tipoUsuario == null) {
            throw new IllegalArgumentException("No se pudo determinar el tipo de usuario");
        }

        return switch (tipoUsuario) {
            case "cooperativa" -> registerCooperativa(onBoardingDto, ipOrigen, userAgent);
            case "socio" -> registerSocio(onBoardingDto, ipOrigen, userAgent);
            case "ingenio" -> registerIngenio(onBoardingDto, ipOrigen, userAgent);
            case "comercializadora" -> registerComercializadora(onBoardingDto, ipOrigen, userAgent);
            default -> throw new IllegalArgumentException("Tipo de usuario no válido: " + tipoUsuario);
        };
    }

    /**
     * Registra una cooperativa completa con todos sus datos relacionados
     */
    @Transactional
    public Usuarios registerCooperativa(OnBoardingDto dto, String ipOrigen, String userAgent) {
        log.info("Registrando cooperativa - Email: {}", dto.getUsuario().getCorreo());

        // 1. Buscar o crear tipo de usuario
        TipoUsuario tipoUsuario = findOrCreateTipoUsuario("cooperativa");

        // 2. Crear usuario
        Usuarios usuario = createUsuario(dto.getUsuario(), tipoUsuario);

        // 3. Crear persona
        createPersona(dto.getPersona(), usuario);

        // 4. Crear cooperativa
        Cooperativa cooperativa = createCooperativa(dto.getCooperativa(), usuario);

        // 5. Crear sectores con coordenadas
        if (dto.getCooperativa().getSectores() != null) {
            dto.getCooperativa().getSectores().forEach(sectorDto -> {
                Sectores sector = createSector(sectorDto, cooperativa);
                if (sectorDto.getCoordenadas() != null) {
                    sectorDto.getCoordenadas().forEach(coordDto ->
                            createSectorCoordenada(coordDto, sector));
                }
            });
        }

        // 6. Crear balanza
        if (dto.getCooperativa().getBalanza() != null) {
            createBalanzaCooperativa(dto.getCooperativa().getBalanza(), cooperativa);
        }

        // 7. Registrar en auditoría CON CONTEXTO HTTP
        auditoriaBl.registrarRegistro(usuario, "cooperativa", ipOrigen, userAgent);

        // 8. Enviar notificación de bienvenida
        enviarNotificacionBienvenidaCooperativa(usuario, cooperativa);

        log.info("Cooperativa registrada exitosamente - ID: {}", cooperativa.getId());
        return usuario;
    }

    /**
     * Registra un socio completo con todos sus datos relacionados
     */
    @Transactional
    public Usuarios registerSocio(OnBoardingDto dto, String ipOrigen, String userAgent) {
        log.info("Registrando socio - Email: {}", dto.getUsuario().getCorreo());

        // 1. Buscar o crear tipo de usuario
        TipoUsuario tipoUsuario = findOrCreateTipoUsuario("socio");

        // 2. Crear usuario
        Usuarios usuario = createUsuario(dto.getUsuario(), tipoUsuario);

        // 3. Crear persona
        Persona persona = createPersona(dto.getPersona(), usuario);

        // 4. Crear socio
        Socio socio = createSocio(dto.getSocio(), usuario);

        // 5. Crear relación cooperativa-socio
        CooperativaSocio cooperativaSocio = createCooperativaSocio(dto.getSocio(), socio);

        // 6. Registrar en auditoría CON CONTEXTO HTTP
        auditoriaBl.registrarRegistro(usuario, "socio", ipOrigen, userAgent);

        // 7. Notificar a la cooperativa
        enviarNotificacionSolicitudSocio(cooperativaSocio, persona, usuario, socio);

        // 8. Notificar al socio
        enviarNotificacionSocioEnviada(usuario, cooperativaSocio, socio);

        log.info("Socio registrado exitosamente - ID: {}", socio.getId());
        return usuario;
    }

    /**
     * Registra un ingenio minero completo con todos sus datos relacionados
     */
    @Transactional
    public Usuarios registerIngenio(OnBoardingDto dto, String ipOrigen, String userAgent) {
        log.info("Registrando ingenio - Email: {}", dto.getUsuario().getCorreo());

        // 1. Buscar o crear tipo de usuario
        TipoUsuario tipoUsuario = findOrCreateTipoUsuario("ingenio");

        // 2. Crear usuario
        Usuarios usuario = createUsuario(dto.getUsuario(), tipoUsuario);

        // 3. Crear persona
        createPersona(dto.getPersona(), usuario);

        // 4. Crear ingenio minero
        IngenioMinero ingenio = createIngenioMinero(dto.getIngenio(), usuario);

        // 5. Crear planta
        int numeroAlmacenes = 0;
        if (dto.getIngenio().getPlanta() != null) {
            Planta planta = createPlanta(dto.getIngenio().getPlanta(), ingenio);

            // 5.1 Crear relaciones planta-minerales
            if (dto.getIngenio().getPlanta().getMinerales() != null) {
                dto.getIngenio().getPlanta().getMinerales()
                        .forEach(mineralId -> createPlantaMineral(planta, mineralId));
            }

            // 5.2 Crear relaciones planta-procesos
            if (dto.getIngenio().getPlanta().getProcesos() != null) {
                dto.getIngenio().getPlanta().getProcesos()
                        .forEach(procesoDto -> createPlantaProceso(planta, procesoDto.getId()));
            }
        }

        // 6. Crear balanza
        if (dto.getIngenio().getBalanza() != null) {
            createBalanzaIngenio(dto.getIngenio().getBalanza(), ingenio);
        }

        // 7. Crear almacenes
        if (dto.getIngenio().getAlmacenes() != null) {
            numeroAlmacenes = dto.getIngenio().getAlmacenes().size();
            dto.getIngenio().getAlmacenes()
                    .forEach(almacenDto -> createAlmacenIngenio(almacenDto, ingenio));
        }

        // 8. Registrar en auditoría CON CONTEXTO HTTP
        auditoriaBl.registrarRegistro(usuario, "ingenio", ipOrigen, userAgent);

        // 9. Enviar notificación de bienvenida
        enviarNotificacionBienvenidaIngenio(usuario, ingenio, numeroAlmacenes);

        log.info("Ingenio registrado exitosamente - ID: {}", ingenio.getId());
        return usuario;
    }

    /**
     * Registra una comercializadora completa con todos sus datos relacionados
     */
    @Transactional
    public Usuarios registerComercializadora(OnBoardingDto dto, String ipOrigen, String userAgent) {
        log.info("Registrando comercializadora - Email: {}", dto.getUsuario().getCorreo());

        // 1. Buscar o crear tipo de usuario
        TipoUsuario tipoUsuario = findOrCreateTipoUsuario("comercializadora");

        // 2. Crear usuario
        Usuarios usuario = createUsuario(dto.getUsuario(), tipoUsuario);

        // 3. Crear persona
        createPersona(dto.getPersona(), usuario);

        // 4. Crear comercializadora
        Comercializadora comercializadora = createComercializadora(dto.getComercializadora(), usuario);

        // 5. Crear almacenes
        int numeroAlmacenes = 0;
        if (dto.getComercializadora().getAlmacenes() != null) {
            numeroAlmacenes = dto.getComercializadora().getAlmacenes().size();
            dto.getComercializadora().getAlmacenes()
                    .forEach(almacenDto -> createAlmacenComercializadora(almacenDto, comercializadora));
        }

        // 6. Crear balanza
        if (dto.getComercializadora().getBalanza() != null) {
            createBalanzaComercializadora(dto.getComercializadora().getBalanza(), comercializadora);
        }

        // 7. Registrar en auditoría CON CONTEXTO HTTP
        auditoriaBl.registrarRegistro(usuario, "comercializadora", ipOrigen, userAgent);

        // 8. Enviar notificación de bienvenida
        enviarNotificacionBienvenidaComercializadora(usuario, comercializadora, numeroAlmacenes);

        log.info("Comercializadora registrada exitosamente - ID: {}", comercializadora.getId());
        return usuario;
    }

    // ==================== MÉTODOS AUXILIARES DE CREACIÓN ====================

    private TipoUsuario findOrCreateTipoUsuario(String tipo) {
        return tipoUsuarioRepository.findByTipoUsuario(tipo)
                .orElseGet(() -> tipoUsuarioRepository.save(
                        TipoUsuario.builder()
                                .tipoUsuario(tipo)
                                .build()
                ));
    }

    private Usuarios createUsuario(UsuarioDto dto, TipoUsuario tipoUsuario) {
        return usuariosRepository.save(
                Usuarios.builder()
                        .correo(dto.getCorreo())
                        .contrasena(passwordEncoder.encode(dto.getContrasena()))
                        .tipoUsuarioId(tipoUsuario)
                        .build()
        );
    }

    private Persona createPersona(PersonaDto dto, Usuarios usuario) {
        return personaRepository.save(
                Persona.builder()
                        .nombres(dto.getNombres())
                        .primerApellido(dto.getPrimerApellido())
                        .segundoApellido(dto.getSegundoApellido())
                        .ci(dto.getCi())
                        .fechaNacimiento(dto.getFechaNacimiento())
                        .numeroCelular(dto.getNumeroCelular())
                        .genero(dto.getGenero())
                        .nacionalidad(dto.getNacionalidad())
                        .departamento(dto.getDepartamento())
                        .provincia(dto.getProvincia())
                        .municipio(dto.getMunicipio())
                        .direccion(dto.getDireccion())
                        .latitud(dto.getLatitud())
                        .longitud(dto.getLongitud())
                        .usuariosId(usuario)
                        .build()
        );
    }

    private Cooperativa createCooperativa(CooperativaDto dto, Usuarios usuario) {
        return cooperativaRepository.save(
                Cooperativa.builder()
                        .razonSocial(dto.getRazonSocial())
                        .nit(dto.getNit())
                        .nim(dto.getNim())
                        .correoContacto(dto.getCorreoContacto())
                        .numeroTelefonoFijo(dto.getNumeroTelefonoFijo())
                        .numeroTelefonoMovil(dto.getNumeroTelefonoMovil())
                        .departamento(dto.getDepartamento())
                        .provincia(dto.getProvincia())
                        .municipio(dto.getMunicipio())
                        .direccion(dto.getDireccion())
                        .latitud(dto.getLatitud())
                        .longitud(dto.getLongitud())
                        .usuariosId(usuario)
                        .build()
        );
    }

    private Sectores createSector(SectorDto dto, Cooperativa cooperativa) {
        return sectoresRepository.save(
                Sectores.builder()
                        .nombre(dto.getNombre())
                        .color(dto.getColor())
                        .estado("activo")
                        .cooperativaId(cooperativa)
                        .build()
        );
    }

    private SectoresCoordenadas createSectorCoordenada(CoordenadaDto dto, Sectores sector) {
        return sectoresCoordenadasRepository.save(
                SectoresCoordenadas.builder()
                        .orden(dto.getOrden())
                        .latitud(dto.getLatitud())
                        .longitud(dto.getLongitud())
                        .sectoresId(sector)
                        .build()
        );
    }

    private BalanzaCooperativa createBalanzaCooperativa(BalanzaDto dto, Cooperativa cooperativa) {
        return balanzaCooperativaRepository.save(
                BalanzaCooperativa.builder()
                        .nombre(dto.getNombre())
                        .marca(dto.getMarca())
                        .modelo(dto.getModelo())
                        .numeroSerie(dto.getNumeroSerie())
                        .capacidadMaxima(dto.getCapacidadMaxima())
                        .precisionMinima(dto.getPrecisionMinima())
                        .fechaUltimaCalibracion(dto.getFechaUltimaCalibracion())
                        .fechaProximaCalibracion(dto.getFechaProximaCalibracion())
                        .departamento(dto.getDepartamento())
                        .provincia(dto.getProvincia())
                        .municipio(dto.getMunicipio())
                        .direccion(dto.getDireccion())
                        .latitud(dto.getLatitud())
                        .longitud(dto.getLongitud())
                        .cooperativaId(cooperativa)
                        .build()
        );
    }

    private Socio createSocio(SocioDto dto, Usuarios usuario) {
        return socioRepository.save(
                Socio.builder()
                        .fechaEnvio(LocalDateTime.now())
                        .estado("pendiente")
                        .carnetAfiliacionUrl(dto.getCarnetAfiliacionUrl())
                        .carnetIdentidadUrl(dto.getCarnetIdentidadUrl())
                        .usuariosId(usuario)
                        .build()
        );
    }

    private CooperativaSocio createCooperativaSocio(SocioDto dto, Socio socio) {
        Cooperativa cooperativa = cooperativaRepository.findById(dto.getCooperativaId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Cooperativa no encontrada con ID: " + dto.getCooperativaId()));

        return cooperativaSocioRepository.save(
                CooperativaSocio.builder()
                        .cooperativaId(cooperativa)
                        .socioId(socio)
                        .fechaAfiliacion(dto.getFechaAfiliacion())
                        .estado("pendiente")
                        .observaciones("Solicitud de afiliación pendiente de aprobación")
                        .build()
        );
    }

    private IngenioMinero createIngenioMinero(IngenioDto dto, Usuarios usuario) {
        return ingenioMineroRepository.save(
                IngenioMinero.builder()
                        .razonSocial(dto.getRazonSocial())
                        .nit(dto.getNit())
                        .nim(dto.getNim())
                        .correoContacto(dto.getCorreoContacto())
                        .numeroTelefonoFijo(dto.getNumeroTelefonoFijo())
                        .numeroTelefonoMovil(dto.getNumeroTelefonoMovil())
                        .departamento(dto.getDepartamento())
                        .provincia(dto.getProvincia())
                        .municipio(dto.getMunicipio())
                        .direccion(dto.getDireccion())
                        .latitud(dto.getLatitud())
                        .longitud(dto.getLongitud())
                        .usuariosId(usuario)
                        .build()
        );
    }

    private Planta createPlanta(PlantaDto dto, IngenioMinero ingenio) {
        return plantaRepository.save(
                Planta.builder()
                        .cupoMinimo(dto.getCupoMinimo())
                        .capacidadProcesamiento(dto.getCapacidadProcesamiento())
                        .costoProcesamiento(dto.getCostoProcesamiento())
                        .licenciaAmbientalUrl(dto.getLicenciaAmbientalUrl())
                        .departamento(dto.getDepartamento())
                        .provincia(dto.getProvincia())
                        .municipio(dto.getMunicipio())
                        .direccion(dto.getDireccion())
                        .latitud(dto.getLatitud())
                        .longitud(dto.getLongitud())
                        .ingenioMineroId(ingenio)
                        .build()
        );
    }

    private PlantaMinerales createPlantaMineral(Planta planta, Integer mineralId) {
        Minerales mineral = mineralesRepository.findById(mineralId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Mineral no encontrado con ID: " + mineralId));

        return plantaMineralesRepository.save(
                PlantaMinerales.builder()
                        .plantaId(planta)
                        .mineralesId(mineral)
                        .build()
        );
    }

    private ProcesosPlanta createPlantaProceso(Planta planta, Integer procesoId) {
        Procesos proceso = procesosRepository.findById(procesoId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Proceso no encontrado con ID: " + procesoId));

        return procesosPlantaRepository.save(
                ProcesosPlanta.builder()
                        .plantaId(planta)
                        .procesosId(proceso)
                        .build()
        );
    }

    private BalanzaIngenio createBalanzaIngenio(BalanzaDto dto, IngenioMinero ingenio) {
        return balanzaIngenioRepository.save(
                BalanzaIngenio.builder()
                        .nombre(dto.getNombre())
                        .marca(dto.getMarca())
                        .modelo(dto.getModelo())
                        .numeroSerie(dto.getNumeroSerie())
                        .capacidadMaxima(dto.getCapacidadMaxima())
                        .precisionMinima(dto.getPrecisionMinima())
                        .fechaUltimaCalibracion(dto.getFechaUltimaCalibracion())
                        .fechaProximaCalibracion(dto.getFechaProximaCalibracion())
                        .departamento(dto.getDepartamento())
                        .provincia(dto.getProvincia())
                        .municipio(dto.getMunicipio())
                        .direccion(dto.getDireccion())
                        .latitud(dto.getLatitud())
                        .longitud(dto.getLongitud())
                        .ingenioMineroId(ingenio)
                        .build()
        );
    }

    private AlmacenIngenio createAlmacenIngenio(AlmacenDto dto, IngenioMinero ingenio) {
        return almacenIngenioRepository.save(
                AlmacenIngenio.builder()
                        .nombre(dto.getNombre())
                        .capacidadMaxima(dto.getCapacidadMaxima())
                        .area(dto.getArea())
                        .departamento(dto.getDepartamento())
                        .provincia(dto.getProvincia())
                        .municipio(dto.getMunicipio())
                        .direccion(dto.getDireccion())
                        .latitud(dto.getLatitud())
                        .longitud(dto.getLongitud())
                        .ingenioMineroId(ingenio)
                        .build()
        );
    }

    private Comercializadora createComercializadora(ComercializadoraDto dto, Usuarios usuario) {
        return comercializadoraRepository.save(
                Comercializadora.builder()
                        .razonSocial(dto.getRazonSocial())
                        .nit(dto.getNit())
                        .nim(dto.getNim())
                        .correoContacto(dto.getCorreoContacto())
                        .numeroTelefonoFijo(dto.getNumeroTelefonoFijo())
                        .numeroTelefonoMovil(dto.getNumeroTelefonoMovil())
                        .departamento(dto.getDepartamento())
                        .provincia(dto.getProvincia())
                        .municipio(dto.getMunicipio())
                        .direccion(dto.getDireccion())
                        .latitud(dto.getLatitud())
                        .longitud(dto.getLongitud())
                        .usuariosId(usuario)
                        .build()
        );
    }

    private AlmacenComercializadora createAlmacenComercializadora(AlmacenDto dto, Comercializadora comercializadora) {
        return almacenComercializadoraRepository.save(
                AlmacenComercializadora.builder()
                        .nombre(dto.getNombre())
                        .capacidadMaxima(dto.getCapacidadMaxima())
                        .area(dto.getArea())
                        .departamento(dto.getDepartamento())
                        .provincia(dto.getProvincia())
                        .municipio(dto.getMunicipio())
                        .direccion(dto.getDireccion())
                        .latitud(dto.getLatitud())
                        .longitud(dto.getLongitud())
                        .comercializadoraId(comercializadora)
                        .build()
        );
    }

    private BalanzaComercializadora createBalanzaComercializadora(BalanzaDto dto, Comercializadora comercializadora) {
        return balanzaComercializadoraRepository.save(
                BalanzaComercializadora.builder()
                        .nombre(dto.getNombre())
                        .marca(dto.getMarca())
                        .modelo(dto.getModelo())
                        .numeroSerie(dto.getNumeroSerie())
                        .capacidadMaxima(dto.getCapacidadMaxima())
                        .precisionMinima(dto.getPrecisionMinima())
                        .fechaUltimaCalibracion(dto.getFechaUltimaCalibracion())
                        .fechaProximaCalibracion(dto.getFechaProximaCalibracion())
                        .departamento(dto.getDepartamento())
                        .provincia(dto.getProvincia())
                        .municipio(dto.getMunicipio())
                        .direccion(dto.getDireccion())
                        .latitud(dto.getLatitud())
                        .longitud(dto.getLongitud())
                        .comercializadoraId(comercializadora)
                        .build()
        );
    }

    // ==================== MÉTODOS AUXILIARES DE LÓGICA ====================

    private Boolean verificarAprobacion(Usuarios usuario, String rol) {
        if ("socio".equals(rol)) {
            Socio socio = socioRepository.findByUsuariosId(usuario)
                    .orElseThrow(() -> new IllegalArgumentException("Datos de socio no encontrados"));
            return "aprobado".equals(socio.getEstado());
        }
        return true;
    }

    private UserInfoDto crearUserInfo(Usuarios usuario, Persona persona, String rol) {
        return new UserInfoDto(
                usuario.getId(),
                usuario.getCorreo(),
                rol,
                persona.getNombres(),
                persona.getPrimerApellido(),
                persona.getSegundoApellido()
        );
    }

    // ==================== MÉTODOS DE NOTIFICACIONES ====================

    private void enviarNotificacionBienvenidaCooperativa(Usuarios usuario, Cooperativa cooperativa) {
        Map<String, Object> metadata = Map.of(
                "cooperativaId", cooperativa.getId(),
                "razonSocial", cooperativa.getRazonSocial()
        );

        notificacionBl.crearNotificacion(
                usuario.getId(),
                "success",
                "¡Bienvenido a SumajFlow!",
                "Tu cuenta de cooperativa ha sido creada exitosamente. Ahora puedes gestionar tus operaciones mineras.",
                metadata
        );
    }

    private void enviarNotificacionSolicitudSocio(
            CooperativaSocio cooperativaSocio,
            Persona persona,
            Usuarios usuario,
            Socio socio
    ) {
        Integer cooperativaUsuarioId = cooperativaSocio.getCooperativaId().getUsuariosId().getId();

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("tipo", "nueva_solicitud_socio");
        metadata.put("socioId", socio.getId());
        metadata.put("solicitudId", socio.getId().toString());
        metadata.put("cooperativaSocioId", cooperativaSocio.getId());
        metadata.put("nombreCompleto", persona.getNombres() + " " + persona.getPrimerApellido());
        metadata.put("nombres", persona.getNombres());
        metadata.put("primerApellido", persona.getPrimerApellido());
        metadata.put("segundoApellido", persona.getSegundoApellido() != null ? persona.getSegundoApellido() : "");
        metadata.put("ci", persona.getCi());
        metadata.put("correo", usuario.getCorreo());
        metadata.put("estado", "pendiente");
        metadata.put("fechaEnvio", socio.getFechaEnvio().toString());

        notificacionBl.crearNotificacion(
                cooperativaUsuarioId,
                "info",
                "Nueva solicitud de socio",
                persona.getNombres() + " " + persona.getPrimerApellido() +
                        " ha solicitado unirse a tu cooperativa",
                metadata
        );
    }

    private void enviarNotificacionSocioEnviada(
            Usuarios usuario,
            CooperativaSocio cooperativaSocio,
            Socio socio
    ) {
        Map<String, Object> metadata = Map.of(
                "socioId", socio.getId(),
                "cooperativaId", cooperativaSocio.getCooperativaId().getId(),
                "cooperativaNombre", cooperativaSocio.getCooperativaId().getRazonSocial(),
                "estado", "pendiente"
        );

        notificacionBl.crearNotificacion(
                usuario.getId(),
                "info",
                "Solicitud enviada",
                "Tu solicitud para unirte a " + cooperativaSocio.getCooperativaId().getRazonSocial() +
                        " está en revisión. Te notificaremos cuando sea procesada.",
                metadata
        );
    }

    private void enviarNotificacionBienvenidaIngenio(
            Usuarios usuario,
            IngenioMinero ingenio,
            int numeroAlmacenes
    ) {
        Map<String, Object> metadata = Map.of(
                "ingenioId", ingenio.getId(),
                "razonSocial", ingenio.getRazonSocial(),
                "nPlanta", 1,
                "nAlmacenes", numeroAlmacenes
        );

        notificacionBl.crearNotificacion(
                usuario.getId(),
                "success",
                "¡Bienvenido a SumajFlow!",
                "Tu ingenio minero ha sido registrado exitosamente. Puedes comenzar a gestionar el procesamiento de minerales.",
                metadata
        );
    }

    private void enviarNotificacionBienvenidaComercializadora(
            Usuarios usuario,
            Comercializadora comercializadora,
            int numeroAlmacenes
    ) {
        Map<String, Object> metadata = Map.of(
                "comercializadoraId", comercializadora.getId(),
                "razonSocial", comercializadora.getRazonSocial(),
                "nAlmacenes", numeroAlmacenes
        );

        notificacionBl.crearNotificacion(
                usuario.getId(),
                "success",
                "¡Bienvenido a SumajFlow!",
                "Tu comercializadora ha sido registrada exitosamente. Ya puedes comenzar a gestionar la compra y venta de minerales.",
                metadata
        );
    }
}