package ucb.edu.bo.sumajflow.bl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * Servicio para envío de códigos de verificación por WhatsApp usando Twilio
 */
@Slf4j
@Service
public class WhatsAppService {

    @Value("${twilio.account.sid:}")
    private String accountSid;

    @Value("${twilio.auth.token:}")
    private String authToken;

    @Value("${twilio.whatsapp.from:whatsapp:+14155238886}")
    private String fromNumber;

    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * Enviar código de verificación por WhatsApp
     */
    public String enviarCodigoVerificacion(
            String numeroCelular,
            String nombreCompleto,
            String codigoVerificacion
    ) {
        try {
            log.info("Enviando código de verificación de WhatsApp a: {}", numeroCelular);

            String numeroLimpio = limpiarNumeroCelular(numeroCelular);
            String mensaje = construirMensajeCodigoVerificacion(nombreCompleto, codigoVerificacion);

            // Verificar configuración
            if (!isConfigured()) {
                log.warn("Twilio WhatsApp no configurado. Modo de prueba activado.");
                log.info("Código que se enviaría a {}: {}", numeroCelular, codigoVerificacion);
                return "TEST_MESSAGE_ID_" + System.currentTimeMillis();
            }

            return enviarMensajeTwilio(numeroLimpio, mensaje);

        } catch (Exception e) {
            log.error("Error al enviar código de WhatsApp: {}", e.getMessage(), e);
            throw new RuntimeException("No se pudo enviar el código por WhatsApp: " + e.getMessage());
        }
    }

    /**
     * Reenviar código de verificación
     */
    public String reenviarCodigoVerificacion(
            String numeroCelular,
            String nombreCompleto,
            String codigoVerificacion
    ) {
        log.info("Reenviando código de verificación a: {}", numeroCelular);
        return enviarCodigoVerificacion(numeroCelular, nombreCompleto, codigoVerificacion);
    }

    /**
     * Enviar mensaje usando Twilio API
     */
    private String enviarMensajeTwilio(String numeroDestino, String mensaje) {
        try {
            String url = String.format(
                    "https://api.twilio.com/2010-04-01/Accounts/%s/Messages.json",
                    accountSid
            );

            // Crear headers con Basic Auth
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            String auth = accountSid + ":" + authToken;
            String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8));
            headers.set("Authorization", "Basic " + encodedAuth);

            // Usar MultiValueMap para form data
            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("From", fromNumber);
            body.add("To", "whatsapp:" + numeroDestino);
            body.add("Body", mensaje);

            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);

            log.debug("Enviando request a Twilio: {}", url);
            ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);

            if (response.getStatusCode() == HttpStatus.CREATED && response.getBody() != null) {
                Map<String, Object> responseBody = response.getBody();
                String sid = (String) responseBody.get("sid");
                log.info("Mensaje enviado exitosamente. SID: {}", sid);
                return sid;
            }

            log.warn("Respuesta inesperada de Twilio");
            return "UNKNOWN_MESSAGE_ID";

        } catch (HttpClientErrorException e) {
            log.error("Error HTTP {} al enviar a Twilio: {}",
                    e.getStatusCode(), e.getResponseBodyAsString());

            String errorMsg = extraerErrorTwilio(e.getResponseBodyAsString());
            throw new RuntimeException("Error en Twilio API: " + errorMsg);

        } catch (Exception e) {
            log.error("Error inesperado en API de Twilio: {}", e.getMessage(), e);
            throw new RuntimeException("Error al comunicarse con Twilio API");
        }
    }
    /**
     * Construir mensaje de código de verificación
     */
    private String construirMensajeCodigoVerificacion(String nombreCompleto, String codigo) {
        return String.format(
                "¡Hola %s! 👋\n\n" +
                        "Tu código de verificación para SumajFlow es:\n\n" +
                        "🔐 *%s*\n\n" +
                        "Este código es válido por 10 minutos.\n" +
                        "No compartas este código con nadie.",
                nombreCompleto,
                codigo
        );
    }

    /**
     * Limpiar número de celular para formato internacional
     */
    private String limpiarNumeroCelular(String numero) {
        if (numero == null || numero.isEmpty()) {
            throw new IllegalArgumentException("Número de celular no puede estar vacío");
        }

        // Remover espacios, guiones, paréntesis
        String limpio = numero.replaceAll("[\\s\\-\\(\\)]", "");

        // Asegurar que empiece con +
        if (!limpio.startsWith("+")) {
            if (limpio.startsWith("591")) {
                limpio = "+" + limpio;
            } else {
                limpio = "+591" + limpio;
            }
        }

        log.debug("Número limpiado: {} -> {}", numero, limpio);
        return limpio;
    }

    /**
     * Extraer mensaje de error de respuesta de Twilio
     */
    private String extraerErrorTwilio(String responseBody) {
        try {
            // Twilio devuelve JSON con campo "message"
            if (responseBody.contains("\"message\"")) {
                int start = responseBody.indexOf("\"message\":\"") + 11;
                int end = responseBody.indexOf("\"", start);
                return responseBody.substring(start, end);
            }
        } catch (Exception e) {
            log.debug("No se pudo extraer mensaje de error", e);
        }
        return "Error desconocido";
    }

    /**
     * Validar configuración de Twilio
     */
    public boolean isConfigured() {
        boolean configured = accountSid != null && !accountSid.isEmpty()
                && authToken != null && !authToken.isEmpty();

        if (!configured) {
            log.warn("Twilio no está configurado correctamente. SID: {}, Token: {}",
                    accountSid != null ? "presente" : "ausente",
                    authToken != null ? "presente" : "ausente");
        }

        return configured;
    }
}