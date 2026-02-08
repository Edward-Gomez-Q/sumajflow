package ucb.edu.bo.sumajflow.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ucb.edu.bo.sumajflow.bl.CotizacionMineralBl;
import ucb.edu.bo.sumajflow.dto.CooperativaPublicDto;
import ucb.edu.bo.sumajflow.dto.CotizacionMineralDto;
import ucb.edu.bo.sumajflow.entity.*;
import ucb.edu.bo.sumajflow.repository.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


@RestController
@RequestMapping("/public")
@CrossOrigin(origins = "*")
public class CooperativaPublicController {

    private final CooperativaRepository cooperativaRepository;
    private final ProcesosRepository procesosRepository;
    private final MineralesRepository mineralesRepository;
    private final CotizacionMineralBl cotizacionMineralService;
    private final TablaPreciosMineralRepository tablaPreciosMineralRepository;
    private final DeduccionConfiguracionRepository deduccionConfiguracionRepository;

    public CooperativaPublicController(
            CooperativaRepository cooperativaRepository,
            ProcesosRepository procesosRepository,
            MineralesRepository mineralesRepository,
            CotizacionMineralBl cotizacionMineralService,
            TablaPreciosMineralRepository tablaPreciosMineralRepository,
            DeduccionConfiguracionRepository deduccionConfiguracionRepository
    ) {
        this.cooperativaRepository = cooperativaRepository;
        this.procesosRepository = procesosRepository;
        this.mineralesRepository = mineralesRepository;
        this.cotizacionMineralService = cotizacionMineralService;
        this.tablaPreciosMineralRepository = tablaPreciosMineralRepository;
        this.deduccionConfiguracionRepository = deduccionConfiguracionRepository;
    }

    /**
     * Obtiene lista de todas las cooperativas (solo ID y razón social)
     * GET /public/cooperativas
     */
    @GetMapping("/cooperativas")
    public ResponseEntity<Map<String, Object>> getAllCooperativas() {
        Map<String, Object> response = new HashMap<>();

        try {
            List<Cooperativa> cooperativas = cooperativaRepository.findAll();

            List<CooperativaPublicDto> cooperativasDto = cooperativas.stream()
                    .map(c -> new CooperativaPublicDto(c.getId(), c.getRazonSocial()))
                    .collect(Collectors.toList());

            response.put("success", true);
            response.put("data", cooperativasDto);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error al obtener cooperativas: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * Obtiene lista de todos los procesos (solo ID y nombre)
     * GET /public/procesos
     */
    @GetMapping("/procesos")
    public ResponseEntity<Map<String, Object>> getAllProcesos() {
        Map<String, Object> response = new HashMap<>();
        try {
            List<Procesos> procesos = procesosRepository.findAll();

            List<Map<String, Object>> procesosDto = procesos.stream()
                    .map(p -> {
                        Map<String, Object> dto = new HashMap<>();
                        dto.put("id", p.getId());
                        dto.put("nombre", p.getNombre());
                        return dto;
                    })
                    .collect(Collectors.toList());

            response.put("success", true);
            response.put("data", procesosDto);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error al obtener procesos: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }
    /**
     * Obtiene lista de todos los minerales (id, nombre y nomenclatura)
     * GET /public/minerales
     */
    @GetMapping("/minerales")
    public ResponseEntity<Map<String, Object>> getAllMinerales() {
        Map<String, Object> response = new HashMap<>();
        try {
            List<Minerales> minerales = mineralesRepository.findAll();

            List<Map<String, Object>> mineralesDto = minerales.stream()
                    .map(m -> {
                        Map<String, Object> dto = new HashMap<>();
                        dto.put("id", m.getId());
                        dto.put("nombre", m.getNombre());
                        dto.put("nomenclatura", m.getNomenclatura());
                        return dto;
                    })
                    .collect(Collectors.toList());

            response.put("success", true);
            response.put("data", mineralesDto);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error al obtener minerales: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }
    @GetMapping("/cotizaciones-minerales")
    public ResponseEntity<Map<String, Object>> getCotizacionesMinerales() {
        Map<String, Object> response = new HashMap<>();
        try {
           Map<String, CotizacionMineralDto> cotizaciones = cotizacionMineralService.obtenerCotizacionesActuales();

            Map<String, Object> deducciones = cotizacionMineralService.obtenerCotizacionesYDeducciones("todos");
            response.put("success", true);
            response.put("data", cotizaciones);
            response.put("dolarOficial", cotizacionMineralService.obtenerDolarOficial());
            response.put("deducciones", deducciones.get("deducciones"));
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error al obtener cotizaciones: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }
    @GetMapping("/tabla-precios-mineral")
    public ResponseEntity<Map<String, Object>> obtenerTablaPreciosMineral(
            @RequestParam Integer comercializadoraId
    ) {
        LocalDate hoy = LocalDate.now();

        // 1. Precios vigentes de la comercializadora
        List<TablaPreciosMineral> precios = tablaPreciosMineralRepository
                .findPreciosVigentes(comercializadoraId, hoy);

        // 2. Deducciones aplicables a venta_lote_complejo
        List<DeduccionConfiguracion> deducciones = deduccionConfiguracionRepository
                .findDeduccionesAplicables(hoy, "venta_lote_complejo");

        // 3. Dólar oficial
        BigDecimal dolarOficial = cotizacionMineralService.obtenerDolarOficial();

        // Mapear precios
        List<Map<String, Object>> preciosList = precios.stream().map(p -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", p.getId());
            m.put("mineral", p.getMineral());
            m.put("unidadMedida", p.getUnidadMedida());
            m.put("rangoMinimo", p.getRangoMinimo());
            m.put("rangoMaximo", p.getRangoMaximo());
            m.put("precioUsd", p.getPrecioUsd());
            m.put("activo", p.getActivo());
            return m;
        }).toList();

        // Mapear deducciones
        List<Map<String, Object>> deduccionesList = deducciones.stream().map(d -> {
            Map<String, Object> m = new HashMap<>();
            m.put("codigo", d.getCodigo());
            m.put("concepto", d.getConcepto());
            m.put("porcentaje", d.getPorcentaje());
            m.put("tipoDeduccion", d.getTipoDeduccion());
            m.put("baseCalculo", d.getBaseCalculo());
            m.put("aplicaAMineral", d.getAplicaAMineral());
            m.put("orden", d.getOrden());
            return m;
        }).toList();

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", preciosList);
        response.put("deducciones", deduccionesList);
        response.put("dolarOficial", dolarOficial);

        return ResponseEntity.ok(response);
    }
}