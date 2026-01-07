package taf.yugioh.scanner.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import taf.yugioh.scanner.entity.Card;
import taf.yugioh.scanner.model.CardResponse;
import taf.yugioh.scanner.repository.CardRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import jakarta.annotation.PostConstruct;
import java.time.Duration;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * Service for fetching Yu-Gi-Oh card data.
 *
 * Uses YGOProDeck API v7: https://db.ygoprodeck.com/api/v7/cardinfo.php
 * API Docs: https://ygoprodeck.com/api-guide/
 *
 * Rate Limit: 20 requests/second (blocked 1 hour if exceeded)
 */
@Service
public class YugiohApiService {

    @Value("${yugioh.api.base.url:https://db.ygoprodeck.com/api/v7/cardinfo.php}")
    private String apiBaseUrl;

    @Value("${app.backend.url:http://localhost:8080}")
    private String backendUrl;

    private final DatabaseImageService databaseImageService;
    private final CardRepository cardRepository;
    private final ObjectMapper objectMapper;
    private RestTemplate restTemplate;

    private static final Logger logger = LoggerFactory.getLogger(YugiohApiService.class);


    // Constructor injection (recommended over @Autowired field injection)
    public YugiohApiService(DatabaseImageService databaseImageService, CardRepository cardRepository) {
        this.databaseImageService = databaseImageService;
        this.cardRepository = cardRepository;
        this.objectMapper = new ObjectMapper();
    }

