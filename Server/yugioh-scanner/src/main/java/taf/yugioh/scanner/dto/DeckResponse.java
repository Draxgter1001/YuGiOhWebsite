package taf.yugioh.scanner.dto;

import taf.yugioh.scanner.entity.UserDeck;
import java.time.LocalDateTime;
import java.util.List;

public class DeckResponse {

    private Long id;
    private String name;
    private String description;
    private Boolean isPublic;
    private String ownerUsername;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Card counts
    private int mainDeckCount;
    private int extraDeckCount;
    private int sideDeckCount;
    private int totalCards;

    // Validation status
    private boolean isValid;
    private List<String> validationErrors;

    // Cards (optional, loaded when needed)
    private List<DeckCardDTO> mainDeck;
    private List<DeckCardDTO> extraDeck;
    private List<DeckCardDTO> sideDeck;

    // Constructors
    public DeckResponse() {}

    // Factory method from entity (basic info only)
    public static DeckResponse fromEntity(UserDeck deck) {
        DeckResponse response = new DeckResponse();
        response.setId(deck.getId());
        response.setName(deck.getName());
        response.setDescription(deck.getDescription());
        response.setIsPublic(deck.getIsPublic());
        response.setOwnerUsername(deck.getUser().getUsername());
        response.setCreatedAt(deck.getCreatedAt());
        response.setUpdatedAt(deck.getUpdatedAt());
        return response;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Boolean getIsPublic() { return isPublic; }
    public void setIsPublic(Boolean isPublic) { this.isPublic = isPublic; }

    public String getOwnerUsername() { return ownerUsername; }
    public void setOwnerUsername(String ownerUsername) { this.ownerUsername = ownerUsername; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public int getMainDeckCount() { return mainDeckCount; }
    public void setMainDeckCount(int mainDeckCount) { this.mainDeckCount = mainDeckCount; }

    public int getExtraDeckCount() { return extraDeckCount; }
    public void setExtraDeckCount(int extraDeckCount) { this.extraDeckCount = extraDeckCount; }

    public int getSideDeckCount() { return sideDeckCount; }
    public void setSideDeckCount(int sideDeckCount) { this.sideDeckCount = sideDeckCount; }

    public int getTotalCards() { return totalCards; }
    public void setTotalCards(int totalCards) { this.totalCards = totalCards; }

    public boolean isValid() { return isValid; }
    public void setValid(boolean valid) { isValid = valid; }

    public List<String> getValidationErrors() { return validationErrors; }
    public void setValidationErrors(List<String> validationErrors) { this.validationErrors = validationErrors; }

    public List<DeckCardDTO> getMainDeck() { return mainDeck; }
    public void setMainDeck(List<DeckCardDTO> mainDeck) { this.mainDeck = mainDeck; }

    public List<DeckCardDTO> getExtraDeck() { return extraDeck; }
    public void setExtraDeck(List<DeckCardDTO> extraDeck) { this.extraDeck = extraDeck; }

    public List<DeckCardDTO> getSideDeck() { return sideDeck; }
    public void setSideDeck(List<DeckCardDTO> sideDeck) { this.sideDeck = sideDeck; }
}