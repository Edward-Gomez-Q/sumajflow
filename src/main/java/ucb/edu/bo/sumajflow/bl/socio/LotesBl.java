package ucb.edu.bo.sumajflow.bl.socio;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ucb.edu.bo.sumajflow.bl.AuditoriaBl;
import ucb.edu.bo.sumajflow.bl.NotificacionBl;
import ucb.edu.bo.sumajflow.dto.socio.LoteCreateDto;
import ucb.edu.bo.sumajflow.dto.socio.LoteResponseDto;
import ucb.edu.bo.sumajflow.dto.socio.MineralInfoDto;
import ucb.edu.bo.sumajflow.entity.*;
import ucb.edu.bo.sumajflow.repository.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class LotesBl {

    // Repositorios
    private final LotesRepository lotesRepository;
    private final LoteMineralesRepository loteMineralesRepository;
    private final LoteIngenioRepository loteIngenioRepository;
    private final LoteComercializadoraRepository loteComercializadoraRepository;
    private final MinasRepository minasRepository;
    private final MineralesRepository mineralesRepository;
    private final IngenioMineroRepository ingenioMineroRepository;
    private final ComercializadoraRepository comercializadoraRepository;
    private final SocioRepository socioRepository;
    private final UsuariosRepository usuariosRepository;
    private final AuditoriaBl auditoriaBl;
    private final NotificacionBl notificacionBl;

    // Constantes de estados
    private static final String ESTADO_INICIAL = "Pendiente de aprobación cooperativa";

    /**
     * Crear un nuevo lote
     */
    @Transactional
    public LoteResponseDto createLote(
            LoteCreateDto dto,
            Integer usuarioId,
            String ipOrigen,
            String metodoHttp,
            String endpoint
    ) {
        log.info("Creando nuevo lote para mina ID: {}", dto.getMinaId());

        // 1. Validaciones generales
        validarDatosLote(dto);

        // 2. Obtener y validar usuario/socio
        Usuarios usuario = obtenerUsuario(usuarioId);
        Socio socio = obtenerSocioDelUsuario(usuario);

        // 3. Obtener y validar mina
        Minas mina = validarYObtenerMina(dto.getMinaId(), socio);

        // 4. Validar y obtener minerales
        List<Minerales> minerales = validarYObtenerMinerales(dto.getMineralesIds());

        // 5. Validar tipo de operación y tipo de mineral
        validarTipoOperacionYMineral(dto.getTipoOperacion(), dto.getTipoMineral());

        // 6. Validar y obtener destino (ingenio o comercializadora)
        Object destino = validarYObtenerDestino(
                dto.getTipoOperacion(),
                dto.getDestinoId(),
                dto.getTipoMineral()
        );

        // 7. Crear el lote
        Lotes lote = crearYGuardarLote(dto, mina);

        // 8. Crear relaciones lote_minerales
        crearRelacionesMinerales(lote, minerales);

        // 9. Crear registro en lote_ingenio o lote_comercializadora
        crearRelacionDestino(lote, dto.getTipoOperacion(), dto.getDestinoId());

        // 10. Registrar en auditoría
        registrarAuditoriaCreacion(usuario, lote, mina, ipOrigen, metodoHttp, endpoint);

        // 11. Crear notificación
        enviarNotificacionCreacion(usuarioId, lote, mina);

        log.info("Lote creado exitosamente - ID: {}", lote.getId());

        // 12. Retornar DTO
        return convertToDto(lote, minerales, destino, dto.getTipoOperacion());
    }

    // ==================== MÉTODOS DE VALIDACIÓN ====================

    private void validarDatosLote(LoteCreateDto dto) {
        if (dto.getMinaId() == null) {
            throw new IllegalArgumentException("La mina es requerida");
        }

        if (dto.getMineralesIds() == null || dto.getMineralesIds().isEmpty()) {
            throw new IllegalArgumentException("Debe seleccionar al menos un mineral");
        }

        if (dto.getCamionlesSolicitados() == null || dto.getCamionlesSolicitados() <= 0) {
            throw new IllegalArgumentException("El número de camiones debe ser mayor a 0");
        }

        if (dto.getTipoOperacion() == null || dto.getTipoOperacion().trim().isEmpty()) {
            throw new IllegalArgumentException("El tipo de operación es requerido");
        }

        if (!dto.getTipoOperacion().equals("procesamiento_planta") &&
                !dto.getTipoOperacion().equals("venta_directa")) {
            throw new IllegalArgumentException(
                    "Tipo de operación inválido. Debe ser 'procesamiento_planta' o 'venta_directa'"
            );
        }

        if (dto.getDestinoId() == null) {
            throw new IllegalArgumentException("El destino es requerido");
        }

        if (dto.getTipoMineral() == null || dto.getTipoMineral().trim().isEmpty()) {
            throw new IllegalArgumentException("El tipo de mineral es requerido");
        }

        if (!dto.getTipoMineral().equals("complejo") && !dto.getTipoMineral().equals("concentrado")) {
            throw new IllegalArgumentException(
                    "Tipo de mineral inválido. Debe ser 'complejo' o 'concentrado'"
            );
        }
    }

    private void validarTipoOperacionYMineral(String tipoOperacion, String tipoMineral) {
        // Regla: concentrado solo puede ir a comercializadora
        if (tipoMineral.equals("concentrado")) {
            if (tipoOperacion.equals("procesamiento_planta")) {
                throw new IllegalArgumentException(
                        "Mineral tipo 'concentrado' solo puede tener operación 'venta_directa' a comercializadora"
                );
            }
        }
    }

    private Minas validarYObtenerMina(Integer minaId, Socio socio) {
        Minas mina = minasRepository.findByIdAndEstadoActivo(minaId)
                .orElseThrow(() -> new IllegalArgumentException("Mina no encontrada o inactiva"));

        // Verificar permisos
        if (!mina.getSocioId().getId().equals(socio.getId())) {
            throw new IllegalArgumentException("No tienes permiso para crear lotes en esta mina");
        }

        return mina;
    }

    private List<Minerales> validarYObtenerMinerales(List<Integer> mineralesIds) {
        List<Minerales> minerales = new ArrayList<>();

        for (Integer mineralId : mineralesIds) {
            Minerales mineral = mineralesRepository.findById(mineralId)
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Mineral con ID " + mineralId + " no encontrado"
                    ));
            minerales.add(mineral);
        }

        return minerales;
    }

    private Object validarYObtenerDestino(
            String tipoOperacion,
            Integer destinoId,
            String tipoMineral
    ) {
        if (tipoOperacion.equals("procesamiento_planta")) {
            // Debe ser un ingenio
            return ingenioMineroRepository.findById(destinoId)
                    .orElseThrow(() -> new IllegalArgumentException("Ingenio minero no encontrado"));

        } else { // venta_directa
            // Debe ser una comercializadora
            Comercializadora comercializadora = comercializadoraRepository.findById(destinoId)
                    .orElseThrow(() -> new IllegalArgumentException("Comercializadora no encontrada"));

            // Validar que si es concentrado, solo pueda ir a comercializadora
            // (ya validado arriba, pero doble check)
            return comercializadora;
        }
    }

    // ==================== MÉTODOS DE CREACIÓN ====================

    private Lotes crearYGuardarLote(LoteCreateDto dto, Minas mina) {
        Lotes lote = Lotes.builder()
                .minasId(mina)
                .camionesSolicitados(dto.getCamionlesSolicitados())
                .tipoOperacion(dto.getTipoOperacion())
                .tipoMineral(dto.getTipoMineral())
                .estado(ESTADO_INICIAL)
                .fechaCreacion(LocalDateTime.now())
                .pesoTotalEstimado(dto.getPesoTotalEstimado())
                .observaciones(dto.getObservaciones())
                .build();

        return lotesRepository.save(lote);
    }

    private void crearRelacionesMinerales(Lotes lote, List<Minerales> minerales) {
        for (Minerales mineral : minerales) {
            LoteMinerales loteMinerales = LoteMinerales.builder()
                    .lotesId(lote)
                    .mineralesId(mineral)
                    .build();

            loteMineralesRepository.save(loteMinerales);
        }
    }

    private void crearRelacionDestino(Lotes lote, String tipoOperacion, Integer destinoId) {
        if (tipoOperacion.equals("procesamiento_planta")) {
            // Crear en lote_ingenio
            IngenioMinero ingenio = ingenioMineroRepository.findById(destinoId)
                    .orElseThrow(() -> new IllegalArgumentException("Ingenio no encontrado"));

            LoteIngenio loteIngenio = LoteIngenio.builder()
                    .lotesId(lote)
                    .ingenioMineroId(ingenio)
                    .estado("Pendiente de aprobación cooperativa")
                    .build();

            loteIngenioRepository.save(loteIngenio);

        } else { // venta_directa
            // Crear en lote_comercializadora
            Comercializadora comercializadora = comercializadoraRepository.findById(destinoId)
                    .orElseThrow(() -> new IllegalArgumentException("Comercializadora no encontrada"));

            LoteComercializadora loteComercializadora = LoteComercializadora.builder()
                    .lotesId(lote)
                    .comercializadoraId(comercializadora)
                    .estado("Pendiente de aprobación cooperativa")
                    .build();

            loteComercializadoraRepository.save(loteComercializadora);
        }
    }

    // ==================== MÉTODOS AUXILIARES ====================

    private Usuarios obtenerUsuario(Integer usuarioId) {
        return usuariosRepository.findById(usuarioId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));
    }

    private Socio obtenerSocioDelUsuario(Usuarios usuario) {
        return socioRepository.findByUsuariosId(usuario)
                .orElseThrow(() -> new IllegalArgumentException("Socio no encontrado"));
    }

    private void registrarAuditoriaCreacion(
            Usuarios usuario,
            Lotes lote,
            Minas mina,
            String ipOrigen,
            String metodoHttp,
            String endpoint
    ) {
        Map<String, Object> datosNuevos = new HashMap<>();
        datosNuevos.put("lote_id", lote.getId());
        datosNuevos.put("mina_id", mina.getId());
        datosNuevos.put("mina_nombre", mina.getNombre());
        datosNuevos.put("estado", lote.getEstado());
        datosNuevos.put("tipo_operacion", lote.getTipoOperacion());
        datosNuevos.put("tipo_mineral", lote.getTipoMineral());
        datosNuevos.put("camiones_solicitados", lote.getCamionesSolicitados());

        auditoriaBl.registrar(
                usuario,
                "lotes",
                "INSERT",
                "Creación de lote",
                lote.getId(),
                null,
                datosNuevos,
                null,
                ipOrigen,
                metodoHttp,
                endpoint,
                "medio",
                "lotes",
                "ALTO"
        );
    }

    private void enviarNotificacionCreacion(Integer usuarioId, Lotes lote, Minas mina) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("loteId", lote.getId());
        metadata.put("minaId", mina.getId());
        metadata.put("minaNombre", mina.getNombre());
        metadata.put("estado", lote.getEstado());

        notificacionBl.crearNotificacion(
                usuarioId,
                "success",
                "Lote creado",
                "El lote para la mina '" + mina.getNombre() +
                        "' ha sido creado y está pendiente de aprobación por la cooperativa",
                metadata
        );
    }

    private LoteResponseDto convertToDto(
            Lotes lote,
            List<Minerales> minerales,
            Object destino,
            String tipoOperacion
    ) {
        LoteResponseDto dto = new LoteResponseDto();

        dto.setId(lote.getId());
        dto.setMinaId(lote.getMinasId().getId());
        dto.setMinaNombre(lote.getMinasId().getNombre());

        // Minerales
        List<MineralInfoDto> mineralesDto = minerales.stream()
                .map(m -> new MineralInfoDto(m.getId(), m.getNombre(), m.getNomenclatura()))
                .collect(Collectors.toList());
        dto.setMinerales(mineralesDto);

        dto.setCamionlesSolicitados(lote.getCamionesSolicitados());
        dto.setTipoOperacion(lote.getTipoOperacion());
        dto.setTipoMineral(lote.getTipoMineral());
        dto.setEstado(lote.getEstado());
        dto.setFechaCreacion(lote.getFechaCreacion());
        dto.setPesoTotalEstimado(lote.getPesoTotalEstimado());
        dto.setObservaciones(lote.getObservaciones());

        // Destino
        if (tipoOperacion.equals("procesamiento_planta")) {
            IngenioMinero ingenio = (IngenioMinero) destino;
            dto.setDestinoId(ingenio.getId());
            dto.setDestinoNombre(ingenio.getRazonSocial());
            dto.setDestinoTipo("ingenio");
        } else {
            Comercializadora comercializadora = (Comercializadora) destino;
            dto.setDestinoId(comercializadora.getId());
            dto.setDestinoNombre(comercializadora.getRazonSocial());
            dto.setDestinoTipo("comercializadora");
        }

        dto.setCreatedAt(lote.getFechaCreacion());
        dto.setUpdatedAt(lote.getUpdatedAt());

        return dto;
    }
}