    @PostConstruct
    public void init() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(5));
        factory.setReadTimeout(Duration.ofSeconds(10));
        this.restTemplate = new RestTemplate(factory);
        logger.info("YugiohApiService initialized with backend URL: " + backendUrl);
    }

    /**
     * Get card by name - for manual search and OCR results
     */
    public CardResponse getCardByName(String cardName) {
        if (cardName == null || cardName.trim().isEmpty()) {
            return null;
        }
        String cleanName = cardName.trim();

        // 1. Check database first (fastest)
        CardResponse cached = findInDatabase(cleanName);
        if (cached != null) {
            logger.info("✓ DB hit: " + cleanName);
            return cached;
        }

        // 2. Try exact match from API
        CardResponse result = fetchFromApi("name", cleanName);
        if (result != null) {
            saveToDatabase(result);
            return result;
        }

        // 3. Try fuzzy search (better for OCR typos)
        result = fetchFromApi("fname", cleanName);
        if (result != null) {
            saveToDatabase(result);
            return result;
        }

        logger.info("✗ Not found: " + cleanName);
        return null;
    }

    /**
     * Get card by ID - used by DeckService for deck operations
     */
    public CardResponse getCardById(Long cardId) {
        if (cardId == null) {
            return null;
        }
        Optional<CardResponse> cached = databaseImageService.getCardFromDatabase(cardId);
        if (cached.isPresent()) {
            return cached.get();
        }
        CardResponse result = fetchFromApi("id", String.valueOf(cardId));
        if (result != null) {
            saveToDatabase(result);
        }
        return result;
    }

    // ==================== Private Helper Methods ====================

    private CardResponse findInDatabase(String cardName) {
        try {
            Optional<Card> cardOpt = cardRepository.findByNameIgnoreCase(cardName);
            if (cardOpt.isPresent()) {
                return mapToResponse(cardOpt.get());
            }
        } catch (Exception e) {
            logger.error("DB lookup error: " + e.getMessage());
        }
        return null;
    }

    private CardResponse fetchFromApi(String paramName, String paramValue) {
        try {
            UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(apiBaseUrl)
                    .queryParam(paramName, paramValue);

            if ("fname".equals(paramName)) {
                builder.queryParam("num", 1).queryParam("offset", 0);
            }

            String response = restTemplate.getForObject(builder.build().toUriString(), String.class);
            if (response == null) {
                return null;
            }

            JsonNode data = objectMapper.readTree(response).get("data");
            if (data == null || !data.isArray() || data.isEmpty()) {
                return null;
            }

            CardResponse card = parseJson(data.get(0));
            logger.info("✓ API hit (" + paramName + "): " + card.getName());
            return card;
        } catch (Exception e) {
            if (!e.getMessage().contains("404")) {
                logger.error("API error: " + e.getMessage());
            }
            return null;
        }
    }

    private void saveToDatabase(CardResponse card) {
        // Run database saving and image downloading in a background thread
        // so the user gets the API response immediately.
        java.util.concurrent.CompletableFuture.runAsync(() -> {
            try {
                // 1. Save Card Data
                databaseImageService.saveCardToDatabase(card);

                // 2. Download Image (This is the slow part)
                if (card.getImageUrl() != null) {
                    databaseImageService.downloadAndStoreImage(
                            card.getImageUrl(),
                            card.getImageUrlSmall(),
                            card.getId()
                    );
                    // Note: We don't need to update the 'card' object here
                    // because the User has already received their response.
                    // The NEXT user to query this card will get the local images.
                }
            } catch (Exception e) {
                logger.error("Background save error: " + e.getMessage());
            }
        });
    }

    private CardResponse mapToResponse(Card card) {
        CardResponse r = new CardResponse();
        r.setId(card.getCardId());
        r.setName(card.getName());
        r.setType(card.getType());
        r.setFrameType(card.getFrameType());
        r.setDesc(card.getDescription());
        r.setAtk(card.getAtk());
        r.setDef(card.getDef());
        r.setLevel(card.getLevel());
        r.setRace(card.getRace());
        r.setAttribute(card.getAttribute());
        r.setImageUrl(buildImageUrl(card.getCardId(), false));
        r.setImageUrlSmall(buildImageUrl(card.getCardId(), true));
        return r;
    }

    private CardResponse parseJson(JsonNode node) {
        CardResponse card = new CardResponse();
        card.setId(node.get("id").asLong());
        card.setName(node.get("name").asText());
        card.setType(node.get("type").asText());
        card.setFrameType(node.get("frameType").asText());
        card.setDesc(node.get("desc").asText());

        // Optional monster stats - using helper to avoid duplicate null checks
        setIfPresent(node, "atk", val -> card.setAtk(val.asInt()));
        setIfPresent(node, "def", val -> card.setDef(val.asInt()));
        setIfPresent(node, "level", val -> card.setLevel(val.asInt()));
        setIfPresent(node, "race", val -> card.setRace(val.asText()));
        setIfPresent(node, "attribute", val -> card.setAttribute(val.asText()));

        // Card images
        JsonNode images = node.get("card_images");
        if (images != null && images.isArray() && !images.isEmpty()) {
            JsonNode img = images.get(0);
            card.setImageUrl(img.get("image_url").asText());
            card.setImageUrlSmall(img.get("image_url_small").asText());
        }

        // Card sets (optional)
        JsonNode sets = node.get("card_sets");
        if (sets != null && sets.isArray()) {
            card.setCardSets(objectMapper.convertValue(sets, Object[].class));
        }

        // Card prices (optional)
        JsonNode prices = node.get("card_prices");
        if (prices != null && prices.isArray() && !prices.isEmpty()) {
            card.setCardPrices(objectMapper.convertValue(prices.get(0), Object.class));
        }

        return card;
    }

    /**
     * Helper to reduce duplicate null-checking code for optional JSON fields
     */
    private void setIfPresent(JsonNode node, String field, Consumer<JsonNode> setter) {
        if (node.has(field) && !node.get(field).isNull()) {
            setter.accept(node.get(field));
        }
    }

    /**
     * Build image URL using configurable backend URL
     * Must match DatabaseImageController endpoints
     */
    private String buildImageUrl(Long cardId, boolean small) {
        String endpoint = small ? "/small" : "/regular";
        return backendUrl + "/api/images/" + cardId + endpoint;
    }
}