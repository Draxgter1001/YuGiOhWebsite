package taf.yugioh.scanner.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
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
        this.restTemplate = restTemplateBuilder
                .setConnectTimeout(Duration.ofSeconds(5))
                .setReadTimeout(Duration.ofSeconds(60)) // Give EasyOCR time to think
                .build();
    }

    public String extractCardName(MultipartFile imageFile) throws Exception {
        Path tempDir = Paths.get(tempUploadDir);
        if (!Files.exists(tempDir)) {
            Files.createDirectories(tempDir);
        }

        String safeFileName = UUID.randomUUID().toString() + ".jpg";
        Path tempFilePath = tempDir.resolve(safeFileName);

        try {
            // 1. Save file locally temporarily
            Files.copy(imageFile.getInputStream(), tempFilePath);

            // 2. Prepare Multipart Request for Hugging Face
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("image", new FileSystemResource(tempFilePath.toFile()));

            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

            // 3. Call External OCR Server
            // Append "/extract" to the base URL
            String endpoint = ocrServerUrl.endsWith("/") ? ocrServerUrl + "extract" : ocrServerUrl + "/extract";

            Map response = restTemplate.postForObject(endpoint, requestEntity, Map.class);

            if (response != null && response.containsKey("card_name")) {
                return (String) response.get("card_name");
            }

            return null;

        } catch (Exception e) {
            System.err.println("OCR Service Error: " + e.getMessage());
            throw new RuntimeException("Could not process image via OCR service.");
        } finally {
            try {
                Files.deleteIfExists(tempFilePath);
            } catch (IOException e) {
                // ignore
            }
        }
    }
}