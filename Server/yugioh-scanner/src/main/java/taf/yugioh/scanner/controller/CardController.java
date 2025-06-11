// CardController.java
package taf.yugioh.scanner.controller;

import taf.yugioh.scanner.service.CardOCRService;
import taf.yugioh.scanner.service.YugiohApiService;
import taf.yugioh.scanner.model.CardResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/cards")
@CrossOrigin(origins = "http://localhost:3000") // Adjust for your React app URL
public class CardController {

    @Autowired
    private CardOCRService cardOcrService;

    @Autowired
    private YugiohApiService yugiohApiService;

    @PostMapping("/upload")
    public ResponseEntity<?> uploadCard(@RequestParam("image") MultipartFile imageFile) {
        try {
            // Validate file
            if (imageFile.isEmpty()) {
                return ResponseEntity.badRequest().body("No image file provided");
            }

            // Check file type
            String contentType = imageFile.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                return ResponseEntity.badRequest().body("File must be an image");
            }

            // Extract card name using OCR
            String cardName = cardOcrService.extractCardName(imageFile);
            
            if (cardName == null || cardName.trim().isEmpty()) {
                return ResponseEntity.badRequest().body("Could not extract card name from image");
            }

            // Get card details from Yu-Gi-Oh API
            CardResponse cardDetails = yugiohApiService.getCardByName(cardName);
            
            if (cardDetails == null) {
                return ResponseEntity.ok().body(new ApiResponse(
                    false, 
                    "Card name extracted: '" + cardName + "' but not found in database",
                    cardName
                ));
            }

            return ResponseEntity.ok(new ApiResponse(true, "Card found successfully", cardDetails));

        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(new ApiResponse(false, "Error processing image: " + e.getMessage(), null));
        }
    }

    @GetMapping("/search")
    public ResponseEntity<?> searchCard(@RequestParam("name") String cardName) {
        try {
            CardResponse cardDetails = yugiohApiService.getCardByName(cardName);
            
            if (cardDetails == null) {
                return ResponseEntity.notFound().build();
            }

            return ResponseEntity.ok(cardDetails);

        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(new ApiResponse(false, "Error searching for card: " + e.getMessage(), null));
        }
    }

    // Response wrapper class
    public static class ApiResponse {
        private boolean success;
        private String message;
        private Object data;

        public ApiResponse(boolean success, String message, Object data) {
            this.success = success;
            this.message = message;
            this.data = data;
        }

        // Getters and setters
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        
        public Object getData() { return data; }
        public void setData(Object data) { this.data = data; }
    }
}