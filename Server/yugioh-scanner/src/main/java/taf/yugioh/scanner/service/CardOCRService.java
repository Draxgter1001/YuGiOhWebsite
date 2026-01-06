package taf.yugioh.scanner.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class CardOCRService {

    private final String tempUploadDir;
    private final String ocrServerUrl;
    private final RestTemplate restTemplate;

    public CardOCRService(
            @Value("${app.upload.temp.dir}") String tempUploadDir,
            @Value("${app.python.ocr.url}") String ocrServerUrl,
            RestTemplateBuilder restTemplateBuilder) {
        this.tempUploadDir = tempUploadDir;
        this.ocrServerUrl = ocrServerUrl;
        // Set timeouts to prevent hanging threads if Python hangs
        this.restTemplate = restTemplateBuilder
                .setConnectTimeout(Duration.ofSeconds(2))
                .setReadTimeout(Duration.ofSeconds(30))
                .build();
    }

    public String extractCardName(MultipartFile imageFile) throws Exception {
        Path tempDir = Paths.get(tempUploadDir);
        if (!Files.exists(tempDir)) {
            Files.createDirectories(tempDir);
        }

        // SECURITY: Use UUID to prevent Path Traversal attacks via malicious filenames
        String originalExt = getFileExtension(imageFile.getOriginalFilename());
        String safeFileName = UUID.randomUUID().toString() + originalExt;
        Path tempFilePath = tempDir.resolve(safeFileName);

        try {
            // Save file so Python can read it
            Files.copy(imageFile.getInputStream(), tempFilePath);

            // Prepare Request
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, String> body = new HashMap<>();
            // Pass absolute path to Python
            body.put("image_path", tempFilePath.toAbsolutePath().toString());

            HttpEntity<Map<String, String>> request = new HttpEntity<>(body, headers);

            // Call Python Server
            Map response = restTemplate.postForObject(ocrServerUrl, request, Map.class);

            if (response != null && response.containsKey("card_name")) {
                return (String) response.get("card_name");
            }

            return null;

        } catch (Exception e) {
            // Log the error (consider using SLF4J in real code)
            System.err.println("OCR Service Error: " + e.getMessage());
            throw new RuntimeException("Could not process image via OCR service.");
        } finally {
            // Cleanup: Always delete temp file
            try {
                Files.deleteIfExists(tempFilePath);
            } catch (IOException e) {
                System.err.println("Failed to delete temp file: " + safeFileName);
            }
        }
    }

    private String getFileExtension(String filename) {
        if (filename == null || filename.lastIndexOf('.') == -1) {
            return ".jpg"; // Default
        }
        return filename.substring(filename.lastIndexOf('.'));
    }
}