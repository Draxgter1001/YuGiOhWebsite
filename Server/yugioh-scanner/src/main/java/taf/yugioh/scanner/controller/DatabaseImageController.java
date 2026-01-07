package taf.yugioh.scanner.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import taf.yugioh.scanner.service.DatabaseImageService;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * Controller for serving card images from database.
 * CORS is handled by global SecurityConfig - DO NOT add @CrossOrigin here.
 */
@RestController
@RequestMapping("/api/images")
public class DatabaseImageController {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseImageController.class);

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
                return ResponseEntity.ok()
                        .contentType(MediaType.IMAGE_JPEG)
                        .contentLength(imageData.get().length)
                        .cacheControl(CacheControl.maxAge(7, TimeUnit.DAYS).cachePublic())
                        .header(HttpHeaders.ETAG, "\"" + cardId + "-regular\"")
                        .body(imageData.get());
            } else {
                return ResponseEntity.notFound().build();
            }

        } catch (Exception e) {
            logger.error("Error retrieving regular image for card {}: {}", cardId, e.getMessage());
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
                return ResponseEntity.ok()
                        .contentType(MediaType.IMAGE_JPEG)
                        .contentLength(imageData.get().length)
                        .cacheControl(CacheControl.maxAge(7, TimeUnit.DAYS).cachePublic())
                        .header(HttpHeaders.ETAG, "\"" + cardId + "-small\"")
                        .body(imageData.get());
            } else {
                return ResponseEntity.notFound().build();
            }

        } catch (Exception e) {
            logger.error("Error retrieving small image for card {}: {}", cardId, e.getMessage());
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
            return ResponseEntity.ok()
                    .cacheControl(CacheControl.maxAge(5, TimeUnit.MINUTES))
                    .body(exists);
        } catch (Exception e) {
            logger.error("Error checking image existence for card {}: {}", cardId, e.getMessage());
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
            return ResponseEntity.ok()
                    .cacheControl(CacheControl.maxAge(1, TimeUnit.MINUTES))
                    .body(stats);
        } catch (Exception e) {
            logger.error("Error retrieving image statistics: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Delete image for a specific card (requires authentication via SecurityConfig)
     */
    @DeleteMapping("/{cardId}")
    public ResponseEntity<String> deleteImage(@PathVariable Long cardId) {
        try {
            if (databaseImageService.imageExists(cardId)) {
                databaseImageService.deleteCardImage(cardId);
                logger.info("Deleted image for card {}", cardId);
                return ResponseEntity.ok("Image deleted successfully for card " + cardId);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            logger.error("Error deleting image for card {}: {}", cardId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error deleting image");
        }
    }
}