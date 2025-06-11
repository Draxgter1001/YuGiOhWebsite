package taf.yugioh.scanner.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import taf.yugioh.scanner.service.DatabaseImageService;

import java.util.Optional;

@RestController
@RequestMapping("/api/images")
@CrossOrigin(origins = "*")
public class DatabaseImageController {

    @Autowired
    private DatabaseImageService databaseImageService;

    /**
     * Get regular-sized image from database
     */
    @GetMapping("/{cardId}/regular")
    public ResponseEntity<byte[]> getRegularImage(@PathVariable Long cardId) {
        try {
            Optional<byte[]> imageData = databaseImageService.getImageData(cardId, false);
            
            if (imageData.isPresent() && imageData.get().length > 0) {
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.IMAGE_JPEG);
                headers.setContentLength(imageData.get().length);
                headers.setCacheControl("max-age=3600"); // Cache for 1 hour
                
                return ResponseEntity.ok()
                    .headers(headers)
                    .body(imageData.get());
            } else {
                return ResponseEntity.notFound().build();
            }
            
        } catch (Exception e) {
            System.err.println("Error retrieving regular image for card " + cardId + ": " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get small-sized image from database
     */
    @GetMapping("/{cardId}/small")
    public ResponseEntity<byte[]> getSmallImage(@PathVariable Long cardId) {
        try {
            Optional<byte[]> imageData = databaseImageService.getImageData(cardId, true);
            
            if (imageData.isPresent() && imageData.get().length > 0) {
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.IMAGE_JPEG);
                headers.setContentLength(imageData.get().length);
                headers.setCacheControl("max-age=3600"); // Cache for 1 hour
                
                return ResponseEntity.ok()
                    .headers(headers)
                    .body(imageData.get());
            } else {
                return ResponseEntity.notFound().build();
            }
            
        } catch (Exception e) {
            System.err.println("Error retrieving small image for card " + cardId + ": " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Check if image exists for a card
     */
    @GetMapping("/{cardId}/exists")
    public ResponseEntity<Boolean> imageExists(@PathVariable Long cardId) {
        try {
            boolean exists = databaseImageService.imageExists(cardId);
            return ResponseEntity.ok(exists);
        } catch (Exception e) {
            System.err.println("Error checking if image exists for card " + cardId + ": " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(false);
        }
    }

    /**
     * Get image statistics
     */
    @GetMapping("/stats")
    public ResponseEntity<DatabaseImageService.ImageStats> getImageStats() {
        try {
            DatabaseImageService.ImageStats stats = databaseImageService.getImageStats();
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            System.err.println("Error retrieving image statistics: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Delete image for a specific card
     */
    @DeleteMapping("/{cardId}")
    public ResponseEntity<String> deleteImage(@PathVariable Long cardId) {
        try {
            if (databaseImageService.imageExists(cardId)) {
                databaseImageService.deleteCardImage(cardId);
                return ResponseEntity.ok("Image deleted successfully for card " + cardId);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            System.err.println("Error deleting image for card " + cardId + ": " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error deleting image: " + e.getMessage());
        }
    }
}