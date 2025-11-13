package ucb.edu.bo.sumajflow.controller;

import io.minio.StatObjectResponse;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import ucb.edu.bo.sumajflow.bl.MinioService;
import ucb.edu.bo.sumajflow.dto.FileUploadResponseDto;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * Controlador para gestión de archivos con MinIO
 */
@RestController
@RequestMapping("/files")
@CrossOrigin(origins = "*")
public class FileController {

    private final MinioService minioService;

    public FileController(MinioService minioService) {
        this.minioService = minioService;
    }

    /**
     * Endpoint para subir archivos (imágenes o PDFs)
     * POST /files/upload
     */
    @PostMapping("/upload")
    public ResponseEntity<Map<String, Object>> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "folder", defaultValue = "general") String folder
    ) {
        Map<String, Object> response = new HashMap<>();

        try {
            // Validar tipo de archivo
            String contentType = file.getContentType();
            if (contentType == null ||
                    (!contentType.startsWith("image/") && !contentType.equals("application/pdf"))) {
                response.put("success", false);
                response.put("message", "Solo se permiten imágenes y archivos PDF");
                return ResponseEntity.badRequest().body(response);
            }

            // Validar tamaño (max 10MB)
            if (file.getSize() > 10 * 1024 * 1024) {
                response.put("success", false);
                response.put("message", "El archivo excede el tamaño máximo de 10MB");
                return ResponseEntity.badRequest().body(response);
            }

            // Subir archivo
            String objectName = minioService.uploadFile(file, folder);
            String fileUrl = minioService.getFileUrl(objectName);

            FileUploadResponseDto fileResponse = new FileUploadResponseDto(
                    objectName,
                    fileUrl,
                    file.getContentType(),
                    file.getSize()
            );

            response.put("success", true);
            response.put("message", "Archivo subido exitosamente");
            response.put("data", fileResponse);

            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (IllegalArgumentException e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error al subir el archivo: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Endpoint para obtener un archivo
     * GET /files/{folder}/{filename}
     */
    @GetMapping("/{folder}/{filename}")
    public ResponseEntity<?> getFile(
            @PathVariable String folder,
            @PathVariable String filename
    ) {
        try {
            String objectName = folder + "/" + filename;

            // Obtener información del archivo
            StatObjectResponse stat = minioService.getFileInfo(objectName);

            // Obtener archivo
            InputStream fileStream = minioService.getFile(objectName);
            InputStreamResource resource = new InputStreamResource(fileStream);

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(stat.contentType()))
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "inline; filename=\"" + filename + "\"")
                    .contentLength(stat.size())
                    .body(resource);

        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Error al obtener el archivo: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }
    }

    /**
     * Endpoint para obtener la URL de un archivo
     * GET /files/url?objectName=folder/filename
     */
    @GetMapping("/url")
    public ResponseEntity<Map<String, Object>> getFileUrl(
            @RequestParam String objectName
    ) {
        Map<String, Object> response = new HashMap<>();

        try {
            String fileUrl = minioService.getFileUrl(objectName);

            response.put("success", true);
            response.put("url", fileUrl);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error al obtener la URL: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Endpoint para eliminar un archivo
     * DELETE /files?objectName=folder/filename
     */
    @DeleteMapping
    public ResponseEntity<Map<String, Object>> deleteFile(
            @RequestParam String objectName
    ) {
        Map<String, Object> response = new HashMap<>();

        try {
            minioService.deleteFile(objectName);

            response.put("success", true);
            response.put("message", "Archivo eliminado exitosamente");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error al eliminar el archivo: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}