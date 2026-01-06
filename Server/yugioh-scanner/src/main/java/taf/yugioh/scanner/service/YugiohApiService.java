package taf.yugioh.scanner.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import taf.yugioh.scanner.model.CardResponse;

import java.time.Duration;
import java.util.Map;
import java.util.function.Consumer;

@Service
public class YugiohApiService {

    private static final Logger logger = LoggerFactory.getLogger(YugiohApiService.class);
    private final RestTemplate restTemplate;
    private final String baseUrl;
    private final ObjectMapper objectMapper;

    public YugiohApiService(@Value("${yugioh.api.base.url}") String baseUrl, RestTemplateBuilder builder, ObjectMapper objectMapper) {
        this.baseUrl = baseUrl;
        this.objectMapper = objectMapper;
        this.restTemplate = builder
                .setConnectTimeout(Duration.ofSeconds(5))
                .setReadTimeout(Duration.ofSeconds(10))
                .requestFactory(() -> new SimpleClientHttpRequestFactory()) // Simple factory avoids some pooling issues in serverless envs
                .build();
    }

    /**
     * Fetch card details from YGOProDeck API.
     * Caches the result in 'card_api_data' to avoid redundant external calls.
     */
    @Cacheable(value = "card_api_data", key = "#name", unless = "#result == null")
    public CardResponse getCardByName(String name) {
        // Use 'fname' (Fuzzy Name) search which is more tolerant of OCR minor typos
        String url = UriComponentsBuilder.fromHttpUrl(baseUrl)
                .queryParam("fname", name)
                .toUriString();

        try {
            logger.info("Fetching from YGOProDeck API: {}", name);
            // Fetch raw map first to inspect structure
            Map response = restTemplate.getForObject(url, Map.class);

            if (response != null && response.containsKey("data")) {
                // Convert the raw map data into a JsonNode for easier safe parsing
                JsonNode rootNode = objectMapper.valueToTree(response);
                JsonNode dataArray = rootNode.get("data");

                if (dataArray.isArray() && !dataArray.isEmpty()) {
                    // The API returns a list of matches; we take the first one as the best match
                    JsonNode cardData = dataArray.get(0);
                    return mapJsonToCardResponse(cardData);
                }
            }
        } catch (HttpClientErrorException.BadRequest e) {
            // 400 means "No card found" usually
            logger.warn("Card not found in External API: {}", name);
        } catch (Exception e) {
            logger.error("External API Error for card '{}': {}", name, e.getMessage());
        }
        return null;
    }

    /**
     * Maps the YGOProDeck JSON structure to our internal CardResponse model.
     * Handles nulls and missing fields safely.
     */
    private CardResponse mapJsonToCardResponse(JsonNode node) {
        CardResponse card = new CardResponse();

        // Basic Fields
        card.setId(node.get("id").asLong());
        card.setName(node.get("name").asText());
        card.setType(node.has("type") ? node.get("type").asText() : "Unknown");
        card.setDesc(node.has("desc") ? node.get("desc").asText() : "");

        // Stats (Check existence first)
        setIfPresent(node, "atk", (val) -> card.setAtk(val.asInt()));
        setIfPresent(node, "def", (val) -> card.setDef(val.asInt()));
        setIfPresent(node, "level", (val) -> card.setLevel(val.asInt()));
        setIfPresent(node, "race", (val) -> card.setRace(val.asText()));
        setIfPresent(node, "attribute", (val) -> card.setAttribute(val.asText()));

        // Images - Vital for the frontend
        // We prioritize the 'image_url' (full size) and 'image_url_small' provided by the API.
        JsonNode images = node.get("card_images");
        if (images != null && images.isArray() && !images.isEmpty()) {
            JsonNode img = images.get(0);
            if (img.has("image_url")) {
                card.setImageUrl(img.get("image_url").asText());
            }
            if (img.has("image_url_small")) {
                card.setImageUrlSmall(img.get("image_url_small").asText());
            }
        }

        // Optional: Prices (if you use them)
        if (node.has("card_prices") && !node.get("card_prices").isEmpty()) {
            // Mapping logic for prices could go here if needed
        }

        return card;
    }

    // Helper utility to avoid repetitive 'if (node.has...)' blocks
    private void setIfPresent(JsonNode node, String fieldName, Consumer<JsonNode> setter) {
        if (node.has(fieldName) && !node.get(fieldName).isNull()) {
            setter.accept(node.get(fieldName));
        }
    }
}