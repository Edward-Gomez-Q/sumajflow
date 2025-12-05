// src/main/java/ucb/edu/bo/sumajflow/bl/cooperativa/SectoresBl.java
package ucb.edu.bo.sumajflow.bl.cooperativa;

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

@Service
public class SectoresBl {

    private final SectoresRepository sectoresRepository;
    private final SectoresCoordenadasRepository sectoresCoordenadasRepository;
    private final CooperativaRepository cooperativaRepository;
    private final UsuariosRepository usuariosRepository;
    private final MinasRepository minasRepository;
    private final AuditoriaBl auditoriaBl;
    private final NotificacionBl notificacionBl;

    public SectoresBl(
            SectoresRepository sectoresRepository,
            SectoresCoordenadasRepository sectoresCoordenadasRepository,
            CooperativaRepository cooperativaRepository,
            UsuariosRepository usuariosRepository,
            MinasRepository minasRepository,
            AuditoriaBl auditoriaBl,
            NotificacionBl notificacionBl
    ) {
        this.sectoresRepository = sectoresRepository;
        this.sectoresCoordenadasRepository = sectoresCoordenadasRepository;
        this.cooperativaRepository = cooperativaRepository;
        this.usuariosRepository = usuariosRepository;
        this.minasRepository = minasRepository;
        this.auditoriaBl = auditoriaBl;
        this.notificacionBl = notificacionBl;
    }

