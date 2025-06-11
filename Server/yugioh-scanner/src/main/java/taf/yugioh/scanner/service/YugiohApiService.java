package taf.yugioh.scanner.service;

import taf.yugioh.scanner.model.CardResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;


@Service
public class YugiohApiService {

    @Value("${yugioh.api.base.url:https://db.ygoprodeck.com/api/v7/cardinfo.php}")
    private String yugiohApiBaseUrl;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public YugiohApiService() {
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
    }

    public CardResponse getCardByName(String cardName) {
        // Try the original name first
        CardResponse result = searchCard(cardName);
        if (result != null) {
            return result;
        }

        // If not found, try different case variations
        String[] variations = {
            cardName.toLowerCase(),  // all lowercase
            toTitleCase(cardName),   // proper title case
            cardName.toUpperCase(),  // all uppercase
            cardName.trim()          // just trimmed
        };

        for (String variation : variations) {
            if (!variation.equals(cardName)) { // Don't try the same name twice
                result = searchCard(variation);
                if (result != null) {
                    return result;
                }
            }
        }

        return null; // Card not found with any variation
    }

    private CardResponse searchCard(String cardName) {
        try {
            // Build the API URL with card name parameter
            String url = UriComponentsBuilder.fromUriString(yugiohApiBaseUrl)
                    .queryParam("name", cardName)
                    .queryParam("misc", "yes") // Include additional info
                    .build()
                    .toUriString();

            // Make API call
            String response = restTemplate.getForObject(url, String.class);
            
            if (response == null) {
                return null;
            }

            // Parse JSON response
            JsonNode rootNode = objectMapper.readTree(response);
            JsonNode dataNode = rootNode.get("data");
            
            if (dataNode == null || !dataNode.isArray() || dataNode.size() == 0) {
                return null;
            }

            // Get the first card (exact match should be first)
            JsonNode cardNode = dataNode.get(0);
            
            return parseCardFromJson(cardNode);

        } catch (Exception e) {
            // Log but don't throw - this allows trying other variations
            System.err.println("Error fetching card data for: " + cardName + " - " + e.getMessage());
            return null;
        }
    }

    private String toTitleCase(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }

        String[] words = input.toLowerCase().split("\\s+");
        StringBuilder titleCase = new StringBuilder();

        // Words that should remain lowercase unless they're the first word
        Set<String> smallWords = Set.of("the", "of", "and", "or", "but", "in", "on", "at", "to", "for", "from", "with", "by");

        for (int i = 0; i < words.length; i++) {
            if (i > 0) {
                titleCase.append(" ");
            }

            String word = words[i];
            if (i == 0 || !smallWords.contains(word)) {
                // Capitalize first letter, rest lowercase
                titleCase.append(Character.toUpperCase(word.charAt(0)))
                         .append(word.substring(1));
            } else {
                // Keep small words lowercase unless they're first
                titleCase.append(word);
            }
        }

        return titleCase.toString();
    }

    private CardResponse parseCardFromJson(JsonNode cardNode) {
        CardResponse card = new CardResponse();
        
        card.setId(cardNode.get("id").asLong());
        card.setName(cardNode.get("name").asText());
        card.setType(cardNode.get("type").asText());
        card.setFrameType(cardNode.get("frameType").asText());
        card.setDesc(cardNode.get("desc").asText());
        
        // Handle optional fields
        if (cardNode.has("atk")) {
            card.setAtk(cardNode.get("atk").asInt());
        }
        if (cardNode.has("def")) {
            card.setDef(cardNode.get("def").asInt());
        }
        if (cardNode.has("level")) {
            card.setLevel(cardNode.get("level").asInt());
        }
        if (cardNode.has("race")) {
            card.setRace(cardNode.get("race").asText());
        }
        if (cardNode.has("attribute")) {
            card.setAttribute(cardNode.get("attribute").asText());
        }
        
        // Get card images
        JsonNode imagesNode = cardNode.get("card_images");
        if (imagesNode != null && imagesNode.isArray() && imagesNode.size() > 0) {
            JsonNode firstImage = imagesNode.get(0);
            card.setImageUrl(firstImage.get("image_url").asText());
            card.setImageUrlSmall(firstImage.get("image_url_small").asText());
        }

        // Get card sets
        JsonNode setsNode = cardNode.get("card_sets");
        if (setsNode != null && setsNode.isArray()) {
            card.setCardSets(objectMapper.convertValue(setsNode, Object[].class));
        }

        // Get card prices
        JsonNode pricesNode = cardNode.get("card_prices");
        if (pricesNode != null && pricesNode.isArray() && pricesNode.size() > 0) {
            card.setCardPrices(objectMapper.convertValue(pricesNode.get(0), Object.class));
        }

        return card;
    }
}