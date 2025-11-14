package ucb.edu.bo.sumajflow.bl;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ucb.edu.bo.sumajflow.dto.BalanzaDto;
import ucb.edu.bo.sumajflow.dto.OnBoardingDto;
import ucb.edu.bo.sumajflow.dto.PersonaDto;
import ucb.edu.bo.sumajflow.entity.*;
import ucb.edu.bo.sumajflow.repository.*;

import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDate;

@Service
public class AuthBl {

    private final UsuariosRepository usuariosRepository;
    private final TipoUsuarioRepository tipoUsuarioRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final PersonaRepository personaRepository;
    private final AuditoriaRepository auditoriaRepository;
    private final CooperativaRepository cooperativaRepository;
    private final SectoresRepository sectoresRepository;
    private final SectoresCoordenadasRepository sectoresCoordenadasRepository;
    private final BalanzaCooperativaRepository balanzaCooperativaRepository;
    private final SocioRepository socioRepository;
    private final CooperativaSocioRepository cooperativaSocioRepository;

    public AuthBl(
            UsuariosRepository usuariosRepository,
            TipoUsuarioRepository tipoUsuarioRepository,
            BCryptPasswordEncoder passwordEncoder,
            PersonaRepository personaRepository,
            AuditoriaRepository auditoriaRepository,
            CooperativaRepository cooperativaRepository,
            SectoresRepository sectoresRepository,
            SectoresCoordenadasRepository sectoresCoordenadasRepository,
            BalanzaCooperativaRepository balanzaCooperativaRepository,
            SocioRepository socioRepository,
            CooperativaSocioRepository cooperativaSocioRepository
    ) {
        this.usuariosRepository = usuariosRepository;
        this.tipoUsuarioRepository = tipoUsuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.personaRepository = personaRepository;
        this.auditoriaRepository = auditoriaRepository;
        this.cooperativaRepository = cooperativaRepository;
        this.sectoresRepository = sectoresRepository;
        this.sectoresCoordenadasRepository = sectoresCoordenadasRepository;
        this.balanzaCooperativaRepository = balanzaCooperativaRepository;
        this.socioRepository = socioRepository;
        this.cooperativaSocioRepository = cooperativaSocioRepository;
    }

    /**
     * Método principal para procesar el onboarding según el tipo de usuario
     */
    @Transactional
    public Usuarios processOnBoarding(OnBoardingDto onBoardingDto) {
        String tipoUsuario = onBoardingDto.getTipoUsuario();

        if (tipoUsuario == null) {
            throw new IllegalArgumentException("No se pudo determinar el tipo de usuario");
        }

        switch (tipoUsuario) {
            case "cooperativa":
                return registerCooperativa(onBoardingDto);
            case "socio":
                return registerSocio(onBoardingDto);
            case "ingenio":
                return registerIngenio(onBoardingDto);
            case "comercializadora":
                return registerComercializadora(onBoardingDto);
            default:
                throw new IllegalArgumentException("Tipo de usuario no válido: " + tipoUsuario);
        }
    }

    /**
     * Registra una cooperativa completa con todos sus datos relacionados
     */
    @Transactional
    public Usuarios registerCooperativa(OnBoardingDto dto) {
        // 1. Buscar o crear tipo de usuario
        TipoUsuario tipoUsuario = findOrCreateTipoUsuario("cooperativa");

        // 2. Crear usuario
        Usuarios usuario = createUsuario(dto.getUsuario(), tipoUsuario);

        // 3. Crear persona
        Persona persona = createPersona(dto.getPersona(), usuario);

        // 4. Registrar en auditoría
        registrarAuditoria(usuario, "usuarios", "INSERT",
                "Registro de nuevo usuario tipo cooperativa: " + usuario.getCorreo());

        // 5. Crear cooperativa
        Cooperativa cooperativa = createCooperativa(dto.getCooperativa(), usuario);

        // 6. Crear sectores con coordenadas
        if (dto.getCooperativa().getSectores() != null) {
            dto.getCooperativa().getSectores().forEach(sectorDto -> {
                Sectores sector = createSector(sectorDto, cooperativa);

                // Crear coordenadas del sector
                if (sectorDto.getCoordenadas() != null) {
                    sectorDto.getCoordenadas().forEach(coordDto -> {
                        createSectorCoordenada(coordDto, sector);
                    });
                }
            });
        }

        // 7. Crear balanza
        if (dto.getCooperativa().getBalanza() != null) {
            createBalanzaCooperativa(dto.getCooperativa().getBalanza(), cooperativa);
        }

        return usuario;
    }

