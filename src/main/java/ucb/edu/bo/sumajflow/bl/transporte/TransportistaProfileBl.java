package ucb.edu.bo.sumajflow.bl.transporte;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ucb.edu.bo.sumajflow.bl.AuditoriaBl;
import ucb.edu.bo.sumajflow.bl.NotificacionBl;
import ucb.edu.bo.sumajflow.dto.profile.UpdateTransportistaDataDto;
import ucb.edu.bo.sumajflow.entity.Transportista;
import ucb.edu.bo.sumajflow.entity.Usuarios;
import ucb.edu.bo.sumajflow.repository.TransportistaRepository;
import ucb.edu.bo.sumajflow.repository.UsuariosRepository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;


@Slf4j
@Service
@RequiredArgsConstructor
public class TransportistaProfileBl {

    private final TransportistaRepository transportistaRepository;
    private final UsuariosRepository usuariosRepository;
    private final AuditoriaBl auditoriaBl;
    private final NotificacionBl notificacionBl;

    /**
     * Obtener datos completos del transportista
     */
    @Transactional(readOnly = true)
    public Map<String, Object> obtenerDatosTransportista(Integer usuarioId) {
        log.info("📋 Obteniendo datos de transportista para usuario ID: {}", usuarioId);

        Usuarios usuario = usuariosRepository.findById(usuarioId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        Transportista transportista = transportistaRepository.findByUsuariosId(usuario)
                .orElseThrow(() -> new IllegalArgumentException("Datos de transportista no encontrados"));

        return construirDatosTransportista(transportista);
    }

    /**
     * Actualizar datos del transportista
     * RESTRICCIÓN: Solo permite modificar campos NO críticos para trazabilidad
     */
    @Transactional
    public void actualizarDatosTransportista(
            Integer usuarioId,
            UpdateTransportistaDataDto dto,
            String ipOrigen
    ) {
        log.info("✏️ Actualizando datos de transportista para usuario ID: {}", usuarioId);

        Usuarios usuario = usuariosRepository.findById(usuarioId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        Transportista transportista = transportistaRepository.findByUsuariosId(usuario)
                .orElseThrow(() -> new IllegalArgumentException("Datos de transportista no encontrados"));

        // Guardar estado anterior para auditoría
        Map<String, Object> datosAnteriores = construirDatosTransportista(transportista);

        // ✅ Actualizar SOLO campos permitidos (no críticos)
        if (dto.getCategoriaLicencia() != null) {
            transportista.setCategoriaLicencia(dto.getCategoriaLicencia());
        }

        if (dto.getFechaVencimientoLicencia() != null) {
            transportista.setFechaVencimientoLicencia(dto.getFechaVencimientoLicencia());
        }

        if (dto.getLicenciaConducir() != null) {
            transportista.setLicenciaConducir(dto.getLicenciaConducir());
        }

        if (dto.getColorVehiculo() != null) {
            transportista.setColorVehiculo(dto.getColorVehiculo());
        }

        // Guardar cambios
        transportistaRepository.save(transportista);

        // Registrar auditoría
        registrarAuditoria(
                usuario,
                transportista.getId(),
                datosAnteriores,
                construirDatosTransportista(transportista),
                ipOrigen
        );

        // Enviar notificación
        enviarNotificacion(usuario);

        log.info("✅ Datos de transportista actualizados exitosamente");
    }

    // ==================== MÉTODOS AUXILIARES ====================

    /**
     * Construir mapa con todos los datos del transportista
     */
    private Map<String, Object> construirDatosTransportista(Transportista transportista) {
        Map<String, Object> datos = new HashMap<>();

        // Datos básicos
        datos.put("id", transportista.getId());
        datos.put("ci", transportista.getCi());
        datos.put("estado", transportista.getEstado());

        // Licencia de conducir
        datos.put("licenciaConducir", transportista.getLicenciaConducir());
        datos.put("categoriaLicencia", transportista.getCategoriaLicencia());
        datos.put("fechaVencimientoLicencia", transportista.getFechaVencimientoLicencia());

        // Datos del vehículo (TODOS, incluso los bloqueados)
        datos.put("placaVehiculo", transportista.getPlacaVehiculo());
        datos.put("marcaVehiculo", transportista.getMarcaVehiculo());
        datos.put("modeloVehiculo", transportista.getModeloVehiculo());
        datos.put("colorVehiculo", transportista.getColorVehiculo());
        datos.put("pesoTara", transportista.getPesoTara());
        datos.put("capacidadCarga", transportista.getCapacidadCarga());

        // Estadísticas
        datos.put("viajesCompletados", transportista.getViajesCompletados());
        datos.put("calificacionPromedio", transportista.getCalificacionPromedio());

        // Fechas
        datos.put("fechaAprobacion", transportista.getFechaAprobacion());
        datos.put("createdAt", transportista.getCreatedAt());
        datos.put("updatedAt", transportista.getUpdatedAt());

        return datos;
    }

    /**
     * Registrar auditoría de cambios
     */
    private void registrarAuditoria(
            Usuarios usuario,
            Integer transportistaId,
            Map<String, Object> datosAnteriores,
            Map<String, Object> datosNuevos,
            String ipOrigen
    ) {
        auditoriaBl.registrar(
                usuario,
                "transportista",
                "UPDATE",
                "Actualización de datos del transportista",
                transportistaId,
                datosAnteriores,
                datosNuevos,
                List.of("datos_transportista"),
                ipOrigen,
                null,
                "PUT",
                "/transportista/perfil/datos-transportista",
                "RF_TRANSPORTISTA_PROFILE",
                "MEDIO"
        );
    }

    /**
     * Enviar notificación al transportista
     */
    private void enviarNotificacion(Usuarios usuario) {
        Map<String, Object> metadata = Map.of(
                "tipo", "actualizacion_perfil",
                "seccion", "datos_transportista"
        );

        notificacionBl.crearNotificacion(
                usuario.getId(),
                "info",
                "Datos del vehículo actualizados",
                "Tus datos de transportista han sido actualizados exitosamente",
                metadata
        );
    }
}