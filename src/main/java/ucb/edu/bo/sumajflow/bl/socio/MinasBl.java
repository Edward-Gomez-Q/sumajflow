package ucb.edu.bo.sumajflow.bl.socio;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ucb.edu.bo.sumajflow.bl.AuditoriaBl;
import ucb.edu.bo.sumajflow.bl.NotificacionBl;
import ucb.edu.bo.sumajflow.dto.socio.MinaCreateDto;
import ucb.edu.bo.sumajflow.dto.socio.MinaResponseDto;
import ucb.edu.bo.sumajflow.entity.*;
import ucb.edu.bo.sumajflow.repository.*;
import ucb.edu.bo.sumajflow.utils.GeometryUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Servicio de lógica de negocio para gestión de Minas
 * Cumple con validaciones de negocio, auditoría completa y notificaciones
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MinasBl {

    // Inyección de dependencias
    private final MinasRepository minasRepository;
    private final SocioRepository socioRepository;
    private final SectoresRepository sectoresRepository;
    private final UsuariosRepository usuariosRepository;
    private final CooperativaRepository cooperativaRepository;
    private final AuditoriaBl auditoriaBl;
    private final NotificacionBl notificacionBl;

    /**
     * Obtener todas las minas ACTIVAS del socio autenticado
     */
    @Transactional(readOnly = true)
    public List<MinaResponseDto> getMinasBySocio(Integer usuarioId) {
        log.debug("Obteniendo minas para usuario ID: {}", usuarioId);

        Socio socio = obtenerSocioDelUsuario(usuarioId);
        List<Minas> minas = minasRepository.findByActiveSocio(socio);

        log.info("Se encontraron {} minas activas", minas.size());

        return minas.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    /**
     * Obtener una mina por ID (solo si está activa y pertenece al socio)
     */
    @Transactional(readOnly = true)
    public MinaResponseDto getMinaById(Integer minaId, Integer usuarioId) {
        log.debug("Obteniendo mina ID: {} para usuario ID: {}", minaId, usuarioId);

        Minas mina = minasRepository.findByIdAndEstadoActivo(minaId)
                .orElseThrow(() -> new IllegalArgumentException("Mina no encontrada o inactiva"));

        // Verificar permisos
        Socio socio = obtenerSocioDelUsuario(usuarioId);
        validarPermisosAccesoMina(mina, socio);

        return convertToDto(mina);
    }

    /**
     * Crear una nueva mina
     */
    @Transactional
    public MinaResponseDto createMina(
            MinaCreateDto dto,
            Integer usuarioId,
            String ipOrigen,
            String metodoHttp,
            String endpoint
    ) {
        log.info("Creando nueva mina: {}", dto.getNombre());

        // Validar datos básicos
        validateMinaData(dto);

        // Buscar usuario y socio
        Usuarios usuario = obtenerUsuario(usuarioId);
        Socio socio = obtenerSocioDelUsuario(usuario);

        // Validar sector y permisos
        Sectores sector = validarYObtenerSector(dto.getSectorId(), socio);

        // Validar nombre único para el socio
        validarNombreUnico(dto.getNombre(), socio, null);

        // Validar que el punto esté dentro del polígono del sector
        validarPuntoEnSector(dto.getLatitud(), dto.getLongitud(), sector);

        // OPCIONAL: Validar distancia mínima entre minas (descomenta si lo necesitas)
        // validarDistanciaMinimaEntreMinasOpcional(dto.getLatitud(), dto.getLongitud(), socio, null);

        // Crear mina
        Minas mina = crearYGuardarMina(dto, socio, sector);

        // Auditoría
        auditoriaBl.registrarCreacionMina(
                usuario,
                mina.getId(),
                mina.getNombre(),
                sector.getId(),
                ipOrigen,
                metodoHttp,
                endpoint
        );

        // Notificación
        enviarNotificacionCreacion(usuarioId, mina, sector);

        log.info("Mina creada exitosamente - ID: {}", mina.getId());
        return convertToDto(mina);
    }

    /**
     * Actualizar una mina existente
     */
    @Transactional
    public MinaResponseDto updateMina(
            Integer minaId,
            MinaCreateDto dto,
            Integer usuarioId,
            String ipOrigen,
            String metodoHttp,
            String endpoint
    ) {
        log.info("Actualizando mina ID: {}", minaId);

        // Validar datos
        validateMinaData(dto);

        // Buscar mina y validar permisos
        Minas mina = obtenerMinaActivaConPermisos(minaId, usuarioId);
        Usuarios usuario = obtenerUsuario(usuarioId);
        Socio socio = mina.getSocioId();

        // Guardar datos anteriores para auditoría
        String nombreAnterior = mina.getNombre();
        String fotoUrlAnterior = mina.getFotoUrl();
        Integer sectorIdAnterior = mina.getSectoresId().getId();

        // Validar sector y permisos
        Sectores sectorNuevo = validarYObtenerSector(dto.getSectorId(), socio);

        // Validar nombre único
        validarNombreUnico(dto.getNombre(), socio, minaId);

        // Validar que el nuevo punto esté dentro del nuevo sector
        validarPuntoEnSector(dto.getLatitud(), dto.getLongitud(), sectorNuevo);

        // OPCIONAL: Validar distancia mínima entre minas (descomenta si lo necesitas)
        // validarDistanciaMinimaEntreMinasOpcional(dto.getLatitud(), dto.getLongitud(), socio, minaId);

        // Determinar campos modificados
        List<String> camposModificados = determinarCamposModificados(mina, dto, sectorNuevo);

        // Actualizar mina
        actualizarMina(mina, dto, sectorNuevo);

        // Auditoría
        auditoriaBl.registrarActualizacionMina(
                usuario,
                mina.getId(),
                mina.getNombre(),
                camposModificados,
                ipOrigen,
                metodoHttp,
                endpoint
        );

        // Notificación
        enviarNotificacionActualizacion(usuarioId, mina, camposModificados);

        log.info("Mina actualizada exitosamente - ID: {}", minaId);
        return convertToDto(mina);
    }

    /**
     * ELIMINACIÓN LÓGICA de una mina
     * VALIDACIÓN CRÍTICA: No se puede eliminar si tiene lotes activos
     */
    @Transactional
    public void deleteMina(
            Integer minaId,
            Integer usuarioId,
            String ipOrigen,
            String metodoHttp,
            String endpoint
    ) {
        log.info("Eliminando lógicamente mina ID: {}", minaId);

        // Buscar mina y validar permisos
        Minas mina = obtenerMinaActivaConPermisos(minaId, usuarioId);
        Usuarios usuario = obtenerUsuario(usuarioId);

        // ⚠️ VALIDACIÓN CRÍTICA: Verificar que NO tenga lotes ACTIVOS
        validarMinaSinLotesActivos(mina);

        String nombreMina = mina.getNombre();
        Integer minaIdAudit = mina.getId();

        // ELIMINACIÓN LÓGICA: Cambiar estado a 'inactivo'
        mina.setEstado("inactivo");
        minasRepository.save(mina);

        // Auditoría
        auditoriaBl.registrarEliminacionMina(
                usuario,
                minaIdAudit,
                nombreMina,
                ipOrigen,
                metodoHttp,
                endpoint
        );

        // Notificación
        enviarNotificacionEliminacion(usuarioId, nombreMina);

        log.info("Mina eliminada lógicamente - ID: {}", minaIdAudit);
    }

    /**
     * Obtener estadísticas de minas del socio
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getEstadisticas(Integer usuarioId) {
        log.debug("Obteniendo estadísticas de minas para usuario ID: {}", usuarioId);

        Socio socio = obtenerSocioDelUsuario(usuarioId);

        Map<String, Object> estadisticas = new HashMap<>();
        estadisticas.put("totalMinasActivas", minasRepository.countMinasActivasBySocio(socio));
        estadisticas.put("totalMinasInactivas", minasRepository.countMinasInactivasBySocio(socio));

        // Agrupar minas activas por sector
        List<Minas> minasActivas = minasRepository.findByActiveSocio(socio);
        Map<String, Long> minasPorSector = minasActivas.stream()
                .collect(Collectors.groupingBy(
                        m -> m.getSectoresId().getNombre(),
                        Collectors.counting()
                ));

        estadisticas.put("minasPorSector", minasPorSector);

        // Calcular estadísticas geográficas adicionales
        if (!minasActivas.isEmpty()) {
            estadisticas.put("estadisticasGeograficas", calcularEstadisticasGeograficas(minasActivas));
        }

        return estadisticas;
    }

    /**
     * Calcula estadísticas geográficas de las minas
     */
    private Map<String, Object> calcularEstadisticasGeograficas(List<Minas> minas) {
        Map<String, Object> geoStats = new HashMap<>();

        if (minas.size() < 2) {
            geoStats.put("distanciaPromedioEntreMinasKm", 0.0);
            geoStats.put("distanciaMaximaEntreMinasKm", 0.0);
            geoStats.put("distanciaMinimaEntreMinasKm", 0.0);
            return geoStats;
        }

        double sumaDistancias = 0.0;
        double distanciaMaxima = 0.0;
        double distanciaMinima = Double.MAX_VALUE;
        int contadorPares = 0;

        // Calcular distancias entre todas las parejas de minas
        for (int i = 0; i < minas.size(); i++) {
            for (int j = i + 1; j < minas.size(); j++) {
                Minas mina1 = minas.get(i);
                Minas mina2 = minas.get(j);

                double distancia = GeometryUtils.calcularDistancia(
                        mina1.getLatitud(), mina1.getLongitud(),
                        mina2.getLatitud(), mina2.getLongitud()
                );

                sumaDistancias += distancia;
                contadorPares++;

                if (distancia > distanciaMaxima) {
                    distanciaMaxima = distancia;
                }

                if (distancia < distanciaMinima) {
                    distanciaMinima = distancia;
                }
            }
        }

        double distanciaPromedio = sumaDistancias / contadorPares;

        geoStats.put("distanciaPromedioEntreMinasKm", Math.round(distanciaPromedio * 100.0) / 100.0);
        geoStats.put("distanciaMaximaEntreMinasKm", Math.round(distanciaMaxima * 100.0) / 100.0);
        geoStats.put("distanciaMinimaEntreMinasKm", Math.round(distanciaMinima * 100.0) / 100.0);

        return geoStats;
    }

    /**
     * VALIDACIÓN OPCIONAL: Verifica que no haya minas demasiado cerca
     * (Útil para evitar duplicados accidentales o conflictos)
     */
    private void validarDistanciaMinimaEntreMinasOpcional(
            java.math.BigDecimal latitud,
            java.math.BigDecimal longitud,
            Socio socio,
            Integer minaIdExcluir
    ) {
        final double DISTANCIA_MINIMA_METROS = 50.0; // 50 metros

        List<Minas> minasActivas = minasRepository.findByActiveSocio(socio);

        for (Minas mina : minasActivas) {
            // Excluir la mina que estamos actualizando
            if (minaIdExcluir != null && mina.getId().equals(minaIdExcluir)) {
                continue;
            }

            double distanciaKm = GeometryUtils.calcularDistancia(
                    latitud, longitud,
                    mina.getLatitud(), mina.getLongitud()
            );

            double distanciaMetros = distanciaKm * 1000;

            if (distanciaMetros < DISTANCIA_MINIMA_METROS) {
                throw new IllegalArgumentException(
                        String.format(
                                "Ya existe una mina ('%s') a menos de %.0f metros de esta ubicación (%.1f metros). " +
                                        "Por favor, verifica que no sea un duplicado.",
                                mina.getNombre(),
                                DISTANCIA_MINIMA_METROS,
                                distanciaMetros
                        )
                );
            }
        }
    }

    // ==================== MÉTODOS AUXILIARES PRIVADOS ====================

    /**
     * Obtiene socio del usuario
     */
    private Socio obtenerSocioDelUsuario(Integer usuarioId) {
        Usuarios usuario = obtenerUsuario(usuarioId);
        return obtenerSocioDelUsuario(usuario);
    }

    /**
     * Obtiene socio del usuario (sobrecarga)
     */
    private Socio obtenerSocioDelUsuario(Usuarios usuario) {
        return socioRepository.findByUsuariosId(usuario)
                .orElseThrow(() -> new IllegalArgumentException("Socio no encontrado"));
    }

    /**
     * Obtiene usuario por ID
     */
    private Usuarios obtenerUsuario(Integer usuarioId) {
        return usuariosRepository.findById(usuarioId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));
    }

    /**
     * Valida permisos de acceso a la mina
     */
    private void validarPermisosAccesoMina(Minas mina, Socio socio) {
        if (!mina.getSocioId().getId().equals(socio.getId())) {
            throw new IllegalArgumentException("No tienes permiso para acceder a esta mina");
        }
    }

    /**
     * Obtiene mina activa con validación de permisos
     */
    private Minas obtenerMinaActivaConPermisos(Integer minaId, Integer usuarioId) {
        Minas mina = minasRepository.findByIdAndEstadoActivo(minaId)
                .orElseThrow(() -> new IllegalArgumentException("Mina no encontrada o inactiva"));

        Socio socio = obtenerSocioDelUsuario(usuarioId);
        validarPermisosAccesoMina(mina, socio);

        return mina;
    }

    /**
     * Valida y obtiene sector, verificando que pertenezca a la cooperativa del socio
     */
    private Sectores validarYObtenerSector(Integer sectorId, Socio socio) {
        Sectores sector = sectoresRepository.findByIdAndEstadoActivo(sectorId)
                .orElseThrow(() -> new IllegalArgumentException("Sector no encontrado o inactivo"));

        // Verificar que el socio pertenezca a la cooperativa del sector
        if (!esSocioDeCooperativa(socio, sector.getCooperativaId())) {
            throw new IllegalArgumentException("El sector no pertenece a tu cooperativa");
        }

        return sector;
    }

    /**
     * Verifica si un socio pertenece a una cooperativa
     */
    private boolean esSocioDeCooperativa(Socio socio, Cooperativa cooperativa) {
        // Aquí deberías verificar en cooperativa_socio que el socio esté APROBADO
        // Esto depende de tu tabla cooperativa_socio
        // Por simplicidad, asumiré que tienes un repository para esto
        // Si no, puedes hacer la query directamente
        return true; // TODO: Implementar verificación real cuando tengas CooperativaSocioRepository
    }

    /**
     * Valida nombre único para el socio
     */
    private void validarNombreUnico(String nombre, Socio socio, Integer minaIdExcluir) {
        Optional<Minas> minaConMismoNombre = minasRepository.findByNombreAndActiveSocio(nombre, socio);

        if (minaConMismoNombre.isPresent() &&
                (minaIdExcluir == null || !minaConMismoNombre.get().getId().equals(minaIdExcluir))) {
            throw new IllegalArgumentException("Ya tienes una mina activa con ese nombre");
        }
    }

    /**
     * Valida que un punto esté dentro del polígono del sector
     */
    private void validarPuntoEnSector(
            java.math.BigDecimal latitud,
            java.math.BigDecimal longitud,
            Sectores sector
    ) {
        boolean dentroDelPoligono = GeometryUtils.puntoEnPoligono(
                latitud,
                longitud,
                sector.getCoordenadasList()
        );

        if (!dentroDelPoligono) {
            throw new IllegalArgumentException(
                    "La ubicación de la mina debe estar dentro del sector: " + sector.getNombre()
            );
        }
    }

    /**
     * Crea y guarda una nueva mina usando Builder
     */
    private Minas crearYGuardarMina(MinaCreateDto dto, Socio socio, Sectores sector) {
        return minasRepository.save(
                Minas.builder()
                        .nombre(dto.getNombre())
                        .fotoUrl(dto.getFotoUrl())
                        .latitud(dto.getLatitud())
                        .longitud(dto.getLongitud())
                        .estado("activo") // Siempre inicia como activo
                        .socioId(socio)
                        .sectoresId(sector)
                        .build()
        );
    }

    /**
     * Actualiza una mina existente
     */
    private void actualizarMina(Minas mina, MinaCreateDto dto, Sectores sector) {
        mina.setNombre(dto.getNombre());
        mina.setFotoUrl(dto.getFotoUrl());
        mina.setLatitud(dto.getLatitud());
        mina.setLongitud(dto.getLongitud());
        mina.setSectoresId(sector);

        minasRepository.save(mina);
    }

    /**
     * Determina campos modificados
     */
    private List<String> determinarCamposModificados(Minas mina, MinaCreateDto dto, Sectores sectorNuevo) {
        List<String> camposModificados = new ArrayList<>();

        if (!mina.getNombre().equals(dto.getNombre())) {
            camposModificados.add("nombre");
        }

        if (mina.getFotoUrl() == null || !mina.getFotoUrl().equals(dto.getFotoUrl())) {
            if (dto.getFotoUrl() != null) {
                camposModificados.add("fotoUrl");
            }
        }

        if (!mina.getLatitud().equals(dto.getLatitud()) || !mina.getLongitud().equals(dto.getLongitud())) {
            camposModificados.add("ubicacion");
        }

        if (!mina.getSectoresId().getId().equals(sectorNuevo.getId())) {
            camposModificados.add("sector");
        }

        return camposModificados;
    }

    /**
     * Valida que la mina no tenga lotes activos
     */
    private void validarMinaSinLotesActivos(Minas mina) {
        if (minasRepository.hasLotesActivos(mina)) {
            long cantidadLotesActivos = minasRepository.countLotesActivosByMina(mina);
            throw new IllegalArgumentException(
                    "No se puede eliminar la mina porque tiene " + cantidadLotesActivos +
                            " lote(s) activo(s). Solo se pueden eliminar minas sin lotes activos o con lotes completados/rechazados/cancelados."
            );
        }
    }

    /**
     * Envía notificación de creación
     */
    private void enviarNotificacionCreacion(Integer usuarioId, Minas mina, Sectores sector) {
        Map<String, Object> metadata = Map.of(
                "minaId", mina.getId(),
                "nombre", mina.getNombre(),
                "sectorId", sector.getId(),
                "sectorNombre", sector.getNombre(),
                "estado", "activo"
        );

        notificacionBl.crearNotificacion(
                usuarioId,
                "success",
                "Mina creada",
                "La mina '" + mina.getNombre() + "' ha sido creada exitosamente en el sector " + sector.getNombre(),
                metadata
        );
    }

    /**
     * Envía notificación de actualización
     */
    private void enviarNotificacionActualizacion(
            Integer usuarioId,
            Minas mina,
            List<String> camposModificados
    ) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("minaId", mina.getId());
        metadata.put("nombre", mina.getNombre());
        metadata.put("camposModificados", camposModificados);

        notificacionBl.crearNotificacion(
                usuarioId,
                "info",
                "Mina actualizada",
                "La mina '" + mina.getNombre() + "' ha sido actualizada",
                metadata
        );
    }

    /**
     * Envía notificación de eliminación
     */
    private void enviarNotificacionEliminacion(Integer usuarioId, String nombreMina) {
        Map<String, Object> metadata = Map.of(
                "nombre", nombreMina,
                "accion", "eliminacion_logica"
        );

        notificacionBl.crearNotificacion(
                usuarioId,
                "warning",
                "Mina eliminada",
                "La mina '" + nombreMina + "' ha sido marcada como inactiva",
                metadata
        );
    }

    /**
     * Convierte entidad a DTO
     */
    private MinaResponseDto convertToDto(Minas mina) {
        MinaResponseDto dto = new MinaResponseDto();

        dto.setId(mina.getId());
        dto.setNombre(mina.getNombre());
        dto.setFotoUrl(mina.getFotoUrl());
        dto.setLatitud(mina.getLatitud());
        dto.setLongitud(mina.getLongitud());
        dto.setEstado(mina.getEstado());

        // Información del sector
        dto.setSectorId(mina.getSectoresId().getId());
        dto.setSectorNombre(mina.getSectoresId().getNombre());
        dto.setSectorColor(mina.getSectoresId().getColor());

        // Información del socio
        dto.setSocioId(mina.getSocioId().getId());
        // Aquí deberías obtener el nombre completo del socio desde la tabla Persona
        // Por simplicidad, asumo que tienes una relación o puedes hacerlo
        dto.setSocioNombre("Socio #" + mina.getSocioId().getId()); // TODO: Obtener nombre real

        // Metadatos
        dto.setCreatedAt(mina.getCreatedAt());
        dto.setUpdatedAt(mina.getUpdatedAt());

        return dto;
    }

    /**
     * Valida datos de la mina
     */
    private void validateMinaData(MinaCreateDto dto) {
        if (dto.getNombre() == null || dto.getNombre().trim().isEmpty()) {
            throw new IllegalArgumentException("El nombre de la mina es requerido");
        }

        if (dto.getLatitud() == null) {
            throw new IllegalArgumentException("La latitud es requerida");
        }

        if (dto.getLongitud() == null) {
            throw new IllegalArgumentException("La longitud es requerida");
        }

        if (dto.getSectorId() == null) {
            throw new IllegalArgumentException("El sector es requerido");
        }

        // Validar rangos de latitud y longitud (Bolivia)
        // Bolivia: Latitud ~ -22° a -10°, Longitud ~ -70° a -58°
        if (dto.getLatitud().doubleValue() < -23 || dto.getLatitud().doubleValue() > -9) {
            throw new IllegalArgumentException("La latitud debe estar en el rango válido para Bolivia");
        }

        if (dto.getLongitud().doubleValue() < -71 || dto.getLongitud().doubleValue() > -57) {
            throw new IllegalArgumentException("La longitud debe estar en el rango válido para Bolivia");
        }
    }
}