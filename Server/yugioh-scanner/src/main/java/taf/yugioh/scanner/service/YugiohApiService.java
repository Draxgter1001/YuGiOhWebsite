package taf.yugioh.scanner.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import taf.yugioh.scanner.dto.CardPrices;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

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
     * Search for multiple cards by partial name - for autocomplete dropdown
     * Uses YGOProDeck's fname (fuzzy name) parameter
     */
    public List<CardResponse> searchCardsByName(String query, int limit) {
        List<CardResponse> results = new ArrayList<>();

        if (query == null || query.trim().isEmpty()) {
            return results;
        }

        String cleanQuery = query.trim();
        logger.info("Autocomplete search for: " + cleanQuery);

        try {
            // Use fname (fuzzy name) to search for partial matches
            UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(apiBaseUrl)
                    .queryParam("fname", cleanQuery)
                    .queryParam("num", limit)
                    .queryParam("offset", 0);

            String response = restTemplate.getForObject(builder.build().toUriString(), String.class);
            if (response == null) {
                return results;
            }

            JsonNode data = objectMapper.readTree(response).get("data");
            if (data == null || !data.isArray()) {
                return results;
            }

            // Parse each card in the response
            for (int i = 0; i < data.size() && i < limit; i++) {
                try {
                    CardResponse card = parseJson(data.get(i));
                    results.add(card);

                    // Save to database in background (don't block response)
                    saveToDatabase(card);
                } catch (Exception e) {
                    logger.warn("Error parsing card: " + e.getMessage());
                }
            }

            logger.info("✓ Autocomplete found " + results.size() + " cards for: " + cleanQuery);

        } catch (Exception e) {
            if (!e.getMessage().contains("404")) {
                logger.error("Autocomplete API error: " + e.getMessage());
            }
        }

        return results;
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
        java.util.concurrent.CompletableFuture.runAsync(() -> {
            try {
                // 1. Save Card Data
                databaseImageService.saveCardToDatabase(card);

                // 2. Download Image (This is the slow part)
                if (card.getImageUrl() != null && !databaseImageService.imageExists(card.getId())) {
                    databaseImageService.downloadAndStoreImage(
                            card.getImageUrl(),
                            card.getImageUrlSmall(),
                            card.getId()
                    );
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

        // Parse prices from stored JSON
        if (card.getCardPrices() != null && !card.getCardPrices().isEmpty()) {
            try {
                CardPrices prices = parsePricesFromJson(card.getCardPrices());
                r.setPrices(prices);
            } catch (Exception e) {
                logger.warn("Error parsing stored prices: " + e.getMessage());
            }
        }

        return r;
    }

    private CardResponse parseJson(JsonNode node) {
        CardResponse card = new CardResponse();
        card.setId(node.get("id").asLong());
        card.setName(node.get("name").asText());
        card.setType(node.get("type").asText());
        card.setFrameType(node.get("frameType").asText());
        card.setDesc(node.get("desc").asText());

        // Optional monster stats
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

        // Card prices - parse into structured format
        JsonNode prices = node.get("card_prices");
        if (prices != null && prices.isArray() && !prices.isEmpty()) {
            JsonNode priceNode = prices.get(0);
            card.setCardPrices(objectMapper.convertValue(priceNode, Object.class));

            // Parse into structured CardPrices object
            CardPrices cardPrices = new CardPrices();
            setIfPresent(priceNode, "cardmarket_price", val -> cardPrices.setCardmarketPrice(val.asText()));
            setIfPresent(priceNode, "tcgplayer_price", val -> cardPrices.setTcgplayerPrice(val.asText()));
            setIfPresent(priceNode, "ebay_price", val -> cardPrices.setEbayPrice(val.asText()));
            setIfPresent(priceNode, "amazon_price", val -> cardPrices.setAmazonPrice(val.asText()));
            setIfPresent(priceNode, "coolstuffinc_price", val -> cardPrices.setCoolstuffincPrice(val.asText()));
            card.setPrices(cardPrices);
        }

        return card;
    }

    /**
     * Parse CardPrices from stored JSON string
     */
    private CardPrices parsePricesFromJson(String jsonString) {
        try {
            JsonNode priceNode = objectMapper.readTree(jsonString);
            CardPrices prices = new CardPrices();

            setIfPresent(priceNode, "cardmarket_price", val -> prices.setCardmarketPrice(val.asText()));
            setIfPresent(priceNode, "tcgplayer_price", val -> prices.setTcgplayerPrice(val.asText()));
            setIfPresent(priceNode, "ebay_price", val -> prices.setEbayPrice(val.asText()));
            setIfPresent(priceNode, "amazon_price", val -> prices.setAmazonPrice(val.asText()));
            setIfPresent(priceNode, "coolstuffinc_price", val -> prices.setCoolstuffincPrice(val.asText()));

            return prices;
        } catch (Exception e) {
            logger.error("Error parsing prices JSON: " + e.getMessage());
            return null;
        }
    }

    private void setIfPresent(JsonNode node, String field, Consumer<JsonNode> setter) {
        if (node.has(field) && !node.get(field).isNull()) {
            setter.accept(node.get(field));
        }
    }

    private String buildImageUrl(Long cardId, boolean small) {
        String endpoint = small ? "/small" : "/regular";
        return backendUrl + "/api/images/" + cardId + endpoint;
    }
}