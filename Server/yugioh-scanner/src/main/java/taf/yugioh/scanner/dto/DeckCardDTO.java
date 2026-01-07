package taf.yugioh.scanner.dto;

import taf.yugioh.scanner.entity.Card;
import taf.yugioh.scanner.entity.DeckCard;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class DeckCardDTO {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    private Long id;
    private Long cardId;
    private String cardName;
    private String cardType;
    private String frameType;
    private Integer quantity;
    private String deckType;

    // Card details
    private Integer atk;
    private Integer def;
    private Integer level;
    private String race;
    private String attribute;
    private String description;
    private String imageUrl;
    private String imageUrlSmall;

    // Price information
    private CardPrices prices;

    // Constructors
    public DeckCardDTO() {}

    // Factory method from DeckCard entity
    public static DeckCardDTO fromEntity(DeckCard deckCard, String baseImageUrl) {
        DeckCardDTO dto = new DeckCardDTO();
        dto.setId(deckCard.getId());
        dto.setCardId(deckCard.getCardId());
        dto.setQuantity(deckCard.getQuantity());
        dto.setDeckType(deckCard.getDeckType().name());

        // Get card details if available
        Card card = deckCard.getCard();
        if (card != null) {
            dto.setCardName(card.getName());
            dto.setCardType(card.getType());
            dto.setFrameType(card.getFrameType());
            dto.setAtk(card.getAtk());
            dto.setDef(card.getDef());
            dto.setLevel(card.getLevel());
            dto.setRace(card.getRace());
            dto.setAttribute(card.getAttribute());
            dto.setDescription(card.getDescription());

            // Set image URLs
            if (baseImageUrl != null) {
                dto.setImageUrl(baseImageUrl + "/api/images/" + deckCard.getCardId() + "/regular");
                dto.setImageUrlSmall(baseImageUrl + "/api/images/" + deckCard.getCardId() + "/small");
            }

            // Parse prices from stored JSON
            if (card.getCardPrices() != null && !card.getCardPrices().isEmpty()) {
                try {
                    CardPrices prices = parsePricesFromJson(card.getCardPrices());
                    dto.setPrices(prices);
                } catch (Exception e) {
                    // Ignore parse errors
                }
            }
        }

        return dto;
    }

    /**
     * Parse CardPrices from stored JSON string
     */
    private static CardPrices parsePricesFromJson(String jsonString) {
        try {
            JsonNode priceNode = objectMapper.readTree(jsonString);
            CardPrices prices = new CardPrices();

            if (priceNode.has("cardmarket_price") && !priceNode.get("cardmarket_price").isNull()) {
                prices.setCardmarketPrice(priceNode.get("cardmarket_price").asText());
            }
            if (priceNode.has("tcgplayer_price") && !priceNode.get("tcgplayer_price").isNull()) {
                prices.setTcgplayerPrice(priceNode.get("tcgplayer_price").asText());
            }
            if (priceNode.has("ebay_price") && !priceNode.get("ebay_price").isNull()) {
                prices.setEbayPrice(priceNode.get("ebay_price").asText());
            }
            if (priceNode.has("amazon_price") && !priceNode.get("amazon_price").isNull()) {
                prices.setAmazonPrice(priceNode.get("amazon_price").asText());
            }
            if (priceNode.has("coolstuffinc_price") && !priceNode.get("coolstuffinc_price").isNull()) {
                prices.setCoolstuffincPrice(priceNode.get("coolstuffinc_price").asText());
            }

            return prices;
        } catch (Exception e) {
            return null;
        }
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getCardId() { return cardId; }
    public void setCardId(Long cardId) { this.cardId = cardId; }

    public String getCardName() { return cardName; }
    public void setCardName(String cardName) { this.cardName = cardName; }

    public String getCardType() { return cardType; }
    public void setCardType(String cardType) { this.cardType = cardType; }

    public String getFrameType() { return frameType; }
    public void setFrameType(String frameType) { this.frameType = frameType; }

    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }

    public String getDeckType() { return deckType; }
    public void setDeckType(String deckType) { this.deckType = deckType; }

    public Integer getAtk() { return atk; }
    public void setAtk(Integer atk) { this.atk = atk; }

    public Integer getDef() { return def; }
    public void setDef(Integer def) { this.def = def; }

    public Integer getLevel() { return level; }
    public void setLevel(Integer level) { this.level = level; }

    public String getRace() { return race; }
    public void setRace(String race) { this.race = race; }

    public String getAttribute() { return attribute; }
    public void setAttribute(String attribute) { this.attribute = attribute; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public String getImageUrlSmall() { return imageUrlSmall; }
    public void setImageUrlSmall(String imageUrlSmall) { this.imageUrlSmall = imageUrlSmall; }

    public CardPrices getPrices() { return prices; }
    public void setPrices(CardPrices prices) { this.prices = prices; }
}