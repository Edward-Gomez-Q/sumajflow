package ucb.edu.bo.sumajflow.bl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ucb.edu.bo.sumajflow.dto.ingenio.*;
import ucb.edu.bo.sumajflow.dto.socio.MineralInfoDto;
import ucb.edu.bo.sumajflow.entity.*;
import ucb.edu.bo.sumajflow.repository.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Servicio base de Concentrados - Lógica común compartida por todos los roles
 * Responsabilidades:
 * - Conversión de DTOs
 * - Gestión de historial
 * - Validaciones comunes
 * - Eventos WebSocket
 * - Utilidades compartidas
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ConcentradoBl {

    // Repositorios
    protected final ConcentradoRepository concentradoRepository;
    protected final PersonaRepository personaRepository;
    protected final LoteMineralesRepository loteMineralesRepository;
    protected final LoteProcesoPlantaRepository loteProcesoPlantaRepository;
    protected final ObjectMapper objectMapper;
    protected final SimpMessagingTemplate messagingTemplate;

    // ==================== LISTAR CONCENTRADOS (GENÉRICO) ====================

    /**
     * Listar concentrados con filtros - Metodo genérico
     * Los BL específicos llaman a este metodo pasando los concentrados base
     */
    @Transactional(readOnly = true)
    public Page<ConcentradoResponseDto> listarConcentradosConFiltros(
            List<Concentrado> concentradosBase,
            String estado,
            String mineralPrincipal,
            LocalDateTime fechaDesde,
            LocalDateTime fechaHasta,
            int page,
            int size
    ) {
        log.debug("Aplicando filtros a {} concentrados", concentradosBase.size());

        // Aplicar filtros
        List<Concentrado> concentradosFiltrados = concentradosBase.stream()
                .filter(c -> aplicarFiltros(c, estado, mineralPrincipal, fechaDesde, fechaHasta))
                .collect(Collectors.toList());

        // Aplicar paginación
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), concentradosFiltrados.size());

        List<ConcentradoResponseDto> paginaActual = concentradosFiltrados.subList(start, end).stream()
                .map(this::convertirAResponseDto)
                .collect(Collectors.toList());

        return new PageImpl<>(paginaActual, pageable, concentradosFiltrados.size());
    }

    /**
     * Aplicar filtros a un concentrado individual
     */
    private boolean aplicarFiltros(
            Concentrado c,
            String estado,
            String mineralPrincipal,
            LocalDateTime fechaDesde,
            LocalDateTime fechaHasta
    ) {
        if (estado != null && !estado.isEmpty() && !estado.equals(c.getEstado())) {
            return false;
        }
        if (mineralPrincipal != null && !mineralPrincipal.isEmpty() && !mineralPrincipal.equals(c.getMineralPrincipal())) {
            return false;
        }
        if (fechaDesde != null && c.getCreatedAt().isBefore(fechaDesde)) {
            return false;
        }
        if (fechaHasta != null && c.getCreatedAt().isAfter(fechaHasta)) {
            return false;
        }
        return true;
    }

    // ==================== OBTENER DETALLE ====================

    /**
     * Obtener detalle de concentrado - Metodo genérico
     */
    @Transactional(readOnly = true)
    public ConcentradoResponseDto obtenerDetalle(Integer concentradoId) {
        log.debug("Obteniendo detalle del concentrado ID: {}", concentradoId);

        Concentrado concentrado = concentradoRepository.findById(concentradoId)
                .orElseThrow(() -> new IllegalArgumentException("Concentrado no encontrado"));

        return convertirAResponseDto(concentrado);
    }

    /**
     * Validar que el concentrado existe
     */
    @Transactional(readOnly = true)
    public Concentrado obtenerConcentrado(Integer concentradoId) {
        return concentradoRepository.findById(concentradoId)
                .orElseThrow(() -> new IllegalArgumentException("Concentrado no encontrado con ID: " + concentradoId));
    }

    // ==================== CONVERSIÓN A DTO ====================

    /**
     * Convertir Concentrado a DTO - Metodo común para todos los roles
     */
    public ConcentradoResponseDto convertirAResponseDto(Concentrado concentrado) {
        ConcentradoResponseDto dto = new ConcentradoResponseDto();

        // Datos básicos
        dto.setId(concentrado.getId());
        dto.setCodigoConcentrado(concentrado.getCodigoConcentrado());
        dto.setEstado(concentrado.getEstado());
        dto.setPesoInicial(concentrado.getPesoInicial());
        dto.setPesoFinal(concentrado.getPesoFinal());
        dto.setMerma(concentrado.getMerma());
        dto.setMineralPrincipal(concentrado.getMineralPrincipal());
        dto.setMineralesSecundarios(concentrado.getMineralesSecundarios());
        dto.setLoteOrigenMultiple(concentrado.getLoteOrigenMultiple());
        dto.setNumeroSacos(concentrado.getNumeroSacos());

        // Datos del ingenio
        dto.setIngenioId(concentrado.getIngenioMineroId().getId());
        dto.setIngenioNombre(concentrado.getIngenioMineroId().getRazonSocial());

        // Datos del socio propietario
        Socio socio = concentrado.getSocioPropietarioId();
        if (socio != null) {
            dto.setSocioId(socio.getId());
            Persona persona = personaRepository.findByUsuariosId(socio.getUsuariosId()).orElse(null);
            if (persona != null) {
                dto.setSocioNombres(persona.getNombres());
                dto.setSocioApellidos(persona.getPrimerApellido() +
                        (persona.getSegundoApellido() != null ? " " + persona.getSegundoApellido() : ""));
                dto.setSocioCi(persona.getCi());
            }
        }

        // Lotes relacionados
        List<LoteSimpleDto> lotesDto = concentrado.getLoteConcentradoRelacionList().stream()
                .map(relacion -> {
                    Lotes lote = relacion.getLoteComplejoId();
                    return LoteSimpleDto.builder()
                            .id(lote.getId())
                            .minaNombre(lote.getMinasId().getNombre())
                            .tipoMineral(lote.getTipoMineral())
                            .pesoTotalReal(lote.getPesoTotalReal())
                            .estado(lote.getEstado())
                            .build();
                })
                .collect(Collectors.toList());
        dto.setLotes(lotesDto);

        // Minerales (del primer lote como referencia)
        if (!concentrado.getLoteConcentradoRelacionList().isEmpty()) {
            Lotes primerLote = concentrado.getLoteConcentradoRelacionList().get(0).getLoteComplejoId();
            List<LoteMinerales> loteMinerales = loteMineralesRepository.findByLotesId(primerLote);
            dto.setMinerales(
                    loteMinerales.stream()
                            .map(lm -> new MineralInfoDto(
                                    lm.getMineralesId().getId(),
                                    lm.getMineralesId().getNombre(),
                                    lm.getMineralesId().getNomenclatura()))
                            .collect(Collectors.toList())
            );
        }

        // Fechas
        dto.setFechaInicio(concentrado.getFechaInicio());
        dto.setFechaFin(concentrado.getFechaFin());
        dto.setCreatedAt(concentrado.getCreatedAt());
        dto.setUpdatedAt(concentrado.getUpdatedAt());

        // Historial de observaciones
        try {
            List<Map<String, Object>> historial = objectMapper.readValue(
                    concentrado.getObservaciones(),
                    new TypeReference<List<Map<String, Object>>>() {}
            );
            dto.setObservaciones(historial.isEmpty() ? null : historial);
        } catch (Exception e) {
            log.warn("Error al parsear observaciones del concentrado ID: {}", concentrado.getId());
            dto.setObservaciones(null);
        }

        return dto;
    }

    /**
     * Convertir proceso a DTO
     */
    public ProcesoPlantaDto convertirProcesoADto(LoteProcesoPlanta proceso) {
        return ProcesoPlantaDto.builder()
                .id(proceso.getId())
                .nombreProceso(proceso.getProcesoId().getNombre())
                .orden(proceso.getOrden())
                .estado(proceso.getEstado())
                .fechaInicio(proceso.getFechaInicio())
                .fechaFin(proceso.getFechaFin())
                .observaciones(proceso.getObservaciones())
                .build();
    }

    // ==================== GESTIÓN DE HISTORIAL ====================

    /**
     * Obtener historial de observaciones
     */
    public List<Map<String, Object>> obtenerHistorial(Concentrado concentrado) {
        if (concentrado.getObservaciones() == null || concentrado.getObservaciones().isBlank()) {
            return new ArrayList<>();
        }

        try {
            return objectMapper.readValue(
                    concentrado.getObservaciones(),
                    new TypeReference<List<Map<String, Object>>>() {}
            );
        } catch (JsonProcessingException e) {
            log.error("Error al parsear historial del concentrado ID: {}", concentrado.getId(), e);
            return new ArrayList<>();
        }
    }

    /**
     * Convertir historial a JSON
     */
    public String convertirHistorialAJson(List<Map<String, Object>> historial) {
        try {
            return objectMapper.writeValueAsString(historial);
        } catch (JsonProcessingException e) {
            log.error("Error al convertir historial a JSON", e);
            return "[]";
        }
    }

    /**
     * Crear registro de historial
     */
    public Map<String, Object> crearRegistroHistorial(
            String estado,
            String descripcion,
            String observaciones,
            Integer usuarioId,
            String ipOrigen
    ) {
        Map<String, Object> registro = new HashMap<>();
        registro.put("estado", estado);
        registro.put("descripcion", descripcion);
        registro.put("observaciones", observaciones);
        registro.put("usuario_id", usuarioId);
        registro.put("ip_origen", ipOrigen);
        registro.put("timestamp", LocalDateTime.now().toString());
        return registro;
    }

    /**
     * Transicionar estado del concentrado con historial
     */
    public void transicionarEstado(
            Concentrado concentrado,
            String nuevoEstado,
            String descripcion,
            String observacionesAdicionales,
            Integer usuarioId,
            String ipOrigen
    ) {
        String estadoAnterior = concentrado.getEstado();
        concentrado.setEstado(nuevoEstado);

        List<Map<String, Object>> historial = obtenerHistorial(concentrado);
        Map<String, Object> registro = crearRegistroHistorial(
                nuevoEstado,
                descripcion,
                observacionesAdicionales,
                usuarioId,
                ipOrigen
        );
        registro.put("estado_anterior", estadoAnterior);
        historial.add(registro);

        concentrado.setObservaciones(convertirHistorialAJson(historial));
        concentradoRepository.save(concentrado);
    }

    // ==================== WEBSOCKET ====================

    /**
     * Publicar evento WebSocket a todos los interesados
     */
    public void publicarEventoWebSocket(Concentrado concentrado, String evento) {
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("evento", evento);
            payload.put("concentradoId", concentrado.getId());
            payload.put("codigoConcentrado", concentrado.getCodigoConcentrado());
            payload.put("mineralPrincipal", concentrado.getMineralPrincipal());
            payload.put("estado", concentrado.getEstado());
            payload.put("timestamp", LocalDateTime.now().toString());

            // Canal del ingenio
            String canalIngenio = "/topic/ingenio/" + concentrado.getIngenioMineroId().getId() + "/concentrados";
            messagingTemplate.convertAndSend(canalIngenio, payload);

            // Canal del socio
            String canalSocio = "/topic/socio/" + concentrado.getSocioPropietarioId().getId() + "/concentrados";
            messagingTemplate.convertAndSend(canalSocio, payload);

        } catch (Exception e) {
            log.error("Error al publicar evento WebSocket", e);
        }
    }

    /**
     * Publicar evento Kanban WebSocket
     */
    public void publicarEventoKanban(Concentrado concentrado, LoteProcesoPlanta proceso, String estadoAnterior) {
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("evento", "etapa_actualizada");
            payload.put("concentradoId", concentrado.getId());
            payload.put("codigoConcentrado", concentrado.getCodigoConcentrado());
            payload.put("procesoId", proceso.getId());
            payload.put("procesoNombre", proceso.getProcesoId().getNombre());
            payload.put("estadoAnterior", estadoAnterior);
            payload.put("estadoNuevo", proceso.getEstado());
            payload.put("orden", proceso.getOrden());
            payload.put("timestamp", LocalDateTime.now().toString());

            String canal = "/topic/ingenio/" + concentrado.getIngenioMineroId().getId() + "/concentrados";
            messagingTemplate.convertAndSend(canal, payload);

            String canalSocio = "/topic/socio/" + concentrado.getSocioPropietarioId().getId() + "/concentrados";
            messagingTemplate.convertAndSend(canalSocio, payload);

        } catch (Exception e) {
            log.error("Error al publicar evento Kanban WebSocket", e);
        }
    }

    // ==================== VALIDACIONES COMUNES ====================

    /**
     * Validar que el concentrado está en un estado esperado
     */
    public void validarEstado(Concentrado concentrado, String estadoEsperado) {
        if (!estadoEsperado.equals(concentrado.getEstado())) {
            throw new IllegalArgumentException(
                    "El concentrado no está en estado '" + estadoEsperado + "'. Estado actual: " + concentrado.getEstado()
            );
        }
    }

    /**
     * Validar que el concentrado está en uno de varios estados esperados
     */
    public void validarEstadoMultiple(Concentrado concentrado, List<String> estadosEsperados) {
        if (!estadosEsperados.contains(concentrado.getEstado())) {
            throw new IllegalArgumentException(
                    "El concentrado no está en un estado válido. Estado actual: " + concentrado.getEstado() +
                            ". Estados esperados: " + String.join(", ", estadosEsperados)
            );
        }
    }

    // ==================== CONSTRUCCIÓN DE PROCESOS DTO ====================

    /**
     * Construir DTO de procesos del concentrado (para Kanban)
     */
    public ProcesosConcentradoResponseDto construirProcesosResponseDto(Concentrado concentrado) {
        List<LoteProcesoPlanta> procesos = loteProcesoPlantaRepository
                .findByConcentradoIdOrderByOrdenAsc(concentrado);

        List<ProcesoPlantaDto> procesosDto = procesos.stream()
                .map(this::convertirProcesoADto)
                .collect(Collectors.toList());

        long completados = procesos.stream().filter(p -> "completado".equals(p.getEstado())).count();
        long pendientes = procesos.stream().filter(p -> "pendiente".equals(p.getEstado())).count();

        ProcesoPlantaDto procesoActual = procesos.stream()
                .filter(p -> "en_proceso".equals(p.getEstado()) || "pendiente".equals(p.getEstado()))
                .findFirst()
                .map(this::convertirProcesoADto)
                .orElse(null);

        return ProcesosConcentradoResponseDto.builder()
                .concentradoId(concentrado.getId())
                .codigoConcentrado(concentrado.getCodigoConcentrado())
                .estadoConcentrado(concentrado.getEstado())
                .totalProcesos(procesos.size())
                .procesosCompletados((int) completados)
                .procesosPendientes((int) pendientes)
                .procesoActual(procesoActual)
                .todosProcesos(procesosDto)
                .build();
    }
}