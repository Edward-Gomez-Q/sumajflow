package ucb.edu.bo.sumajflow.bl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ucb.edu.bo.sumajflow.dto.profile.UpdatePasswordDto;
import ucb.edu.bo.sumajflow.dto.profile.UpdatePersonalDataDto;
import ucb.edu.bo.sumajflow.entity.*;
import ucb.edu.bo.sumajflow.repository.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Servicio para gestión de perfiles
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProfileBl {

    private final UsuariosRepository usuariosRepository;
    private final PersonaRepository personaRepository;
    private final CooperativaRepository cooperativaRepository;
    private final SocioRepository socioRepository;
    private final IngenioMineroRepository ingenioMineroRepository;
    private final ComercializadoraRepository comercializadoraRepository;
    private final TransportistaRepository transportistaRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final AuditoriaBl auditoriaBl;
    private final NotificacionBl notificacionBl;

    /**
     * Obtener perfil completo del usuario
     */
    @Transactional(readOnly = true)
    public Map<String, Object> obtenerPerfil(Integer usuarioId) {
        log.info("Obteniendo perfil para usuario ID: {}", usuarioId);

        Usuarios usuario = usuariosRepository.findById(usuarioId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        Persona persona = personaRepository.findByUsuariosId(usuario)
                .orElseThrow(() -> new IllegalArgumentException("Datos de persona no encontrados"));

        String tipoUsuario = usuario.getTipoUsuarioId().getTipoUsuario();

        Map<String, Object> perfil = new HashMap<>();
        perfil.put("usuario", construirDatosUsuario(usuario));
        perfil.put("persona", construirDatosPersona(persona));
        perfil.put("tipoUsuario", tipoUsuario);

        // Datos específicos según el rol
        switch (tipoUsuario) {
            case "cooperativa":
                cooperativaRepository.findByUsuariosId(usuario).ifPresent(cooperativa ->
                        perfil.put("cooperativa", construirDatosCooperativa(cooperativa))
                );
                break;

            case "socio":
                socioRepository.findByUsuariosId(usuario).ifPresent(socio ->
                        perfil.put("socio", construirDatosSocio(socio))
                );
                break;

            case "ingenio":
                ingenioMineroRepository.findByUsuariosId(usuario).ifPresent(ingenio ->
                        perfil.put("ingenio", construirDatosIngenio(ingenio))
                );
                break;

            case "comercializadora":
                comercializadoraRepository.findByUsuariosId(usuario).ifPresent(comercializadora ->
                        perfil.put("comercializadora", construirDatosComercializadora(comercializadora))
                );
                break;

            case "transportista":
                transportistaRepository.findByUsuariosId(usuario).ifPresent(transportista ->
                        perfil.put("transportista", construirDatosTransportista(transportista))
                );
                break;
        }

        return perfil;
    }

    /**
     * Actualizar datos personales
     */
    @Transactional
    public void actualizarDatosPersonales(
            Integer usuarioId,
            UpdatePersonalDataDto dto,
            String ipOrigen
    ) {
        log.info("Actualizando datos personales para usuario ID: {}", usuarioId);

        Usuarios usuario = usuariosRepository.findById(usuarioId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        Persona persona = personaRepository.findByUsuariosId(usuario)
                .orElseThrow(() -> new IllegalArgumentException("Datos de persona no encontrados"));

        String tipoUsuario = usuario.getTipoUsuarioId().getTipoUsuario();

        // Validar restricciones para transportistas
        if ("transportista".equals(tipoUsuario)) {
            if (!persona.getCi().equals(dto.getCi())) {
                throw new IllegalArgumentException(
                        "Los transportistas no pueden cambiar su CI. Contacta al administrador."
                );
            }
            if (dto.getNumeroCelular() != null && !persona.getNumeroCelular().equals(dto.getNumeroCelular())) {
                throw new IllegalArgumentException(
                        "Los transportistas no pueden cambiar su número de celular. Contacta al administrador."
                );
            }
        }

        Map<String, Object> datosAnteriores = construirDatosPersona(persona);

        // Actualizar campos
        persona.setNombres(dto.getNombres());
        persona.setPrimerApellido(dto.getPrimerApellido());
        persona.setSegundoApellido(dto.getSegundoApellido());

        if (!"transportista".equals(tipoUsuario)) {
            persona.setCi(dto.getCi());
        }

        if (dto.getFechaNacimiento() != null) {
            persona.setFechaNacimiento(dto.getFechaNacimiento());
        }

        if (!"transportista".equals(tipoUsuario) && dto.getNumeroCelular() != null) {
            persona.setNumeroCelular(dto.getNumeroCelular());
        }

        persona.setGenero(dto.getGenero());
        persona.setNacionalidad(dto.getNacionalidad());
        persona.setDepartamento(dto.getDepartamento());
        persona.setProvincia(dto.getProvincia());
        persona.setMunicipio(dto.getMunicipio());
        persona.setDireccion(dto.getDireccion());

        personaRepository.save(persona);

        registrarAuditoria(usuario, "persona", persona.getId(), datosAnteriores,
                construirDatosPersona(persona), ipOrigen, "PUT",
                "/" + tipoUsuario + "/perfil/datos-personales");

        enviarNotificacion(usuario, "datos personales");
    }

    /**
     * Actualizar correo electrónico
     */
    @Transactional
    public void actualizarCorreo(
            Integer usuarioId,
            String nuevoCorreo,
            String contrasenaActual,
            String ipOrigen
    ) {
        log.info("Actualizando correo para usuario ID: {}", usuarioId);

        Usuarios usuario = usuariosRepository.findById(usuarioId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        if (!passwordEncoder.matches(contrasenaActual, usuario.getContrasena())) {
            throw new IllegalArgumentException("La contraseña actual es incorrecta");
        }

        if (usuariosRepository.findByCorreo(nuevoCorreo).isPresent()) {
            throw new IllegalArgumentException("El correo electrónico ya está en uso");
        }

        String correoAnterior = usuario.getCorreo();
        usuario.setCorreo(nuevoCorreo);
        usuariosRepository.save(usuario);

        String tipoUsuario = usuario.getTipoUsuarioId().getTipoUsuario();
        registrarAuditoria(usuario, "usuarios", usuario.getId(),
                Map.of("correo", correoAnterior),
                Map.of("correo", nuevoCorreo), ipOrigen, "PUT",
                "/" + tipoUsuario + "/perfil/correo");

        enviarNotificacion(usuario, "correo electrónico");
    }

    /**
     * Actualizar contraseña
     */
    @Transactional
    public void actualizarContrasena(
            Integer usuarioId,
            UpdatePasswordDto dto,
            String ipOrigen
    ) {
        log.info("Actualizando contraseña para usuario ID: {}", usuarioId);

        Usuarios usuario = usuariosRepository.findById(usuarioId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        if (!passwordEncoder.matches(dto.getContrasenaActual(), usuario.getContrasena())) {
            throw new IllegalArgumentException("La contraseña actual es incorrecta");
        }

        if (!dto.getNuevaContrasena().equals(dto.getConfirmarContrasena())) {
            throw new IllegalArgumentException("Las contraseñas nuevas no coinciden");
        }

        if (passwordEncoder.matches(dto.getNuevaContrasena(), usuario.getContrasena())) {
            throw new IllegalArgumentException("La nueva contraseña debe ser diferente de la actual");
        }

        usuario.setContrasena(passwordEncoder.encode(dto.getNuevaContrasena()));
        usuariosRepository.save(usuario);

        String tipoUsuario = usuario.getTipoUsuarioId().getTipoUsuario();
        registrarAuditoria(usuario, "usuarios", usuario.getId(),
                Map.of("accion", "cambio_contrasena"),
                Map.of("accion", "contrasena_actualizada"), ipOrigen, "PUT",
                "/" + tipoUsuario + "/perfil/contrasena");

        enviarNotificacion(usuario, "contraseña");
    }

    /**
     * Actualizar dirección
     */
    @Transactional
    public void actualizarDireccion(
            Integer usuarioId,
            Map<String, String> addressData,
            String ipOrigen
    ) {
        log.info("Actualizando dirección para usuario ID: {}", usuarioId);

        Usuarios usuario = usuariosRepository.findById(usuarioId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        Persona persona = personaRepository.findByUsuariosId(usuario)
                .orElseThrow(() -> new IllegalArgumentException("Datos de persona no encontrados"));

        Map<String, Object> datosAnteriores = Map.of(
                "departamento", persona.getDepartamento() != null ? persona.getDepartamento() : "",
                "provincia", persona.getProvincia() != null ? persona.getProvincia() : "",
                "municipio", persona.getMunicipio() != null ? persona.getMunicipio() : "",
                "direccion", persona.getDireccion() != null ? persona.getDireccion() : ""
        );

        persona.setDepartamento(addressData.get("departamento"));
        persona.setProvincia(addressData.get("provincia"));
        persona.setMunicipio(addressData.get("municipio"));
        persona.setDireccion(addressData.get("direccion"));

        personaRepository.save(persona);

        String tipoUsuario = usuario.getTipoUsuarioId().getTipoUsuario();
        registrarAuditoria(usuario, "persona", persona.getId(), datosAnteriores,
                Map.of(
                        "departamento", persona.getDepartamento() != null ? persona.getDepartamento() : "",
                        "provincia", persona.getProvincia() != null ? persona.getProvincia() : "",
                        "municipio", persona.getMunicipio() != null ? persona.getMunicipio() : "",
                        "direccion", persona.getDireccion() != null ? persona.getDireccion() : ""
                ), ipOrigen, "PUT", "/" + tipoUsuario + "/perfil/direccion");

        enviarNotificacion(usuario, "dirección");
    }

    // ==================== MÉTODOS AUXILIARES ====================

    private Map<String, Object> construirDatosUsuario(Usuarios usuario) {
        Map<String, Object> datos = new HashMap<>();
        datos.put("id", usuario.getId());
        datos.put("correo", usuario.getCorreo());
        datos.put("createdAt", usuario.getCreatedAt());
        datos.put("updatedAt", usuario.getUpdatedAt());
        return datos;
    }

    private Map<String, Object> construirDatosPersona(Persona persona) {
        Map<String, Object> datos = new HashMap<>();
        datos.put("id", persona.getId());
        datos.put("nombres", persona.getNombres());
        datos.put("primerApellido", persona.getPrimerApellido());
        datos.put("segundoApellido", persona.getSegundoApellido());
        datos.put("ci", persona.getCi());
        datos.put("fechaNacimiento", persona.getFechaNacimiento());
        datos.put("numeroCelular", persona.getNumeroCelular());
        datos.put("genero", persona.getGenero());
        datos.put("nacionalidad", persona.getNacionalidad());
        datos.put("departamento", persona.getDepartamento());
        datos.put("provincia", persona.getProvincia());
        datos.put("municipio", persona.getMunicipio());
        datos.put("direccion", persona.getDireccion());
        return datos;
    }

    private Map<String, Object> construirDatosCooperativa(Cooperativa cooperativa) {
        Map<String, Object> datos = new HashMap<>();
        datos.put("id", cooperativa.getId());
        datos.put("razonSocial", cooperativa.getRazonSocial());
        datos.put("nit", cooperativa.getNit());
        datos.put("nim", cooperativa.getNim());
        return datos;
    }

    private Map<String, Object> construirDatosSocio(Socio socio) {
        Map<String, Object> datos = new HashMap<>();
        datos.put("id", socio.getId());
        datos.put("estado", socio.getEstado());
        return datos;
    }

    private Map<String, Object> construirDatosIngenio(IngenioMinero ingenio) {
        Map<String, Object> datos = new HashMap<>();
        datos.put("id", ingenio.getId());
        datos.put("razonSocial", ingenio.getRazonSocial());
        datos.put("nit", ingenio.getNit());
        datos.put("nim", ingenio.getNim());
        return datos;
    }

    private Map<String, Object> construirDatosComercializadora(Comercializadora comercializadora) {
        Map<String, Object> datos = new HashMap<>();
        datos.put("id", comercializadora.getId());
        datos.put("razonSocial", comercializadora.getRazonSocial());
        datos.put("nit", comercializadora.getNit());
        datos.put("nim", comercializadora.getNim());
        return datos;
    }

    private Map<String, Object> construirDatosTransportista(Transportista transportista) {
        Map<String, Object> datos = new HashMap<>();
        datos.put("id", transportista.getId());
        datos.put("ci", transportista.getCi());
        datos.put("placaVehiculo", transportista.getPlacaVehiculo());
        datos.put("estado", transportista.getEstado());
        return datos;
    }

    private void registrarAuditoria(
            Usuarios usuario,
            String tabla,
            Integer registroId,
            Map<String, Object> datosAnteriores,
            Map<String, Object> datosNuevos,
            String ipOrigen,
            String metodoHttp,
            String endpoint
    ) {
        auditoriaBl.registrar(
                usuario,
                tabla,
                "UPDATE",
                "Actualización de perfil",
                registroId,
                datosAnteriores,
                datosNuevos,
                List.of("datos_perfil"),
                ipOrigen,
                null,
                metodoHttp,
                endpoint,
                "RF_PROFILE",
                "MEDIO"
        );
    }

    private void enviarNotificacion(Usuarios usuario, String campo) {
        Map<String, Object> metadata = Map.of(
                "tipo", "actualizacion_perfil",
                "campo", campo
        );

        notificacionBl.crearNotificacion(
                usuario.getId(),
                "info",
                "Perfil actualizado",
                "Tu " + campo + " ha sido actualizado exitosamente",
                metadata
        );
    }
}