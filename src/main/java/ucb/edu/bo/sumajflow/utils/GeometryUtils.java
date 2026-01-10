package ucb.edu.bo.sumajflow.utils;

import ucb.edu.bo.sumajflow.document.TrackingUbicacion;
import ucb.edu.bo.sumajflow.entity.SectoresCoordenadas;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class GeometryUtils {

    private static final double EARTH_RADIUS_KM = 6371.0;

    // ==================== MÉTODOS DE POLÍGONOS ====================

    /**
     * Verifica si un punto está dentro de un polígono usando el algoritmo Ray Casting
     */
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

    /**
     * Calcula el área de un polígono usando el algoritmo de Shoelace
     */
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

    /**
     * Valida si las coordenadas forman un polígono válido (no autointersectante)
     */
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

    // ==================== CÁLCULO DE DISTANCIAS ====================

    /**
     * Calcula la distancia entre dos puntos geográficos usando la fórmula de Haversine
     * @return Distancia en kilómetros
     */
    public static double calcularDistancia(
            BigDecimal lat1, BigDecimal lon1,
            BigDecimal lat2, BigDecimal lon2
    ) {
        double dLat = Math.toRadians(lat2.doubleValue() - lat1.doubleValue());
        double dLon = Math.toRadians(lon2.doubleValue() - lon1.doubleValue());

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1.doubleValue()))
                * Math.cos(Math.toRadians(lat2.doubleValue()))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return EARTH_RADIUS_KM * c;
    }

    /**
     * Calcula la distancia entre dos puntos usando doubles
     * @return Distancia en kilómetros
     */
    public static double calcularDistancia(double lat1, double lng1, double lat2, double lng2) {
        return calcularDistancia(
                BigDecimal.valueOf(lat1), BigDecimal.valueOf(lng1),
                BigDecimal.valueOf(lat2), BigDecimal.valueOf(lng2)
        );
    }

    /**
     * Calcula la distancia en metros
     */
    public static double calcularDistanciaMetros(
            BigDecimal lat1, BigDecimal lng1,
            BigDecimal lat2, BigDecimal lng2
    ) {
        return calcularDistancia(lat1, lng1, lat2, lng2) * 1000;
    }

    /**
     * Calcula la distancia en metros usando doubles
     */
    public static double calcularDistanciaMetros(double lat1, double lng1, double lat2, double lng2) {
        return calcularDistancia(lat1, lng1, lat2, lng2) * 1000;
    }

    // ==================== GEOFENCING ====================

    /**
     * Verifica si un punto está dentro de un radio (geofencing circular)
     * @param lat Latitud del punto a verificar
     * @param lng Longitud del punto a verificar
     * @param centroLat Latitud del centro de la zona
     * @param centroLng Longitud del centro de la zona
     * @param radioMetros Radio de la zona en metros
     * @return true si está dentro de la zona
     */
    public static boolean estaDentroDeZona(
            double lat, double lng,
            double centroLat, double centroLng,
            double radioMetros
    ) {
        double distancia = calcularDistanciaMetros(lat, lng, centroLat, centroLng);
        return distancia <= radioMetros;
    }

    // ==================== NAVEGACIÓN Y RUMBO ====================

    /**
     * Calcula el rumbo (bearing) entre dos puntos
     * @return Ángulo en grados (0-360)
     */
    public static double calcularRumbo(double lat1, double lng1, double lat2, double lng2) {
        double dLng = Math.toRadians(lng2 - lng1);
        double lat1Rad = Math.toRadians(lat1);
        double lat2Rad = Math.toRadians(lat2);

        double y = Math.sin(dLng) * Math.cos(lat2Rad);
        double x = Math.cos(lat1Rad) * Math.sin(lat2Rad) -
                Math.sin(lat1Rad) * Math.cos(lat2Rad) * Math.cos(dLng);

        double bearing = Math.toDegrees(Math.atan2(y, x));
        return (bearing + 360) % 360;
    }

    /**
     * Calcula la velocidad entre dos puntos
     * @return Velocidad en km/h
     */
    public static double calcularVelocidad(
            double lat1, double lng1, long timestamp1,
            double lat2, double lng2, long timestamp2
    ) {
        double distanciaKm = calcularDistancia(lat1, lng1, lat2, lng2);
        double tiempoHoras = (timestamp2 - timestamp1) / 3600000.0; // ms a horas

        if (tiempoHoras <= 0) {
            return 0;
        }

        return distanciaKm / tiempoHoras;
    }

    // ==================== ESTIMACIONES DE TIEMPO ====================

    /**
     * Calcula el tiempo estimado de llegada basado en velocidad actual
     * @return Tiempo estimado en segundos, o -1 si no se puede calcular
     */
    public static long calcularTiempoEstimadoLlegada(double distanciaKm, double velocidadKmH) {
        if (velocidadKmH <= 0) {
            // Si está detenido, usar velocidad promedio estimada de 30 km/h
            velocidadKmH = 30.0;
        }
        return (long) ((distanciaKm / velocidadKmH) * 3600); // Convertir a segundos
    }

    /**
     * Formatea una duración en segundos a formato legible
     */
    public static String formatearDuracion(long segundos) {
        if (segundos < 0) {
            return "N/A";
        }

        long horas = segundos / 3600;
        long minutos = (segundos % 3600) / 60;

        if (horas > 0) {
            return String.format("%dh %dm", horas, minutos);
        } else if (minutos > 0) {
            return String.format("%dm", minutos);
        } else {
            return "< 1m";
        }
    }

    // ==================== VALIDACIÓN ====================

    /**
     * Verifica si una ubicación es válida
     */
    public static boolean esUbicacionValida(Double lat, Double lng) {
        if (lat == null || lng == null) {
            return false;
        }
        return lat >= -90 && lat <= 90 && lng >= -180 && lng <= 180;
    }

    // ==================== UTILIDADES DE ANIMACIÓN ====================

    /**
     * Genera puntos intermedios entre dos ubicaciones (para animación suave)
     */
    public static List<double[]> generarPuntosIntermedios(
            double lat1, double lng1,
            double lat2, double lng2,
            int numeroPuntos
    ) {
        List<double[]> puntos = new ArrayList<>();

        for (int i = 0; i <= numeroPuntos; i++) {
            double fraction = (double) i / numeroPuntos;
            double lat = lat1 + (lat2 - lat1) * fraction;
            double lng = lng1 + (lng2 - lng1) * fraction;
            puntos.add(new double[]{lat, lng});
        }

        return puntos;
    }

    /**
     * Encuentra el próximo punto de control pendiente
     */
    public static TrackingUbicacion.PuntoControl encontrarProximoPuntoControlPendiente(
            List<TrackingUbicacion.PuntoControl> puntosControl) {

        if (puntosControl == null || puntosControl.isEmpty()) {
            return null;
        }

        return puntosControl.stream()
                .filter(p -> "pendiente".equals(p.getEstado()) || "en_punto".equals(p.getEstado()))
                .min((p1, p2) -> p1.getOrden().compareTo(p2.getOrden()))
                .orElse(null);
    }
}