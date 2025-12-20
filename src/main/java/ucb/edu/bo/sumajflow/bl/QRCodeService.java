package ucb.edu.bo.sumajflow.bl;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * Servicio para generación de códigos QR
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class QRCodeService {

    private static final int QR_CODE_SIZE = 350; // Tamaño del QR en píxeles

    /**
     * Generar código QR como imagen Base64
     *
     * @param data Datos a codificar en el QR
     * @return String con la imagen en Base64 (data:image/png;base64,...)
     */
    public String generateQRCodeBase64(String data) {
        try {
            log.info("Generando QR code para: {}", data);

            // Configurar hints para el QR
            Map<EncodeHintType, Object> hints = new HashMap<>();
            hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H);
            hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
            hints.put(EncodeHintType.MARGIN, 1);

            // Generar matriz del QR
            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            BitMatrix bitMatrix = qrCodeWriter.encode(
                    data,
                    BarcodeFormat.QR_CODE,
                    QR_CODE_SIZE,
                    QR_CODE_SIZE,
                    hints
            );

            // Convertir a imagen
            BufferedImage qrImage = MatrixToImageWriter.toBufferedImage(bitMatrix);

            // Convertir a Base64
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(qrImage, "PNG", baos);
            byte[] imageBytes = baos.toByteArray();
            String base64Image = Base64.getEncoder().encodeToString(imageBytes);

            // Retornar con prefijo data URI
            return "data:image/png;base64," + base64Image;

        } catch (WriterException | IOException e) {
            log.error("Error al generar QR code: {}", e.getMessage(), e);
            throw new RuntimeException("No se pudo generar el código QR: " + e.getMessage());
        }
    }

    /**
     * Generar código QR como bytes
     *
     * @param data Datos a codificar
     * @return bytes de la imagen PNG
     */
    public byte[] generateQRCodeBytes(String data) {
        try {
            Map<EncodeHintType, Object> hints = new HashMap<>();
            hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H);
            hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
            hints.put(EncodeHintType.MARGIN, 1);

            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            BitMatrix bitMatrix = qrCodeWriter.encode(
                    data,
                    BarcodeFormat.QR_CODE,
                    QR_CODE_SIZE,
                    QR_CODE_SIZE,
                    hints
            );

            BufferedImage qrImage = MatrixToImageWriter.toBufferedImage(bitMatrix);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(qrImage, "PNG", baos);

            return baos.toByteArray();

        } catch (WriterException | IOException e) {
            log.error("Error al generar QR code bytes: {}", e.getMessage(), e);
            throw new RuntimeException("No se pudo generar el código QR");
        }
    }

    /**
     * Generar datos para QR de invitación de transportista
     *
     * @param cooperativaId ID de la cooperativa
     * @param token Token de la invitación
     * @return JSON string con los datos
     */
    public String generateInvitacionQRData(Integer cooperativaId, String token) {
        // Formato JSON simple que la app móvil puede parsear
        return String.format(
                "{\"tipo\":\"invitacion_transportista\",\"cooperativaId\":%d,\"token\":\"%s\"}",
                cooperativaId,
                token
        );
    }
}