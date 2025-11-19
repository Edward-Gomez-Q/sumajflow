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
            CooperativaSocioRepository cooperativaSocioRepository,
            IngenioMineroRepository ingenioMineroRepository,
            PlantaRepository plantaRepository,
            BalanzaIngenioRepository balanzaIngenioRepository,
            AlmacenIngenioRepository almacenIngenioRepository,
            MineralesRepository mineralesRepository,
            ProcesosRepository procesosRepository,
            PlantaMineralesRepository plantaMineralesRepository,
            ProcesosPlantaRepository procesosPlantaRepository,
            ComercializadoraRepository comercializadoraRepository,
            BalanzaComercializadoraRepository balanzaComercializadoraRepository,
            AlmacenComercializadoraRepository almacenComercializadoraRepository
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
        this.ingenioMineroRepository = ingenioMineroRepository;
        this.plantaRepository = plantaRepository;
        this.balanzaIngenioRepository = balanzaIngenioRepository;
        this.almacenIngenioRepository = almacenIngenioRepository;
        this.mineralesRepository = mineralesRepository;
        this.procesosRepository = procesosRepository;
        this.plantaMineralesRepository = plantaMineralesRepository;
        this.procesosPlantaRepository = procesosPlantaRepository;
        this.comercializadoraRepository = comercializadoraRepository;
        this.balanzaComercializadoraRepository = balanzaComercializadoraRepository;
        this.almacenComercializadoraRepository = almacenComercializadoraRepository;
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
     * Registra un ingenio minero completo con todos sus datos relacionados
     */
    @Transactional
    public Usuarios registerIngenio(OnBoardingDto dto) {
        // 1. Buscar o crear tipo de usuario
        TipoUsuario tipoUsuario = findOrCreateTipoUsuario("ingenio");

        // 2. Crear usuario
        Usuarios usuario = createUsuario(dto.getUsuario(), tipoUsuario);

        // 3. Crear persona
        Persona persona = createPersona(dto.getPersona(), usuario);

        // 4. Registrar en auditoría
        registrarAuditoria(usuario, "usuarios", "INSERT",
                "Registro de nuevo usuario tipo ingenio: " + usuario.getCorreo());

        // 5. Crear ingenio minero
        IngenioMinero ingenio = createIngenioMinero(dto.getIngenio(), usuario);

        // 6. Crear planta
        if (dto.getIngenio().getPlanta() != null) {
            Planta planta = createPlanta(dto.getIngenio().getPlanta(), ingenio);

            // 6.1 Crear relaciones planta-minerales
            if (dto.getIngenio().getPlanta().getMinerales() != null) {
                dto.getIngenio().getPlanta().getMinerales().forEach(mineralId -> {
                    createPlantaMineral(planta, mineralId);
                });
            }

            // 6.2 Crear relaciones planta-procesos
            if (dto.getIngenio().getPlanta().getProcesos() != null) {
                dto.getIngenio().getPlanta().getProcesos().forEach(procesoDto -> {
                    createPlantaProceso(planta, procesoDto.getId());
                });
            }
        }

        // 7. Crear balanza
        if (dto.getIngenio().getBalanza() != null) {
            createBalanzaIngenio(dto.getIngenio().getBalanza(), ingenio);
        }

        // 8. Crear almacenes
        if (dto.getIngenio().getAlmacenes() != null) {
            dto.getIngenio().getAlmacenes().forEach(almacenDto -> {
                createAlmacenIngenio(almacenDto, ingenio);
            });
        }

        return usuario;
    }

    /**
     * Registra una comercializadora completa con todos sus datos relacionados
     */
    @Transactional
    public Usuarios registerComercializadora(OnBoardingDto dto) {
        // 1. Buscar o crear tipo de usuario
        TipoUsuario tipoUsuario = findOrCreateTipoUsuario("comercializadora");

        // 2. Crear usuario
        Usuarios usuario = createUsuario(dto.getUsuario(), tipoUsuario);

        // 3. Crear persona
        Persona persona = createPersona(dto.getPersona(), usuario);

        // 4. Registrar en auditoría
        registrarAuditoria(usuario, "usuarios", "INSERT",
                "Registro de nuevo usuario tipo comercializadora: " + usuario.getCorreo());

        // 5. Crear comercializadora
        Comercializadora comercializadora = createComercializadora(dto.getComercializadora(), usuario);

        // 6. Crear almacenes
        if (dto.getComercializadora().getAlmacenes() != null) {
            dto.getComercializadora().getAlmacenes().forEach(almacenDto -> {
                createAlmacenComercializadora(almacenDto, comercializadora);
            });
        }

        // 7. Crear balanza
        if (dto.getComercializadora().getBalanza() != null) {
            createBalanzaComercializadora(dto.getComercializadora().getBalanza(), comercializadora);
        }

        return usuario;
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

    private IngenioMinero createIngenioMinero(ucb.edu.bo.sumajflow.dto.IngenioDto dto, Usuarios usuario) {
        IngenioMinero ingenio = new IngenioMinero();
        ingenio.setRazonSocial(dto.getRazonSocial());
        ingenio.setNit(dto.getNit());
        ingenio.setNim(dto.getNim());
        ingenio.setCorreoContacto(dto.getCorreoContacto());
        ingenio.setNumeroTelefonoFijo(dto.getNumeroTelefonoFijo());
        ingenio.setNumeroTelefonoMovil(dto.getNumeroTelefonoMovil());
        ingenio.setDepartamento(dto.getDepartamento());
        ingenio.setProvincia(dto.getProvincia());
        ingenio.setMunicipio(dto.getMunicipio());
        ingenio.setDireccion(dto.getDireccion());
        ingenio.setLatitud(dto.getLatitud());
        ingenio.setLongitud(dto.getLongitud());
        ingenio.setUsuariosId(usuario);
        return ingenioMineroRepository.save(ingenio);
    }

    private Planta createPlanta(ucb.edu.bo.sumajflow.dto.PlantaDto dto, IngenioMinero ingenio) {
        Planta planta = new Planta();
        planta.setCupoMinimo(dto.getCupoMinimo());
        planta.setCapacidadProcesamiento(dto.getCapacidadProcesamiento());
        planta.setCostoProcesamiento(dto.getCostoProcesamiento());
        planta.setLicenciaAmbientalUrl(dto.getLicenciaAmbientalUrl());
        planta.setDepartamento(dto.getDepartamento());
        planta.setProvincia(dto.getProvincia());
        planta.setMunicipio(dto.getMunicipio());
        planta.setDireccion(dto.getDireccion());
        planta.setLatitud(dto.getLatitud());
        planta.setLongitud(dto.getLongitud());
        planta.setIngenioMineroId(ingenio);
        return plantaRepository.save(planta);
    }

    private PlantaMinerales createPlantaMineral(Planta planta, Integer mineralId) {
        // Buscar mineral
        Minerales mineral = mineralesRepository.findById(mineralId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Mineral no encontrado con ID: " + mineralId));

        PlantaMinerales plantaMineral = new PlantaMinerales();
        plantaMineral.setPlantaId(planta);
        plantaMineral.setMineralesId(mineral);
        return plantaMineralesRepository.save(plantaMineral);
    }

    private ProcesosPlanta createPlantaProceso(Planta planta, Integer procesoId) {
        // Buscar proceso
        Procesos proceso = procesosRepository.findById(procesoId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Proceso no encontrado con ID: " + procesoId));

        ProcesosPlanta procesosPlanta = new ProcesosPlanta();
        procesosPlanta.setPlantaId(planta);
        procesosPlanta.setProcesosId(proceso);
        return procesosPlantaRepository.save(procesosPlanta);
    }

    private BalanzaIngenio createBalanzaIngenio(BalanzaDto dto, IngenioMinero ingenio) {
        BalanzaIngenio balanza = new BalanzaIngenio();
        balanza.setNombre(dto.getNombre());
        balanza.setMarca(dto.getMarca());
        balanza.setModelo(dto.getModelo());
        balanza.setNumeroSerie(dto.getNumeroSerie());
        balanza.setCapacidadMaxima(dto.getCapacidadMaxima());
        balanza.setPrecisionMinima(dto.getPrecisionMinima());
        balanza.setFechaUltimaCalibracion(convertToDate(dto.getFechaUltimaCalibracion()));
        balanza.setFechaProximaCalibracion(convertToDate(dto.getFechaProximaCalibracion()));
        balanza.setDepartamento(dto.getDepartamento());
        balanza.setProvincia(dto.getProvincia());
        balanza.setMunicipio(dto.getMunicipio());
        balanza.setDireccion(dto.getDireccion());
        balanza.setLatitud(dto.getLatitud());
        balanza.setLongitud(dto.getLongitud());
        balanza.setIngenioMineroId(ingenio);
        return balanzaIngenioRepository.save(balanza);
    }

    private AlmacenIngenio createAlmacenIngenio(ucb.edu.bo.sumajflow.dto.AlmacenDto dto, IngenioMinero ingenio) {
        AlmacenIngenio almacen = new AlmacenIngenio();
        almacen.setNombre(dto.getNombre());
        almacen.setCapacidadMaxima(dto.getCapacidadMaxima());
        almacen.setArea(dto.getArea());
        almacen.setDepartamento(dto.getDepartamento());
        almacen.setProvincia(dto.getProvincia());
        almacen.setMunicipio(dto.getMunicipio());
        almacen.setDireccion(dto.getDireccion());
        almacen.setLatitud(dto.getLatitud());
        almacen.setLongitud(dto.getLongitud());
        almacen.setIngenioMineroId(ingenio);
        return almacenIngenioRepository.save(almacen);
    }

    private Comercializadora createComercializadora(ucb.edu.bo.sumajflow.dto.ComercializadoraDto dto, Usuarios usuario) {
        Comercializadora comercializadora = new Comercializadora();
        comercializadora.setRazonSocial(dto.getRazonSocial());
        comercializadora.setNit(dto.getNit());
        comercializadora.setNim(dto.getNim());
        comercializadora.setCorreoContacto(dto.getCorreoContacto());
        comercializadora.setNumeroTelefonoFijo(dto.getNumeroTelefonoFijo());
        comercializadora.setNumeroTelefonoMovil(dto.getNumeroTelefonoMovil());
        comercializadora.setDepartamento(dto.getDepartamento());
        comercializadora.setProvincia(dto.getProvincia());
        comercializadora.setMunicipio(dto.getMunicipio());
        comercializadora.setDireccion(dto.getDireccion());
        comercializadora.setLatitud(dto.getLatitud());
        comercializadora.setLongitud(dto.getLongitud());
        comercializadora.setUsuariosId(usuario);
        return comercializadoraRepository.save(comercializadora);
    }

    private AlmacenComercializadora createAlmacenComercializadora(
            ucb.edu.bo.sumajflow.dto.AlmacenDto dto, Comercializadora comercializadora) {
        AlmacenComercializadora almacen = new AlmacenComercializadora();
        almacen.setNombre(dto.getNombre());
        almacen.setCapacidadMaxima(dto.getCapacidadMaxima());
        almacen.setArea(dto.getArea());
        almacen.setDepartamento(dto.getDepartamento());
        almacen.setProvincia(dto.getProvincia());
        almacen.setMunicipio(dto.getMunicipio());
        almacen.setDireccion(dto.getDireccion());
        almacen.setLatitud(dto.getLatitud());
        almacen.setLongitud(dto.getLongitud());
        almacen.setComercializadoraId(comercializadora);
        return almacenComercializadoraRepository.save(almacen);
    }

    private BalanzaComercializadora createBalanzaComercializadora(
            BalanzaDto dto, Comercializadora comercializadora) {
        BalanzaComercializadora balanza = new BalanzaComercializadora();
        balanza.setNombre(dto.getNombre());
        balanza.setMarca(dto.getMarca());
        balanza.setModelo(dto.getModelo());
        balanza.setNumeroSerie(dto.getNumeroSerie());
        balanza.setCapacidadMaxima(dto.getCapacidadMaxima());
        balanza.setPrecisionMinima(dto.getPrecisionMinima());
        balanza.setFechaUltimaCalibracion(convertToDate(dto.getFechaUltimaCalibracion()));
        balanza.setFechaProximaCalibracion(convertToDate(dto.getFechaProximaCalibracion()));
        balanza.setDepartamento(dto.getDepartamento());
        balanza.setProvincia(dto.getProvincia());
        balanza.setMunicipio(dto.getMunicipio());
        balanza.setDireccion(dto.getDireccion());
        balanza.setLatitud(dto.getLatitud());
        balanza.setLongitud(dto.getLongitud());
        balanza.setComercializadoraId(comercializadora);
        return balanzaComercializadoraRepository.save(balanza);
    }
}