    /**
     * Registra un socio completo con todos sus datos relacionados
     */
    @Transactional
    public Usuarios registerSocio(OnBoardingDto dto) {
        // 1. Buscar o crear tipo de usuario
        TipoUsuario tipoUsuario = findOrCreateTipoUsuario("socio");

        // 2. Crear usuario
        Usuarios usuario = createUsuario(dto.getUsuario(), tipoUsuario);

        // 3. Crear persona
        Persona persona = createPersona(dto.getPersona(), usuario);

        // 4. Registrar en auditoría
        registrarAuditoria(usuario, "usuarios", "INSERT",
                "Registro de nuevo usuario tipo socio: " + usuario.getCorreo());

        // 5. Crear socio
        Socio socio = createSocio(dto.getSocio(), usuario);

        // 6. Crear relación cooperativa-socio
        createCooperativaSocio(dto.getSocio(), socio);

        return usuario;
    }

    /**
     * Placeholder para registrar ingenio - implementar cuando se requiera
     */
    @Transactional
    public Usuarios registerIngenio(OnBoardingDto dto) {
        throw new UnsupportedOperationException("Registro de ingenio aún no implementado");
    }

    /**
     * Placeholder para registrar comercializadora - implementar cuando se requiera
     */
    @Transactional
    public Usuarios registerComercializadora(OnBoardingDto dto) {
        throw new UnsupportedOperationException("Registro de comercializadora aún no implementado");
    }

    // ==================== MÉTODOS AUXILIARES ====================

    private TipoUsuario findOrCreateTipoUsuario(String tipo) {
        return tipoUsuarioRepository.findByTipoUsuario(tipo)
                .orElseGet(() -> {
                    TipoUsuario nuevoTipo = new TipoUsuario();
                    nuevoTipo.setTipoUsuario(tipo);
                    return tipoUsuarioRepository.save(nuevoTipo);
                });
    }

    private Usuarios createUsuario(ucb.edu.bo.sumajflow.dto.UsuarioDto dto, TipoUsuario tipoUsuario) {
        Usuarios usuario = new Usuarios();
        usuario.setCorreo(dto.getCorreo());
        // Encriptar contraseña usando BCrypt
        usuario.setContrasena(passwordEncoder.encode(dto.getContrasena()));
        usuario.setTipoUsuarioId(tipoUsuario);
        return usuariosRepository.save(usuario);
    }

    private Persona createPersona(PersonaDto dto, Usuarios usuario) {
        Persona persona = new Persona();
        persona.setNombres(dto.getNombres());
        persona.setPrimerApellido(dto.getPrimerApellido());
        persona.setSegundoApellido(dto.getSegundoApellido());
        persona.setCi(dto.getCi());
        // Convertir LocalDate a java.sql.Date
        persona.setFechaNacimiento(convertToDate(dto.getFechaNacimiento()));
        persona.setNumeroCelular(dto.getNumeroCelular());
        persona.setGenero(dto.getGenero());
        persona.setNacionalidad(dto.getNacionalidad());
        persona.setDepartamento(dto.getDepartamento());
        persona.setProvincia(dto.getProvincia());
        persona.setMunicipio(dto.getMunicipio());
        persona.setDireccion(dto.getDireccion());
        persona.setLatitud(dto.getLatitud());
        persona.setLongitud(dto.getLongitud());
        persona.setUsuariosId(usuario);
        return personaRepository.save(persona);
    }

    private void registrarAuditoria(Usuarios usuario, String tabla, String accion, String descripcion) {
        Auditoria auditoria = new Auditoria();
        auditoria.setUsuariosId(usuario);
        auditoria.setTablaAfectada(tabla);
        auditoria.setAccion(accion);
        auditoria.setDescripcion(descripcion);
        auditoria.setFecha(new Timestamp(System.currentTimeMillis()));
        auditoriaRepository.save(auditoria);
    }