    /**
     * Obtener todos los sectores ACTIVOS de una cooperativa
     */
    public List<SectorResponseDto> getSectoresByCooperativa(Integer usuarioId) {
        Usuarios usuario = usuariosRepository.findById(usuarioId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        Cooperativa cooperativa = cooperativaRepository.findByUsuariosId(usuario)
                .orElseThrow(() -> new IllegalArgumentException("Cooperativa no encontrada"));

        // Solo sectores activos
        List<Sectores> sectores = sectoresRepository.findByCooperativaIdAndEstadoActivo(cooperativa);

        return sectores.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    /**
     * Obtener un sector por ID (solo si está activo)
     */
    public SectorResponseDto getSectorById(Integer sectorId, Integer usuarioId) {
        Sectores sector = sectoresRepository.findByIdAndEstadoActivo(sectorId)
                .orElseThrow(() -> new IllegalArgumentException("Sector no encontrado o inactivo"));

        // Verificar permisos
        Usuarios usuario = usuariosRepository.findById(usuarioId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        Cooperativa cooperativa = cooperativaRepository.findByUsuariosId(usuario)
                .orElseThrow(() -> new IllegalArgumentException("Cooperativa no encontrada"));

        if (!sector.getCooperativaId().getId().equals(cooperativa.getId())) {
            throw new IllegalArgumentException("No tienes permiso para acceder a este sector");
        }

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
        // Validar datos básicos
        validateSectorData(dto);

        // Buscar usuario y cooperativa
        Usuarios usuario = usuariosRepository.findById(usuarioId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        Cooperativa cooperativa = cooperativaRepository.findByUsuariosId(usuario)
                .orElseThrow(() -> new IllegalArgumentException("Cooperativa no encontrada"));

        // Validar nombre único entre sectores ACTIVOS
        if (sectoresRepository.existsByNombreAndCooperativaIdAndEstadoActivo(dto.getNombre(), cooperativa)) {
            throw new IllegalArgumentException("Ya existe un sector activo con ese nombre");
        }

        // Validar color único entre sectores ACTIVOS
        if (sectoresRepository.existsByColorAndCooperativaIdAndEstadoActivo(dto.getColor(), cooperativa)) {
            throw new IllegalArgumentException("Ya existe un sector activo con ese color");
        }

        // Crear sector con estado 'activo'
        Sectores sector = new Sectores();
        sector.setNombre(dto.getNombre());
        sector.setColor(dto.getColor());
        sector.setEstado("activo");
        sector.setCooperativaId(cooperativa);
        sector = sectoresRepository.save(sector);

        // Crear coordenadas
        final Sectores finalSector = sector;
        List<SectoresCoordenadas> coordenadas = dto.getCoordenadas().stream()
                .map(coordDto -> {
                    SectoresCoordenadas coord = new SectoresCoordenadas();
                    coord.setOrden(coordDto.getOrden());
                    coord.setLatitud(coordDto.getLatitud());
                    coord.setLongitud(coordDto.getLongitud());
                    coord.setSectoresId(finalSector);
                    return coord;
                })
                .collect(Collectors.toList());

        sectoresCoordenadasRepository.saveAll(coordenadas);

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
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("sectorId", sector.getId());
        metadata.put("nombre", sector.getNombre());
        metadata.put("numeroCoordenadas", coordenadas.size());
        metadata.put("estado", "activo");

        notificacionBl.crearNotificacion(
                usuarioId,
                "success",
                "Sector creado",
                "El sector '" + sector.getNombre() + "' ha sido creado exitosamente",
                metadata
        );

        sector.setSectoresCoordenadasList(coordenadas);
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
        // Validar datos
        validateSectorData(dto);

        // Buscar sector (solo activos)
        Sectores sector = sectoresRepository.findByIdAndEstadoActivo(sectorId)
                .orElseThrow(() -> new IllegalArgumentException("Sector no encontrado o inactivo"));

        // Verificar permisos
        Usuarios usuario = usuariosRepository.findById(usuarioId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        Cooperativa cooperativa = cooperativaRepository.findByUsuariosId(usuario)
                .orElseThrow(() -> new IllegalArgumentException("Cooperativa no encontrada"));

        if (!sector.getCooperativaId().getId().equals(cooperativa.getId())) {
            throw new IllegalArgumentException("No tienes permiso para modificar este sector");
        }

        // Guardar datos anteriores para auditoría
        String nombreAnterior = sector.getNombre();
        String colorAnterior = sector.getColor();

        // Validar nombre único
        Optional<Sectores> sectorConMismoNombre = sectoresRepository
                .findByNombreAndCooperativaIdAndEstadoActivo(dto.getNombre(), cooperativa);
        if (sectorConMismoNombre.isPresent() && !sectorConMismoNombre.get().getId().equals(sectorId)) {
            throw new IllegalArgumentException("Ya existe un sector activo con ese nombre");
        }

        // Validar color único
        Optional<Sectores> sectorConMismoColor = sectoresRepository
                .findByColorAndCooperativaIdAndEstadoActivo(dto.getColor(), cooperativa);
        if (sectorConMismoColor.isPresent() && !sectorConMismoColor.get().getId().equals(sectorId)) {
            throw new IllegalArgumentException("Ya existe un sector activo con ese color");
        }

        // Si hay cambio de coordenadas, verificar que todas las minas activas
        // del sector sigan dentro del nuevo polígono
        List<SectoresCoordenadas> coordenadasNuevas = dto.getCoordenadas().stream()
                .map(coordDto -> {
                    SectoresCoordenadas coord = new SectoresCoordenadas();
                    coord.setOrden(coordDto.getOrden());
                    coord.setLatitud(coordDto.getLatitud());
                    coord.setLongitud(coordDto.getLongitud());
                    coord.setSectoresId(sector);
                    return coord;
                })
                .sorted(Comparator.comparing(SectoresCoordenadas::getOrden))
                .collect(Collectors.toList());

        // Verificar si las coordenadas han cambiado
        boolean coordenadasCambiaron = hanCambiadoCoordenadas(sector.getSectoresCoordenadasList(), coordenadasNuevas);

        if (coordenadasCambiaron) {
            // Validar que todas las minas activas sigan dentro del nuevo polígono
            List<Minas> minasActivas = minasRepository.findMinasActivasBySector(sector);

            if (!minasActivas.isEmpty()) {
                List<String> minasFuera = new ArrayList<>();

                for (Minas mina : minasActivas) {
                    boolean dentroDelPoligono = GeometryUtils.puntoEnPoligono(
                            mina.getLatitud(),
                            mina.getLongitud(),
                            coordenadasNuevas
                    );

                    if (!dentroDelPoligono) {
                        minasFuera.add(mina.getNombre());
                    }
                }

                if (!minasFuera.isEmpty()) {
                    throw new IllegalArgumentException(
                            "No se puede modificar el sector porque las siguientes minas activas quedarían fuera: "
                                    + String.join(", ", minasFuera)
                    );
                }
            }
        }

        // Actualizar sector
        sector.setNombre(dto.getNombre());
        sector.setColor(dto.getColor());

        // Eliminar coordenadas antiguas
        sectoresCoordenadasRepository.deleteBySectoresId(sector);

        // Guardar nuevas coordenadas
        sectoresCoordenadasRepository.saveAll(coordenadasNuevas);
        sectoresRepository.save(sector);

        // Auditoría
        List<String> camposModificados = new ArrayList<>();
        if (!nombreAnterior.equals(dto.getNombre())) camposModificados.add("nombre");
        if (!colorAnterior.equals(dto.getColor())) camposModificados.add("color");
        if (coordenadasCambiaron) camposModificados.add("coordenadas");

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
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("sectorId", sector.getId());
        metadata.put("nombre", sector.getNombre());
        metadata.put("numeroCoordenadas", coordenadasNuevas.size());
        metadata.put("camposModificados", camposModificados);

        notificacionBl.crearNotificacion(
                usuarioId,
                "info",
                "Sector actualizado",
                "El sector '" + sector.getNombre() + "' ha sido actualizado",
                metadata
        );

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
        // Buscar sector (solo activos)
        Sectores sector = sectoresRepository.findByIdAndEstadoActivo(sectorId)
                .orElseThrow(() -> new IllegalArgumentException("Sector no encontrado o ya está inactivo"));

        // Verificar permisos
        Usuarios usuario = usuariosRepository.findById(usuarioId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        Cooperativa cooperativa = cooperativaRepository.findByUsuariosId(usuario)
                .orElseThrow(() -> new IllegalArgumentException("Cooperativa no encontrada"));

        if (!sector.getCooperativaId().getId().equals(cooperativa.getId())) {
            throw new IllegalArgumentException("No tienes permiso para eliminar este sector");
        }

        // VALIDACIÓN CRÍTICA: Verificar que NO tenga minas ACTIVAS
        if (minasRepository.existsMinasActivasInSector(sector)) {
            long cantidadMinasActivas = minasRepository.countMinasActivasBySector(sector);
            throw new IllegalArgumentException(
                    "No se puede eliminar el sector porque tiene " + cantidadMinasActivas +
                            " mina(s) activa(s). Solo se pueden eliminar sectores sin minas activas o con minas inactivas."
            );
        }

        String nombreSector = sector.getNombre();
        Integer sectorIdAudit = sector.getId();

        // ELIMINACIÓN LÓGICA: Cambiar estado a 'inactivo'
        sector.setEstado("inactivo");
        sectoresRepository.save(sector);

        // Auditoría
        Map<String, Object> datosAnteriores = new HashMap<>();
        datosAnteriores.put("nombre", nombreSector);
        datosAnteriores.put("estado", "activo");

        Map<String, Object> datosNuevos = new HashMap<>();
        datosNuevos.put("nombre", nombreSector);
        datosNuevos.put("estado", "inactivo");

        auditoriaBl.registrar(
                usuario,
                "sectores",
                "DELETE_LOGICO",
                "Eliminación lógica del sector: " + nombreSector + " (ID: " + sectorIdAudit + ")",
                sectorIdAudit,
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

        // Notificación
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("nombre", nombreSector);
        metadata.put("accion", "eliminacion_logica");

        notificacionBl.crearNotificacion(
                usuarioId,
                "warning",
                "Sector eliminado",
                "El sector '" + nombreSector + "' ha sido marcado como inactivo",
                metadata
        );
    }

    /**
     * Obtener estadísticas de sectores (solo activos por defecto)
     */
    public Map<String, Object> getEstadisticas(Integer usuarioId) {
        Usuarios usuario = usuariosRepository.findById(usuarioId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        Cooperativa cooperativa = cooperativaRepository.findByUsuariosId(usuario)
                .orElseThrow(() -> new IllegalArgumentException("Cooperativa no encontrada"));

        // Sectores activos
        List<Sectores> sectoresActivos = sectoresRepository.findByCooperativaIdAndEstadoActivo(cooperativa);

        Map<String, Object> estadisticas = new HashMap<>();
        estadisticas.put("totalSectoresActivos", sectoresActivos.size());

        // Contar sectores con minas activas
        long sectoresConMinasActivas = sectoresActivos.stream()
                .filter(s -> minasRepository.existsMinasActivasInSector(s))
                .count();

        estadisticas.put("sectoresConMinasActivas", sectoresConMinasActivas);
        estadisticas.put("sectoresSinMinasActivas", sectoresActivos.size() - sectoresConMinasActivas);

        // Calcular área total de sectores activos
        double areaTotal = sectoresActivos.stream()
                .mapToDouble(s -> GeometryUtils.calcularArea(s.getSectoresCoordenadasList()))
                .sum();
        estadisticas.put("areaTotalHectareas", Math.round(areaTotal * 100.0) / 100.0);

        // Contar sectores inactivos
        long sectoresInactivos = sectoresRepository.countByCooperativaIdAndEstadoInactivo(cooperativa);
        estadisticas.put("sectoresInactivos", sectoresInactivos);

        return estadisticas;
    }

    private SectorResponseDto convertToDto(Sectores sector) {
        List<CoordenadaResponseDto> coordenadas = sector.getSectoresCoordenadasList().stream()
                .sorted(Comparator.comparing(SectoresCoordenadas::getOrden))
                .map(coord -> new CoordenadaResponseDto(
                        coord.getId(),
                        coord.getOrden(),
                        coord.getLatitud(),
                        coord.getLongitud()
                ))
                .collect(Collectors.toList());

        // El estado viene directamente de la entidad
        String estado = sector.getEstado();

        return new SectorResponseDto(
                sector.getId(),
                sector.getNombre(),
                sector.getColor(),
                coordenadas,
                GeometryUtils.calcularArea(sector.getSectoresCoordenadasList()),
                estado
        );
    }

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

        // Ordenar ambas listas por orden
        List<SectoresCoordenadas> antiguasOrdenadas = coordenadasAnteriores.stream()
                .sorted(Comparator.comparing(SectoresCoordenadas::getOrden))
                .collect(Collectors.toList());

        List<SectoresCoordenadas> nuevasOrdenadas = coordenadasNuevas.stream()
                .sorted(Comparator.comparing(SectoresCoordenadas::getOrden))
                .collect(Collectors.toList());

        // Comparar cada coordenada
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
}