package ucb.edu.bo.sumajflow.bl;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import ucb.edu.bo.sumajflow.dto.CotizacionMineralDto;
import ucb.edu.bo.sumajflow.entity.DeduccionConfiguracion;
import ucb.edu.bo.sumajflow.repository.DeduccionConfiguracionRepository;
import ucb.edu.bo.sumajflow.dto.DeduccionConfiguracionDto;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class CotizacionMineralBl {

    private final RestTemplate restTemplate;
    private final DeduccionConfiguracionRepository deduccionConfiguracionRepository;


    private static final BigDecimal DOLAR_OFICIAL = new BigDecimal("6.96");
    private static final BigDecimal TROY_OZ_POR_TON = new BigDecimal("32150.7466");

    // ========== CACHÉ TEMPORAL ==========
    private Map<String, CotizacionMineralDto> cotizacionesCache = null;
    private LocalDateTime ultimaActualizacionCache = null;
    private static final int MINUTOS_CACHE = 720;

    @Value("${metalsapi.base-url:https://metals-api.com/api}")
    private String baseUrl;

    @Value("${metalsapi.api-key:}")
    private String apiKey;

    /**
     * Obtener cotizaciones Y deducciones vigentes para liquidaciones
     */
    public Map<String, Object> obtenerCotizacionesYDeducciones(String tipoLiquidacion) {
        LocalDate hoy = LocalDate.now();

        // Obtener cotizaciones
        Map<String, CotizacionMineralDto> cotizaciones = obtenerCotizacionesActuales();

        // Obtener deducciones vigentes y aplicables
        List<DeduccionConfiguracion> deduccionesConfig = deduccionConfiguracionRepository
                .findDeduccionesAplicables(hoy, tipoLiquidacion);

        List<DeduccionConfiguracionDto> deducciones = deduccionesConfig.stream()
                .map(d -> DeduccionConfiguracionDto.builder()
                        .codigo(d.getCodigo())
                        .concepto(d.getConcepto())
                        .descripcion(d.getDescripcion())
                        .tipoDeduccion(d.getTipoDeduccion())
                        .categoria(d.getCategoria())
                        .aplicaAMineral(d.getAplicaAMineral())
                        .porcentaje(d.getPorcentaje())
                        .baseCalculo(d.getBaseCalculo())
                        .orden(d.getOrden())
                        .build())
                .toList();

        Map<String, Object> response = new HashMap<>();
        response.put("cotizaciones", cotizaciones);
        response.put("dolarOficial", DOLAR_OFICIAL);
        response.put("deducciones", deducciones);
        response.put("success", true);

        return response;
    }

    /**
     * Verifica si el caché es válido
     */
    private boolean esCacheValido() {
        if (cotizacionesCache == null || ultimaActualizacionCache == null) {
            return false;
        }
        return minutosDesdeUltimaActualizacion() < MINUTOS_CACHE;
    }

    /**
     * Calcula minutos desde la última actualización
     */
    private long minutosDesdeUltimaActualizacion() {
        if (ultimaActualizacionCache == null) return Long.MAX_VALUE;
        return java.time.Duration.between(ultimaActualizacionCache, LocalDateTime.now()).toMinutes();
    }

    /**
     * Forzar actualización del caché (útil para testing o admin)
     */
    public void invalidarCache() {
        cotizacionesCache = null;
        ultimaActualizacionCache = null;
        log.info("🔄 Caché de cotizaciones invalidado");
    }

    public Map<String, CotizacionMineralDto> obtenerCotizacionesActuales() {
        // Verificar si el cache es válido
        if (esCacheValido()) {
            log.info("✅ Usando cotizaciones desde caché (edad: {} min)",
                    minutosDesdeUltimaActualizacion());
            return cotizacionesCache;
        }

        log.info("📡 Obteniendo cotizaciones nuevas desde Metals-API...");

        LocalDate hoy = LocalDate.now();

        try {
            // Llamada a API con símbolos LBMA/LME
            MetalsApiLatestResponse resp = callLatest("LBXAG,LME-LEAD,LME-ZNC");
            if (resp == null || !Boolean.TRUE.equals(resp.success) || resp.rates == null) {
                throw new IllegalStateException("Respuesta inválida Metals-API");
            }

            // Fallback si faltan símbolos
            boolean faltaAlgo = !(resp.rates.containsKey("LBXAG")
                    && resp.rates.containsKey("LME-LEAD")
                    && resp.rates.containsKey("LME-ZNC"));

            if (faltaAlgo) {
                log.warn("Faltan símbolos LBMA/LME. Intentando fallback...");
                resp = callLatest("XAG,LEAD,ZNC");
            }

            Map<String, CotizacionMineralDto> cotizaciones = new HashMap<>();

            // Plata
            String symAg = pick(resp.rates, "LBXAG", "XAG");
            BigDecimal agUsdOz = usdOzFromRate(resp.rates.get(symAg));
            cotizaciones.put("Ag", CotizacionMineralDto.builder()
                    .nomenclatura("Ag").nombre("Plata")
                    .cotizacionUsdOz(agUsdOz).cotizacionUsdTon(null)
                    .unidad("USD/oz").dolarOficial(DOLAR_OFICIAL)
                    .fecha(hoy).fuente("Metals-API (" + symAg + ")").build());

            // Plomo
            String symPb = pick(resp.rates, "LME-LEAD", "LEAD");
            BigDecimal pbUsdTon = usdTonFromRate(resp.rates.get(symPb));
            cotizaciones.put("Pb", CotizacionMineralDto.builder()
                    .nomenclatura("Pb").nombre("Plomo")
                    .cotizacionUsdOz(null).cotizacionUsdTon(pbUsdTon)
                    .unidad("USD/ton").dolarOficial(DOLAR_OFICIAL)
                    .fecha(hoy).fuente("Metals-API (" + symPb + ")").build());

            // Zinc
            String symZn = pick(resp.rates, "LME-ZNC", "ZNC");
            BigDecimal znUsdTon = usdTonFromRate(resp.rates.get(symZn));
            cotizaciones.put("Zn", CotizacionMineralDto.builder()
                    .nomenclatura("Zn").nombre("Zinc")
                    .cotizacionUsdOz(null).cotizacionUsdTon(znUsdTon)
                    .unidad("USD/ton").dolarOficial(DOLAR_OFICIAL)
                    .fecha(hoy).fuente("Metals-API (" + symZn + ")").build());

            // Actualizar caché
            cotizacionesCache = cotizaciones;
            ultimaActualizacionCache = LocalDateTime.now();

            log.info("✅ Cotizaciones actualizadas y guardadas en caché");
            return cotizaciones;

        } catch (Exception e) {
            log.error("❌ Error al obtener cotizaciones: {}", e.getMessage(), e);

            // Si hay caché antiguo, usarlo como fallback
            if (cotizacionesCache != null) {
                log.warn("⚠️  Usando caché antiguo como fallback (edad: {} min)",
                        minutosDesdeUltimaActualizacion());
                return cotizacionesCache;
            }

            // Si no hay caché, usar valores por defecto
            log.warn("⚠️  Usando valores fallback fijos");
            return obtenerCotizacionesFallback();
        }
    }
    private MetalsApiLatestResponse callLatest(String symbols) {
        String url = UriComponentsBuilder
                .fromHttpUrl(baseUrl + "/latest")
                .queryParam("access_key", apiKey)
                .queryParam("base", "USD")
                .queryParam("symbols", symbols)
                .toUriString();

        return restTemplate.getForObject(url, MetalsApiLatestResponse.class);
    }

    /**
     * Para plata: convertir rate a USD/oz
     * rate = onzas troy por 1 USD
     * USD/oz = 1 / rate
     */
    private BigDecimal usdOzFromRate(BigDecimal rateOzPorUsd) {
        if (rateOzPorUsd == null || rateOzPorUsd.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalStateException("Rate inválido: " + rateOzPorUsd);
        }
        return BigDecimal.ONE.divide(rateOzPorUsd, 6, RoundingMode.HALF_UP);
    }

    /**
     * Para Pb/Zn: convertir rate a USD/ton
     * rate = metalUnits por 1 USD (ej. onzas por USD)
     * USD/oz = 1/rate
     * USD/ton = (1/rate) * 32150.7466
     */
    private BigDecimal usdTonFromRate(BigDecimal rateMetalPorUsd) {
        if (rateMetalPorUsd == null || rateMetalPorUsd.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalStateException("Rate inválido: " + rateMetalPorUsd);
        }
        BigDecimal usdPorOz = BigDecimal.ONE.divide(rateMetalPorUsd, 12, RoundingMode.HALF_UP);
        return usdPorOz.multiply(TROY_OZ_POR_TON).setScale(2, RoundingMode.HALF_UP);
    }

    private String pick(Map<String, BigDecimal> rates, String prefer, String fallback) {
        if (rates.containsKey(prefer) && rates.get(prefer) != null) return prefer;
        if (rates.containsKey(fallback) && rates.get(fallback) != null) return fallback;
        throw new IllegalStateException("No existe rate para " + prefer + " ni " + fallback);
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class MetalsApiLatestResponse {
        public Boolean success;
        public String base;
        public String date;
        public Long timestamp;
        public Map<String, BigDecimal> rates;
    }

    private Map<String, CotizacionMineralDto> obtenerCotizacionesFallback() {
        Map<String, CotizacionMineralDto> cotizaciones = new HashMap<>();
        LocalDate hoy = LocalDate.now();

        cotizaciones.put("Pb", CotizacionMineralDto.builder()
                .nomenclatura("Pb").nombre("Plomo")
                .cotizacionUsdTon(new BigDecimal("2200.00"))
                .cotizacionUsdOz(null)
                .unidad("USD/ton")
                .dolarOficial(DOLAR_OFICIAL).fecha(hoy).fuente("Fallback").build());

        cotizaciones.put("Ag", CotizacionMineralDto.builder()
                .nomenclatura("Ag").nombre("Plata")
                .cotizacionUsdOz(new BigDecimal("25.50"))  // USD/oz
                .cotizacionUsdTon(null)
                .unidad("USD/oz")
                .dolarOficial(DOLAR_OFICIAL).fecha(hoy).fuente("Fallback").build());

        cotizaciones.put("Zn", CotizacionMineralDto.builder()
                .nomenclatura("Zn").nombre("Zinc")
                .cotizacionUsdTon(new BigDecimal("2800.00"))
                .cotizacionUsdOz(null)
                .unidad("USD/ton")
                .dolarOficial(DOLAR_OFICIAL).fecha(hoy).fuente("Fallback").build());

        return cotizaciones;
    }

    public BigDecimal obtenerDolarOficial() {
        return DOLAR_OFICIAL;
    }
}