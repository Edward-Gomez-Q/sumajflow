package ucb.edu.bo.sumajflow.bl;

import io.minio.*;
import io.minio.errors.*;
import io.minio.http.Method;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * Servicio para gestionar archivos en MinIO
 */
@Service
public class MinioService {

    private final MinioClient minioClient;

    @Value("${minio.bucket-name}")
    private String bucketName;

    public MinioService(MinioClient minioClient) {
        this.minioClient = minioClient;
    }

    /**
     * Verifica si el bucket existe, si no, lo crea
     */
    private void ensureBucketExists() throws Exception {
        boolean found = minioClient.bucketExists(
                BucketExistsArgs.builder()
                        .bucket(bucketName)
                        .build()
        );

        if (!found) {
            minioClient.makeBucket(
                    MakeBucketArgs.builder()
                            .bucket(bucketName)
                            .build()
            );

            // Hacer el bucket público para lectura
            String policy = """
                {
                    "Version": "2012-10-17",
                    "Statement": [
                        {
                            "Effect": "Allow",
                            "Principal": {"AWS": "*"},
                            "Action": ["s3:GetObject"],
                            "Resource": ["arn:aws:s3:::%s/*"]
                        }
                    ]
                }
                """.formatted(bucketName);

            minioClient.setBucketPolicy(
                    SetBucketPolicyArgs.builder()
                            .bucket(bucketName)
                            .config(policy)
                            .build()
            );
        }
    }

    /**
     * Genera un nombre de archivo corto y único
     * Formato: timestamp_random.extension (ej: 20241113_a3f9.jpg)
     */
    private String generateShortFilename(String originalFilename) {
        // Obtener extensión
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }

        // Generar timestamp corto (solo los últimos 8 dígitos del timestamp)
        String timestamp = String.valueOf(System.currentTimeMillis()).substring(5);

        // Generar 4 caracteres aleatorios
        String randomChars = Long.toHexString(Double.doubleToLongBits(Math.random())).substring(0, 4);

        return timestamp + "_" + randomChars + extension;
    }

    /**
     * Limpia el nombre de archivo de caracteres especiales
     */
    private String sanitizeFilename(String filename) {
        if (filename == null) return "file";

        // Remover caracteres especiales y espacios
        return filename.replaceAll("[^a-zA-Z0-9.-]", "_")
                .replaceAll("_{2,}", "_")
                .toLowerCase();
    }

    /**
     * Sube un archivo a MinIO
     * @param file archivo a subir
     * @param folder carpeta donde guardar (ej: "documentos-socios", "pdfs", "fotos")
     * @return nombre del archivo guardado (objectName completo: folder/filename)
     */
    public String uploadFile(MultipartFile file, String folder) throws Exception {
        ensureBucketExists();

        // Validar que el archivo no esté vacío
        if (file.isEmpty()) {
            throw new IllegalArgumentException("El archivo está vacío");
        }

        // Generar nombre corto y único para el archivo
        String filename = generateShortFilename(file.getOriginalFilename());

        // Sanitizar el folder
        String sanitizedFolder = sanitizeFilename(folder);

        String objectName = sanitizedFolder + "/" + filename;

        // Subir archivo
        minioClient.putObject(
                PutObjectArgs.builder()
                        .bucket(bucketName)
                        .object(objectName)
                        .stream(file.getInputStream(), file.getSize(), -1)
                        .contentType(file.getContentType())
                        .build()
        );

        return objectName;
    }

    /**
     * Obtiene la URL pública de un archivo con firma temporal
     * @param objectName nombre del objeto en MinIO (folder/filename)
     * @return URL temporal del archivo
     */
    public String getFileUrl(String objectName) throws Exception {
        return minioClient.getPresignedObjectUrl(
                GetPresignedObjectUrlArgs.builder()
                        .method(Method.GET)
                        .bucket(bucketName)
                        .object(objectName)
                        .expiry(7, TimeUnit.DAYS) // URL válida por 7 días
                        .build()
        );
    }

    /**
     * Descarga un archivo como stream de bytes
     * @param objectName nombre del objeto en MinIO
     * @return InputStream del archivo
     */
    public InputStream getFile(String objectName) throws Exception {
        return minioClient.getObject(
                GetObjectArgs.builder()
                        .bucket(bucketName)
                        .object(objectName)
                        .build()
        );
    }

    /**
     * Obtiene información del archivo
     * @param objectName nombre del objeto en MinIO
     * @return StatObjectResponse con metadatos del archivo
     */
    public StatObjectResponse getFileInfo(String objectName) throws Exception {
        return minioClient.statObject(
                StatObjectArgs.builder()
                        .bucket(bucketName)
                        .object(objectName)
                        .build()
        );
    }

    /**
     * Elimina un archivo
     * @param objectName nombre del objeto en MinIO (folder/filename)
     */
    public void deleteFile(String objectName) throws Exception {
        minioClient.removeObject(
                RemoveObjectArgs.builder()
                        .bucket(bucketName)
                        .object(objectName)
                        .build()
        );
    }

    /**
     * Verifica si un archivo existe
     * @param objectName nombre del objeto en MinIO
     * @return true si existe, false si no
     */
    public boolean fileExists(String objectName) {
        try {
            minioClient.statObject(
                    StatObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .build()
            );
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}