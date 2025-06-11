package taf.yugioh.scanner.service;

import taf.yugioh.scanner.entity.Card;
import taf.yugioh.scanner.entity.CardImage;
import taf.yugioh.scanner.repository.CardImageRepository;
import taf.yugioh.scanner.repository.CardRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Service
@Transactional
public class DatabaseImageService {

    @Value("${server.port:8080}")
    private String serverPort;

    @Autowired
    private CardImageRepository cardImageRepository;

    @Autowired
    private CardRepository cardRepository;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public DatabaseImageService() {
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Downloads and stores a card image in the database, returns the local URL
     */
    public String downloadAndStoreImage(String externalImageUrl, String externalSmallImageUrl, Long cardId) {
        try {
            // Check if image already exists
            if (cardImageRepository.existsByCardId(cardId)) {
                return buildLocalImageUrl(cardId, false);
            }

            // Download both images
            byte[] imageData = null;
            byte[] smallImageData = null;

            if (externalImageUrl != null && !externalImageUrl.isEmpty()) {
                try {
                    imageData = restTemplate.getForObject(externalImageUrl, byte[].class);
                } catch (Exception e) {
                    System.err.println("Failed to download main image: " + e.getMessage());
                }
            }

            if (externalSmallImageUrl != null && !externalSmallImageUrl.isEmpty()) {
                try {
                    smallImageData = restTemplate.getForObject(externalSmallImageUrl, byte[].class);
                } catch (Exception e) {
                    System.err.println("Failed to download small image: " + e.getMessage());
                }
            }

            if (imageData != null && imageData.length > 0) {
                // Create and save card image
                CardImage cardImage = new CardImage();
                cardImage.setCardId(cardId);
                cardImage.setImageData(imageData); // This will automatically set fileSize
                cardImage.setImageSmallData(smallImageData); // This will automatically set smallFileSize
                cardImage.setOriginalUrl(externalImageUrl);
                cardImage.setOriginalSmallUrl(externalSmallImageUrl);
                cardImage.setContentType("image/jpeg");

                // Save with proper transaction handling
                CardImage savedImage = cardImageRepository.save(cardImage);

                System.out.println("Downloaded and stored image for card " + cardId + 
                    " (Size: " + imageData.length + " bytes" +
                    (smallImageData != null ? ", Small: " + smallImageData.length + " bytes" : "") + ")");

                return buildLocalImageUrl(cardId, false);
            } else {
                System.err.println("Failed to download image: " + externalImageUrl);
                return null;
            }

        } catch (Exception e) {
            System.err.println("Error downloading image " + externalImageUrl + ": " + e.getMessage());
            e.printStackTrace(); // Add stack trace for debugging
            return null;
        }
    }

    /**
     * Downloads images asynchronously to avoid blocking the main response
     */
    public CompletableFuture<String> downloadImageAsync(String externalImageUrl, String externalSmallImageUrl, Long cardId) {
        return CompletableFuture.supplyAsync(() -> 
            downloadAndStoreImage(externalImageUrl, externalSmallImageUrl, cardId)
        );
    }

    /**
     * Get image data from database
     */
    public Optional<byte[]> getImageData(Long cardId, boolean isSmall) {
        if (isSmall) {
            return cardImageRepository.findSmallImageDataByCardId(cardId);
        } else {
            Optional<CardImage> cardImage = cardImageRepository.findImageDataByCardId(cardId);
            return cardImage.map(CardImage::getImageData);
        }
    }

    /**
     * Get full card image entity from database
     */
    public Optional<CardImage> getCardImage(Long cardId) {
        return cardImageRepository.findByCardId(cardId);
    }

    /**
     * Builds the local URL that the frontend will use to access the image
     */
    public String buildLocalImageUrl(Long cardId, boolean isSmall) {
        String endpoint = isSmall ? "small" : "regular";
        return "http://localhost:" + serverPort + "/api/images/" + cardId + "/" + endpoint;
    }

    /**
     * Check if an image exists in database
     */
    public boolean imageExists(Long cardId) {
        return cardImageRepository.existsByCardId(cardId);
    }

    /**
     * Save or update card information in database
     */
    @Transactional
    public Card saveCardToDatabase(taf.yugioh.scanner.model.CardResponse cardResponse) {
        try {
            // Check if card already exists
            Optional<Card> existingCard = cardRepository.findByCardId(cardResponse.getId());
            
            Card card;
            if (existingCard.isPresent()) {
                card = existingCard.get();
                System.out.println("Updating existing card: " + card.getName());
            } else {
                card = new Card();
                card.setCardId(cardResponse.getId());
                System.out.println("Creating new card: " + cardResponse.getName());
            }

            // Update card information
            card.setName(cardResponse.getName());
            card.setType(cardResponse.getType());
            card.setFrameType(cardResponse.getFrameType());
            card.setDescription(cardResponse.getDesc());
            card.setAtk(cardResponse.getAtk());
            card.setDef(cardResponse.getDef());
            card.setLevel(cardResponse.getLevel());
            card.setRace(cardResponse.getRace());
            card.setAttribute(cardResponse.getAttribute());

            // Convert objects to JSON strings for database storage - FIXED
            if (cardResponse.getCardSets() != null) {
                String jsonSets = convertToJsonString(cardResponse.getCardSets());
                card.setCardSets(jsonSets);
            }
            if (cardResponse.getCardPrices() != null) {
                String jsonPrices = convertToJsonString(cardResponse.getCardPrices());
                card.setCardPrices(jsonPrices);
            }

            Card savedCard = cardRepository.save(card);
            System.out.println("Successfully saved card: " + savedCard.getName() + " (ID: " + savedCard.getCardId() + ")");
            return savedCard;

        } catch (Exception e) {
            System.err.println("Error saving card to database: " + e.getMessage());
            e.printStackTrace(); // Add stack trace for debugging
            throw e; // Re-throw to trigger transaction rollback
        }
    }

    /**
     * Get card from database - UPDATED to work without relationship
     */
    public Optional<taf.yugioh.scanner.model.CardResponse> getCardFromDatabase(Long cardId) {
        Optional<Card> cardOpt = cardRepository.findByCardId(cardId);
        
        if (cardOpt.isPresent()) {
            Card card = cardOpt.get();
            taf.yugioh.scanner.model.CardResponse response = new taf.yugioh.scanner.model.CardResponse();
            
            // Map card data
            response.setId(card.getCardId());
            response.setName(card.getName());
            response.setType(card.getType());
            response.setFrameType(card.getFrameType());
            response.setDesc(card.getDescription());
            response.setAtk(card.getAtk());
            response.setDef(card.getDef());
            response.setLevel(card.getLevel());
            response.setRace(card.getRace());
            response.setAttribute(card.getAttribute());

            // Set image URLs if they exist (query separately)
            if (imageExists(cardId)) {
                response.setImageUrl(buildLocalImageUrl(cardId, false));
                response.setImageUrlSmall(buildLocalImageUrl(cardId, true));
            }

            return Optional.of(response);
        }
        
        return Optional.empty();
    }

    /**
     * Helper method to convert objects to JSON strings - FIXED
     */
    private String convertToJsonString(Object obj) {
        try {
            if (obj == null) {
                return null;
            }
            
            // If it's already a string, return as-is
            if (obj instanceof String) {
                return (String) obj;
            }
            
            // Convert object to JSON string
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            System.err.println("Error converting to JSON: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Delete card image from database
     */
    @Transactional
    public void deleteCardImage(Long cardId) {
        cardImageRepository.deleteByCardId(cardId);
    }

    /**
     * Get image statistics
     */
    public ImageStats getImageStats() {
        long totalImages = cardImageRepository.count();
        return new ImageStats(totalImages);
    }

    public static class ImageStats {
        private final long totalImages;

        public ImageStats(long totalImages) {
            this.totalImages = totalImages;
        }

        public long getTotalImages() {
            return totalImages;
        }
    }
    
}