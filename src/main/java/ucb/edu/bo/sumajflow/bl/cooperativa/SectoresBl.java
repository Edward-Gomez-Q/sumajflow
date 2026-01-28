package ucb.edu.bo.sumajflow.bl.cooperativa;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ucb.edu.bo.sumajflow.bl.AuditoriaBl;
import ucb.edu.bo.sumajflow.bl.NotificacionBl;
import ucb.edu.bo.sumajflow.dto.CoordenadaDto;
import ucb.edu.bo.sumajflow.dto.cooperativa.*;
import ucb.edu.bo.sumajflow.entity.*;
import ucb.edu.bo.sumajflow.repository.*;
import ucb.edu.bo.sumajflow.utils.GeometryUtils;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SectoresBl {

    // Inyección de dependencias por constructor con @RequiredArgsConstructor
    private final SectoresRepository sectoresRepository;
    private final SectoresCoordenadasRepository sectoresCoordenadasRepository;
    private final CooperativaRepository cooperativaRepository;
    private final UsuariosRepository usuariosRepository;
    private final MinasRepository minasRepository;
    private final AuditoriaBl auditoriaBl;
    private final NotificacionBl notificacionBl;
    private final CooperativaSocioRepository cooperativaSocioRepository;
    private final SocioRepository socioRepository;

    /**
     * Obtener todos los sectores ACTIVOS de una cooperativa
     */
    @Transactional(readOnly = true)
    public List<SectorResponseDto> getSectoresByCooperativa(Integer usuarioId) {
        log.debug("Obteniendo sectores para usuario ID: {}", usuarioId);

        Cooperativa cooperativa = obtenerCooperativaDelUsuario(usuarioId);
        List<Sectores> sectores = sectoresRepository.findByCooperativaIdAndEstadoActivo(cooperativa);

        log.info("Se encontraron {} sectores activos", sectores.size());

        return sectores.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    /**
     * Obtener un sector por ID (solo si está activo)
     */
    @Transactional(readOnly = true)
    public SectorResponseDto getSectorById(Integer sectorId, Integer usuarioId) {
        log.debug("Obteniendo sector ID: {} para usuario ID: {}", sectorId, usuarioId);

        Sectores sector = sectoresRepository.findByIdAndEstadoActivo(sectorId)
                .orElseThrow(() -> new IllegalArgumentException("Sector no encontrado o inactivo"));

        // Verificar permisos
        Cooperativa cooperativa = obtenerCooperativaDelUsuario(usuarioId);
        validarPermisosAccesoSector(sector, cooperativa);

        return convertToDto(sector);
    }

    /**
     * Crear un nuevo sector
     */
    @Transactional
    public SectorResponseDto createSector(
            SectorCreateDto dto,
            Integer usuarioId,
            String ipOrigen,
            String metodoHttp,
            String endpoint
    ) {
        log.info("Creando nuevo sector: {}", dto.getNombre());

        // Validar datos básicos
        validateSectorData(dto);

        // Buscar usuario y cooperativa
        Usuarios usuario = obtenerUsuario(usuarioId);
        Cooperativa cooperativa = obtenerCooperativaDelUsuario(usuario);

        // Validar unicidad
        validarNombreYColorUnicos(dto, cooperativa, null);

        // Crear sector
        Sectores sector = crearYGuardarSector(dto, cooperativa);

        // Crear coordenadas
        List<SectoresCoordenadas> coordenadas = crearYGuardarCoordenadas(dto.getCoordenadas(), sector);

        // Auditoría
        auditoriaBl.registrarCreacionSector(
                usuario,
                sector.getId(),
                sector.getNombre(),
                coordenadas.size(),
                ipOrigen,
                metodoHttp,
                endpoint
        );

        // Notificación
        enviarNotificacionCreacion(usuarioId, sector, coordenadas.size());

        sector.setCoordenadasList(coordenadas);
        log.info("Sector creado exitosamente - ID: {}", sector.getId());
        return convertToDto(sector);
    }

    /**
     * Actualizar un sector existente
     */
    @Transactional
    public SectorResponseDto updateSector(
            Integer sectorId,
            SectorCreateDto dto,
            Integer usuarioId,
            String ipOrigen,
            String metodoHttp,
            String endpoint
    ) {
        log.info("Actualizando sector ID: {}", sectorId);

        // Validar datos
        validateSectorData(dto);

        // Buscar sector y validar permisos
        Sectores sector = obtenerSectorActivoConPermisos(sectorId, usuarioId);
        Usuarios usuario = obtenerUsuario(usuarioId);
        Cooperativa cooperativa = sector.getCooperativaId();

        // Guardar datos anteriores
        String nombreAnterior = sector.getNombre();
        String colorAnterior = sector.getColor();

        // Validar unicidad
        validarNombreYColorUnicos(dto, cooperativa, sectorId);

        // Preparar nuevas coordenadas
        List<SectoresCoordenadas> coordenadasNuevas = prepararCoordenadas(dto.getCoordenadas(), sector);

        // Validar minas si cambió el polígono
        boolean coordenadasCambiaron = hanCambiadoCoordenadas(sector.getCoordenadasList(), coordenadasNuevas);
        if (coordenadasCambiaron) {
            validarMinasEnNuevoPoligono(sector, coordenadasNuevas);
        }

        // Actualizar sector y coordenadas
        actualizarSectorYCoordenadas(sector, dto, coordenadasNuevas);

        // Auditoría
        List<String> camposModificados = determinarCamposModificados(
                nombreAnterior, colorAnterior, dto, coordenadasCambiaron);

        auditoriaBl.registrarActualizacionSector(
                usuario,
                sector.getId(),
                nombreAnterior,
                sector.getNombre(),
                coordenadasNuevas.size(),
                ipOrigen,
                metodoHttp,
                endpoint
        );

        // Notificación
        enviarNotificacionActualizacion(usuarioId, sector, coordenadasNuevas.size(), camposModificados);

        log.info("Sector actualizado exitosamente - ID: {}", sectorId);
        return convertToDto(sector);
    }

    /**
     * ELIMINACIÓN LÓGICA de un sector
     */
    @Transactional
    public void deleteSector(
            Integer sectorId,
            Integer usuarioId,
            String ipOrigen,
            String metodoHttp,
            String endpoint
    ) {
        log.info("Eliminando lógicamente sector ID: {}", sectorId);

        // Buscar sector y validar permisos
        Sectores sector = obtenerSectorActivoConPermisos(sectorId, usuarioId);
        Usuarios usuario = obtenerUsuario(usuarioId);

        // VALIDACIÓN CRÍTICA: Verificar que NO tenga minas ACTIVAS
        validarSectorSinMinasActivas(sector);

        String nombreSector = sector.getNombre();
        Integer sectorIdAudit = sector.getId();

        // ELIMINACIÓN LÓGICA: Cambiar estado a 'inactivo'
        sector.setEstado("inactivo");
        sectoresRepository.save(sector);

        // Auditoría
        registrarAuditoriaEliminacion(usuario, sectorIdAudit, nombreSector, ipOrigen, metodoHttp, endpoint);

        // Notificación
        enviarNotificacionEliminacion(usuarioId, nombreSector);

        log.info("Sector eliminado lógicamente - ID: {}", sectorIdAudit);
    }

    /**
     * Obtener estadísticas de sectores (solo activos por defecto)
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getEstadisticas(Integer usuarioId) {
        log.debug("Obteniendo estadísticas de sectores para usuario ID: {}", usuarioId);

        Cooperativa cooperativa = obtenerCooperativaDelUsuario(usuarioId);
        List<Sectores> sectoresActivos = sectoresRepository.findByCooperativaIdAndEstadoActivo(cooperativa);

        Map<String, Object> estadisticas = new HashMap<>();
        estadisticas.put("totalSectoresActivos", sectoresActivos.size());

        // Contar sectores con minas activas
        long sectoresConMinasActivas = sectoresActivos.stream()
                .filter(minasRepository::existsMinasActivasInSector)
                .count();

        estadisticas.put("sectoresConMinasActivas", sectoresConMinasActivas);
        estadisticas.put("sectoresSinMinasActivas", sectoresActivos.size() - sectoresConMinasActivas);

        // Calcular área total de sectores activos
        double areaTotal = sectoresActivos.stream()
                .mapToDouble(s -> GeometryUtils.calcularArea(s.getCoordenadasList()))
                .sum();
        estadisticas.put("areaTotalHectareas", Math.round(areaTotal * 100.0) / 100.0);

        // Contar sectores inactivos
        long sectoresInactivos = sectoresRepository.countByCooperativaIdAndEstadoInactivo(cooperativa);
        estadisticas.put("sectoresInactivos", sectoresInactivos);

        return estadisticas;
    }

    // ==================== MÉTODOS AUXILIARES PRIVADOS ====================

    /**
     * Obtiene cooperativa del usuario
     */
    private Cooperativa obtenerCooperativaDelUsuario(Integer usuarioId) {
        Usuarios usuario = obtenerUsuario(usuarioId);
        return obtenerCooperativaDelUsuario(usuario);
    }

    /**
     * Obtiene cooperativa del usuario (sobrecarga)
     */
    private Cooperativa obtenerCooperativaDelUsuario(Usuarios usuario) {
        return cooperativaRepository.findByUsuariosId(usuario)
                .orElseThrow(() -> new IllegalArgumentException("Cooperativa no encontrada"));
    }

    /**
     * Obtiene usuario por ID
     */
    private Usuarios obtenerUsuario(Integer usuarioId) {
        return usuariosRepository.findById(usuarioId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));
    }

    /**
     * Valida permisos de acceso al sector
     */
    private void validarPermisosAccesoSector(Sectores sector, Cooperativa cooperativa) {
        if (!sector.getCooperativaId().getId().equals(cooperativa.getId())) {
            throw new IllegalArgumentException("No tienes permiso para acceder a este sector");
        }
    }

    /**
     * Obtiene sector activo con validación de permisos
     */
    private Sectores obtenerSectorActivoConPermisos(Integer sectorId, Integer usuarioId) {
        Sectores sector = sectoresRepository.findByIdAndEstadoActivo(sectorId)
                .orElseThrow(() -> new IllegalArgumentException("Sector no encontrado o inactivo"));

        Cooperativa cooperativa = obtenerCooperativaDelUsuario(usuarioId);
        validarPermisosAccesoSector(sector, cooperativa);

        return sector;
    }

    /**
     * Valida nombre y color únicos
     */
    private void validarNombreYColorUnicos(SectorCreateDto dto, Cooperativa cooperativa, Integer sectorIdExcluir) {
        // Validar nombre único
        Optional<Sectores> sectorConMismoNombre = sectoresRepository
                .findByNombreAndCooperativaIdAndEstadoActivo(dto.getNombre(), cooperativa);

        if (sectorConMismoNombre.isPresent() &&
                (sectorIdExcluir == null || !sectorConMismoNombre.get().getId().equals(sectorIdExcluir))) {
            throw new IllegalArgumentException("Ya existe un sector activo con ese nombre");
        }

        // Validar color único
        Optional<Sectores> sectorConMismoColor = sectoresRepository
                .findByColorAndCooperativaIdAndEstadoActivo(dto.getColor(), cooperativa);

        if (sectorConMismoColor.isPresent() &&
                (sectorIdExcluir == null || !sectorConMismoColor.get().getId().equals(sectorIdExcluir))) {
            throw new IllegalArgumentException("Ya existe un sector activo con ese color");
        }
    }

    /**
     * Crea y guarda un nuevo sector usando Builder
     */
    private Sectores crearYGuardarSector(SectorCreateDto dto, Cooperativa cooperativa) {
        return sectoresRepository.save(
                Sectores.builder()
                        .nombre(dto.getNombre())
                        .color(dto.getColor())
                        .estado("activo")
                        .cooperativaId(cooperativa)
                        .build()
        );
    }

    /**
     * Crea y guarda coordenadas usando Builder
     */
    private List<SectoresCoordenadas> crearYGuardarCoordenadas(
            List<CoordenadaDto> coordenadasDto,
            Sectores sector
    ) {
        List<SectoresCoordenadas> coordenadas = coordenadasDto.stream()
                .map(coordDto -> SectoresCoordenadas.builder()
                        .orden(coordDto.getOrden())
                        .latitud(coordDto.getLatitud())
                        .longitud(coordDto.getLongitud())
                        .sectoresId(sector)
                        .build())
                .collect(Collectors.toList());

        return sectoresCoordenadasRepository.saveAll(coordenadas);
    }

    /**
     * Prepara coordenadas sin guardar
     */
    private List<SectoresCoordenadas> prepararCoordenadas(
            List<CoordenadaDto> coordenadasDto,
            Sectores sector
    ) {
        return coordenadasDto.stream()
                .map(coordDto -> SectoresCoordenadas.builder()
                        .orden(coordDto.getOrden())
                        .latitud(coordDto.getLatitud())
                        .longitud(coordDto.getLongitud())
                        .sectoresId(sector)
                        .build())
                .sorted(Comparator.comparing(SectoresCoordenadas::getOrden))
                .collect(Collectors.toList());
    }

    /**
     * Valida que las minas activas estén dentro del nuevo polígono
     */
    private void validarMinasEnNuevoPoligono(Sectores sector, List<SectoresCoordenadas> coordenadasNuevas) {
        List<Minas> minasActivas = minasRepository.findMinasActivasBySector(sector);

        if (!minasActivas.isEmpty()) {
            List<String> minasFuera = minasActivas.stream()
                    .filter(mina -> !GeometryUtils.puntoEnPoligono(
                            mina.getLatitud(),
                            mina.getLongitud(),
                            coordenadasNuevas))
                    .map(Minas::getNombre)
                    .collect(Collectors.toList());

            if (!minasFuera.isEmpty()) {
                throw new IllegalArgumentException(
                        "No se puede modificar el sector porque las siguientes minas activas quedarían fuera: "
                                + String.join(", ", minasFuera)
                );
            }
        }
    }

    /**
     * Actualiza sector y coordenadas
     */
    private void actualizarSectorYCoordenadas(
            Sectores sector,
            SectorCreateDto dto,
            List<SectoresCoordenadas> coordenadasNuevas
    ) {
        sector.setNombre(dto.getNombre());
        sector.setColor(dto.getColor());
        sector.getCoordenadasList().clear();
        sectoresRepository.saveAndFlush(sector);
        coordenadasNuevas.forEach(c -> c.setSectoresId(sector));
        List<SectoresCoordenadas> coordenadasGuardadas = sectoresCoordenadasRepository.saveAll(coordenadasNuevas);
        sector.getCoordenadasList().addAll(coordenadasGuardadas);
    }

    /**
     * Determina campos modificados
     */
    private List<String> determinarCamposModificados(
            String nombreAnterior,
            String colorAnterior,
            SectorCreateDto dto,
            boolean coordenadasCambiaron
    ) {
        List<String> camposModificados = new ArrayList<>();
        if (!nombreAnterior.equals(dto.getNombre())) camposModificados.add("nombre");
        if (!colorAnterior.equals(dto.getColor())) camposModificados.add("color");
        if (coordenadasCambiaron) camposModificados.add("coordenadas");
        return camposModificados;
    }

    /**
     * Valida que el sector no tenga minas activas
     */
    private void validarSectorSinMinasActivas(Sectores sector) {
        if (minasRepository.existsMinasActivasInSector(sector)) {
            long cantidadMinasActivas = minasRepository.countMinasActivasBySector(sector);
            throw new IllegalArgumentException(
                    "No se puede eliminar el sector porque tiene " + cantidadMinasActivas +
                            " mina(s) activa(s). Solo se pueden eliminar sectores sin minas activas o con minas inactivas."
            );
        }
    }

    /**
     * Registra auditoría de eliminación
     */
    private void registrarAuditoriaEliminacion(
            Usuarios usuario,
            Integer sectorId,
            String nombreSector,
            String ipOrigen,
            String metodoHttp,
            String endpoint
    ) {
        Map<String, Object> datosAnteriores = Map.of(
                "nombre", nombreSector,
                "estado", "activo"
        );

        Map<String, Object> datosNuevos = Map.of(
                "nombre", nombreSector,
                "estado", "inactivo"
        );

        auditoriaBl.registrar(
                usuario,
                "sectores",
                "DELETE_LOGICO",
                "Eliminación lógica del sector: " + nombreSector + " (ID: " + sectorId + ")",
                sectorId,
                datosAnteriores,
                datosNuevos,
                List.of("estado"),
                ipOrigen,
                null,
                metodoHttp,
                endpoint,
                "RF02",
                "ALTO"
        );
    }

    /**
     * Envía notificación de creación
     */
    private void enviarNotificacionCreacion(Integer usuarioId, Sectores sector, int numeroCoordenadas) {
        Map<String, Object> metadata = Map.of(
                "sectorId", sector.getId(),
                "nombre", sector.getNombre(),
                "numeroCoordenadas", numeroCoordenadas,
                "estado", "activo"
        );

        notificacionBl.crearNotificacion(
                usuarioId,
                "success",
                "Sector creado",
                "El sector '" + sector.getNombre() + "' ha sido creado exitosamente",
                metadata
        );
    }

    /**
     * Envía notificación de actualización
     */
    private void enviarNotificacionActualizacion(
            Integer usuarioId,
            Sectores sector,
            int numeroCoordenadas,
            List<String> camposModificados
    ) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("sectorId", sector.getId());
        metadata.put("nombre", sector.getNombre());
        metadata.put("numeroCoordenadas", numeroCoordenadas);
        metadata.put("camposModificados", camposModificados);

        notificacionBl.crearNotificacion(
                usuarioId,
                "info",
                "Sector actualizado",
                "El sector '" + sector.getNombre() + "' ha sido actualizado",
                metadata
        );
    }

    /**
     * Envía notificación de eliminación
     */
    private void enviarNotificacionEliminacion(Integer usuarioId, String nombreSector) {
        Map<String, Object> metadata = Map.of(
                "nombre", nombreSector,
                "accion", "eliminacion_logica"
        );

        notificacionBl.crearNotificacion(
                usuarioId,
                "warning",
                "Sector eliminado",
                "El sector '" + nombreSector + "' ha sido marcado como inactivo",
                metadata
        );
    }

    /**
     * Convierte entidad a DTO
     */
    private SectorResponseDto convertToDto(Sectores sector) {
        List<CoordenadaResponseDto> coordenadas = sector.getCoordenadasList().stream()
                .sorted(Comparator.comparing(SectoresCoordenadas::getOrden))
                .map(coord -> new CoordenadaResponseDto(
                        coord.getId(),
                        coord.getOrden(),
                        coord.getLatitud(),
                        coord.getLongitud()
                ))
                .collect(Collectors.toList());

        return new SectorResponseDto(
                sector.getId(),
                sector.getNombre(),
                sector.getColor(),
                coordenadas,
                GeometryUtils.calcularArea(sector.getCoordenadasList()),
                sector.getEstado()
        );
    }

    /**
     * Valida datos del sector
     */
    private void validateSectorData(SectorCreateDto dto) {
        if (dto.getNombre() == null || dto.getNombre().trim().isEmpty()) {
            throw new IllegalArgumentException("El nombre del sector es requerido");
        }

        if (dto.getColor() == null || dto.getColor().trim().isEmpty()) {
            throw new IllegalArgumentException("El color del sector es requerido");
        }

        if (dto.getCoordenadas() == null || dto.getCoordenadas().size() < 3) {
            throw new IllegalArgumentException("El sector debe tener al menos 3 coordenadas");
        }

        // Validar cada coordenada
        for (CoordenadaDto coord : dto.getCoordenadas()) {
            if (coord.getLatitud() == null || coord.getLongitud() == null) {
                throw new IllegalArgumentException("Todas las coordenadas deben tener latitud y longitud");
            }
            if (coord.getOrden() == null || coord.getOrden() < 1) {
                throw new IllegalArgumentException("El orden de las coordenadas debe ser válido");
            }
        }
    }

    /**
     * Verifica si las coordenadas de un sector han cambiado
     */
    private boolean hanCambiadoCoordenadas(
            List<SectoresCoordenadas> coordenadasAnteriores,
            List<SectoresCoordenadas> coordenadasNuevas
    ) {
        if (coordenadasAnteriores.size() != coordenadasNuevas.size()) {
            return true;
        }

        List<SectoresCoordenadas> antiguasOrdenadas = coordenadasAnteriores.stream()
                .sorted(Comparator.comparing(SectoresCoordenadas::getOrden))
                .toList();

        List<SectoresCoordenadas> nuevasOrdenadas = coordenadasNuevas.stream()
                .sorted(Comparator.comparing(SectoresCoordenadas::getOrden))
                .toList();

        for (int i = 0; i < antiguasOrdenadas.size(); i++) {
            SectoresCoordenadas antigua = antiguasOrdenadas.get(i);
            SectoresCoordenadas nueva = nuevasOrdenadas.get(i);

            if (!antigua.getLatitud().equals(nueva.getLatitud()) ||
                    !antigua.getLongitud().equals(nueva.getLongitud())) {
                return true;
            }
        }

        return false;
    }
    /**
     * Obtener todos los sectores ACTIVOS de la cooperativa a la que pertenece el socio
     * Este metodo se usa desde el endpoint /socio/sectores
     */
    @Transactional(readOnly = true)
    public List<SectorResponseDto> getSectoresByCooperativaParaSocio(Integer usuarioId) {
        log.debug("Obteniendo sectores para socio - Usuario ID: {}", usuarioId);

        Socio socio = obtenerSocioDelUsuario(usuarioId);

        // Obtener cooperativa del socio (a través de cooperativa_socio)
        Cooperativa cooperativa = obtenerCooperativaDelSocio(socio);

        // Obtener sectores activos de esa cooperativa
        List<Sectores> sectores = sectoresRepository.findByCooperativaIdAndEstadoActivo(cooperativa);

        log.info("Se encontraron {} sectores activos para el socio", sectores.size());

        return sectores.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    /**
     * Obtener un sector por ID (validando que el socio pertenezca a la cooperativa del sector)
     * Este metodo se usa desde el endpoint /socio/sectores/{id}
     */
    @Transactional(readOnly = true)
    public SectorResponseDto getSectorByIdParaSocio(Integer sectorId, Integer usuarioId) {
        log.debug("Obteniendo sector ID: {} para socio - Usuario ID: {}", sectorId, usuarioId);

        Sectores sector = sectoresRepository.findByIdAndEstadoActivo(sectorId)
                .orElseThrow(() -> new IllegalArgumentException("Sector no encontrado o inactivo"));

        // Obtener socio y su cooperativa
        Socio socio = obtenerSocioDelUsuario(usuarioId);
        Cooperativa cooperativaDelSocio = obtenerCooperativaDelSocio(socio);

        // Validar que el sector pertenezca a la cooperativa del socio
        if (!sector.getCooperativaId().getId().equals(cooperativaDelSocio.getId())) {
            throw new IllegalArgumentException("Este sector no pertenece a tu cooperativa");
        }

        return convertToDto(sector);
    }

    /**
     * Obtener estadísticas de sectores para el socio
     * Este metodo se usa desde el endpoint /socio/sectores/estadisticas
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getEstadisticasParaSocio(Integer usuarioId) {
        log.debug("Obteniendo estadísticas de sectores para socio - Usuario ID: {}", usuarioId);

        // Obtener socio y su cooperativa
        Socio socio = obtenerSocioDelUsuario(usuarioId);
        Cooperativa cooperativa = obtenerCooperativaDelSocio(socio);

        // Reutilizar la lógica existente
        List<Sectores> sectoresActivos = sectoresRepository.findByCooperativaIdAndEstadoActivo(cooperativa);

        Map<String, Object> estadisticas = new HashMap<>();
        estadisticas.put("totalSectoresActivos", sectoresActivos.size());

        // Contar sectores con minas activas
        long sectoresConMinasActivas = sectoresActivos.stream()
                .filter(minasRepository::existsMinasActivasInSector)
                .count();

        estadisticas.put("sectoresConMinasActivas", sectoresConMinasActivas);
        estadisticas.put("sectoresSinMinasActivas", sectoresActivos.size() - sectoresConMinasActivas);

        // Contar cuántas de esas minas son del socio autenticado
        long minasPropiasSocio = sectoresActivos.stream()
                .flatMap(sector -> minasRepository.findMinasActivasBySector(sector).stream())
                .filter(mina -> mina.getSocioId().getId().equals(socio.getId()))
                .count();

        estadisticas.put("misPropiasMinas", minasPropiasSocio);

        // Calcular área total
        double areaTotal = sectoresActivos.stream()
                .mapToDouble(s -> GeometryUtils.calcularArea(s.getCoordenadasList()))
                .sum();
        estadisticas.put("areaTotalHectareas", Math.round(areaTotal * 100.0) / 100.0);

        return estadisticas;
    }

    /**
     * Obtiene socio del usuario
     */
    private Socio obtenerSocioDelUsuario(Integer usuarioId) {
        Usuarios usuario = obtenerUsuario(usuarioId);
        return socioRepository.findByUsuariosId(usuario)
                .orElseThrow(() -> new IllegalArgumentException("Socio no encontrado"));
    }

    /**
     * Obtiene la cooperativa a la que pertenece un socio
     * Busca en la tabla cooperativa_socio donde el socio esté APROBADO
     */
    private Cooperativa obtenerCooperativaDelSocio(Socio socio) {
        CooperativaSocio cooperativaSocio = cooperativaSocioRepository
                .findBySocioIdAndEstado(socio, "aprobado")
                .orElseThrow(() -> new IllegalArgumentException("No perteneces a ninguna cooperativa aprobada"));

        return cooperativaSocio.getCooperativaId();
    }
}