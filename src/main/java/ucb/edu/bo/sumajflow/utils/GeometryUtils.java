package ucb.edu.bo.sumajflow.utils;

import ucb.edu.bo.sumajflow.entity.SectoresCoordenadas;

import java.math.BigDecimal;
import java.util.List;

public class GeometryUtils {

    //Verifica si un punto está dentro de un polígono usando el algoritmo Ray Casting
    public static boolean puntoEnPoligono(
            BigDecimal lat,
            BigDecimal lon,
            List<SectoresCoordenadas> coordenadas
    ) {
        if (coordenadas == null || coordenadas.size() < 3) {
            return false;
        }

        double x = lon.doubleValue();
        double y = lat.doubleValue();

        int n = coordenadas.size();
        boolean inside = false;

        // Ray casting algorithm
        for (int i = 0, j = n - 1; i < n; j = i++) {
            double xi = coordenadas.get(i).getLongitud().doubleValue();
            double yi = coordenadas.get(i).getLatitud().doubleValue();
            double xj = coordenadas.get(j).getLongitud().doubleValue();
            double yj = coordenadas.get(j).getLatitud().doubleValue();

            boolean intersect = ((yi > y) != (yj > y))
                    && (x < (xj - xi) * (y - yi) / (yj - yi) + xi);

            if (intersect) {
                inside = !inside;
            }
        }

        return inside;
    }

    //Calcula el área de un polígono usando el algoritmo de Shoelace
    public static double calcularArea(List<SectoresCoordenadas> coordenadas) {
        if (coordenadas == null || coordenadas.size() < 3) {
            return 0.0;
        }

        double area = 0.0;
        int n = coordenadas.size();

        for (int i = 0; i < n; i++) {
            int j = (i + 1) % n;
            area += coordenadas.get(i).getLongitud().doubleValue()
                    * coordenadas.get(j).getLatitud().doubleValue();
            area -= coordenadas.get(j).getLongitud().doubleValue()
                    * coordenadas.get(i).getLatitud().doubleValue();
        }

        area = Math.abs(area / 2.0);

        // Convertir a hectáreas (aproximación básica)
        // 1 grado ≈ 111 km (latitud) y ≈ 106 km (longitud en Bolivia)
        double kmSquared = area * 111 * 106;
        return kmSquared * 100; // 1 km² = 100 hectáreas
    }

    //Calcula la distancia entre dos puntos geográficos usando la fórmula de Haversine
    public static double calcularDistancia(
            BigDecimal lat1, BigDecimal lon1,
            BigDecimal lat2, BigDecimal lon2
    ) {
        final int RADIO_TIERRA_KM = 6371;

        double dLat = Math.toRadians(lat2.doubleValue() - lat1.doubleValue());
        double dLon = Math.toRadians(lon2.doubleValue() - lon1.doubleValue());

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1.doubleValue()))
                * Math.cos(Math.toRadians(lat2.doubleValue()))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return RADIO_TIERRA_KM * c;
    }

    //Valida si las coordenadas forman un polígono válido (no autointersectante)
    public static boolean esPoligonoValido(List<SectoresCoordenadas> coordenadas) {
        if (coordenadas == null || coordenadas.size() < 3) {
            return false;
        }

        // Validar que no haya coordenadas duplicadas consecutivas
        for (int i = 0; i < coordenadas.size(); i++) {
            int j = (i + 1) % coordenadas.size();
            SectoresCoordenadas c1 = coordenadas.get(i);
            SectoresCoordenadas c2 = coordenadas.get(j);

            if (c1.getLatitud().equals(c2.getLatitud())
                    && c1.getLongitud().equals(c2.getLongitud())) {
                return false;
            }
        }

        return true;
    }
}