package ucb.edu.bo.sumajflow.bl.routing;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import ucb.edu.bo.sumajflow.dto.routing.OsrmResponse;
import ucb.edu.bo.sumajflow.dto.routing.RutaCalculadaDto;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class RoutingService {

    private final RestTemplate restTemplate;

    private static final String OSRM_API_URL = "https://router.project-osrm.org/route/v1/driving/";

    /**
     * Calcula la ruta completa pasando por todos los waypoints
     */
    public RutaCalculadaDto calcularRutaCompleta(
            BigDecimal minaLat, BigDecimal minaLng,
            BigDecimal balanzaCoopLat, BigDecimal balanzaCoopLng,
            BigDecimal balanzaDestinoLat, BigDecimal balanzaDestinoLng,
            BigDecimal almacenLat, BigDecimal almacenLng
    ) {
        try {
            // Construir la URL con todos los waypoints en orden
            String coordinates = String.format("%s,%s;%s,%s;%s,%s;%s,%s",
                    minaLng, minaLat,
                    balanzaCoopLng, balanzaCoopLat,
                    balanzaDestinoLng, balanzaDestinoLat,
                    almacenLng, almacenLat
            );

            String url = OSRM_API_URL + coordinates + "?overview=false&geometries=polyline";

            log.debug("Consultando OSRM API: {}", url);

            // Hacer la petición
            OsrmResponse response = restTemplate.getForObject(url, OsrmResponse.class);

            if (response == null || !"Ok".equals(response.getCode()) || response.getRoutes().isEmpty()) {
                log.warn("No se pudo calcular la ruta, usando distancia en línea recta");
                return calcularRutaLineaRecta(
                        minaLat, minaLng,
                        balanzaCoopLat, balanzaCoopLng,
                        balanzaDestinoLat, balanzaDestinoLng,
                        almacenLat, almacenLng
                );
            }

            // Obtener la primera ruta
            OsrmResponse.Route route = response.getRoutes().get(0);

            // Convertir a km y horas
            Double distanciaKm = route.getDistance() / 1000.0;
            Double tiempoHoras = route.getDuration() / 3600.0;

            log.info("Ruta calculada: {} km, {} horas",
                    String.format("%.2f", distanciaKm),
                    String.format("%.2f", tiempoHoras));

            return RutaCalculadaDto.builder()
                    .distanciaKm(distanciaKm)
                    .tiempoHoras(tiempoHoras)
                    .exitosa(true)
                    .metodoCalculo("osrm")
                    .build();

        } catch (Exception e) {
            log.error("Error al calcular ruta con OSRM: {}", e.getMessage());
            return calcularRutaLineaRecta(
                    minaLat, minaLng,
                    balanzaCoopLat, balanzaCoopLng,
                    balanzaDestinoLat, balanzaDestinoLng,
                    almacenLat, almacenLng
            );
        }
    }

    /**
     * Calcula distancia en línea recta como fallback
     */
    private RutaCalculadaDto calcularRutaLineaRecta(
            BigDecimal lat1, BigDecimal lng1,
            BigDecimal lat2, BigDecimal lng2,
            BigDecimal lat3, BigDecimal lng3,
            BigDecimal lat4, BigDecimal lng4
    ) {
        double d1 = calcularDistanciaHaversine(
                lat1.doubleValue(), lng1.doubleValue(),
                lat2.doubleValue(), lng2.doubleValue()
        );
        double d2 = calcularDistanciaHaversine(
                lat2.doubleValue(), lng2.doubleValue(),
                lat3.doubleValue(), lng3.doubleValue()
        );
        double d3 = calcularDistanciaHaversine(
                lat3.doubleValue(), lng3.doubleValue(),
                lat4.doubleValue(), lng4.doubleValue()
        );

        double distanciaTotal = d1 + d2 + d3;
        double tiempoHoras = distanciaTotal / 40.0;

        log.info("Ruta calculada (línea recta): {} km, {} horas estimadas",
                String.format("%.2f", distanciaTotal),
                String.format("%.2f", tiempoHoras));

        return RutaCalculadaDto.builder()
                .distanciaKm(distanciaTotal)
                .tiempoHoras(tiempoHoras)
                .exitosa(false)
                .metodoCalculo("linea_recta")
                .build();
    }

    private double calcularDistanciaHaversine(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371; // Radio de la Tierra en km

        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);

        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return R * c;
    }
}