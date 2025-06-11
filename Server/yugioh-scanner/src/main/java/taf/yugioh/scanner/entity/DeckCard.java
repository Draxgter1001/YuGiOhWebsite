package taf.yugioh.scanner.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "deck_cards")
public class DeckCard {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "deck_id", nullable = false)
    private UserDeck deck;

    @Column(name = "card_id", nullable = false)
    private Long cardId; // Yu-Gi-Oh card ID

    @Column(name = "quantity")
    private Integer quantity = 1;

    @Column(name = "is_main_deck")
    private Boolean isMainDeck = true;

    @Enumerated(EnumType.STRING)
    @Column(name = "deck_type")
    private DeckType deckType = DeckType.MAIN;

    @Column(name = "added_at")
    private LocalDateTime addedAt;

    // Many-to-one relationship with Card entity
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "card_id", referencedColumnName = "card_id", insertable = false, updatable = false)
    private Card card;

    public enum DeckType {
        MAIN, EXTRA, SIDE
    }

    // Constructors
    public DeckCard() {
        this.addedAt = LocalDateTime.now();
    }

    public DeckCard(UserDeck deck, Long cardId, Integer quantity, DeckType deckType) {
        this();
        this.deck = deck;
        this.cardId = cardId;
        this.quantity = quantity;
        this.deckType = deckType;
        this.isMainDeck = (deckType == DeckType.MAIN);
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public UserDeck getDeck() { return deck; }
    public void setDeck(UserDeck deck) { this.deck = deck; }

    public Long getCardId() { return cardId; }
    public void setCardId(Long cardId) { this.cardId = cardId; }

    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }

    public Boolean getIsMainDeck() { return isMainDeck; }
    public void setIsMainDeck(Boolean isMainDeck) { this.isMainDeck = isMainDeck; }

    public DeckType getDeckType() { return deckType; }
    public void setDeckType(DeckType deckType) { 
        this.deckType = deckType; 
        this.isMainDeck = (deckType == DeckType.MAIN);
    }

    public LocalDateTime getAddedAt() { return addedAt; }
    public void setAddedAt(LocalDateTime addedAt) { this.addedAt = addedAt; }

    public Card getCard() { return card; }
    public void setCard(Card card) { this.card = card; }
}