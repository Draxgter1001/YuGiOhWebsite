package taf.yugioh.scanner.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import taf.yugioh.scanner.dto.ApiResponse; // Import the correct DTO
import taf.yugioh.scanner.entity.Card;
import taf.yugioh.scanner.model.CardResponse;
import taf.yugioh.scanner.repository.CardRepository;
import taf.yugioh.scanner.service.CardOCRService;
import taf.yugioh.scanner.service.DatabaseImageService;
import taf.yugioh.scanner.service.YugiohApiService;

import java.util.Optional;

@RestController
@RequestMapping("/api/cards")
@CrossOrigin(origins = "${app.cors.allowed-origins:http://localhost:3000}")
public class CardController {

    private static final Logger logger = LoggerFactory.getLogger(CardController.class);

    private final CardOCRService cardOCRService;
    private final YugiohApiService yugiohApiService;
    private final DatabaseImageService databaseImageService;
    private final CardRepository cardRepository;

    // Constructor Injection (Production Best Practice)
    public CardController(CardOCRService cardOCRService,
                          YugiohApiService yugiohApiService,
                          DatabaseImageService databaseImageService,
                          CardRepository cardRepository) {
        this.cardOCRService = cardOCRService;
        this.yugiohApiService = yugiohApiService;
        this.databaseImageService = databaseImageService;
        this.cardRepository = cardRepository;
    }

    @PostMapping("/upload")
    public ResponseEntity<ApiResponse<Card>> handleImageUpload(@RequestParam("image") MultipartFile file) {
        try {
            // 1. Extract Name via OCR (Hugging Face)
            String extractedName = cardOCRService.extractCardName(file);

            if (extractedName == null || extractedName.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(ApiResponse.error("Could not detect card name from image."));
            }

            logger.info("OCR Extracted Name: {}", extractedName);

            // 2. SPEED OPTIMIZATION: Check Local DB First!
            // Use findByNameIgnoreCase as defined in your Repository
            Optional<Card> existingCard = cardRepository.findByNameIgnoreCase(extractedName);
            if (existingCard.isPresent()) {
                logger.info("✓ DB Hit: Returning local data for {}", extractedName);
                return ResponseEntity.ok(ApiResponse.success("Card found in local database", existingCard.get()));
            }

            // 3. Fallback: Call External API (Cached)
            CardResponse apiCard = yugiohApiService.getCardByName(extractedName);

            if (apiCard == null) {
                return ResponseEntity.badRequest().body(ApiResponse.error("Card name extracted: '" + extractedName + "' but not found in YGOProDeck database."));
            }

            // 4. Save to DB for next time
            // Use saveCardToDatabase as defined in your Service
            Card savedCard = databaseImageService.saveCardToDatabase(apiCard);

            // 5. Trigger Async Image Download (so frontend has local images later)
            if (apiCard.getImageUrl() != null) {
                databaseImageService.downloadImageAsync(apiCard.getImageUrl(), apiCard.getImageUrlSmall(), savedCard.getCardId());
            }

            return ResponseEntity.ok(ApiResponse.success("Card identified and processed", savedCard));

        } catch (Exception e) {
            logger.error("Upload failed", e);
            return ResponseEntity.internalServerError().body(ApiResponse.error("Error processing card: " + e.getMessage()));
        }
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<CardResponse>> searchCard(@RequestParam("name") String cardName) {
        try {
            CardResponse cardDetails = yugiohApiService.getCardByName(cardName);

            if (cardDetails == null) {
                return ResponseEntity.badRequest().body(ApiResponse.error("Card not found"));
            }

            return ResponseEntity.ok(ApiResponse.success("Card found", cardDetails));

        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Error searching for card: " + e.getMessage()));
        }
    }
}