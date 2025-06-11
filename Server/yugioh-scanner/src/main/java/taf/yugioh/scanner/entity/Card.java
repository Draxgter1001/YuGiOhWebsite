package taf.yugioh.scanner.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.LocalDateTime;

@Entity
@Table(name = "cards")
public class Card {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "card_id", unique = true, nullable = false)
    private Long cardId;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "type")
    private String type;

    @Column(name = "frame_type")
    private String frameType;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "atk")
    private Integer atk;

    @Column(name = "def")
    private Integer def;

    @Column(name = "level")
    private Integer level;

    @Column(name = "race")
    private String race;

    @Column(name = "attribute")
    private String attribute;

    // Fixed JSONB columns with proper annotations
    @Column(name = "card_sets", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private String cardSets; // Store as JSON string

    @Column(name = "card_prices", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private String cardPrices; // Store as JSON string

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // REMOVED: Foreign key relationship that was causing constraint violations
    // This allows cards to exist without images
    
    // Constructors
    public Card() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getCardId() { return cardId; }
    public void setCardId(Long cardId) { this.cardId = cardId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getFrameType() { return frameType; }
    public void setFrameType(String frameType) { this.frameType = frameType; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

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

    public String getCardSets() { return cardSets; }
    public void setCardSets(String cardSets) { this.cardSets = cardSets; }

    public String getCardPrices() { return cardPrices; }
    public void setCardPrices(String cardPrices) { this.cardPrices = cardPrices; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}