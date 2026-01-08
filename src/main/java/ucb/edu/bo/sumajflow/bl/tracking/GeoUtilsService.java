package ucb.edu.bo.sumajflow.bl.tracking;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ucb.edu.bo.sumajflow.document.TrackingUbicacion;

/**
 * Servicio de utilidades geográficas
 * Cálculos de distancia, geofencing, etc.
 */
@Slf4j
@Service
public class GeoUtilsService {

    // Radio de la Tierra en kilómetros
    private static final double EARTH_RADIUS_KM = 6371.0;

    /**
     * Calcula la distancia entre dos puntos usando la fórmula de Haversine
     * @return Distancia en kilómetros
     */
    public double calcularDistancia(double lat1, double lng1, double lat2, double lng2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return EARTH_RADIUS_KM * c;
    }

    /**
     * Calcula la distancia en metros
     */
    public double calcularDistanciaMetros(double lat1, double lng1, double lat2, double lng2) {
        return calcularDistancia(lat1, lng1, lat2, lng2) * 1000;
    }

    /**
     * Verifica si un punto está dentro de un radio (geofencing)
     * @param lat Latitud del punto a verificar
     * @param lng Longitud del punto a verificar
     * @param centroLat Latitud del centro de la zona
     * @param centroLng Longitud del centro de la zona
     * @param radioMetros Radio de la zona en metros
     * @return true si está dentro de la zona
     */
    public boolean estaDentroDeZona(double lat, double lng,
                                    double centroLat, double centroLng,
                                    double radioMetros) {
        double distancia = calcularDistanciaMetros(lat, lng, centroLat, centroLng);
        return distancia <= radioMetros;
    }

    /**
     * Encuentra el punto de control más cercano a una ubicación
     */
    public TrackingUbicacion.PuntoControl encontrarPuntoControlMasCercano(
            double lat, double lng,
            java.util.List<TrackingUbicacion.PuntoControl> puntosControl) {

        if (puntosControl == null || puntosControl.isEmpty()) {
            return null;
        }

        TrackingUbicacion.PuntoControl masCercano = null;
        double distanciaMinima = Double.MAX_VALUE;

        for (TrackingUbicacion.PuntoControl punto : puntosControl) {
            double distancia = calcularDistanciaMetros(lat, lng, punto.getLat(), punto.getLng());
            if (distancia < distanciaMinima) {
                distanciaMinima = distancia;
                masCercano = punto;
            }
        }

        return masCercano;
    }

    /**
     * Encuentra el próximo punto de control pendiente
     */
    public TrackingUbicacion.PuntoControl encontrarProximoPuntoControlPendiente(
            java.util.List<TrackingUbicacion.PuntoControl> puntosControl) {

        if (puntosControl == null || puntosControl.isEmpty()) {
            return null;
        }

        return puntosControl.stream()
                .filter(p -> "pendiente".equals(p.getEstado()) || "en_punto".equals(p.getEstado()))
                .min((p1, p2) -> p1.getOrden().compareTo(p2.getOrden()))
                .orElse(null);
    }

    /**
     * Calcula el rumbo (bearing) entre dos puntos
     * @return Ángulo en grados (0-360)
     */
    public double calcularRumbo(double lat1, double lng1, double lat2, double lng2) {
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
     * Calcula el tiempo estimado de llegada basado en velocidad actual
     * @return Tiempo estimado en segundos, o -1 si no se puede calcular
     */
    public long calcularTiempoEstimadoLlegada(double distanciaKm, double velocidadKmH) {
        if (velocidadKmH <= 0) {
            // Si está detenido, usar velocidad promedio estimada de 30 km/h
            velocidadKmH = 30.0;
        }
        return (long) ((distanciaKm / velocidadKmH) * 3600); // Convertir a segundos
    }

    /**
     * Formatea una duración en segundos a formato legible
     */
    public String formatearDuracion(long segundos) {
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

    /**
     * Verifica si una ubicación es válida
     */
    public boolean esUbicacionValida(Double lat, Double lng) {
        if (lat == null || lng == null) {
            return false;
        }
        return lat >= -90 && lat <= 90 && lng >= -180 && lng <= 180;
    }

    /**
     * Calcula la velocidad entre dos puntos
     * @return Velocidad en km/h
     */
    public double calcularVelocidad(double lat1, double lng1, long timestamp1,
                                    double lat2, double lng2, long timestamp2) {
        double distanciaKm = calcularDistancia(lat1, lng1, lat2, lng2);
        double tiempoHoras = (timestamp2 - timestamp1) / 3600000.0; // ms a horas

        if (tiempoHoras <= 0) {
            return 0;
        }

        return distanciaKm / tiempoHoras;
    }

    /**
     * Genera puntos intermedios entre dos ubicaciones (para animación suave)
     */
    public java.util.List<double[]> generarPuntosIntermedios(
            double lat1, double lng1,
            double lat2, double lng2,
            int numeroPuntos) {

        java.util.List<double[]> puntos = new java.util.ArrayList<>();

        for (int i = 0; i <= numeroPuntos; i++) {
            double fraction = (double) i / numeroPuntos;
            double lat = lat1 + (lat2 - lat1) * fraction;
            double lng = lng1 + (lng2 - lng1) * fraction;
            puntos.add(new double[]{lat, lng});
        }

        return puntos;
    }
}