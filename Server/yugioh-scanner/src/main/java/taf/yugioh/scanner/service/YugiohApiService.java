package taf.yugioh.scanner.service;

import taf.yugioh.scanner.model.CardResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Optional;
import java.util.Set;

@Service
public class YugiohApiService {

    @Value("${yugioh.api.base.url:https://db.ygoprodeck.com/api/v7/cardinfo.php}")
    private String yugiohApiBaseUrl;

    @Autowired
    private DatabaseImageService databaseImageService;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public YugiohApiService() {
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
    }

    public CardResponse getCardByName(String cardName) {
        // First, try to get from database
        Optional<CardResponse> cachedCard = findCardInDatabase(cardName);
        if (cachedCard.isPresent()) {
            System.out.println("Found card in database: " + cardName);
            return cachedCard.get();
        }

        // If not in database, search API with different variations
        CardResponse result = searchCardFromApi(cardName);
        if (result != null) {
            // Save to database and download images
            saveCardToDatabase(result);
            return result;
        }

        // Try different case variations
        String[] variations = {
            cardName.toLowerCase(),
            toTitleCase(cardName),
            cardName.toUpperCase(),
            cardName.trim()
        };

        for (String variation : variations) {
            if (!variation.equals(cardName)) {
                result = searchCardFromApi(variation);
                if (result != null) {
                    saveCardToDatabase(result);
                    return result;
                }
            }
        }

        return null; // Card not found
    }

    private Optional<CardResponse> findCardInDatabase(String cardName) {
        // This is a simple implementation - you might want to improve the search logic
        // For now, we'll skip database search by name as it would require more complex matching
        return Optional.empty();
    }

    private CardResponse searchCardFromApi(String cardName) {
        try {
            String url = UriComponentsBuilder.fromUriString(yugiohApiBaseUrl)
                    .queryParam("name", cardName)
                    .queryParam("misc", "yes")
                    .build()
                    .toUriString();

            String response = restTemplate.getForObject(url, String.class);
            
            if (response == null) {
                return null;
            }

            JsonNode rootNode = objectMapper.readTree(response);
            JsonNode dataNode = rootNode.get("data");
            
            if (dataNode == null || !dataNode.isArray() || dataNode.size() == 0) {
                return null;
            }

            JsonNode cardNode = dataNode.get(0);
            return parseCardFromJson(cardNode);

        } catch (Exception e) {
            System.err.println("Error fetching card data for: " + cardName + " - " + e.getMessage());
            return null;
        }
    }

    private void saveCardToDatabase(CardResponse cardResponse) {
        try {
            // Save card information to database
            databaseImageService.saveCardToDatabase(cardResponse);

            // Download and save images if they exist
            if (cardResponse.getImageUrl() != null && cardResponse.getImageUrlSmall() != null) {
                String localImageUrl = databaseImageService.downloadAndStoreImage(
                    cardResponse.getImageUrl(),
                    cardResponse.getImageUrlSmall(),
                    cardResponse.getId()
                );

                if (localImageUrl != null) {
                    // Update the response with local URLs
                    cardResponse.setImageUrl(databaseImageService.buildLocalImageUrl(cardResponse.getId(), false));
                    cardResponse.setImageUrlSmall(databaseImageService.buildLocalImageUrl(cardResponse.getId(), true));
                }
            }

        } catch (Exception e) {
            System.err.println("Error saving card to database: " + e.getMessage());
        }
    }

    private String toTitleCase(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }

        String[] words = input.toLowerCase().split("\\s+");
        StringBuilder titleCase = new StringBuilder();

        Set<String> smallWords = Set.of("the", "of", "and", "or", "but", "in", "on", "at", "to", "for", "from", "with", "by");

        for (int i = 0; i < words.length; i++) {
            if (i > 0) {
                titleCase.append(" ");
            }

            String word = words[i];
            if (i == 0 || !smallWords.contains(word)) {
                titleCase.append(Character.toUpperCase(word.charAt(0)))
                         .append(word.substring(1));
            } else {
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
        
        // Get card images - store external URLs for now, will be replaced with local URLs after download
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

    /**
     * Get card by ID - first checks database, then API if needed
     */
    public CardResponse getCardById(Long cardId) {
        // Try database first
        Optional<CardResponse> cachedCard = databaseImageService.getCardFromDatabase(cardId);
        if (cachedCard.isPresent()) {
            return cachedCard.get();
        }

        // If not in database, search API
        try {
            String url = UriComponentsBuilder.fromUriString(yugiohApiBaseUrl)
                    .queryParam("id", cardId)
                    .queryParam("misc", "yes")
                    .build()
                    .toUriString();

            String response = restTemplate.getForObject(url, String.class);
            
            if (response != null) {
                JsonNode rootNode = objectMapper.readTree(response);
                JsonNode dataNode = rootNode.get("data");
                
                if (dataNode != null && dataNode.isArray() && dataNode.size() > 0) {
                    CardResponse card = parseCardFromJson(dataNode.get(0));
                    saveCardToDatabase(card);
                    return card;
                }
            }
        } catch (Exception e) {
            System.err.println("Error fetching card by ID " + cardId + ": " + e.getMessage());
        }

        return null;
    }

    
}