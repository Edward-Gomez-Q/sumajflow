// src/main/java/ucb/edu/bo/sumajflow/bl/SectoresBl.java
package ucb.edu.bo.sumajflow.bl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ucb.edu.bo.sumajflow.dto.CoordenadaDto;
import ucb.edu.bo.sumajflow.dto.cooperativa.*;
import ucb.edu.bo.sumajflow.entity.*;
import ucb.edu.bo.sumajflow.repository.*;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class SectoresBl {

    private final SectoresRepository sectoresRepository;
    private final SectoresCoordenadasRepository sectoresCoordenadasRepository;
    private final CooperativaRepository cooperativaRepository;
    private final UsuariosRepository usuariosRepository;
    private final AuditoriaBl auditoriaBl;
    private final NotificacionBl notificacionBl;

    public SectoresBl(
            SectoresRepository sectoresRepository,
            SectoresCoordenadasRepository sectoresCoordenadasRepository,
            CooperativaRepository cooperativaRepository,
            UsuariosRepository usuariosRepository,
            AuditoriaBl auditoriaBl,
            NotificacionBl notificacionBl
    ) {
        this.sectoresRepository = sectoresRepository;
        this.sectoresCoordenadasRepository = sectoresCoordenadasRepository;
        this.cooperativaRepository = cooperativaRepository;
        this.usuariosRepository = usuariosRepository;
        this.auditoriaBl = auditoriaBl;
        this.notificacionBl = notificacionBl;
    }

    /**
     * Obtener todos los sectores de una cooperativa
     */
    public List<SectorResponseDto> getSectoresByCooperativa(Integer usuarioId) {
        // Buscar cooperativa por usuario
        Usuarios usuario = usuariosRepository.findById(usuarioId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        Cooperativa cooperativa = cooperativaRepository.findByUsuariosId(usuario)
                .orElseThrow(() -> new IllegalArgumentException("Cooperativa no encontrada"));

        // Obtener sectores
        List<Sectores> sectores = sectoresRepository.findByCooperativaId(cooperativa);

        // Convertir a DTO
        return sectores.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    /**
     * Obtener un sector por ID
     */
    public SectorResponseDto getSectorById(Integer sectorId, Integer usuarioId) {
        Sectores sector = sectoresRepository.findById(sectorId)
                .orElseThrow(() -> new IllegalArgumentException("Sector no encontrado"));

        // Verificar que el sector pertenece a la cooperativa del usuario
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
    public SectorResponseDto createSector(SectorCreateDto dto, Integer usuarioId) {
        // Validar datos
        validateSectorData(dto);

        // Buscar usuario y cooperativa
        Usuarios usuario = usuariosRepository.findById(usuarioId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        Cooperativa cooperativa = cooperativaRepository.findByUsuariosId(usuario)
                .orElseThrow(() -> new IllegalArgumentException("Cooperativa no encontrada"));

        // Validar nombre único
        if (sectoresRepository.existsByNombreAndCooperativaId(dto.getNombre(), cooperativa)) {
            throw new IllegalArgumentException("Ya existe un sector con ese nombre");
        }

        // Validar color único
        if (sectoresRepository.existsByColorAndCooperativaId(dto.getColor(), cooperativa)) {
            throw new IllegalArgumentException("Ya existe un sector con ese color");
        }

        // Crear sector
        Sectores sector = new Sectores();
        sector.setNombre(dto.getNombre());
        sector.setColor(dto.getColor());
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

        // Auditoría - USANDO EL MÉTODO CORRECTO
        auditoriaBl.registrar(
                usuario,
                "sectores",
                "INSERT",
                "Sector creado: " + sector.getNombre() + " (ID: " + sector.getId() + ") con " +
                        coordenadas.size() + " coordenadas"
        );

        // Notificación
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("sectorId", sector.getId());
        metadata.put("nombre", sector.getNombre());
        metadata.put("numeroCoordenadas", coordenadas.size());

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
    public SectorResponseDto updateSector(Integer sectorId, SectorCreateDto dto, Integer usuarioId) {
        // Validar datos
        validateSectorData(dto);

        // Buscar sector
        Sectores sector = sectoresRepository.findById(sectorId)
                .orElseThrow(() -> new IllegalArgumentException("Sector no encontrado"));

        // Verificar permisos
        Usuarios usuario = usuariosRepository.findById(usuarioId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        Cooperativa cooperativa = cooperativaRepository.findByUsuariosId(usuario)
                .orElseThrow(() -> new IllegalArgumentException("Cooperativa no encontrada"));

        if (!sector.getCooperativaId().getId().equals(cooperativa.getId())) {
            throw new IllegalArgumentException("No tienes permiso para modificar este sector");
        }

        // Guardar nombre anterior para auditoría
        String nombreAnterior = sector.getNombre();

        // Validar nombre único (excluyendo el sector actual)
        Optional<Sectores> sectorConMismoNombre = sectoresRepository
                .findByNombreAndCooperativaId(dto.getNombre(), cooperativa);
        if (sectorConMismoNombre.isPresent() && !sectorConMismoNombre.get().getId().equals(sectorId)) {
            throw new IllegalArgumentException("Ya existe un sector con ese nombre");
        }

        // Validar color único (excluyendo el sector actual)
        Optional<Sectores> sectorConMismoColor = sectoresRepository
                .findByColorAndCooperativaId(dto.getColor(), cooperativa);
        if (sectorConMismoColor.isPresent() && !sectorConMismoColor.get().getId().equals(sectorId)) {
            throw new IllegalArgumentException("Ya existe un sector con ese color");
        }

        // Actualizar sector
        sector.setNombre(dto.getNombre());
        sector.setColor(dto.getColor());

        // Eliminar coordenadas antiguas
        sectoresCoordenadasRepository.deleteBySectoresId(sector);

        // Crear nuevas coordenadas
        List<SectoresCoordenadas> coordenadas = dto.getCoordenadas().stream()
                .map(coordDto -> {
                    SectoresCoordenadas coord = new SectoresCoordenadas();
                    coord.setOrden(coordDto.getOrden());
                    coord.setLatitud(coordDto.getLatitud());
                    coord.setLongitud(coordDto.getLongitud());
                    coord.setSectoresId(sector);
                    return coord;
                })
                .collect(Collectors.toList());

        sectoresCoordenadasRepository.saveAll(coordenadas);
        sectoresRepository.save(sector);

        // Auditoría - USANDO EL MÉTODO CORRECTO
        auditoriaBl.registrarActualizacion(
                usuario,
                "sectores",
                "Sector actualizado: '" + nombreAnterior + "' -> '" + sector.getNombre() +
                        "' (ID: " + sector.getId() + ") con " + coordenadas.size() + " coordenadas"
        );

        // Notificación
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("sectorId", sector.getId());
        metadata.put("nombre", sector.getNombre());
        metadata.put("numeroCoordenadas", coordenadas.size());

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
     * Eliminar un sector
     */
    @Transactional
    public void deleteSector(Integer sectorId, Integer usuarioId) {
        // Buscar sector
        Sectores sector = sectoresRepository.findById(sectorId)
                .orElseThrow(() -> new IllegalArgumentException("Sector no encontrado"));

        // Verificar permisos
        Usuarios usuario = usuariosRepository.findById(usuarioId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        Cooperativa cooperativa = cooperativaRepository.findByUsuariosId(usuario)
                .orElseThrow(() -> new IllegalArgumentException("Cooperativa no encontrada"));

        if (!sector.getCooperativaId().getId().equals(cooperativa.getId())) {
            throw new IllegalArgumentException("No tienes permiso para eliminar este sector");
        }

        // Verificar si tiene minas asociadas
        if (sector.getMinasList() != null && !sector.getMinasList().isEmpty()) {
            throw new IllegalArgumentException("No se puede eliminar un sector con minas asociadas");
        }

        String nombreSector = sector.getNombre();
        Integer sectorIdAudit = sector.getId();

        // Eliminar coordenadas (cascada debe hacerlo automáticamente, pero por seguridad)
        sectoresCoordenadasRepository.deleteBySectoresId(sector);

        // Eliminar sector
        sectoresRepository.delete(sector);

        // Auditoría - USANDO EL MÉTODO CORRECTO
        auditoriaBl.registrarEliminacion(
                usuario,
                "sectores",
                "Sector eliminado: '" + nombreSector + "' (ID: " + sectorIdAudit + ")"
        );

        // Notificación
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("nombre", nombreSector);

        notificacionBl.crearNotificacion(
                usuarioId,
                "warning",
                "Sector eliminado",
                "El sector '" + nombreSector + "' ha sido eliminado",
                metadata
        );
    }

    /**
     * Obtener estadísticas de sectores
     */
    public Map<String, Object> getEstadisticas(Integer usuarioId) {
        Usuarios usuario = usuariosRepository.findById(usuarioId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        Cooperativa cooperativa = cooperativaRepository.findByUsuariosId(usuario)
                .orElseThrow(() -> new IllegalArgumentException("Cooperativa no encontrada"));

        List<Sectores> sectores = sectoresRepository.findByCooperativaId(cooperativa);

        Map<String, Object> estadisticas = new HashMap<>();
        estadisticas.put("totalSectores", sectores.size());

        // Calcular sectores activos (con minas)
        long sectoresActivos = sectores.stream()
                .filter(s -> s.getMinasList() != null && !s.getMinasList().isEmpty())
                .count();

        estadisticas.put("sectoresActivos", sectoresActivos);
        estadisticas.put("sectoresInactivos", sectores.size() - sectoresActivos);

        // Calcular área total
        double areaTotal = sectores.stream()
                .mapToDouble(this::calcularArea)
                .sum();
        estadisticas.put("areaTotalHectareas", Math.round(areaTotal * 100.0) / 100.0);

        return estadisticas;
    }

    // ==================== MÉTODOS AUXILIARES ====================

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

        String estado = (sector.getMinasList() != null && !sector.getMinasList().isEmpty())
                ? "activo" : "inactivo";

        return new SectorResponseDto(
                sector.getId(),
                sector.getNombre(),
                sector.getColor(),
                coordenadas,
                calcularArea(sector),
                estado
        );
    }

    private double calcularArea(Sectores sector) {
        List<SectoresCoordenadas> coords = sector.getSectoresCoordenadasList();
        if (coords == null || coords.size() < 3) {
            return 0.0;
        }

        // Algoritmo de Shoelace para calcular área de polígono
        double area = 0.0;
        for (int i = 0; i < coords.size(); i++) {
            int j = (i + 1) % coords.size();
            area += coords.get(i).getLongitud().doubleValue() * coords.get(j).getLatitud().doubleValue();
            area -= coords.get(j).getLongitud().doubleValue() * coords.get(i).getLatitud().doubleValue();
        }

        area = Math.abs(area / 2.0);

        // Convertir a hectáreas (aproximación)
        double kmSquared = area * 111 * 106;
        return kmSquared * 100;
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
}