package taf.yugioh.scanner.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import taf.yugioh.scanner.entity.DeckCard;

public class AddCardRequest {

    @NotNull(message = "Card ID is required")
    private Long cardId;

    @Min(value = 1, message = "Quantity must be at least 1")
    @Max(value = 3, message = "Quantity cannot exceed 3")
    private Integer quantity = 1;

    @NotNull(message = "Deck type is required")
    private DeckCard.DeckType deckType = DeckCard.DeckType.MAIN;

    // Constructors
    public AddCardRequest() {}

    public AddCardRequest(Long cardId, Integer quantity, DeckCard.DeckType deckType) {
        this.cardId = cardId;
        this.quantity = quantity;
        this.deckType = deckType;
    }

    // Getters and Setters
    public Long getCardId() { return cardId; }
    public void setCardId(Long cardId) { this.cardId = cardId; }

    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }

    public DeckCard.DeckType getDeckType() { return deckType; }
    public void setDeckType(DeckCard.DeckType deckType) { this.deckType = deckType; }
}