    private Cooperativa createCooperativa(ucb.edu.bo.sumajflow.dto.CooperativaDto dto, Usuarios usuario) {
        Cooperativa cooperativa = new Cooperativa();
        cooperativa.setRazonSocial(dto.getRazonSocial());
        cooperativa.setNit(dto.getNit());
        cooperativa.setNim(dto.getNim());
        cooperativa.setCorreoContacto(dto.getCorreoContacto());
        cooperativa.setNumeroTelefonoFijo(dto.getNumeroTelefonoFijo());
        cooperativa.setNumeroTelefonoMovil(dto.getNumeroTelefonoMovil());
        cooperativa.setDepartamento(dto.getDepartamento());
        cooperativa.setProvincia(dto.getProvincia());
        cooperativa.setMunicipio(dto.getMunicipio());
        cooperativa.setDireccion(dto.getDireccion());
        cooperativa.setLatitud(dto.getLatitud());
        cooperativa.setLongitud(dto.getLongitud());
        cooperativa.setUsuariosId(usuario);
        return cooperativaRepository.save(cooperativa);
    }

    private Sectores createSector(ucb.edu.bo.sumajflow.dto.SectorDto dto, Cooperativa cooperativa) {
        Sectores sector = new Sectores();
        sector.setNombre(dto.getNombre());
        sector.setColor(dto.getColor());
        sector.setCooperativaId(cooperativa);
        return sectoresRepository.save(sector);
    }

    private SectoresCoordenadas createSectorCoordenada(
            ucb.edu.bo.sumajflow.dto.CoordenadaDto dto, Sectores sector) {
        SectoresCoordenadas coordenada = new SectoresCoordenadas();
        coordenada.setOrden(dto.getOrden());
        coordenada.setLatitud(dto.getLatitud());
        coordenada.setLongitud(dto.getLongitud());
        coordenada.setSectoresId(sector);
        return sectoresCoordenadasRepository.save(coordenada);
    }

    private BalanzaCooperativa createBalanzaCooperativa(
            BalanzaDto dto, Cooperativa cooperativa) {
        BalanzaCooperativa balanza = new BalanzaCooperativa();
        balanza.setNombre(dto.getNombre());
        balanza.setMarca(dto.getMarca());
        balanza.setModelo(dto.getModelo());
        balanza.setNumeroSerie(dto.getNumeroSerie());
        balanza.setCapacidadMaxima(dto.getCapacidadMaxima());
        balanza.setPrecisionMinima(dto.getPrecisionMinima());
        // Convertir LocalDate a java.sql.Date
        balanza.setFechaUltimaCalibracion(convertToDate(dto.getFechaUltimaCalibracion()));
        balanza.setFechaProximaCalibracion(convertToDate(dto.getFechaProximaCalibracion()));
        balanza.setDepartamento(dto.getDepartamento());
        balanza.setProvincia(dto.getProvincia());
        balanza.setMunicipio(dto.getMunicipio());
        balanza.setDireccion(dto.getDireccion());
        balanza.setLatitud(dto.getLatitud());
        balanza.setLongitud(dto.getLongitud());
        balanza.setCooperativaId(cooperativa);
        return balanzaCooperativaRepository.save(balanza);
    }

    /**
     * Convierte LocalDate a java.sql.Date
     * @param localDate fecha en formato LocalDate
     * @return fecha en formato java.sql.Date, o null si localDate es null
     */
    private Date convertToDate(LocalDate localDate) {
        if (localDate == null) {
            return null;
        }
        return Date.valueOf(localDate);
    }

    private Socio createSocio(ucb.edu.bo.sumajflow.dto.SocioDto dto, Usuarios usuario) {
        Socio socio = new Socio();
        socio.setFechaEnvio(new Timestamp(System.currentTimeMillis()));
        socio.setEstado("pendiente");
        socio.setCarnetAfiliacionUrl(dto.getCarnetAfiliacionUrl());
        socio.setCarnetIdentidadUrl(dto.getCarnetIdentidadUrl());
        socio.setUsuariosId(usuario);
        return socioRepository.save(socio);
    }

    private CooperativaSocio createCooperativaSocio(ucb.edu.bo.sumajflow.dto.SocioDto dto, Socio socio) {
        // Buscar cooperativa
        Cooperativa cooperativa = cooperativaRepository.findById(dto.getCooperativaId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Cooperativa no encontrada con ID: " + dto.getCooperativaId()));

        CooperativaSocio cooperativaSocio = new CooperativaSocio();
        cooperativaSocio.setCooperativaId(cooperativa);
        cooperativaSocio.setSocioId(socio);
        cooperativaSocio.setFechaAfiliacion(new Date(System.currentTimeMillis()));
        cooperativaSocio.setEstado("pendiente");
        cooperativaSocio.setObservaciones("Solicitud de afiliación pendiente de aprobación");
        return cooperativaSocioRepository.save(cooperativaSocio);
    }
}