package taf.yugioh.scanner.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

@Service
public class CardOCRService {

    @Value("${app.upload.temp.dir:temp/uploads}")
    private String tempUploadDir;

    // URL of the Python Flask Server
    private final String OCR_SERVER_URL = "http://127.0.0.1:5000/extract";
    private final RestTemplate restTemplate = new RestTemplate();

    public String extractCardName(MultipartFile imageFile) throws Exception {
        Path tempDir = Paths.get(tempUploadDir);
        if (!Files.exists(tempDir)) {
            Files.createDirectories(tempDir);
        }

        String tempFileName = System.currentTimeMillis() + "_" + imageFile.getOriginalFilename();
        Path tempFilePath = tempDir.resolve(tempFileName);

        try {
            // Save file so Python can read it
            Files.copy(imageFile.getInputStream(), tempFilePath);

            // Call Python Server
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, String> body = new HashMap<>();
            body.put("image_path", tempFilePath.toAbsolutePath().toString());

            HttpEntity<Map<String, String>> request = new HttpEntity<>(body, headers);

            Map response = restTemplate.postForObject(OCR_SERVER_URL, request, Map.class);

            if (response != null && response.containsKey("card_name")) {
                return (String) response.get("card_name");
            }

            return null;

        } catch (Exception e) {
            // If server is down, you might want to fallback to CLI or log error
            System.err.println("OCR Server Error: " + e.getMessage());
            throw new RuntimeException("OCR Service Unavailable");
        } finally {
            try {
                Files.deleteIfExists(tempFilePath);
            } catch (IOException e) {
                // Ignore delete errors
            }
        }
    }
}