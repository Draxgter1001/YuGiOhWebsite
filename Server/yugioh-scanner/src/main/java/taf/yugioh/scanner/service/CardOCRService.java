package taf.yugioh.scanner.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.time.Duration;
import java.util.Map;

@Service
public class CardOCRService {

    private static final Logger logger = LoggerFactory.getLogger(CardOCRService.class);

    private final String ocrServerUrl;
    private final RestTemplate restTemplate;

    public CardOCRService(
            @Value("${app.python.ocr.url}") String ocrServerUrl,
            RestTemplateBuilder restTemplateBuilder) {
        this.ocrServerUrl = ocrServerUrl;
        this.restTemplate = restTemplateBuilder
                .setConnectTimeout(Duration.ofSeconds(10))
                .setReadTimeout(Duration.ofSeconds(90)) // HuggingFace can be slow on cold start
                .build();
    }

    /**
     * Extract card name from uploaded image using external OCR service.
     * Optimized to send bytes directly without saving to disk.
     */
    public String extractCardName(MultipartFile imageFile) {
        try {
            // Get image bytes directly - no disk I/O needed
            byte[] imageBytes = imageFile.getBytes();

            // Get filename - must be final for inner class
            final String filename;
            String originalFilename = imageFile.getOriginalFilename();
            if (originalFilename == null || originalFilename.isBlank()) {
                filename = "card.jpg";
            } else {
                filename = originalFilename;
            }

            logger.info("Processing image: {} ({} bytes)", filename, imageBytes.length);

            // Create a ByteArrayResource that provides the filename
            ByteArrayResource resource = new ByteArrayResource(imageBytes) {
                @Override
                public String getFilename() {
                    return filename;
                }
            };

            // Prepare multipart request
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("image", resource);

            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

            // Build endpoint URL
            String endpoint = ocrServerUrl.endsWith("/")
                    ? ocrServerUrl + "extract"
                    : ocrServerUrl + "/extract";

            logger.debug("Calling OCR service at: {}", endpoint);

            // Call OCR service
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.postForObject(endpoint, requestEntity, Map.class);

            if (response != null && response.containsKey("card_name")) {
                String cardName = (String) response.get("card_name");
                logger.info("OCR extracted card name: {}", cardName);
                return cardName;
            }

            logger.warn("OCR service returned no card name");
            return null;

        } catch (Exception e) {
            logger.error("OCR Service Error: {}", e.getMessage());
            throw new RuntimeException("Could not process image via OCR service: " + e.getMessage());
        }
    }
}