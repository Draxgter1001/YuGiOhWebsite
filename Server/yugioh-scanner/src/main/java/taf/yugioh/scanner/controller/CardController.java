package taf.yugioh.scanner.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import taf.yugioh.scanner.service.CardOCRService;
import taf.yugioh.scanner.service.YugiohApiService;
import taf.yugioh.scanner.model.CardResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * Controller for card scanning and searching.
 * CORS is handled by global SecurityConfig - DO NOT add @CrossOrigin here.
 */
@RestController
@RequestMapping("/api/cards")
public class CardController {

    private static final Logger logger = LoggerFactory.getLogger(CardController.class);

    // Maximum file size for card images (5MB)
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024;

    @Autowired
    private CardOCRService cardOcrService;

    @Autowired
    private YugiohApiService yugiohApiService;

    @PostMapping("/upload")
    public ResponseEntity<?> uploadCard(@RequestParam("image") MultipartFile imageFile) {
        try {
            // Validate file presence
            if (imageFile == null || imageFile.isEmpty()) {
                return ResponseEntity.badRequest().body(
                        new ApiResponse(false, "No image file provided", null)
                );
            }

            // Validate file size
            if (imageFile.getSize() > MAX_FILE_SIZE) {
                return ResponseEntity.badRequest().body(
                        new ApiResponse(false, "File size exceeds 5MB limit", null)
                );
            }

            // Validate file type
            String contentType = imageFile.getContentType();
            if (contentType == null || !isValidImageType(contentType)) {
                return ResponseEntity.badRequest().body(
                        new ApiResponse(false, "File must be a JPEG, PNG, or WebP image", null)
                );
            }

            logger.info("Processing card image upload: {} bytes", imageFile.getSize());

            // Extract card name using OCR
            String cardName = cardOcrService.extractCardName(imageFile);

            if (cardName == null || cardName.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(
                        new ApiResponse(false, "Could not extract card name from image", null)
                );
            }

            // Get card details from Yu-Gi-Oh API/database
            CardResponse cardDetails = yugiohApiService.getCardByName(cardName);

            if (cardDetails == null) {
                return ResponseEntity.ok(new ApiResponse(
                        false,
                        "Card name extracted: '" + cardName + "' but not found in database",
                        cardName
                ));
            }

            return ResponseEntity.ok(new ApiResponse(true, "Card found successfully", cardDetails));

        } catch (Exception e) {
            logger.error("Error processing card upload: {}", e.getMessage());
            return ResponseEntity.internalServerError()
                    .body(new ApiResponse(false, "Error processing image. Please try again.", null));
        }
    }

    @GetMapping("/search")
    public ResponseEntity<?> searchCard(@RequestParam("name") String cardName) {
        try {
            // Validate input
            if (cardName == null || cardName.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(
                        new ApiResponse(false, "Card name is required", null)
                );
            }

            // Limit search term length to prevent abuse
            if (cardName.length() > 200) {
                return ResponseEntity.badRequest().body(
                        new ApiResponse(false, "Search term too long", null)
                );
            }

            CardResponse cardDetails = yugiohApiService.getCardByName(cardName.trim());

            if (cardDetails == null) {
                return ResponseEntity.ok(new ApiResponse(
                        false,
                        "Card not found: '" + cardName + "'",
                        null
                ));
            }

            return ResponseEntity.ok(new ApiResponse(true, "Card found", cardDetails));

        } catch (Exception e) {
            logger.error("Error searching for card: {}", e.getMessage());
            return ResponseEntity.internalServerError()
                    .body(new ApiResponse(false, "Error searching for card. Please try again.", null));
        }
    }

    /**
     * Check if content type is a valid image format
     */
    private boolean isValidImageType(String contentType) {
        return contentType.equals("image/jpeg") ||
                contentType.equals("image/png") ||
                contentType.equals("image/webp") ||
                contentType.equals("image/jpg");
    }

    /**
     * Response wrapper class
     */
    public static class ApiResponse {
        private boolean success;
        private String message;
        private Object data;

        public ApiResponse(boolean success, String message, Object data) {
            this.success = success;
            this.message = message;
            this.data = data;
        }

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }

        public Object getData() { return data; }
        public void setData(Object data) { this.data = data; }
    }
}